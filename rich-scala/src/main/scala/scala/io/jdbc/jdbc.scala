package scala.io

import java.sql._
import scala.collection.frame.{Row, Schema}
import scala.io.jdbc.mappers.{JdbcMapper, Mapper}
import scala.io.jdbc.rich.{RichResultSet, RichStatement}

package object jdbc {
  def builder(): JdbcSourceBuilder = null

  /**
   * 用于标注字符串为SQL的语法, 以SQL、sql或mysql等作为前缀.
   * 需要编译时指定[[JdbcDialect]], 默认为通用的SQL
   *
   * @param sc
   */
  implicit class SQLHint(private val sc: StringContext) extends AnyVal {

    def sql(args: SQLArg[_]*)(implicit dialect: JdbcDialect): SQL = {
      if (sc.parts.length - args.length == 1)
        new SQL(sc.parts, args: _*).setDialect(dialect)
      else {
        require(sc.parts.length == 2, "maybe you use _* in format, there should be only one parameter in a string.")
        new SQL(sc.parts, new SeqSQLValue[Any](args.asInstanceOf[Seq[SQLArg[Any]]]))
      }
    }

    def mysql(args: SQLArg[_]*): SQL = sql(args: _*)(MySQLDialect)

    def postgresql(args: SQLArg[_]*): SQL = sql(args: _*)(PostgresDialect)

    def pg(args: SQLArg[_]*): SQL = postgresql(args: _*)

    def oracle(args: SQLArg[_]*): SQL = sql(args: _*)(OracleDialect)

    def SQL(args: SQLArg[_]*)(implicit dialect: JdbcDialect): SQL = sql(args: _*)(dialect)
  }

  implicit def richStatement(statement0: Statement): RichStatement = new RichStatement {
    override def statement: Statement = statement0
  }

  implicit def richResultSet(resultSet0: ResultSet): RichResultSet = new RichResultSet {
    override def resultSet: ResultSet = resultSet0
  }

  /**
   * 保留了编译时类型的参数容器
   *
   * @param value
   * @param jdbcWrapper
   * @tparam T
   */
  trait SQLArg[T] {
    def value: Option[T]

    def wrapper: JdbcMapper[T]

    def toString(dialect: JdbcDialect): String = wrapper.wrap(if(value.isEmpty) null.asInstanceOf[T] else value.get, dialect)

    override def toString: String = value match {
      case None => "NULL"
      case Some(value) => s"$value"
    }

    // todo: hash
    // todo: equal
  }

  /**
   * SQL表达式中带有类型的值对应的容器
   *
   * @param value
   * @param jdbcWrapper$T$0
   * @tparam T
   */
  class SQLValue[T: JdbcMapper](override val value: Option[T]) extends SQLArg[T] {
    def this(value: T) = this(Option(value))

    if (value.nonEmpty)
      require(value.get != null, "the null should be formed as None in JdbcValue.")

    override def wrapper: JdbcMapper[T] = implicitly[JdbcMapper[T]]
  }

  /**
   * 在SQL表达式中用于表示字段或表名等的参数容器: 在wrap为字符串时会使用JdbcDialect的quoteIdentifier函数
   *
   * @param value
   */
  class SQLIdentifier(fieldName: String) extends SQLArg[String] {
    require(fieldName != null && fieldName.nonEmpty)

    override def value: Option[String] = Some(fieldName)

    override def wrapper: JdbcMapper[String] = Mapper.jdbcMapper4String

    override def toString(dialect: JdbcDialect): String = dialect.quoteIdentifier(fieldName)
  }

  class SeqSQLValue[T](values: Seq[SQLArg[T]]) extends SQLArg[Seq[SQLArg[T]]] {
    override def value: Option[Seq[SQLArg[T]]] = Some(values)

    override def wrapper: JdbcMapper[Seq[SQLArg[T]]] = SeqSQLValue.wrapper[T]

    override def toString(dialect: JdbcDialect): String = values.map(_.toString(dialect)).mkString(",")

    override def toString: String = values.map(_.toString).mkString(",")
  }

  object SeqSQLValue {
    def wrapper[T]: JdbcMapper[Seq[SQLArg[T]]] = new JdbcMapper[Seq[SQLArg[T]]] {
      override def wrapNotNull(value: Seq[SQLArg[T]], dialect: JdbcDialect): String =
        value.map(_.toString(dialect)).mkString(",")

      /**
       * the jdbc information for T
       *
       * @return
       */
      override def jdbcType: JdbcType = throw new UnsupportedOperationException

      /**
       * wrap value of type T into [[PreparedStatement]]
       *
       * @param statement      PreparedStatement
       * @param parameterIndex index in the sql for the statement, started by 1
       * @param value          value
       */
      override def wrap(statement: PreparedStatement, parameterIndex: Int, value: Seq[SQLArg[T]]): Unit = {
        var i = parameterIndex
        value.foreach {
          v =>
            v.wrapper.wrap(statement, i, getOrNull(v.value))
            i += 1
        }
      }
    }
  }

  object SQLIdentifier {
    def apply(value: String) = new SQLIdentifier(value)
  }


  object SQLArg {
    /**
     * SQL表达式中表示表达式的参数容器: wrap为字符串时不会进行任何操作
     *
     * @param expr
     * @return
     */
    def apply(expr: String): SQLArg[String] = new SQLArg[String] {
      require(expr != null && expr.nonEmpty)

      override def value: Option[String] = Some(expr)

      override def wrapper: JdbcMapper[String] = Mapper.jdbcMapper4String

      override def toString(dialect: JdbcDialect): String = expr
    }


    implicit def wrap[T: JdbcMapper](t: T): SQLArg[T] = new SQLValue[T](t)(implicitly[JdbcMapper[T]])

    implicit def wrap[T: JdbcMapper](t: Seq[T]): Seq[SQLArg[T]] = t.map { value => new SQLValue[T](value) }

    implicit def wrap[T: JdbcMapper](t: Option[T]): SQLArg[T] = new SQLValue[T](t)
  }


  /**
   * 一个批量写入的工具函数, 后面会稍加改造
   * @param conn
   * @param table
   * @param values
   * @param batch
   * @tparam T
   */
  def write[T: JdbcMapper](conn: Connection, table: String, values: TraversableOnce[T], batch: Int): Unit = {
    val mapper = implicitly[JdbcMapper[T]]
    val fieldNames = Mapper.getStructOf[T].map(s => SQLIdentifier(s.fieldName))
    val placeHolder = scala.Array.fill(fieldNames.length)("?").mkString(",")
    val sql = sql""" insert into ${SQLIdentifier(table)} """ + sql""" (${fieldNames:_*}) """ +
      s"values($placeHolder);"
    val statement = conn.prepareStatement(sql.sql)

    var i = 0
    values.foreach {
      value =>
        mapper.wrap(statement, 1, value)
        statement.addBatch()
        i += 1
        if(i > batch) {
          statement.executeBatch()
          i = 0
        }
    }

    if(i > 0)
      statement.executeBatch()
  }



  /* trait StringWrapper[T] {
     def wrap(value: T, dialect: JdbcDialect): String

     def unwrap(string: String, dialect: JdbcDialect): T = throw new UnsupportedOperationException()
   }

   trait StatementWrapper[T] {
     def wrap(statement: PreparedStatement, value: T): Unit = wrap(statement, 1, value)

     // 用于HList的wrap, offset, 该复合类型起始位置的indexId
     private[jdbc] def wrapReverse(statement: PreparedStatement, value: T, size: Int, offset: Int = 1): Unit = wrap(statement, size - position + offset, value)

     def wrap(statement: PreparedStatement, index: Int, value: T): Unit

     def unwrap(resultSet: ResultSet): T = unwrap(resultSet, 1)

     def unwrap(resultSet: ResultSet, columnLabel: String): T = resultSet.getObject(columnLabel).asInstanceOf[T]

     def unwrap(resultSet: ResultSet, columnIndex: Int): T = {
       val value = resultSet.getObject(columnIndex)
       try {
         value.asInstanceOf[T]
       } catch {
         case e: ClassCastException =>
           throw new ClassCastException(s"column index '$columnIndex' cannot be casted as target type: ${e.getMessage}.")
         case e: Throwable =>
           throw e
       }
     }

     // 用于HList的wrap
     private[jdbc] def unwrapReverse(resultSet: ResultSet, size: Int, offset: Int = 1): T = unwrap(resultSet, size - position + offset)

     def position: Int = 1
   }


   /**
    * Jdbc和类型T元素之间的交互
    *
    * @tparam T
    */
   trait JdbcWrapper[T] extends StringWrapper[T] with StatementWrapper[T]

   /**
    * 原子类型的wrapper
    */
   trait AtomicWrapper {
     /*
     created by code:
     ----
     def code(typeName: String) = {
       s"""    implicit def jdbcWrapper4$typeName: JdbcWrapper[$typeName] = create4Primitive[$typeName] (
          |      (statement: PreparedStatement, index: Int, value: $typeName) => statement.set$typeName(index, value),
          |      (resultSet: ResultSet, index: Int) => resultSet.get$typeName(index)
          |    )""".stripMargin
     }

     for(each <- Seq("Boolean", "Byte", "Short", "Int", "Long", "Float", "Double")) {
       println()
       println(code(each))
     }
      */
     private def create4Primitive[T](
                                      wrp: (PreparedStatement, Int, T) => Unit,
                                      unWrp: (ResultSet, Int) => T
                                    ): JdbcWrapper[T] = new JdbcWrapper[T] {
       override def wrap(value: T, dialect: JdbcDialect): String = value.toString

       override def wrap(statement: PreparedStatement, index: Int, value: T): Unit =
         wrp(statement, index, value)

       override def unwrap(resultSet: ResultSet, columnIndex: Int): T = unWrp(resultSet, columnIndex)
     }


     implicit def jdbcWrapper4Boolean: JdbcWrapper[Boolean] = create4Primitive[Boolean](
       (statement: PreparedStatement, index: Int, value: Boolean) => statement.setBoolean(index, value),
       (resultSet: ResultSet, index: Int) => resultSet.getBoolean(index)
     )

     implicit def jdbcWrapper4Byte: JdbcWrapper[Byte] = create4Primitive[Byte](
       (statement: PreparedStatement, index: Int, value: Byte) => statement.setByte(index, value),
       (resultSet: ResultSet, index: Int) => resultSet.getByte(index)
     )

     implicit def jdbcWrapper4Short: JdbcWrapper[Short] = create4Primitive[Short](
       (statement: PreparedStatement, index: Int, value: Short) => statement.setShort(index, value),
       (resultSet: ResultSet, index: Int) => resultSet.getShort(index)
     )

     implicit def jdbcWrapper4Int: JdbcWrapper[Int] = create4Primitive[Int](
       (statement: PreparedStatement, index: Int, value: Int) => statement.setInt(index, value),
       (resultSet: ResultSet, index: Int) => resultSet.getInt(index)
     )

     implicit def jdbcWrapper4Long: JdbcWrapper[Long] = create4Primitive[Long](
       (statement: PreparedStatement, index: Int, value: Long) => statement.setLong(index, value),
       (resultSet: ResultSet, index: Int) => resultSet.getLong(index)
     )

     implicit def jdbcWrapper4Float: JdbcWrapper[Float] = create4Primitive[Float](
       (statement: PreparedStatement, index: Int, value: Float) => statement.setFloat(index, value),
       (resultSet: ResultSet, index: Int) => resultSet.getFloat(index)
     )

     implicit def jdbcWrapper4Double: JdbcWrapper[Double] = create4Primitive[Double](
       (statement: PreparedStatement, index: Int, value: Double) => statement.setDouble(index, value),
       (resultSet: ResultSet, index: Int) => resultSet.getDouble(index)
     )

     // todo: big decimal

     implicit def jdbcWrapper4String: JdbcWrapper[String] = new JdbcWrapper[String] {
       override def wrap(value: String, dialect: JdbcDialect): String = {
         // 字符串中出现单引号时将单引号double, 并将字符串以单引号括起来
         "'" + value.replaceAll("'", "''") + "'"
       }

       override def wrap(statement: PreparedStatement, index: Int, value: String): Unit = {
         statement.setString(index, value)
       }

       override def unwrap(resultSet: ResultSet, columnIndex: Int): String = resultSet.getString(columnIndex)

     }

     implicit def jdbcWrapper4Date: JdbcWrapper[Date] = new JdbcWrapper[Date] {
       override def wrap(value: Date, dialect: JdbcDialect): String = {
         // 字符串中出现单引号时将单引号double, 并将字符串以单引号括起来
         "'" + value.toString + "'"
       }

       override def wrap(statement: PreparedStatement, index: Int, value: Date): Unit = {
         statement.setDate(index, value)
       }

       override def unwrap(resultSet: ResultSet, columnIndex: Int): Date = resultSet.getDate(columnIndex)
     }

     implicit def jdbcWrapper4Timestamp: JdbcWrapper[Timestamp] = new JdbcWrapper[Timestamp] {
       override def wrap(value: Timestamp, dialect: JdbcDialect): String = {
         // 字符串中出现单引号时将单引号double, 并将字符串以单引号括起来
         "'" + value.toString + "'"
       }

       override def wrap(statement: PreparedStatement, index: Int, value: Timestamp): Unit = {
         statement.setTimestamp(index, value)
       }

       override def unwrap(resultSet: ResultSet, columnIndex: Int): Timestamp = resultSet.getTimestamp(columnIndex)
     }
   }

   implicit def jdbcWrapper4Time: JdbcWrapper[Time] = new JdbcWrapper[Time] {
     override def wrap(value: Time, dialect: JdbcDialect): String = {
       // 字符串中出现单引号时将单引号double, 并将字符串以单引号括起来
       "'" + value.toString + "'"
     }

     override def wrap(statement: PreparedStatement, index: Int, value: Time): Unit = {
       statement.setTime(index, value)
     }

     override def unwrap(resultSet: ResultSet, columnIndex: Int): Time = resultSet.getTime(columnIndex)
   }


   /**
    * 复合类型的wrapper
    */
   trait UnAtomicWrapper {
     implicit def wrapper4HNil: JdbcWrapper[HNil] = new JdbcWrapper[HNil] {
       override def wrap(value: HNil, dialect: JdbcDialect): String = ""

       override def wrap(statement: PreparedStatement, index: Int, value: HNil): Unit = {}

       override def unwrapReverse(resultSet: ResultSet, size: Int, offset: Int = 1): HNil = {
         HNil
       }

       override def position: Int = 0
     }

     implicit def wrapper4HList[T, HL <: HList](implicit headWrapper: JdbcWrapper[T],
                                                restWrapper: JdbcWrapper[HL]): JdbcWrapper[T :: HL] = new JdbcWrapper[::[T, HL]] {
       override def wrap(value: T :: HL, dialect: JdbcDialect): String = {
         if (value.tail == HNil)
           headWrapper.wrap(value.head, dialect)
         else
           headWrapper.wrap(value.head, dialect) + ", " + restWrapper.wrap(value.tail, dialect)
       }

       override def wrapReverse(statement: PreparedStatement, value: T :: HL, size: Int, offset: Int = 1): Unit = {
         headWrapper.wrap(statement, size - position + offset, value.head)
         restWrapper.wrapReverse(statement, value.tail, size, offset)
       }

       override def wrap(statement: PreparedStatement, value: T :: HL): Unit = {
         wrapReverse(statement, value, position)
       }

       override def wrap(statement: PreparedStatement, index: Int, value: T :: HL): Unit = {
         wrapReverse(statement, value, position, index)
       }


       override def unwrap(resultSet: ResultSet, columnIndex: Int): T :: HL = {
         unwrapReverse(resultSet, position, columnIndex)
       }

       override def unwrapReverse(resultSet: ResultSet, size: Int, offset: Int): T :: HL = {
         val head = headWrapper.unwrap(resultSet, size - position + offset)
         val rest = restWrapper.unwrapReverse(resultSet, size, offset)
         head :: rest
       }

       override def position: Int = restWrapper.position + 1
     }

     /*
     包括:
     Product
     case class
     核心字段全在构造参数中的class
      */
     implicit def wrapper4ClassCan2HList[T, HL <: HList](implicit gen: Generic.Aux[T, HL], hListWrapper: JdbcWrapper[HL]): JdbcWrapper[T] = new JdbcWrapper[T] {
       override def wrap(value: T, dialect: JdbcDialect): String =
         hListWrapper.wrap(gen.to(value), dialect)

       override def wrap(statement: PreparedStatement, index: Int, value: T): Unit = {
         hListWrapper.wrap(statement, index, gen.to(value))
       }

       override def unwrap(resultSet: ResultSet, columnIndex: Int): T =
       //        try{
       {
         //        println(hListWrapper.unwrap(resultSet, columnIndex))
         //        println(hListWrapper.unwrap(resultSet, columnIndex).getClass.getSimpleName)
         gen.from(hListWrapper.unwrap(resultSet, columnIndex))

       }

       //      } catch {
       //        case e: Throwable =>
       //          throw new SQLDataException(
       //            // todo: 尝试给出更详细的信息
       //            s"can not unwrap values as class with fields from result set with offset index $columnIndex.",
       //            e
       //          )
       //      }

       override def position: Int = hListWrapper.position

     }

     // todo: 目前Seq[T] 视为装载SQL参数的容器, 而不是作为一个整体(数组类型)处理的, 后期可以通过环境变量配置是否将Seq视为整体.
     implicit def wrapper4Seq[T](implicit ev: JdbcWrapper[T]): JdbcWrapper[Seq[T]] = new JdbcWrapper[Seq[T]] {
       override def wrap(value: Seq[T], dialect: JdbcDialect): String = {
         value.map {
           v =>
             ev.wrap(v, dialect)
         }.mkString(",")
       }

       override def wrap(statement: PreparedStatement, index: Int, value: Seq[T]): Unit =
         value.foreach {
           v =>
             ev.wrap(statement, index, v)
         }
     }

     implicit def wrapper4SQLValue[T](implicit ev: JdbcWrapper[T]): JdbcWrapper[SQLValue[T]] = new JdbcWrapper[SQLValue[T]] {
       override def wrap(statement: PreparedStatement, index: Int, value: SQLValue[T]): Unit = {
         implicitly[JdbcWrapper[T]].wrap(statement, index, value.value.get) // todo: 考虑下null值
       }

       override def wrap(value: SQLValue[T], dialect: JdbcDialect): String = {
         implicitly[JdbcWrapper[T]].wrap(value.value.get, dialect)
       }
     }

     implicit def wrapper4SeqSQLValue[F: JdbcWrapper]: JdbcWrapper[Seq[SQLValue[F]]] =
       wrapper4Seq[SQLValue[F]](implicitly[JdbcWrapper[SQLValue[F]]])
   }


   object JdbcWrapper extends AtomicWrapper with UnAtomicWrapper
 */

}
