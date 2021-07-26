package scala.io.jdbc.mappers

import java.sql.{PreparedStatement, ResultSet}

// todo: 这里其实可以好好规划一下
// - wrap和unwrap for jdbc是公用的, 除了JdbcMapper外, DataType也可以用到
trait Wrapper[T] {
  def wrap(statement: PreparedStatement, parameterIndex: Int, value: T)
}

trait UnWrapper[T] {
  def unwrap(resultSet: ResultSet, columnIndex: Int): T
  def unwrap(resultSet: ResultSet, columnLabel: String): T
}
