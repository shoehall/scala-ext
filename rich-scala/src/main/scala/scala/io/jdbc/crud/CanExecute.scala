package scala.io.jdbc.crud

import java.sql.{Connection, PreparedStatement, Statement}
import scala.io.jdbc.HasDialect
import scala.io.jdbc.mappers.JdbcMapper


trait CanExecuteBatch[T <: Statement] {
  def statement: T

  private var batchSize: Int = 1

  def setBatchSize(size: Int): this.type = {
    this.batchSize = size
    this
  }

  def getBatchSize: Int = this.batchSize

  private var batchCursor = 0

  private def clearBatchCursor(): Unit = {
    statement.clearBatch()
    batchCursor = 0
  }

  def clear(): Unit = {
    if (batchCursor > 0)
      executeBatch()
    statement.close()
  }


  /**
   * 添加到批次执行中
   *
   * @param value 批次执行的值
   */
  def executeInBatch(doWithStatement: T => Unit): Unit = {
    doWithStatement(statement)
    batchCursor += 1
    if (batchCursor >= batchSize)
      executeBatch()
  }

  def executeBatch(): Array[Int] = {
    require(batchSize > 1, "batch size should be greater than 1, if you want to execute with batch.")
    println("execute batch")
    try {
      val rs = statement.executeBatch()
      clearBatchCursor()
      rs
    } catch {
      case e: Throwable => throw e
    }
  }


}

trait CanWriteValues[T] extends HasDialect with CanExecuteBatch[PreparedStatement] {
  def close(): Unit = {
    clear()
    statement.close()
  }

  def statement: PreparedStatement

  def setValues(value: T)(setter: (PreparedStatement, T) => Unit): this.type = {
    executeInBatch {
      statement: PreparedStatement =>
        setter(statement, value)
        statement.addBatch()
    }

    this
  }

  def setValues(value: TraversableOnce[T])(setter: (PreparedStatement, T) => Unit): this.type = {
    value.foreach {
      data =>
        setValues(data)(setter)
    }
    executeBatch()
    this
  }

  // insert into tableName values()
  def values(value: T)(implicit mapper: JdbcMapper[T]): this.type = {
    setValues(value) {
      case (statement: PreparedStatement, data: T) =>
        mapper.wrap(statement, 1, data)
    }
    this
  }

  def values(data: TraversableOnce[T])(implicit mapper: JdbcMapper[T]): this.type = {
    data.foreach {
      value =>
        mapper.wrap(statement, 1, value)
        statement.addBatch()
    }

//    setValues(data)((statement: PreparedStatement, data: T) => mapper.wrap(statement, 1, data))
    this
  }

}


object CanExecute {

  object Mode {
    val ERRORIfExists: String = "ERROR"
    val APPEND: String = "APPEND"
    val OVERWRITE: String = "OVERWRITE"

  }

}
