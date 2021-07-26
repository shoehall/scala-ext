package scala.io.jdbc.rich

import java.sql.{Connection, PreparedStatement, ResultSet, SQLException, Statement}
import scala.collection.mutable.ArrayBuffer
import scala.io.jdbc.SQL
import scala.io.jdbc.crud.{CanExecuteBatch, CanExecuteQuery}
import scala.io.jdbc.mappers.JdbcMapper

trait RichStatement extends CanExecuteQuery with CanExecuteBatch[Statement] {
  def statement: Statement

  // execute
  def execute(sql: SQL): Unit = statement.execute(sql.sql)

  override def executeQuery(sql: SQL): ResultSet = try {
    statement.executeQuery(sql.sql)
  } catch {
    case e: Throwable =>
      throw new SQLException(s"failed to execute query, sql: ${sql.sql}")
  }


}

object RichStatement {
  implicit def richStatement(statement0: Statement): RichStatement = new RichStatement {
    override def statement: Statement = statement0
  }
}

trait RichPreparedStatement extends CanExecuteQuery with CanExecuteBatch[PreparedStatement] {

}

object RichPreparedStatement {
  implicit def richPreparedStatement(statement0: PreparedStatement) = new RichPreparedStatement {
    override def statement: PreparedStatement = statement0

    override def executeQuery(sql: SQL): ResultSet = statement.executeQuery(sql.sql)
  }
}




