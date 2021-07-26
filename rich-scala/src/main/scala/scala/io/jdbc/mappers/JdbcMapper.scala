package scala.io.jdbc.mappers

import shapeless.{::, Generic, HList, HNil, Lazy}

import java.sql.Types._
import java.sql._
import scala.{Array => SArray}
import scala.io.jdbc.mappers.Mapper.utils
import scala.io.jdbc.{JdbcDialect, JdbcType, SQLValue}
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * 泛型T在JDBC中的type class:
 * wrap到[[PreparedStatement]], 以及从[[ResultSet]]中unwrap的type class
 *
 * @tparam T 泛型
 */
trait JdbcMapper[T] extends SQLExprMapper[T] {
  /**
   * the jdbc information for T
   *
   * @return
   */
  def jdbcType: JdbcType

  def toDDL: StructOfJdbcMapper = PrimitiveStruct(this.asInstanceOf[JdbcMapper[Any]])

  /**
   * wrap value of type T into [[PreparedStatement]]
   *
   * @param statement      PreparedStatement
   * @param parameterIndex index in the sql for the statement, started by 1
   * @param value          value
   */
  def wrap(statement: PreparedStatement, parameterIndex: Int, value: T): Unit
/*

  def wrap(statement: PreparedStatement, parameterIndex: Int, value: Option[T]): Unit =
    if (value.nonEmpty)
      wrap(statement, parameterIndex, value.get)
    else {
      if (size == 1)
        statement.setNull(parameterIndex, jdbcType.sqlType)
      if (size > 1) {
        var i = 0
        while (i < size) {
          wrap(statement, parameterIndex + i, None)
          i += 1
        }
      }
    }

*/

  /**
   * unwrap value from [[ResultSet]] as type T
   *
   * @param resultSet   ResultSet
   * @param columnIndex column index, started by 1
   * @return
   */
  def unwrap(resultSet: ResultSet, columnIndex: Int): T =
    resultSet.getObject(columnIndex).asInstanceOf[T]

  /**
   * unwrap value from [[ResultSet]] as type T, by the column name.
   * only support primitive: type T that correspond of one field in the ResultSet
   *
   * @param resultSet   ResultSet
   * @param columnLabel column index, started by 1
   * @return
   */
  def unwrap(resultSet: ResultSet, columnLabel: String): T =
    resultSet.getObject(columnLabel).asInstanceOf[T]

  def unwrap(resultSet: ResultSet): T = unwrap(resultSet, 1)

  protected def wrapNotNull(value: T, dialect: JdbcDialect): String

  override def wrap(value: T, dialect: JdbcDialect): String =
    if (value != null)
      wrapNotNull(value, dialect)
    else {
      val size = toDDL.flatten.length
      if (size <= 0)
        null
      else
        SArray.fill(size)("NULL").mkString(",")
    }
}

object JdbcMapper {
  def concat(left: String, right: String): String = {
    (left, right) match {
      case (null, null) => null
      case (null, r) => r
      case (l, null) => l
      case (l, r) => l + "," + r
    }
  }

}


trait PrimitiveMapper[T] extends JdbcMapper[T] {
/*
  override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Option[T]): Unit =
    if (value.nonEmpty)
      wrap(statement, parameterIndex, value.get)
    else
      statement.setNull(parameterIndex, jdbcType.sqlType)
*/

  override def wrapNotNull(value: T, dialect: JdbcDialect): String = s"$value"
}

trait JdbcMappers extends PrimitiveJdbcMappers {
  implicit val nilHasFields: JdbcMapper[HNil] = new JdbcMapper[HNil] {
    override val toDDL: StructOfJdbcMapper = CoproductStruct(this.asInstanceOf[JdbcMapper[Any]], null, Seq.empty[StructOfJdbcMapper])

    override def jdbcType: JdbcType = throw new UnsupportedOperationException("there was no jdbcType for HNil")

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: HNil): Unit = {}

    override def unwrap(resultSet: ResultSet, columnIndex: Int): HNil = HNil

    override def unwrap(resultSet: ResultSet, columnLabel: String): HNil = HNil

    /**
     * 根据类型T将其转为SQL表达式中需要的字符串
     *
     * @param value   value of type T, it is not null
     * @param dialect the dialect of the database
     * @return
     */
    override def wrapNotNull(value: HNil, dialect: JdbcDialect): String = null
  }

  implicit def hListHasFields[T, HL <: HList](implicit tRandom: Lazy[JdbcMapper[T]],
                                              hListRandom: JdbcMapper[HL]): JdbcMapper[T :: HL] =
    new JdbcMapper[::[T, HL]] {
      override def jdbcType: JdbcType = throw new UnsupportedOperationException("there was no jdbcType for HList")

      override val toDDL: StructOfJdbcMapper = CoproductStruct(
        this.asInstanceOf[JdbcMapper[Any]],
        null,
        hListRandom.toDDL match {
          case structDDL: CoproductStruct =>
            val mm = tRandom.value
            if (structDDL.children.isEmpty)
              Seq(mm.toDDL)
            else
              mm.toDDL +: structDDL.children

          case _ =>
            throw new UnsupportedOperationException("HList只支持StructDDL")
        }
      )

      private lazy val span4head: Int = tRandom.value.toDDL.flatten.length

      override def wrap(statement: PreparedStatement, parameterIndex: Int, value: T :: HL): Unit = {
        tRandom.value.wrap(statement, parameterIndex, value.head)
        hListRandom.wrap(statement, parameterIndex + span4head, value.tail)
      }

      override def unwrap(resultSet: ResultSet, columnIndex: Int): T :: HL =
        ::(tRandom.value.unwrap(resultSet, columnIndex),
          hListRandom.unwrap(resultSet, columnIndex + span4head))

      /**
       * 根据类型T将其转为SQL表达式中需要的字符串
       *
       * @param value   value of type T, it is not null
       * @param dialect the dialect of the database
       * @return
       */
      override def wrapNotNull(value: T :: HL, dialect: JdbcDialect): String =
        JdbcMapper.concat(tRandom.value.wrap(value.head, dialect), hListRandom.wrap(value.tail, dialect))
    }

  implicit def caseClassRandom[T, HL <: HList](
                                                implicit tag: TypeTag[T],
                                                gen: Generic.Aux[T, HL],
                                                hListRandom: Lazy[JdbcMapper[HL]]
                                              ): JdbcMapper[T] = {
    new JdbcMapper[T] {
      override def jdbcType: JdbcType = throw new UnsupportedOperationException(s"there was no jdbcType for case class $dataType.")

      override val toDDL: StructOfJdbcMapper = {
        val fieldNames = utils.fieldsOf(typeOf[T]).map(_._1.toTermName.toString)
        val HListWrapperValue = hListRandom.value
        val children = HListWrapperValue.toDDL.asInstanceOf[CoproductStruct].children
        require(fieldNames.length == children.length,
          s"the field names size of coproduct class is not equal to the size of HList:" +
            s" ${(fieldNames.length, children.length)}, ${fieldNames.mkString("[", ",", "]")}")

        CoproductStruct(this.asInstanceOf[JdbcMapper[Any]],
          null,
          fieldNames.zip(children).map { case (fieldName, child) => child.concatFieldName(fieldName) })
      }

      override def wrap(statement: PreparedStatement, parameterIndex: Int, value: T): Unit = {
        hListRandom.value.wrap(statement, parameterIndex, gen.to(value))
      }

      override def unwrap(resultSet: ResultSet, columnIndex: Int): T =
        gen.from(hListRandom.value.unwrap(resultSet, columnIndex))

      /**
       * 根据类型T将其转为SQL表达式中需要的字符串
       *
       * @param value   value of type T, it is not null
       * @param dialect the dialect of the database
       * @return
       */
      override def wrapNotNull(value: T, dialect: JdbcDialect): String = hListRandom.value.wrap(gen.to(value), dialect)
    }
  }

  // todo: 其实这里有两种可能, 一种是输入数组类型其实想作为jdbc的array类型处理, 另一种是输入数组类型想作为多列处理
  // 目前是以数组类型处理.
  implicit def wrapper4Seq[T](implicit tag: ClassTag[T], ev: JdbcMapper[T]): JdbcMapper[Seq[T]] = new JdbcMapper[Seq[T]] {
    /**
     * the jdbc information for T
     *
     * @return
     */
    override def jdbcType: JdbcType = JdbcType(Types.ARRAY)

    private val elementWrapper = implicitly[JdbcMapper[T]]

    private val elementSQLType = implicitly[JdbcMapper[T]].jdbcType.toString

    /**
     * wrap value of type T into [[PreparedStatement]]
     *
     * @param statement      PreparedStatement
     * @param parameterIndex index in the sql for the statement, started by 1
     * @param value          value
     */
    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Seq[T]): Unit = {
      println("wrap seq")
      val array = statement.getConnection.createArrayOf(elementSQLType, value.toArray.asInstanceOf[SArray[Object]])
      statement.setArray(parameterIndex, array)
    }

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Seq[T] =
      resultSet.getArray(columnIndex).getArray().asInstanceOf[SArray[T]].toSeq

    override def wrapNotNull(value: Seq[T], dialect: JdbcDialect): String =
      value.map(v => elementWrapper.wrap(v, dialect)).mkString("(", ",", ")")
  }

  /**
   * 逐个处理元素
   *
   * @tparam T
   * @return
   */
  implicit def wrapper4SeqSQLValue[T: JdbcMapper]: JdbcMapper[Seq[SQLValue[T]]] = {
    new JdbcMapper[Seq[SQLValue[T]]] {
      /**
       * the jdbc information for T
       *
       * @return
       */
      override def jdbcType: JdbcType = throw new UnsupportedOperationException("元素Seq集合没有对应的数据类型")

      private val elementWrapper = implicitly[JdbcMapper[T]]

      /**
       * wrap value of type T into [[PreparedStatement]]
       *
       * @param statement      PreparedStatement
       * @param parameterIndex index in the sql for the statement, started by 1
       * @param value          value
       */
      override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Seq[SQLValue[T]]): Unit = {
        println("wrap seq arg")
        var i = parameterIndex
        value.foreach {
          v =>
            v.wrapper.wrap(statement, i, getOrNull(v.value))
            i += 1
        }
      }

      override def unwrap(resultSet: ResultSet, columnIndex: Int): Seq[SQLValue[T]] = throw new UnsupportedOperationException("暂无将数据转为SeqSQLValue的需求")

      override def unwrap(resultSet: ResultSet, columnLabel: String): Seq[SQLValue[T]] = throw new UnsupportedOperationException("暂无将数据转为SeqSQLValue的需求")

      override def wrapNotNull(value: Seq[SQLValue[T]], dialect: JdbcDialect): String =
        value.map { v => elementWrapper.wrap(getOrNull(v.value), dialect) }.mkString(",")
    }
  }

  implicit def wrapper4Option[T: JdbcMapper]: JdbcMapper[Option[T]] = new JdbcMapper[Option[T]] {
    /**
     * the jdbc information for T
     *
     * @return
     */
    override def jdbcType: JdbcType = elementWrapper.jdbcType

    private val elementWrapper = implicitly[JdbcMapper[T]]

    /**
     * wrap value of type T into [[PreparedStatement]]
     *
     * @param statement      PreparedStatement
     * @param parameterIndex index in the sql for the statement, started by 1
     * @param value          value
     */
    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Option[T]): Unit = {
      println("wrap option")
      elementWrapper.wrap(statement, parameterIndex, getOrNull(value))
    }

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Option[T] =
      Some(elementWrapper.unwrap(resultSet, columnIndex))

    override def unwrap(resultSet: ResultSet, columnLabel: String): Option[T] =
      Some(elementWrapper.unwrap(resultSet, columnLabel))

    override def wrapNotNull(value: Option[T], dialect: JdbcDialect): String = {
      elementWrapper.wrap(getOrNull(value), dialect)
    }

  }

//  implicit def wrapper4SQLValue[T: JdbcMapper] = new JdbcMapper[SQLValue[T]] {
//    /**
//     * the jdbc information for T
//     *
//     * @return
//     */
//    override def jdbcType: JdbcType = throw new UnsupportedOperationException
//
//    /**
//     * wrap value of type T into [[PreparedStatement]]
//     *
//     * @param statement      PreparedStatement
//     * @param parameterIndex index in the sql for the statement, started by 1
//     * @param value          value
//     */
//    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: SQLValue[T]): Unit = ???
//
//    private val wrapper = implicitly[JdbcMapper[T]]
//
//    override def wrap(value: SQLValue[T], dialect: JdbcDialect): String = {
//      wrapper.wrap(value.value, dialect)
//    }
//  }


}


/**
 * 非嵌套类型的JdbcMapper
 */
trait PrimitiveJdbcMappers {
  implicit val jdbcMapper4Boolean: JdbcMapper[Boolean] = new PrimitiveMapper[Boolean] {
    override def jdbcType: JdbcType = JdbcType(BOOLEAN)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Boolean): Unit =
      statement.setBoolean(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Boolean =
      resultSet.getBoolean(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Boolean =
      resultSet.getBoolean(columnLabel)

  }


  implicit val jdbcMapper4Byte: JdbcMapper[Byte] = new PrimitiveMapper[Byte] {
    override def jdbcType: JdbcType = JdbcType(TINYINT)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Byte): Unit =
      statement.setByte(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Byte =
      resultSet.getByte(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Byte =
      resultSet.getByte(columnLabel)
  }

  implicit val jdbcMapper4Short: JdbcMapper[Short] = new PrimitiveMapper[Short] {
    override def jdbcType: JdbcType = JdbcType(SMALLINT)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Short): Unit =
      statement.setShort(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Short =
      resultSet.getShort(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Short =
      resultSet.getShort(columnLabel)
  }

  implicit val jdbcMapper4Int: JdbcMapper[Int] = new PrimitiveMapper[Int] {
    override def jdbcType: JdbcType = JdbcType(INTEGER)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Int): Unit =
      statement.setInt(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Int =
      resultSet.getInt(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Int =
      resultSet.getInt(columnLabel)
  }

  implicit val jdbcMapper4Long: JdbcMapper[Long] = new PrimitiveMapper[Long] {
    override def jdbcType: JdbcType = JdbcType(BIGINT)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Long): Unit =
      statement.setLong(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Long =
      resultSet.getLong(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Long =
      resultSet.getLong(columnLabel)
  }

  implicit val jdbcMapper4Float: JdbcMapper[Float] = new PrimitiveMapper[Float] {
    override def jdbcType: JdbcType = JdbcType(FLOAT)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Float): Unit =
      statement.setFloat(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Float =
      resultSet.getFloat(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Float =
      resultSet.getFloat(columnLabel)
  }

  implicit val jdbcMapper4Double: JdbcMapper[Double] = new PrimitiveMapper[Double] {
    override def jdbcType: JdbcType = JdbcType(DOUBLE)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Double): Unit =
      statement.setDouble(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Double =
      resultSet.getDouble(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Double =
      resultSet.getDouble(columnLabel)
  }

  implicit val jdbcMapper4String: JdbcMapper[String] = new PrimitiveMapper[String] {
    override def jdbcType: JdbcType = JdbcType(CLOB)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: String): Unit =
      statement.setString(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): String =
      resultSet.getString(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): String =
      resultSet.getString(columnLabel)

    override def wrapNotNull(value: String, dialect: JdbcDialect): String =
      "'" + value.replaceAll("'", "''") + "'"
  }

  implicit val jdbcMapper4Date: JdbcMapper[Date] = new PrimitiveMapper[Date] {
    override def jdbcType: JdbcType = JdbcType(DATE)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Date): Unit = statement.setDate(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Date = resultSet.getDate(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Date = resultSet.getDate(columnLabel)

    override def wrapNotNull(value: Date, dialect: JdbcDialect): String = "'" + value.toString + "'"
  }

  implicit val jdbcMapper4Time: JdbcMapper[Time] = new PrimitiveMapper[Time] {
    override def jdbcType: JdbcType = JdbcType(TIME)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Time): Unit =
      statement.setTime(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Time =
      resultSet.getTime(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Time =
      resultSet.getTime(columnLabel)

    override def wrapNotNull(value: Time, dialect: JdbcDialect): String = "'" + value.toString + "'"

  }

  implicit val jdbcMapper4Timestamp: JdbcMapper[Timestamp] = new PrimitiveMapper[Timestamp] {
    override def jdbcType: JdbcType = JdbcType(TIMESTAMP)

    override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Timestamp): Unit =
      statement.setTimestamp(parameterIndex, value)

    override def unwrap(resultSet: ResultSet, columnIndex: Int): Timestamp =
      resultSet.getTimestamp(columnIndex)

    override def unwrap(resultSet: ResultSet, columnLabel: String): Timestamp =
      resultSet.getTimestamp(columnLabel)

    override def wrapNotNull(value: Timestamp, dialect: JdbcDialect): String = "'" + value.toString + "'"
  }

  /*
  created by code:
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
    )) {println(code(dType, sqlType) + "\n")}

   */
}