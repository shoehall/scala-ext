package scala.io.jdbc


import java.sql.{Connection, Date, PreparedStatement, ResultSet, SQLException, Statement, Timestamp}
import scala.collection.frame.{BinaryType, BooleanType, DataType, DateType, DecimalType, DoubleType, FloatType, IntegerType, LongType, Row, Schema, ShortType, StringType, TimestampType}

package object utils {
  // 函数类型应该为: (PreparedStatement, Any, Int) => Unit
  def makeSetter(dataType: DataType, index: Int): (PreparedStatement, Any) => Unit = {
    (st: PreparedStatement, value: Any) =>
      dataType match {
        case BooleanType =>
          st.setBoolean(index, value.asInstanceOf[Boolean])
        case BinaryType =>
          st.setBoolean(index, value.asInstanceOf[Boolean])
        case ShortType =>
          st.setShort(index, value.asInstanceOf[Short])
        case IntegerType =>
          st.setInt(index, value.asInstanceOf[Int])
        case LongType =>
          st.setLong(index, value.asInstanceOf[Long])
        case FloatType =>
          st.setFloat(index, value.asInstanceOf[Float])
        case DoubleType =>
          st.setDouble(index, value.asInstanceOf[Double])
        case StringType =>
          st.setString(index, value.asInstanceOf[String])
        case TimestampType =>
          st.setTimestamp(index, value.asInstanceOf[Timestamp])
        case DateType =>
          st.setDate(index, value.asInstanceOf[Date])
        case t =>
          throw new UnsupportedOperationException(s"does not support type $t")
      }
  }

  def makeSetter(schema: Schema): Map[Int, (PreparedStatement, Any) => Unit] = schema.fields.zipWithIndex.map { case (field, idx) => (idx, makeSetter(field.dType, idx + 1) )}.toMap

  def makeSetter4Row(schema: Schema): (PreparedStatement, Row) => Unit = {
    val setter = makeSetter(schema)
    (statement: PreparedStatement, row: Row) => {
      for(i <- 0 until row.length) {
        setter(i)(statement, row(i))
      }
    }
  }

  def makeGetter(dataType: DataType, index: Int): ResultSet => Any = {
    (rs: ResultSet) =>
      dataType match {
        case BooleanType =>
          rs.getBoolean(index)
        case BinaryType =>
          rs.getBoolean(index)
        case ShortType =>
          rs.getShort(index)
        case IntegerType =>
          rs.getInt(index)
        case LongType =>
          rs.getLong(index)
        case FloatType =>
          rs.getFloat(index)
        case DoubleType =>
          rs.getDouble(index)
        case StringType =>
          rs.getString(index)
        case TimestampType =>
          rs.getTimestamp(index)
        case DateType =>
          rs.getDate(index)
        case t =>
          throw new UnsupportedOperationException(s"does not support type $t")
      }
  }

  def makeGetter(schema: Schema): Map[Int, ResultSet => Any] = schema.fields.zipWithIndex.map { case (field, idx) => (idx, makeGetter(field.dType, idx + 1) )}.toMap

  def getSchema(statement: Statement, schemaQuerySQL: SQL, dialect: JdbcDialect): Schema = {
    val rsmd = try {
      statement.executeQuery(schemaQuerySQL.sql)
    } catch {
      case e: Throwable =>
        throw new SQLException(s"failed to load metadata, ${schemaQuerySQL.sql}", e)
    }
    dialect.getSchema(rsmd.getMetaData)
  }

  def read(statement: Statement, sql: SQL): ResultSet = statement.executeQuery(sql)

  def create(connection: Connection, table: String, schema: Schema, dialect: JdbcDialect): Boolean = {
    val ddl = schema.fields.map {
      field =>
        s"${dialect.quoteIdentifier(field.name)} ${getJDBCType(field.dType).get}"
    }.mkString(",")
    val statement = connection.createStatement()
    try {
      statement.execute(s"create table if not exists $table ($ddl);")
    } catch {
      case e: Throwable =>
        throw new SQLException(s"failed to execute: create table if not exists $table $ddl", e)
    }
  }

  def insert(connection: Connection, table: String, schema: Schema, rows: TraversableOnce[Row], batch: Int, dialect: JdbcDialect): Unit = {
    val fieldNames = schema.fields.map { field =>dialect.quoteIdentifier(field.name)}
    val statement = connection.prepareStatement(s"insert into $table (${fieldNames.mkString(",")}) values (${fieldNames.map(_ => "?").mkString(",")});")

    val setters = makeSetter(schema)
    var i = 0
    rows.foreach {
      row =>
        for(i <- 0 until row.length) {
          setters(i).apply(statement, row(i))
        }
        statement.addBatch()
        i += 1
        if(i >= batch) {
          statement.executeBatch()
          i = 0
        }
    }

    if(i > 0) {
      statement.executeBatch()
      i = 0
    }
    statement.close()
  }

  def getJDBCType(dt: DataType): Option[String] = dt match {
    case StringType => Some("TEXT")
    case BinaryType => Some("BYTEA")
    case BooleanType => Some("BOOLEAN")
    case FloatType => Some("FLOAT4")
    case DoubleType => Some("FLOAT8")
    case ShortType => Some("SMALLINT")
    case t: DecimalType => Some(s"NUMERIC(${t.precision},${t.scale})")
    case IntegerType => Option("INTEGER")
    case LongType => Option("BIGINT")
    case TimestampType => Option("TIMESTAMP")
    case DateType => Option("DATE")
    case other => throw new IllegalArgumentException(s"Unsupported type in postgresql: $other");
  }


}
