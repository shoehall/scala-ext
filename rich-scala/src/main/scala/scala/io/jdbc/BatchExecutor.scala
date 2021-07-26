//package scala.io.jdbc
//
//import java.sql.{Connection, PreparedStatement, Statement}
//
//class Batch[T: JdbcWrapper](connection: Connection, prepare: SQL, size: Int) {
//  def compile(): BatchExecutor = {
//    val wrapper = implicitly[JdbcWrapper[T]]
//    val preparedStatement = connection.prepareStatement(prepare.toString)
//
//    new BatchExecutor {
//      override def statement: PreparedStatement = preparedStatement
//
//      override def batchSize: Int = size
//    }
//  }
//}
//
//object BatchExecutor {
//  def apply(size: Int): BatchExecutor =
//    new BatchExecutor {
//      override def statement: PreparedStatement = null
//
//      override def batchSize: Int = size
//    }
//
//  def apply(preparedStatement: PreparedStatement, size: Int): BatchExecutor =
//    new BatchExecutor {
//      override def statement: PreparedStatement = preparedStatement
//
//      override def batchSize: Int = size
//    }
//
//  def apply(connection: Connection, sql: String, size: Int): BatchExecutor =
//    apply(connection.prepareStatement(sql), size)
//
//  def apply(source: JdbcSource, sql: String, size: Int): BatchExecutor =
//    apply(source.getConnection, sql, size)
//
//}
//
//
//trait BatchExecutor {
//  def statement: PreparedStatement
//
//  /**
//   * 添加到批次执行中
//   *
//   * @param value 批次执行的值
//   */
//  def execute(value: SQLArg[_]*): Unit = {
//    var i = 0
//    value.foreach {
//      v =>
////        v.prepare(statement, i)
//        i += 1
//    }
//  }
//
//  def batchSize: Int
//
//  private var batchCursor = 0
//
//  private def clearBatchCursor(): Unit = {
//    statement.clearBatch()
//    batchCursor = 0
//  }
//
//  def close(): Unit = {
//    if (batchCursor > 0)
//      executeBatch()
//    statement.close()
//  }
//
//
//  /**
//   * 添加到批次执行中
//   *
//   * @param value 批次执行的值
//   */
//  def addBatch(values: SQLArg[_]*): Unit = {
//    execute(values: _*)
//    batchCursor += 1
//    if (batchCursor >= batchSize)
//      executeBatch()
//  }
//
//  private[jdbc] def executeBatch(): Array[Int] = {
//    require(batchSize > 1, "batch size should be greater than 1, if you want to execute with batch.")
//    try {
//      val rs = statement.executeBatch()
//      clearBatchCursor()
//      rs
//    } catch {
//      case e: Throwable => throw e
//    } finally {
//      close()
//    }
//  }
//
//  def executeLargeBatch(): Array[Long] = {
//    require(batchSize > 1, "batch size should be greater than 1, if you want to execute with batch.")
//    try {
//      val rs = statement.executeLargeBatch()
//      clearBatchCursor()
//      rs
//    } catch {
//      case e: Throwable => throw e
//    } finally {
//      close()
//    }
//  }
//
//}
