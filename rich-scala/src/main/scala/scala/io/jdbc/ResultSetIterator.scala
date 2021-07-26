package scala.io.jdbc

import java.sql.{Connection, ResultSet, Statement}
import scala.collection.mutable.ArrayBuffer
import scala.io.jdbc.mappers.JdbcMapper

trait IteratorWithHook[T] extends Iterator[T] {
  def hasNextImpl: Boolean

  private var hasExecuteHook: Boolean = false

  override def hasNext: Boolean = if(hasNextImpl) {
    true
  } else {
    if(!hasExecuteHook) {
      hooks.foreach {
        hook =>
          hook()
      }
      hasExecuteHook = true
    }
    false
  }

  var hooks: ArrayBuffer[() => Unit] = ArrayBuffer.empty[() => Unit]

  def setHook(body: => Unit): this.type = {
    hooks.append(() => body)
    this
  }
}

class DataSetIterator[T: JdbcMapper](resultSet: ResultSet) extends IteratorWithHook[T] {
  val resultSetIterator = new ResultSetIterator(resultSet)
  val unWrapper: JdbcMapper[T] = implicitly[JdbcMapper[T]]

  override def hasNextImpl: Boolean = resultSetIterator.hasNext

  override def next(): T =
    unWrapper.unwrap(resultSetIterator.next())

}

class ResultSetIterator(resultSet: ResultSet) extends IteratorWithHook[ResultSet] {
  private var consumed: Boolean = true

  override def hasNextImpl: Boolean =
    if (consumed) {
      if (resultSet.next()) {
        consumed = false
        true
      } else
        false
    } else
      true

  override def next(): ResultSet = if (hasNext) {
    consumed = true
    resultSet
  } else
    Iterator.empty.next()

}

