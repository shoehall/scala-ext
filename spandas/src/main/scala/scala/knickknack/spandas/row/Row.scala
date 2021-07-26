package scala.knickknack.spandas.row

import scala.collection.frame.Schema
import scala.knickknack.spandas.common.{CanBuild, Series}

trait Row extends Series[Any] {
  def schema: Schema

  def fieldIndex(columnName: String): Int = schema.fieldIndex(columnName)

  def get(columnIndex: Int): Any = apply(columnIndex)

  def getAs[T](columnIndex: Int): T = get(columnIndex).asInstanceOf[T]

  def getAs[T](columnName: String): T = getAs[T](fieldIndex(columnName))

  /**
   * 提取元素并转为某种类型, 用于类型之间的转换
   * @param columnIndex
   * @tparam T
   * @return
   */
  def castAs[T: CastAs](columnIndex: Int): T = implicitly[CastAs[T]].cast(get(columnIndex))

  def castAs[T: CastAs](columnName: String): T = castAs[T](fieldIndex(columnName))
}

trait CastAs[T] {
  def cast(value: Any): T
}

object CastAs {
  implicit def castAsString: CastAs[String] = new CastAs[String] {
    override def cast(value: Any): String = if(value == null) null else value.toString
  }

  implicit def castAsBoolean: CastAs[Boolean] = new CastAs[Boolean] {
    override def cast(value: Any): Boolean = if(value == null) null.asInstanceOf[Boolean] else value.toString.toBoolean
  }

  implicit def castAsInt: CastAs[Int] = new CastAs[Int] {
    override def cast(value: Any): Int =
      if(value == null)
        throw new NullPointerException("null could not cast as Int")
      else
        value.toString.toDouble.toInt
  }

}



object Row extends CanBuild {
  implicit def can_build_row_by[T] = new CanBuildBy[T, Row] {
    override def apply(v: T): Row = {
      throw new UnsupportedOperationException
    }
    override def apply(): Row = throw new UnsupportedOperationException
  }
}
