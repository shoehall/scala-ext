package scala.io.jdbc.rich

import java.sql.{Connection, PreparedStatement, SQLException}
import scala.io.jdbc.SQL.Mode
import scala.io.jdbc.crud.{CanExecute, CanWriteValues}
import scala.io.jdbc.mappers.{JdbcMapper, Mapper}
import scala.io.jdbc.{HasDialect, SQL, SQLArg, SQLHint, SQLIdentifier}

trait RichConnection extends HasDialect with CanWithStatement {
  /**
   * 创建表格
   *
   * @param table
   * @param ddl
   * @param mode
   */
  def create(table: String, fieldNames: Seq[String], dataTypes: Seq[String], mode: String): Unit = {

    val ddl = fieldNames.zip(dataTypes).map {
      case (name, dType) =>
        s"${getDialect.quoteIdentifier(name)} $dType"
    }.mkString(",")

    create(table, ddl, mode)
  }

  def create(table: String, ddl: String, mode: String): Unit = {
    mode match {
      case CanExecute.Mode.ERRORIfExists =>
        if (exists(table))
          throw new Exception(s"table $table already exists")
      case CanExecute.Mode.OVERWRITE =>
        drop(table)
      case _ =>
    }

    val sql = sql"create table if not exists ${SQLIdentifier(table)}(${SQLArg(ddl)});"

    withStatement {
      statement =>
        try {
          statement.execute(sql)
        } catch {
          case e: Throwable =>
            throw new SQLException(s"创建表格失败, create sql: $sql", e)
        }
    }
    getLogWriter.println(s"创建表格${table}成功")
  }


  def create[T](table: String, mode: String)(implicit mapper: JdbcMapper[T]): Unit =
    create(table, Mapper.getDDLOf[T](getDialect)(mapper), mode)

  def create[T](table: String)(implicit mapper: JdbcMapper[T]): Unit =
    create[T](table: String, CanExecute.Mode.ERRORIfExists)(mapper)
  /**
   * drop table if exists
   *
   * @param table table name
   */
  def drop(table: String): Unit = withStatement {
    statement => try {
      getLogWriter.println(s"drop table if exists $table")
      statement.execute(sql"drop table if exists ${getDialect.quoteIdentifier(table)};".toString(SQL.Mode.Noop))
    } catch {
      case e: Throwable =>
        throw new SQLException(s"删除表格${table}失败", e)
    }
      getLogWriter.println(s"删除表格${table}成功")
  }


  /**
   * table exists
   *
   * @param table
   * @return
   */
  def exists(table: String): Boolean = {
    val sql = getDialect.getTableExistsQuery(table)
    try {
      withStatement { statement => statement.execute(sql) }
      true
    } catch {
      case _: Throwable =>
        false
    }
  }

  def write[T: JdbcMapper](table: String, fieldNames: String, values: TraversableOnce[T]): CanWriteValues[T] =
    throw new UnsupportedOperationException()

  def write[T: JdbcMapper](table: String, schema: Seq[(String, String)], values: TraversableOnce[T]): CanWriteValues[T] =
    throw new UnsupportedOperationException()


  def write[T: JdbcMapper](table: String, values: TraversableOnce[T], batch: Int): CanWriteValues[T] = {
    insertInto[T](table, CanExecute.Mode.APPEND).setBatchSize(batch).values(values)
  }


  def insertInto[T](table: String, fieldNames: Seq[String], dataTypes: Seq[String], mode: String): CanWriteValues[T] = {

    val preparedSQL = sql"insert into ${SQLIdentifier(table)} " +
      sql"(${fieldNames.map(SQLIdentifier.apply):_*}) values " +
      s"(${fieldNames.map(_ => "?").mkString(",")})"

    println(preparedSQL)

    val conn = getConnection
    val statement = conn.prepareStatement(preparedSQL.prepare)

    create(table, fieldNames, dataTypes, mode)

    new CanWriteValues[T] {
      override def statement: PreparedStatement = statement
    }
  }

  def insertInto[T](table: String, fieldNames: Seq[String], dataTypes: Seq[String]): CanWriteValues[T] =
    insertInto[T](table, fieldNames, dataTypes, CanExecute.Mode.APPEND)

  def insertInto[T](table: String, mode: String)(implicit w: JdbcMapper[T]): CanWriteValues[T] = {
    val fields = Mapper.getStructOf[T]
    val fieldNames = fields.map(_.fieldName)
    val dataTypes = fields.map(struct => getDialect.getSQLTypeName(struct.wrapper.jdbcType))
    insertInto[T](table, fieldNames, dataTypes, mode)
  }

  def insertInto[T](table: String)(implicit w: JdbcMapper[T]): CanWriteValues[T] =
    insertInto[T](table, CanExecute.Mode.APPEND)(w)



}

object RichConnection {
  implicit def richConnection(connection: Connection): RichConnection = new RichConnection {
    override def getConnection: Connection = connection
  }
}



