package scala.io.jdbc

import java.io.PrintWriter
import java.sql._
import java.util.Random
import java.util.logging.Logger
import javax.sql.DataSource
import scala.beans.BeanProperty
import scala.collection.frame.{Row, RowWithSchema, Schema}
import scala.io.jdbc.crud.{CanExecuteQuery, ExecutorBuilder}
import scala.io.jdbc.mappers.JdbcMapper
import scala.io.jdbc.rich.{CanWithConnection, RichConnection}
import scala.util.Failure

/**
 * data source of JDBC
 *
 * @param url      the jdbc url
 * @param username user name
 * @param password password
 * @param driver   driver
 * @param dialect  dialect
 */
class JdbcSource(
                  val url: String,
                  val username: String,
                  val password: String,
                  @BeanProperty var driver: String = null
                ) extends DataSource with HasDialect with CanExecuteQuery with RichConnection with ExecutorBuilder {
  if (getDialect == NoopDialect)
    setDialect(JdbcDialect(url))

  if (driver == null)
    driver = getDialect.driver

  private def loadDriver(): Unit = {
    getLogWriter.println("start load driver")
    try {
      JdbcSource.loadDriver(driver)
    } catch {
      case e: SQLException =>
        getLogWriter.println(s"failed to load driver named '$driver': ${e.getMessage}")
        getLogWriter.flush()
        throw e
    }
  }

  override def getConnection(username: String, password: String): Connection = {
    loadDriver()
    getLogWriter.println("start build connection")
    try {
      DriverManager.getConnection(url, username, password)
    } catch {
      case e: Throwable =>
        getLogWriter.println(s"failed to build connection for $url: ${e.getMessage}")
        getLogWriter.flush()
        throw e
    }
  }

  override def getConnection: Connection = getConnection(username, password)

  override def executeQuery(sql: SQL): ResultSet =
    getConnection.createStatement().executeQuery(sql)

  def execute(sql: SQL): Boolean = {
    withConnection {
      conn =>
        val statement = conn.createStatement()
        statement.execute(sql.sql)
    }
  }


  override def readAs[T: JdbcMapper](sql: SQL): Iterator[T] = {
    val connection = getConnection
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(sql)

    resultSet.toDataSetIterator(implicitly[JdbcMapper[T]]).setHook {
      resultSet.close()
      statement.close()
      connection.close()
    }
  }

  def rows(schemaQuerySQL: SQL, querySQL: SQL): IteratorWithHook[Row] = {
    val connection = getConnection
    val statement = connection.createStatement()
    val rsmd = try {
      statement.executeQuery(schemaQuerySQL.sql)
    } catch {
      case e: Throwable =>
        throw new SQLException(s"failed to load metadata, ${schemaQuerySQL.sql}", e)
    }
    val schema = getDialect.getSchema(rsmd.getMetaData)
    val getters = utils.makeGetter(schema)
    val indices = schema.fields.indices.toArray

    utils.read(statement, querySQL).makeIterator[Row] {
      rs => RowWithSchema(indices.map { i => getters(i)(rs) }, schema)
    }.setHook {
      () => {
        println("执行hook")
        statement.close()
        connection.close()
      }
    }
  }

  def rows(querySQL: SQL): Iterator[Row] = {
    var tmp = "tmp_view"
    var selectClause = querySQL.sql
    while (selectClause.endsWith(";")) {
      selectClause = selectClause.dropRight(1)
    }
    while (selectClause contains tmp) {
      tmp += "_suffix"
    }

    rows(s"select * from ($selectClause) $tmp where 1=0;", querySQL)
  }

  def readTable(table: String): IteratorWithHook[Row] = rows(s"select * from $table where 1=0;", s"select * from $table;")

  override def unwrap[T](iface: Class[T]): T = ???

  override def isWrapperFor(iface: Class[_]): Boolean = ???

  override def getLogWriter: PrintWriter = new PrintWriter(System.out, true)

  override def setLogWriter(out: PrintWriter): Unit = ???

  override def setLoginTimeout(seconds: Int): Unit = ???

  override def getLoginTimeout: Int = ???

  override def getParentLogger: Logger = ???

  override def connectionIsConstant: Boolean = true
}


object JdbcSource {
  @throws[SQLException]
  def loadDriver(driver: String): Unit = {
    val res = if (driver != null) scala.util.Try(Class.forName(driver)) else Failure(new NullPointerException)
    if (res.isFailure)
      try {
        DriverManager.getDriver(driver)
      } catch {
        case e: Throwable =>
          throw new SQLException(s"No suitable driver for $driver", e)
      }
  }
}