//package scala.knickknack.spandas
//
//import scala.reflect.runtime.universe.TypeTag
//
//abstract class Mapper[T: TypeTag] {
//  def dType: String
//}
//
//case class Box[I](value: I)
//case class ArrayBox[I](value: Array[I])
//
//abstract class IndexMapper[T: TypeTag] extends Mapper[T] {
//  override def dType: String
//
//  def indexIn(idx: T, valuesWithIndex: Map[T, Int]): Int = valuesWithIndex(idx)
//}
//
//object Mappers {
//}
