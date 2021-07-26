package scala.io.jdbc.crud

import java.sql.{Connection, PreparedStatement, ResultSet}
import scala.io.jdbc.SQL
abstract class Executor[T] {
  protected def connection: Option[Connection] = None

  val statement: PreparedStatement
  val setter: (T, PreparedStatement) => Unit
  private var batch: Option[Int] = None
  private var cursor: Int = 0

  def setBatch(batch: Int): this.type = {
    if (this.batch.nonEmpty)
      throw new UnsupportedOperationException(s"batch already been defined: ${this.batch.get}")
    else
      this.batch = Some(batch)
    this
  }

  def getBatch: Int = this.batch.getOrElse(1)

  def execute(value: T): Unit = {
    setter(value, statement)
    statement.addBatch()
    cursor += 1
    if (cursor >= getBatch) {
      statement.executeBatch()
      cursor = 0
    }
  }

  def close(): Unit = {
    if (cursor > 0) {
      statement.executeBatch()
      cursor = 0
    }
    statement.close()
    if (connection.nonEmpty)
      connection.get.close()
  }
}

trait ExecutorBuilder {
  def getConnection: Connection

  /**
   * [[getConnection]]返回的结果是一个常量, 如果是则不会在结尾close, 否则会close: 像[[JdbcSource]]返回的不是常量, 而RichConnection则是常量
   * @return
   */
  def connectionIsConstant: Boolean

  def buildExecutor[T](sql: SQL)(setter0: (T, PreparedStatement) => Unit): Executor[T] = {
    val conn = getConnection
    val st = conn.prepareStatement(sql.sql)

    if (connectionIsConstant)
      new Executor[T] {
        override protected def connection: Option[Connection] = Some(conn)

        override val statement: PreparedStatement = st
        override val setter: (T, PreparedStatement) => Unit = setter0
      }
    else
      new Executor[T] {
        override val statement: PreparedStatement = st
        override val setter: (T, PreparedStatement) => Unit = setter0
      }
  }

}


trait Updater[T] extends Executor[T] {
  def update(value: T): Unit = execute(value)
}

trait Inserter[T] extends Executor[T] {
  def insert(value: T): Unit = execute(value)
}
