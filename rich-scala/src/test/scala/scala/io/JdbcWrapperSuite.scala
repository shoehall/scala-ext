/*
package scala.io

import org.scalatest.FunSuite
import shapeless.{::, Generic, HList, HNil, Lazy, nonGeneric}

import java.sql.{PreparedStatement, ResultSet, SQLType}
import scala.io.jdbc.{JdbcDialect, JdbcType, JdbcWrapper, NoopDialect}
import scala.knickknack.spandas.common.Schema

class JdbcWrapperSuite extends FunSuite {
  test("wrap2string") {
    /*
    支持常用的原子类型
    支持case class或构造参数全为val的class
    支持tuple
    支持seq[T], T需要为支持类型中的一个, 否则会报编译错误.
     */
    def wrap2string[T: JdbcWrapper](value: T): String = {
      implicitly[JdbcWrapper[T]].wrap(value, NoopDialect)
    }

    // atomic
    assert(wrap2string("fff") == "'fff'")
    assert(wrap2string(1.5) == "1.5")
    // case class or like case class
    assert(wrap2string(new User("张三", 31, 78.5)) == "'张三', 31, 78.5")
    // tuple
    assert(wrap2string(("张三", 31, 78.5)) == "'张三', 31, 78.5")
    // seq
    assert(wrap2string(Seq("1", "2", "3")) == "'1','2','3'")
    assert(wrap2string(Seq(1, 2, 3)) == "1,2,3")
    assert(wrap2string(Seq(new User("张三", 31, 78.5), new User("李四", 32, 72.3))) == "'张三', 31, 78.5,'李四', 32, 72.3")
    // todo: 目前是视为容器的处理方式
    assert(wrap2string(Seq(Seq(1, 2), Seq(3, 4))) == "1,2,3,4")
  }

  test("wrap2preparedStatement") {
    val statement: PreparedStatement4Test = new PreparedStatement4Test()

    def wrap2Statement[T: JdbcWrapper](value: T, index: Int = 1): Unit = {
      implicitly[JdbcWrapper[T]].wrap(statement, index, value)
    }

    // atomic
    wrap2Statement("fff")
    wrap2Statement(1.5, 2)
    wrap2Statement(new User("张三", 31, 78.5), 3)
    wrap2Statement(("李四", 32, 72.3), 6)

    val map = scala.collection.mutable.Map(
      8 -> ("Double", 72.3),
      2 -> ("Double", 1.5),
      5 -> ("Double", 78.5),
      4 -> ("Int", 31),
      7 -> ("Int", 32),
      1 -> ("String", "fff"),
      3 -> ("String", "张三"),
      6 -> ("String", "李四")
    )

    assert(statement.getProperties == map)
  }

  test("unwrap from result") {
    def unwrapAs[T: JdbcWrapper](resultSet: ResultSet, index: Int): T = {
      if (index <= 0) {
        implicitly[JdbcWrapper[T]].unwrap(resultSet)
      } else
        implicitly[JdbcWrapper[T]].unwrap(resultSet, index)
    }

    val resultSet = new ResultSet4Test(Seq(Map(1 -> "张三", 2 -> 32, 3 -> 66.5)).iterator)

    assert(unwrapAs[String](resultSet, 1) == "张三")
    val user = unwrapAs[User](resultSet, 0)
    assert(user == new User("张三", 32, 66.5))
    assert(unwrapAs[(String, Int, Double)](resultSet, 1) == ("张三", 32, 66.5))
    // 类型不敏感: 这是由ResultSet的get方法决定的
    assert(unwrapAs[(String, Double, Int)](resultSet, 1) == ("张三", 32.0, 66))

  }


  test("create code for primitive wrapper") {
    def code(typeName: String) = {
      s"""    implicit def jdbcWrapper4$typeName: JdbcWrapper[$typeName] = create4Primitive[$typeName] (
         |      (statement: PreparedStatement, index: Int, value: $typeName) => statement.set$typeName(index, value),
         |      (resultSet: ResultSet, index: Int) => resultSet.get$typeName(index)
         |    )""".stripMargin
    }

    for (each <- Seq("Boolean", "Byte", "Short", "Int", "Long", "Float", "Double")) {
      println()
      println(code(each))
    }

  }

}
*/
