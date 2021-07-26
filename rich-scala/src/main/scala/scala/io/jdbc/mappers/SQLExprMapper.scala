package scala.io.jdbc.mappers

import java.sql.{Date, Time, Timestamp}
import scala.io.jdbc.JdbcDialect

/**
 * 泛型T在sql表达式中wrap的type class
 *
 * @tparam T 泛型
 */
trait SQLExprMapper[T] extends Mapper[T] {
  /**
   * 根据类型T将其转为SQL表达式中需要的字符串
   *
   * @param value   value of type T, it is not null
   * @param dialect the dialect of the database
   * @return
   */
  def wrap(value: T, dialect: JdbcDialect): String = {
    if(value == null) "NULL" else s"$value"
  }

}
