package scala.io.jdbc.rich

import java.io.PrintWriter
import java.sql.{Connection, PreparedStatement, SQLException, Statement}
import scala.io.jdbc.{HasDialect, HasLog, SQL, SQLArg, SQLHint, SQLIdentifier}
import scala.io.jdbc.crud.{CanExecute, CanWriteValues}
import scala.io.jdbc.mappers.{JdbcMapper, Mapper}
import scala.reflect.ClassTag


trait CanWithConnection extends HasLog {
  def getConnection: Connection

  /**
   * 执行connection操作
   *
   * @param f 操作函数
   * @tparam T 返回类型
   * @return
   */
  def withConnection[T: ClassTag](f: Connection => T): T = {
    val connection = getConnection
    getLogWriter.println("创建connection")
    try
      f(connection)
    catch {
      case e: Throwable =>
        getLogWriter.println(s"执行Connection操作失败: ${e.getMessage}")
        throw e
    } finally {
      getLogWriter.println("关闭connection")
      connection.close()
    }
  }

}

trait CanWithStatement extends CanWithConnection {
  /**
   * 执行statement操作
   *
   * @param f 操作函数
   * @tparam T 返回类型
   * @return
   */
  def withStatement[T: ClassTag](f: Statement => T): T = withConnection {
    connection: Connection =>
      try {
        val statement = connection.createStatement()
        getLogWriter.println("创建statement成功")
        f(statement)
      } catch {
        case e: Throwable =>
          getLogWriter.println(s"执行Statement操作失败: ${e.getMessage}")
          throw e
      }
  }


  def withPreparedStatement[T: ClassTag](sql: String)(f: PreparedStatement => T): T = withConnection {
    connection: Connection =>
      try {
        val statement = connection.prepareStatement(sql)
        getLogWriter.println("创建prepareStatement成功")
        f(statement)
      } catch {
        case e: Throwable =>
          getLogWriter.println(s"执行Statement操作失败: ${e.getMessage}")
          throw e
      }
  }

}

