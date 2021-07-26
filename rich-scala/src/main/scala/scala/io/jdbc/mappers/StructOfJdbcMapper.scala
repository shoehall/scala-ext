package scala.io.jdbc.mappers

import scala.io.jdbc.{JdbcDialect, NoopDialect}

/**
 * JdbcMapper对应的结构信息
 */
trait StructOfJdbcMapper {
  def fieldName: String

  def wrapper: JdbcMapper[_]

  private[io] def concatFieldName(prefix: String): StructOfJdbcMapper

  private[io] val flatten: Seq[PrimitiveStruct]

  protected def concatFields(fieldNames: String*): String = {
    val validNames = fieldNames.filterNot(_ == null)

    if (validNames.nonEmpty)
      validNames.mkString("_")
    else
      null
  }

  def toString(jdbcDialect: JdbcDialect): String

  override def toString: String = toString(NoopDialect)
}

/**
 * 单个字段对应的类型, 没有嵌套结构
 *
 * @param fieldName 字段名
 * @param wrapper   对应的wrapper
 */
case class PrimitiveStruct(override val wrapper: JdbcMapper[_], override val fieldName: String = null) extends StructOfJdbcMapper {
  def this(fieldName: String) = this(null, fieldName)

  override val flatten: Seq[PrimitiveStruct] = Seq(this)

  override def concatFieldName(prefix: String): PrimitiveStruct = PrimitiveStruct(wrapper, concatFields(prefix, fieldName))

  override def toString(jdbcDialect: JdbcDialect): String = s"${jdbcDialect.quoteIdentifier(fieldName)} ${wrapper.jdbcType.toString(jdbcDialect)}"

  override def equals(obj: Any): Boolean = obj match {
    case primitiveStruct: PrimitiveStruct =>
      wrapper == primitiveStruct.wrapper && fieldName == primitiveStruct.fieldName
    case _ =>
      false
  }
}

/**
 * 嵌套类型, 主要支持coproduct类型(Product, sealed class like case class), 有嵌套结构
 *
 * @param fieldName 字段名
 * @param wrapper   对应类型的wrapper
 * @param children  嵌套的子类型
 */
case class CoproductStruct(override val wrapper: JdbcMapper[_],
                           override val fieldName: String,
                           children: Seq[StructOfJdbcMapper]) extends StructOfJdbcMapper {
  def this(wrapper: JdbcMapper[_], children: Seq[StructOfJdbcMapper]) = this(wrapper, null, children)

  override val flatten: Seq[PrimitiveStruct] = {
    children.flatMap(_.concatFieldName(fieldName).flatten)
  }

  override def concatFieldName(prefix: String): StructOfJdbcMapper =
    CoproductStruct(wrapper, concatFields(prefix, fieldName), children)

  override def toString(jdbcDialect: JdbcDialect): String = flatten.map(_.toString(jdbcDialect)).mkString(",")
}
