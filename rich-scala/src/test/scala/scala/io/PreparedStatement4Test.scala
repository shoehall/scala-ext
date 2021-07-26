package scala.io

import java.io.{InputStream, Reader}
import java.net.URL
import java.sql
import java.sql.{Blob, Clob, Connection, Date, NClob, ParameterMetaData, PreparedStatement, Ref, ResultSet, ResultSetMetaData, RowId, SQLWarning, SQLXML, Time, Timestamp}
import java.util.Calendar
import scala.collection.mutable

class PreparedStatement4Test extends PreparedStatement{
  private val properties = scala.collection.mutable.Map.empty[Int, (String, Any)]

  def getProperties: mutable.Map[Int, (String, Any)] = this.properties

  override def executeQuery(): ResultSet = ???

  override def executeUpdate(): Int = ???

  override def setNull(parameterIndex: Int, sqlType: Int): Unit = {
    properties += parameterIndex -> (sqlType.toString, null)
  }

  override def setBoolean(parameterIndex: Int, x: Boolean): Unit = {
    properties += parameterIndex -> ("Boolean", x)
  }

  override def setByte(parameterIndex: Int, x: Byte): Unit = {
    properties += parameterIndex -> ("Byte", x)
  }

  override def setShort(parameterIndex: Int, x: Short): Unit = {
    properties += parameterIndex -> ("Short", x)
  }

  override def setInt(parameterIndex: Int, x: Int): Unit = {
    properties += parameterIndex -> ("Int", x)
  }

  override def setLong(parameterIndex: Int, x: Long): Unit = {
    properties += parameterIndex -> ("Long", x)
  }

  override def setFloat(parameterIndex: Int, x: Float): Unit = {
    properties += parameterIndex -> ("Float", x)

  }

  override def setDouble(parameterIndex: Int, x: Double): Unit = {
    properties += parameterIndex -> ("Double", x)

  }

  override def setBigDecimal(parameterIndex: Int, x: java.math.BigDecimal): Unit = ???

  override def setString(parameterIndex: Int, x: String): Unit = {
    properties += parameterIndex -> ("String", x)
  }

  override def setBytes(parameterIndex: Int, x: Array[Byte]): Unit = ???

  override def setDate(parameterIndex: Int, x: Date): Unit = ???

  override def setTime(parameterIndex: Int, x: Time): Unit = ???

  override def setTimestamp(parameterIndex: Int, x: Timestamp): Unit = ???

  override def setAsciiStream(parameterIndex: Int, x: InputStream, length: Int): Unit = ???

  override def setUnicodeStream(parameterIndex: Int, x: InputStream, length: Int): Unit = ???

  override def setBinaryStream(parameterIndex: Int, x: InputStream, length: Int): Unit = ???

  override def clearParameters(): Unit = ???

  override def setObject(parameterIndex: Int, x: Any, targetSqlType: Int): Unit = ???

  override def setObject(parameterIndex: Int, x: Any): Unit = ???

  override def execute(): Boolean = ???

  override def addBatch(): Unit = ???

  override def setCharacterStream(parameterIndex: Int, reader: Reader, length: Int): Unit = ???

  override def setRef(parameterIndex: Int, x: Ref): Unit = ???

  override def setBlob(parameterIndex: Int, x: Blob): Unit = ???

  override def setClob(parameterIndex: Int, x: Clob): Unit = ???

  override def setArray(parameterIndex: Int, x: sql.Array): Unit = ???

  override def getMetaData: ResultSetMetaData = ???

  override def setDate(parameterIndex: Int, x: Date, cal: Calendar): Unit = ???

  override def setTime(parameterIndex: Int, x: Time, cal: Calendar): Unit = ???

  override def setTimestamp(parameterIndex: Int, x: Timestamp, cal: Calendar): Unit = ???

  override def setNull(parameterIndex: Int, sqlType: Int, typeName: String): Unit = ???

  override def setURL(parameterIndex: Int, x: URL): Unit = ???

  override def getParameterMetaData: ParameterMetaData = ???

  override def setRowId(parameterIndex: Int, x: RowId): Unit = ???

  override def setNString(parameterIndex: Int, value: String): Unit = ???

  override def setNCharacterStream(parameterIndex: Int, value: Reader, length: Long): Unit = ???

  override def setNClob(parameterIndex: Int, value: NClob): Unit = ???

  override def setClob(parameterIndex: Int, reader: Reader, length: Long): Unit = ???

  override def setBlob(parameterIndex: Int, inputStream: InputStream, length: Long): Unit = ???

  override def setNClob(parameterIndex: Int, reader: Reader, length: Long): Unit = ???

  override def setSQLXML(parameterIndex: Int, xmlObject: SQLXML): Unit = ???

  override def setObject(parameterIndex: Int, x: Any, targetSqlType: Int, scaleOrLength: Int): Unit = ???

  override def setAsciiStream(parameterIndex: Int, x: InputStream, length: Long): Unit = ???

  override def setBinaryStream(parameterIndex: Int, x: InputStream, length: Long): Unit = ???

  override def setCharacterStream(parameterIndex: Int, reader: Reader, length: Long): Unit = ???

  override def setAsciiStream(parameterIndex: Int, x: InputStream): Unit = ???

  override def setBinaryStream(parameterIndex: Int, x: InputStream): Unit = ???

  override def setCharacterStream(parameterIndex: Int, reader: Reader): Unit = ???

  override def setNCharacterStream(parameterIndex: Int, value: Reader): Unit = ???

  override def setClob(parameterIndex: Int, reader: Reader): Unit = ???

  override def setBlob(parameterIndex: Int, inputStream: InputStream): Unit = ???

  override def setNClob(parameterIndex: Int, reader: Reader): Unit = ???

  override def executeQuery(sql: String): ResultSet = ???

  override def executeUpdate(sql: String): Int = ???

  override def close(): Unit = ???

  override def getMaxFieldSize: Int = ???

  override def setMaxFieldSize(max: Int): Unit = ???

  override def getMaxRows: Int = ???

  override def setMaxRows(max: Int): Unit = ???

  override def setEscapeProcessing(enable: Boolean): Unit = ???

  override def getQueryTimeout: Int = ???

  override def setQueryTimeout(seconds: Int): Unit = ???

  override def cancel(): Unit = ???

  override def getWarnings: SQLWarning = ???

  override def clearWarnings(): Unit = ???

  override def setCursorName(name: String): Unit = ???

  override def execute(sql: String): Boolean = ???

  override def getResultSet: ResultSet = ???

  override def getUpdateCount: Int = ???

  override def getMoreResults: Boolean = ???

  override def setFetchDirection(direction: Int): Unit = ???

  override def getFetchDirection: Int = ???

  override def setFetchSize(rows: Int): Unit = ???

  override def getFetchSize: Int = ???

  override def getResultSetConcurrency: Int = ???

  override def getResultSetType: Int = ???

  override def addBatch(sql: String): Unit = ???

  override def clearBatch(): Unit = ???

  override def executeBatch(): Array[Int] = ???

  override def getConnection: Connection = ???

  override def getMoreResults(current: Int): Boolean = ???

  override def getGeneratedKeys: ResultSet = ???

  override def executeUpdate(sql: String, autoGeneratedKeys: Int): Int = ???

  override def executeUpdate(sql: String, columnIndexes: Array[Int]): Int = ???

  override def executeUpdate(sql: String, columnNames: Array[String]): Int = ???

  override def execute(sql: String, autoGeneratedKeys: Int): Boolean = ???

  override def execute(sql: String, columnIndexes: Array[Int]): Boolean = ???

  override def execute(sql: String, columnNames: Array[String]): Boolean = ???

  override def getResultSetHoldability: Int = ???

  override def isClosed: Boolean = ???

  override def setPoolable(poolable: Boolean): Unit = ???

  override def isPoolable: Boolean = ???

  override def closeOnCompletion(): Unit = ???

  override def isCloseOnCompletion: Boolean = ???

  override def unwrap[T](iface: Class[T]): T = ???

  override def isWrapperFor(iface: Class[_]): Boolean = ???
}
