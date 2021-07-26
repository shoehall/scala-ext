package scala.io.jdbc.crud

import java.sql.ResultSet
import scala.io.jdbc.{HasDialect, SQL, richResultSet}
import scala.io.jdbc.mappers.JdbcMapper

trait CanExecuteQuery extends HasDialect {
  // 扩展executeQuery支持SQL对象
  def executeQuery(sql: SQL): ResultSet

  def query(sql: SQL): ResultSet = executeQuery(sql)

  /**
   * read value as a [[ResultSet]]
   *
   * @param sql sql
   * @return
   */
  def read(sql: SQL): ResultSet = executeQuery(sql)

  /**
   * read value and unwrap values from [[ResultSet]] as an [[Iterator]] with element type T
   *
   * @param sql sql
   * @tparam T type T
   * @return
   */
  def readAs[T](sql: SQL)(implicit ev: JdbcMapper[T]): Iterator[T] =
    read(sql).toDataSetIterator(ev)

  def readAndGetAs[T](sql: SQL)(resultSet: ResultSet => T): Iterator[T] = {
    read(sql).makeIterator[T](resultSet)
  }
}
