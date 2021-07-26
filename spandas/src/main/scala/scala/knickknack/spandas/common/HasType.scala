package scala.knickknack.spandas.common

import scala.collection.SeqLike

/**
 * 类型操作
 * @tparam Repr 本身的类型, the type of the actual collection containing the elements
 */
trait HasType[+Repr] {
  def dType(): String

  def asType[A]: Repr
}
