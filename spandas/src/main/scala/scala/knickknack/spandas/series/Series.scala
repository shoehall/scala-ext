//package scala.knickknack.spandas.series
//
//import scala.collection.TraversableOnce
//import scala.knickknack.spandas.{ArrayBox, Box, IndexMapper, Mapper}
//import scala.knickknack.spandas.index.Index
//import scala.reflect.ClassTag
//import scala.reflect.runtime.universe.TypeTag
//import scala.knickknack.spandas.Mappers._
//
//class Series[T: Mapper : TypeTag](val values: Array[T]) {
//  /* 数据类型 */
//  def dType(): Unit = implicitly[Mapper[T]].dType
//
//  /* 元素操作 */
//  def contains(value: T): Boolean = values contains value
//
//  def indexOf(value: T): Int = values.indexOf(value)
//
//  def apply(idx: Int): T = values(idx)
//
//  def apply(indexes: Array[Int]): Series[Int, T] = new Series[Int, T](indexes.map(idx => this.apply(idx)).asInstanceOf[Array[T]], new Index[Int](indexes))
//
//  def apply(indexes: Range): Series[Int, T] = {
//    val vv = new Array[T](indexes.length)
//    val idx = new Array[Int](indexes.length)
//    var cnt = 0
//    for (i <- indexes) {
//      idx(cnt) = i
//      vv(i) = apply(i)
//      cnt += 1
//    }
//    new Series[Int, T](vv, new Index[Int](idx))
//  }
//
//  def update(i: Int, x: T): Unit = values(i) = x
//
//  def update(i: Array[Int], x: T): Unit = for (each <- i) {
//    values(each) = x
//  }
//
//  def update(i: Range, x: T): Unit = for (each <- i) {
//    values(each) = x
//  }
//
//  /**
//   * 目前是不报错, 当idx和x数目不一致的时候仅作用于对应的部分
//   *
//   * @param idx
//   * @param x
//   */
//  def update(idx: Array[Int], x: TraversableOnce[T]): Unit = {
//    var cnt = 0
//    x.foreach {
//      value =>
//        if (cnt < idx.length) {
//          update(cnt, value)
//        }
//        cnt += 1
//    }
//  }
//
//  /* 统计操作 */
//  def length: Int = values.length
//
//  override def toString: String = values.mkString("[", ",", "]")
//
//}
//
//
////class Series[I: IndexMapper : TypeTag : ClassTag, T: Mapper : TypeTag : ClassTag](override val values: Array[T], val index: Index[I])
////  extends PrimitiveSeries(values) {
////
////  def apply(idx: I): T = values(index.indexOf(idx))
////
////  def apply(indexes: Array[I]): Series[I, T] = new Series[I, T](indexes.map(idx => this.apply(idx)), new Index[I](indexes))
////
////  def update(idx: Box[I], x: T): Unit = values(index.indexOf(idx.value)) = x
////
////  def update(idx: ArrayBox[I], x: T): Unit = for (each <- idx.value) {
////    update(each, x)
////  }
////
////  /**
////   * 目前是不报错, 当idx和x数目不一致的时候仅作用于对应的部分
////   *
////   * @param idx
////   * @param x
////   */
////  def update(idx: Array[I], x: TraversableOnce[T]): Unit = {
////    var cnt = 0
////    x.foreach {
////      value =>
////        if (cnt < idx.length) {
////          update(cnt, value)
////        }
////        cnt += 1
////    }
////  }
////}
//
//
//object Series {
//  def apply[V: Mapper : TypeTag : ClassTag](values: Array[V]) =
//    new PrimitiveSeries[V](values)
//
//  def apply[K: IndexMapper : TypeTag : ClassTag, V: Mapper : TypeTag : ClassTag](values: Array[V], index: Array[K]) =
//    new Series[K, V](values, new Index[K](index))
//}
//
