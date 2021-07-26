package scala.collection.frame

import java.sql.{PreparedStatement, ResultSet}
import scala.io.jdbc.mappers.{Mapper, UnWrapper, Wrapper}

trait DataType {
  def name: String = this.getClass.getSimpleName
}

trait AtomicType extends DataType

class StringType extends AtomicType{
  override def name: String = "String"
}
object StringType extends StringType
trait NumericType extends AtomicType
class BooleanType extends AtomicType {
  override def name: String = "Bool"

}
object BooleanType extends BooleanType

class BinaryType extends NumericType
case object BinaryType extends BinaryType

class ShortType extends NumericType {
  override def name: String = "Short"
}
case object ShortType extends ShortType

class IntegerType extends NumericType {
  override def name: String = "Int"
}
case object IntegerType extends IntegerType

class LongType extends NumericType {
  override def name: String = "Long"
}
case object LongType extends LongType

class FloatType extends NumericType {
  override def name: String = "Float"
}
case object FloatType extends FloatType

class DoubleType extends NumericType {
  override def name: String = "Double"
}
case object DoubleType extends DoubleType

case class DecimalType(precision: Int, scale: Int) extends NumericType {
  override def name: String = s"Decimal($precision, $scale)"
}

class TimestampType extends NumericType {
  override def name: String = "Timestamp"
}
case object TimestampType extends TimestampType

class DateType extends NumericType {
  override def name: String = "Date"
}
case object DateType extends DateType

// todo: 自定义类型
trait UserDefinedType extends DataType {
  def jdbcTypeName: String
}