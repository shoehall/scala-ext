package scala.knickknack.spandas.common

import org.scalatest.FunSuite

import java.sql.{PreparedStatement, ResultSetMetaData}
import scala.io.jdbc.{JdbcDialect, JdbcSourceBuilder}
import scala.knickknack.spandas.row.Row

class RowSuite extends FunSuite{
  test("测试ResultSet转为Row") {
    val source = JdbcSourceBuilder()
      .setUrl("jdbc:postgresql://localhost:5432/gis")
      .setDriver("org.postgresql.Driver")
      .setUsername("postgres")
      .setPassword("123456")
      .build()

    val rs = source.read("select * from comemiresdata limit 10;")
    rs.next()
    val size = rs.getMetaData.getColumnCount
    var i = 1
    while(i <= size) {
      println("=" * 80)
      val mt: ResultSetMetaData = rs.getMetaData
      println(rs.getMetaData.getColumnName(i))
      println(rs.getMetaData.getColumnType(i))
      println(rs.getMetaData.getColumnTypeName(i))
      i += 1
    }

    rs.close()


  }

}
