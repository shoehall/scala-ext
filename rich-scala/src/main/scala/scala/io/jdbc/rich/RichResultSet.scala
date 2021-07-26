package scala.io.jdbc.rich

import java.sql.ResultSet
import scala.io.jdbc.{DataSetIterator, IteratorWithHook, ResultSetIterator}
import scala.io.jdbc.mappers.JdbcMapper

/**
 * ResultSet的Rich方法
 * ----
 * - 可以将ResultSet基于一个Getter方法转为Iterator
 * - toIterator 三种模式: column label, column index, all column
 * - rows 将结果转为结构化的Row, 更方便scala处理
 * - head 将当前ResultSet转为Row
 */
trait RichResultSet {
  def resultSet: ResultSet

  def makeIterator[T](getter: ResultSet => T): IteratorWithHook[T] = new IteratorWithHook[T] {
    override def hasNextImpl: Boolean = resultSet.next()

    override def next(): T = getter(resultSet)
  }

  def toIterator[T: JdbcMapper]: Iterator[T] = makeIterator {
    resultSet: ResultSet => implicitly[JdbcMapper[T]].unwrap(resultSet)
  }

  def toResultSetIterator = new ResultSetIterator(resultSet)

  def toDataSetIterator[T: JdbcMapper]: DataSetIterator[T] = new DataSetIterator[T](resultSet)

}
