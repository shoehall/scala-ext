package scala.io.jdbc.mappers

import org.scalatest.FunSuite

import java.sql.ResultSet
import scala.io.{Bike, PreparedStatement4Test, ResultSet4Test, User}

/**
 * 测试类型T在JDBC应用中的type class
 * todo: 加入一些异常测试
 */
class JdbcMapperSuite extends FunSuite {
  test("get struct of type T") {
    assert(Mapper.getStructOf[String].head == PrimitiveStruct(implicitly[JdbcMapper[String]], "_0"))
    assert(Mapper.getStructOf[User].mkString(",") == "name CLOB,age INTEGER,weight DOUBLE")
    assert(Mapper.getDDLOf[Bike] == "name CLOB,user_name CLOB,user_age INTEGER,user_weight DOUBLE")
    assert(Mapper.getDDLOf[(String, User, String, Bike, Int)] == "_1 CLOB,_2_name CLOB,_2_age INTEGER,_2_weight DOUBLE,_3 CLOB,_4_name CLOB,_4_user_name CLOB,_4_user_age INTEGER,_4_user_weight DOUBLE,_5 INTEGER")
  }

  test("wrap value of type T") {
    // wrap value T
    val statement = new PreparedStatement4Test()
    val properties = scala.collection.mutable.Map.empty[Int, (String, Any)]

    def wrapValue[T: JdbcMapper](index: Int, value: T): Unit =
      implicitly[JdbcMapper[T]].wrap(statement, index, value)

    println("-" * 20 + "wrap[String]" + "-" * 20)
    wrapValue(1, "张三")
    properties += (1 -> ("String", "张三"))
    assert(statement.getProperties == properties)

    statement.getProperties.toArray.sortBy(_._1).foreach(println)

    println("-" * 20 + "wrap[Int]" + "-" * 20)
    wrapValue(2, 31)
    properties += (2 -> ("Int", 31))

    assert(statement.getProperties == properties)
    statement.getProperties.toArray.sortBy(_._1).foreach(println)

    println("-" * 20 + "wrap[User]" + "-" * 20)
    wrapValue[User](3, new User("李四", 24, 67.8))
    properties += (3 -> ("String", "李四"))
    properties += (4 -> ("Int", 24))
    properties += (5 -> ("Double", 67.8))

    assert(statement.getProperties == properties)
    statement.getProperties.toArray.sortBy(_._1).foreach(println)

    println("-" * 20 + "wrap[(String, Bike, Double, User)]" + "-" * 20)
    wrapValue(6, ("星期日", Bike("Giant", new User("王五", 42, 72.8)), 15.0, new User("李四", 24, 67.8)))
    properties += (6 -> ("String", "星期日"))
    properties += (7 -> ("String", "Giant"))
    properties += (8 -> ("String", "王五"))
    properties += (9 -> ("Int", 42))
    properties += (10 -> ("Double", 72.8))
    properties += (11 -> ("Double", 15.0))
    properties += (12 -> ("String", "李四"))
    properties += (13 -> ("Int", 24))
    properties += (14 -> ("Double", 67.8))

    assert(statement.getProperties == properties)
    statement.getProperties.toArray.sortBy(_._1).foreach(println)

  }

  test("unwrap value of type T") {
    // unwrap value as T
    def unwrapValue[T: JdbcMapper](resultSet: ResultSet, index: Int): T =
      implicitly[JdbcMapper[T]].unwrap(resultSet, index)

    val content = Map(
      (1, "星期日"),
      (2, "Giant"),
      (3, "王五"),
      (4, 42),
      (5, 72.8),
      (6, 15.0),
      (7, "李四"),
      (8, 24),
      (9, 67.8)
    )
    val resultSet = new ResultSet4Test(Array(content, content).iterator)

    println("-" * 20 + "unwrapAs[(String, Bike, Double, User)]" + "-" * 20)
    resultSet.next()
    val value1 = unwrapValue[(String, Bike, Double, User)](resultSet, 1)
    println(value1)
    assert(value1 == ("星期日", Bike("Giant", new User("王五", 42, 72.8)), 15.0, new User("李四", 24, 67.8)))

    println("-" * 80)
    val value2 = unwrapValue[(Double, Double, String)](resultSet, 5)
    println(value2)
    assert(value2 == (72.8, 15.0, "李四"))
  }


  test("create code for jdbc wrapper") {
    def code(typeName: String, sqlType: String) = {
      s"""  implicit val jdbcMapper4$typeName: JdbcMapper[$typeName] = new JdbcMapper[$typeName] {
         |    override def jdbcType: JdbcType = JdbcType($sqlType)
         |
         |    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: $typeName): Unit =
         |      statement.set$typeName(parameterIndex, value)
         |
         |    override def unwrap(resultSet: ResultSet, columnIndex: Int): $typeName =
         |      resultSet.get$typeName(columnIndex)
         |
         |    override def unwrap(resultSet: ResultSet, columnLabel: String): $typeName =
         |      resultSet.get$typeName(columnLabel)
         |  }""".stripMargin
    }

    for ((dType, sqlType) <- Seq(
      ("Boolean", "BOOLEAN"),
      ("Byte", "TINYINT"),
      ("Short", "SMALLINT"),
      ("Int", "INTEGER"),
      ("Long", "BIGINT"),
      ("Float", "FLOAT"),
      ("Double", "DOUBLE"),
      ("String", "CLOB"),
      ("Date", "DATE"),
      ("Time", "TIME"),
      ("Timestamp", "TIMESTAMP")
    )) {
      println(code(dType, sqlType) + "\n")
    }

    /*
    case IntegerType => Option(JdbcType("INTEGER", java.sql.Types.INTEGER))
case LongType => Option(JdbcType("BIGINT", java.sql.Types.BIGINT))
case DoubleType => Option(JdbcType("DOUBLE PRECISION", java.sql.Types.DOUBLE))
case FloatType => Option(JdbcType("REAL", java.sql.Types.FLOAT))
case ShortType => Option(JdbcType("INTEGER", java.sql.Types.SMALLINT))
case ByteType => Option(JdbcType("BYTE", java.sql.Types.TINYINT))
case BooleanType => Option(JdbcType("BIT(1)", java.sql.Types.BIT))
case StringType => Option(JdbcType("TEXT", java.sql.Types.CLOB))
case BinaryType => Option(JdbcType("BLOB", java.sql.Types.BLOB))
case TimestampType => Option(JdbcType("TIMESTAMP", java.sql.Types.TIMESTAMP))
case DateType => Option(JdbcType("DATE", java.sql.Types.DATE))
case t: DecimalType => Option(
  JdbcType(s"DECIMAL(${t.precision},${t.scale})", java.sql.Types.DECIMAL))

*/


  }

}
