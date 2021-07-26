package scala.io.jdbc

import com.sun.org.apache.xerces.internal.impl.xs.SchemaNamespaceSupport

import java.sql.{Date, ResultSet, ResultSetMetaData, SQLException, Timestamp}
import scala.collection.frame.{BinaryType, BooleanType, DataType, DateType, DecimalType, DoubleType, Field, FloatType, IntegerType, LongType, Schema, ShortType, StringType, TimestampType}

/**
 * 各数据库个性化的部分
 */
trait JdbcDialect {
  def driver: String

  def name: String = this.getClass.getSimpleName

  def pasteUrl(ip: String, port: String, dbname: String): String

  def canHandle(url: String): Boolean

  /**
   * Quotes the identifier. This is used to put quotes around the identifier in case the column
   * name is a reserved keyword, or in case it contains characters that require quotes (e.g. space).
   */
  def quoteIdentifier(colName: String): String = {
    s""""$colName""""
  }


  /**
   * 根据JdbcType信息获得SQL表达式中的字段类型
   *
   * @param jdbcType
   * @return
   */
  def getSQLTypeName(jdbcType: JdbcType): String = {
    jdbcType.sqlType match {
      case java.sql.Types.INTEGER => "INTEGER"
      case java.sql.Types.BIGINT => "BIGINT"
      case java.sql.Types.TINYINT => "TINYINT"
      case java.sql.Types.SMALLINT => "SMALLINT"
      case java.sql.Types.FLOAT => "REAL"
      case java.sql.Types.DOUBLE => "DOUBLE"
      case java.sql.Types.BOOLEAN => "BOOLEAN"
      case java.sql.Types.VARCHAR => "VARCHAR"
      case java.sql.Types.CLOB => "CLOB"
      case java.sql.Types.NCLOB => "NCLOB"
      case java.sql.Types.NULL => "NULL"
      case java.sql.Types.DATE => "DATE"
      case java.sql.Types.TIME => "TIME"
      case java.sql.Types.TIMESTAMP => "TIMESTAMP"
      case java.sql.Types.DECIMAL => s"DECIMAL(${jdbcType.getAs[Int](JdbcType.PRECISION, 10)}, ${jdbcType.getAs[Int](JdbcType.SCALE, 0)})"
      case other =>
        throw new UnsupportedOperationException(s"does not support sql type: $other")
    }
  }

  def toDDL(schema: Schema): String = throw new UnsupportedOperationException

  def getSchema(rsmd: ResultSetMetaData): Schema = {
    val ncols = rsmd.getColumnCount
    val fields = new Array[Field](ncols)
    var i = 0
    while (i < ncols) {
      val columnName = rsmd.getColumnLabel(i + 1)
      val dataType = rsmd.getColumnType(i + 1)
      val typeName = rsmd.getColumnTypeName(i + 1)
      val fieldSize = rsmd.getPrecision(i + 1)
      val fieldScale = rsmd.getScale(i + 1)
      val isSigned = {
        try {
          rsmd.isSigned(i + 1)
        } catch {
          // Workaround for HIVE-14684:
          case e: SQLException if
          e.getMessage == "Method not supported" &&
            rsmd.getClass.getName == "org.apache.hive.jdbc.HiveResultSetMetaData" => true
        }
      }

      // todo: Metadata and nullable
      val nullable = rsmd.isNullable(i + 1) != ResultSetMetaData.columnNoNulls
//      val metadata = new MetadataBuilder()
//        .putString("name", columnName)
//        .putLong("scale", fieldScale)
      val meta = Map.empty[String, Any]
      val columnType = getCatalystType(typeName, fieldSize, meta)
      require(columnType.nonEmpty, s"does not find type for $typeName")
      fields(i) = Field(columnName, columnType.get)
      i = i + 1
    }
    new Schema(fields)
  }

  def getCatalystType(typeName: String, size: Int, meta: Map[String, Any]): Option[DataType] = None

  /**
   * Get the SQL query that should be used to find if the given table exists. Dialects can
   * override this method to return a query that works best in a particular database.
   *
   * @param table The name of the table.
   * @return The SQL query to use for checking the table.
   */
  def getTableExistsQuery(table: String): String = {
    s"SELECT * FROM $table WHERE 1=0"
  }

  /**
   * The SQL query that should be used to discover the schema of a table. It only needs to
   * ensure that the result set has the same schema as the table, such as by calling
   * "SELECT * ...". Dialects can override this method to return a query that works best in a
   * particular database.
   *
   * @param table The name of the table.
   * @return The SQL query to use for discovering the schema.
   */
  def getSchemaQuery(table: String): String = {
    s"SELECT * FROM $table WHERE 1=0"
  }

  /**
   * Escape special characters in SQL string literals.
   * LIKE:
   * if "i" is in the string: "I'm good", should "'" be doubled.
   *
   * @param value The string to be escaped.
   * @return Escaped string.
   */
  protected[jdbc] def escapeSql(value: String): String =
    if (value == null) null else value.replace("'", "''")

  /**
   * Converts value to SQL expression.
   *
   * @param value The value to be converted.
   * @return Converted value.
   */
  def compileValue(value: Any): Any = value match {
    case stringValue: String => s"'${escapeSql(stringValue)}'"
    case timestampValue: Timestamp => "'" + timestampValue + "'"
    case dateValue: Date => "'" + dateValue + "'"
    case arrayValue: Array[Any] => arrayValue.map(compileValue).mkString(", ")
    case _ => value
  }
}

object JdbcDialect {
  implicit val defaultDialect: JdbcDialect = NoopDialect

  def apply(name: String): JdbcDialect = getDialectFromUrl(name)

  /**
   * Register a dialect for use on all new matching jdbc `org.apache.spark.sql.DataFrame`.
   * Reading an existing dialect will cause a move-to-front.
   *
   * @param dialect The new dialect.
   */
  def registerDialect(dialect: JdbcDialect): Unit = {
    dialects = dialect :: dialects.filterNot(_ == dialect)
  }

  /**
   * Unregister a dialect. Does nothing if the dialect is not registered.
   *
   * @param dialect The jdbc dialect.
   */
  def unregisterDialect(dialect: JdbcDialect): Unit = {
    dialects = dialects.filterNot(_ == dialect)
  }

  private[this] var dialects = List[JdbcDialect]()

  registerDialect(MySQLDialect)
  registerDialect(PostgresDialect)
  //    registerDialect(DB2Dialect)
  //    registerDialect(MsSqlServerDialect)
  //    registerDialect(DerbyDialect)
  registerDialect(OracleDialect)
  //    registerDialect(TeradataDialect)

  /**
   * Fetch the JdbcDialect class corresponding to a given database url.
   */
  def getDialectFromUrl(url: String): JdbcDialect = {
    val matchingDialects = dialects.filter(_.canHandle(url))
    matchingDialects.length match {
      case 0 => NoopDialect
      case 1 => matchingDialects.head
      case _ =>
        throw new UnsupportedOperationException(
          s"The there is more than one dialect for the url, you need to get the dialect by 'new'. '$url': ${matchingDialects.mkString(",")}.")
    }
  }

  def get(url: String): JdbcDialect = getDialectFromUrl(url)

  def getDialectFromDriver(driver: String): JdbcDialect = {
    val matchingDialects = dialects.filter(_.driver == driver)
    matchingDialects.length match {
      case 0 => NoopDialect
      case 1 => matchingDialects.head
      case _ =>
        throw new UnsupportedOperationException(
          s"The there is more than one dialect for the driver, you need to get the dialect by 'new'. '$driver': ${matchingDialects.mkString(",")}.")
    }
  }

  def getDialectFromName(name: String): JdbcDialect = {
    val matchingDialects = dialects.filter(dialect => dialect.name == name)
    matchingDialects.length match {
      case 0 => NoopDialect
      case 1 => matchingDialects.head
      case _ =>
        throw new UnsupportedOperationException(
          s"The there is more than one dialect by the dialect name: '$name': ${matchingDialects.mkString(",")}.")
    }
  }
}


trait HasDialect extends Serializable {
  /**
   * JdbcDialect:
   * - 生成SQL时指定;
   * - 在将参数按对应类型转为合法SQL语句时用到;
   *
   */
  private var dialect0: JdbcDialect = NoopDialect

  def setDialect(dialect: JdbcDialect): this.type = {
    this.dialect0 = dialect
    this
  }

  def getDialect: JdbcDialect = this.dialect0
}

/**
 * NOOP dialect object, always returning the neutral element.
 */
class NoopDialect extends JdbcDialect {
  override def canHandle(url: String): Boolean = true

  override def driver: String = null

  override def pasteUrl(ip: String, port: String, dbname: String): String = s"jdbc:$ip:$port/$dbname"

}

case object NoopDialect extends NoopDialect


class MySQLDialect extends JdbcDialect {
  override def canHandle(url: String): Boolean = url.startsWith("jdbc:mysql")

  override def quoteIdentifier(colName: String): String = {
    s"`$colName`"
  }

  override def getTableExistsQuery(table: String): String = {
    s"SELECT 1 FROM $table LIMIT 1"
  }

  override def driver: String = "com.mysql.jdbc.Driver"

  override def pasteUrl(ip: String, port: String, dbname: String): String = s"jdbc:mysql://$ip:$port/$dbname"
}

case object MySQLDialect extends MySQLDialect

class PostgresDialect extends JdbcDialect {
  override def getSQLTypeName(jdbcType: JdbcType): String =
    jdbcType.sqlType match {
      case java.sql.Types.INTEGER => "INTEGER"
      case java.sql.Types.BIGINT => "BIGINT"
      case java.sql.Types.TINYINT => "TINYINT"
      case java.sql.Types.SMALLINT => "SMALLINT"
      case java.sql.Types.FLOAT => "FLOAT4"
      case java.sql.Types.DOUBLE => "FLOAT8"
      case java.sql.Types.BOOLEAN => "BOOLEAN"
      case java.sql.Types.VARCHAR => "VARCHAR"
      case java.sql.Types.CLOB => "TEXT"
      case java.sql.Types.NCLOB => "NCLOB"
      case java.sql.Types.NULL => "NULL"
      case java.sql.Types.DATE => "DATE"
      case java.sql.Types.TIME => "TIME"
      case java.sql.Types.TIMESTAMP => "TIMESTAMP"
      // todo: 后期可以通过属性加入进来
      case java.sql.Types.DECIMAL => s"NUMERIC(${jdbcType.getAs[Int](JdbcType.PRECISION, 10)}, ${jdbcType.getAs[Int](JdbcType.SCALE, 0)})"
      case other =>
        throw new UnsupportedOperationException(s"does not support sql type: $other")
    }

  override def driver: String = "org.postgresql.Driver"

  override def pasteUrl(ip: String, port: String, dbname: String): String =
    s"jdbc:postgresql://$ip:$port/$dbname"

  override def canHandle(url: String): Boolean = {
    url != null && url.startsWith("jdbc:postgresql://")
  }


  override def getCatalystType(typeName: String, size: Int, meta: Map[String, Any]): Option[DataType] = {
    typeName match {
      case "bool" => Some(BooleanType)
      case "bit" => Some(BinaryType)
      case "int2" => Some(ShortType)
      case "int4" => Some(IntegerType)
      case "int8" | "oid" => Some(LongType)
      case "float4" => Some(FloatType)
      case "money" | "float8" => Some(DoubleType)
      case "text" | "varchar" | "char" | "cidr" | "inet" | "json" | "jsonb" | "uuid" =>
        Some(StringType)
      case "bytea" => Some(BinaryType)
      case "timestamp" | "timestamptz" | "time" | "timetz" => Some(TimestampType)
      case "date" => Some(DateType)
      case "numeric" | "decimal" => Some(DecimalType(meta("precision").asInstanceOf[Int], meta("scale").asInstanceOf[Int]))
      case _ => None
    }
  }



}

case object PostgresDialect extends PostgresDialect

class OracleDialect extends JdbcDialect {
  override def driver: String = throw new UnsupportedOperationException

  override def pasteUrl(ip: String, port: String, dbname: String): String = throw new UnsupportedOperationException

  override def canHandle(url: String): Boolean = url.startsWith(" jdbc:oracle:thin:")
}

case object OracleDialect extends OracleDialect



