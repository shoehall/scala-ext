package scala.io.jdbc.mappers

import org.scalatest.FunSuite

import scala.io.User

class MapperSuite extends FunSuite{
  test("equals") {
    assert(implicitly[JdbcMapper[String]] == implicitly[JdbcMapper[String]])
    assert(implicitly[JdbcMapper[Double]] == implicitly[JdbcMapper[Double]])
    // todo: not equal, 看下有无必要解决
    println(implicitly[JdbcMapper[User]] == implicitly[JdbcMapper[User]])
  }

}
