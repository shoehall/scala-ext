package scala.io.jdbc

import org.scalatest.FunSuite

import java.sql.PreparedStatement
import scala.io.{Bike, User}
import scala.io.jdbc.SQL.Mode

case class Person(name: String, age: Int, weight: Double, bike: Bike)

class SQLSuite extends FunSuite {
  test("sql hint") {
    val sql1 = sql"${0}"
    assert(sql1.parts.length == 2)
    val sql2 = sql"select *, "
    assert(sql2.parts.length == 1 && sql2.args.isEmpty)

    val sql3 = sql"select *,${2} as ${"src"} from user;"
    sql3.args.foreach {
      arg =>
        println(arg.getClass.getName, arg)
    }

    val sql4 = sql"select *,${2} as ${SQLIdentifier("src")} from user;"
    sql4.args.foreach {
      arg =>
        println(arg.getClass.getName, arg)
    }

  }


  test("dialect") {
    /*
    有三种方式:
    - 通过隐式转换引入JdbcDialect
    - 通过SQL类型的setDialect
    - 通过字符串前缀
     */
    implicit def mysqlDialect: JdbcDialect = MySQLDialect

    assert(sql"select * from table".getDialect == MySQLDialect)

    assert(sql"select * from user".setDialect(OracleDialect).getDialect == OracleDialect)
    assert(postgresql"select * from table".getDialect == PostgresDialect)
  }


  test("to sql expression and prepared sql") {
    val sql1: SQL = SQL"SELECT *, ${0}, ${1}, ${2} FROM TABLE1"
    assert(sql1.toString == s"""SELECT *, ${0}, ${1}, ${2} FROM TABLE1""")
    val sql2: SQL = "select * from table1"
    assert(sql2.toString == """select * from table1""")
    val sql3 = sql"select * from table1"
    assert(sql3.toString == """select * from table1""")

    /*
    将SQL语句中的参数根据其类型自动嵌入到SQL语句中
     */
    // Int
    val mm1: SQL = sql"select *, ${0} as source from table;"

    // null
    val mm2: SQL = sql"select *, ${None.asInstanceOf[Option[Int]]} as source from table;"
    val mm21 = sql"select *, ${null.asInstanceOf[Int]} as source from table;"
    assert(mm21.toString == """select *, NULL as source from table;""")
    assert(mm2.toString == """select *, NULL as source from table;""")

    // val mm3: SQL = sql"select *, ${None} as source from table;",  // not compile, need type
    val value3 = Some(3)
    val value4 = if (1 == 0) Some(3) else None
    val mm3 = sql"select *, $value3 from table3;"
    val mm4 = sql"select *, $value4 from table4;"

    assert(mm1.toString == """select *, 0 as source from table;""")
    assert(mm2.toString == """select *, NULL as source from table;""")
    assert(mm3.toString == """select *, 3 from table3;""")
    assert(mm4.toString == """select *, NULL from table4;""")

    // String
    val string1: SQL = sql"select *, ${"0"} as source from table;"
    val string2 = sql"select *, ${null.asInstanceOf[String]} as source from table;"

    assert(string1.toString(Mode.Wrapped) == """select *, '0' as source from table;""")
    assert(string2.toString(Mode.Wrapped) == """select *, NULL as source from table;""")

  }

  test("to sql expression: Struct type") {
    val user = new User("张三", 31, 76.5)
    val table = SQLIdentifier("user")

    val sql1 = sql"insert into $table values($user);"
    assert(sql1.toString() == """insert into "user" values('张三',31,76.5);""")
    assert(sql1.prepare == """insert into "user" values(?,?,?);""")

    val sql2 = sql"insert into $table values(${("李四", 32, 72.8)});"
    assert(sql2.toString() == """insert into "user" values('李四',32,72.8);""")
    assert(sql2.prepare == """insert into "user" values(?,?,?);""")

    val bike = Bike("Giant", user)
    val sql3 = sql"${(0, bike, user)}"
    assert(sql3.toString() == "0,'Giant','张三',31,76.5,'张三',31,76.5")
    assert(sql3.prepare == "?,?,?,?,?,?,?,?")
  }


  test("multiple args") {
    /*
    doc:
    - 支持手动通过SeqSQLArg形式引入多个参数
    - 支持通过隐式转换扩展支持的类型, 定义一个JdbcWrapper[T]重新其中的wrap(value: T, dialect: JdbcDialect)和wrap(statement: PreparedStatement, index: Int, value: T)以及position函数
    - 支持Seq[T]以_*的形式引入多个参数，会自动转为SQLValue
    - 单个Seq[T]如果整个作为参数，会被视为单个参数而不是容器, 因此toPrepare时自会构造一个占位符?
    - 支持以case class、Product引入多个参数，会自动转给SQLValue
     */
    val seq = Seq(1, 2, 3)
    val seqString = Seq("张三", "李四", "王五")

    // 作为数组处理
    val sql1 = sql"select *, ${0} from user where id in $seqString;"

    assert(sql1.args(1).value.get == seqString)
    assert(sql1.prepare == """select *, ? from user where id in ?;""")
    assert(sql1.toString == """select *, 0 from user where id in ('张三','李四','王五');""")

    // 逐个元素处理, 有一条限制就是单个sql只能格式化一个参数
    val sql2 = sql"select *, ${0} from user" + sql" where id in ${seqString: _*};"
    assert(sql2.toString() == "select *, 0 from user where id in '张三','李四','王五';")
    assert(sql2.prepare == "select *, ? from user where id in ?,?,?;")

    val sql3 = sql"${seq: _*}"
    assert(sql3.args.length == 1)
    assert(sql3.parts.length == 2)

    val fieldNames = Seq("name", "age", "weight").map(SQLIdentifier.apply)
    val user = new User("张三", 31, 76.5)
    val table = SQLIdentifier("user")

    val sql4 = sql"insert into $table " + sql"(${fieldNames: _*})" + sql" values(${user});"
    assert(sql4.toString() == """insert into "user" ("name","age","weight") values('张三',31,76.5);""")
    assert(sql4.prepare == """insert into "user" ("name","age","weight") values(?,?,?);""")

    val bike = Bike("Giant", user)
    val sql5 = sql"${(0, bike, user)}"
    assert(sql5.toString() == "0,'Giant','张三',31,76.5,'张三',31,76.5")
    assert(sql5.prepare == "?,?,?,?,?,?,?,?")

/*
    class Person {
      var name: String = _
      var age: Int = _
      var weight: Double = _
    }

    implicit def wrapper4Person: JdbcMapper[Person] = new JdbcWrapper[Person] {
      override def wrap(value: Person, dialect: JdbcDialect): String = {
        s"'${value.name}', ${value.age}, ${value.weight}"
      }

      override def wrap(statement: PreparedStatement, index: Int, value: Person): Unit = throw new UnsupportedOperationException

      override def position: Int = 3
    }

    val person = new Person()
    person.name = "李四"
    person.age = 21
    person.weight = 63.5

    val sql5 = sql"insert into $table values($person);"
    assert(sql5.toString == """SQL"insert into "user" values('李四', 21, 63.5);"""")
    assert(sql5.prepare == """insert into "user" values(?,?,?);""")
*/

  }


  test("to string: value of null") {
    val sql1 = sql"insert into table1 values(${None.asInstanceOf[Option[Int]]}, ${null.asInstanceOf[String]})"
    assert(sql1.toString() == "insert into table1 values(NULL, NULL)")


    val tuple2 = (None.asInstanceOf[Option[Int]], "张三", None.asInstanceOf[Option[Double]])
    val sql2 = sql"insert into table1 values($tuple2)"
    println(sql2)
    val p: User = null
    val sql4 = sql"$p"
    assert(sql4.toString() == "NULL,NULL,NULL")

    val values = Seq("张三", null, null)
    val sql5 = sql"${values:_*}"
    assert(sql5.toString() == "'张三',NULL,NULL")




  }


}
