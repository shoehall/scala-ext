package scala.io.jdbc

import org.scalatest.FunSuite

class JdbcDialectSuite extends FunSuite {
  test("测试JdbcDialect作为隐式参数") {
    def fun(value: String)(implicit dialect: JdbcDialect) = {
      println(value, dialect)
    }

    fun("张三")


  }

}
