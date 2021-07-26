package scala.knickknack

import scala.knickknack.spandas.common.{:::, Slice}

package object spandas {
  /**
   * 逻辑运算的快捷方式
   */
  val all = functions.all
  val any = functions.any

  /**
   * 隐式转换 for Slice:
   * i:Int to :::
   *
   * @param int
   */
  implicit class IntCanToTripleColonImpl(int: Int) {
    def to(t: :::): Slice = new Slice {
      override def from: Option[Int] = Some(int)
    }
  }

  /**
   * 隐式转换 for Slice
   * range: Range => Slice
   *
   * @param range
   * @return
   */
  implicit def range_2_slice_impl(range: Range): Slice = Slice(range.start, range.end, range.step)


  //  val DataFrame = frame.DataFrame
  //
  //  val Series = series.Series
  //
  //  val Row = frame.Row
  //
  //
  //  implicit object StringIndexMapper extends IndexMapper[String] {
  //    override def dType: String = "string"
  //  }
  //
  //  implicit object NothingIndexMapper extends IndexMapper[Nothing] {
  //    override def dType: String = "nothing"
  //  }
  //
  //  implicit object IntIndexMapper extends IndexMapper[Int] {
  //    override def dType: String = "int"
  //
  //    override def indexIn(idx: Int, valuesWithIndex: Map[Int, Int]): Int =
  //      if (valuesWithIndex == null) { // Int有两种可能: 1)本身作为index, 2)以索引号作为Index
  //        idx
  //      } else
  //        valuesWithIndex(idx)
  //  }
  //
  //
  //  implicit def il[I](value: I) = Box(value)
  //  implicit def il4array[I](value: Array[I]) = ArrayBox(value)

}
