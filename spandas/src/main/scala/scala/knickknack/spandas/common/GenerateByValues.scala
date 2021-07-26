package scala.knickknack.spandas.common

/**
 * 拥有序列作为value的接口
 *
 * @tparam T
 */
trait GenerateByValues[T] extends Serializable {
  /**
   * 数据类型
   *
   * @param m 防止类型擦除
   * @return
   */
  def dType(implicit m: Manifest[T]): String = m.runtimeClass.getSimpleName.toLowerCase()

  /**
   * values, 主要的值
   *
   * @return
   */
  def values: Seq[T]

  /**
   * 值所拥有的迭代器
   *
   * @return
   */
  def iterator: Iterator[T] = values.iterator

  def apply(i: Int): T = values(i)

  /**
   * 所有元素是否相等
   *
   * @param that
   * @tparam B
   * @return
   */
  def sameElements[B >: T](that: GenerateByValues[B]): Boolean = {
    val these = this.iterator
    val those = that.iterator
    while (these.hasNext && those.hasNext)
      if (these.next != those.next)
        return false

    !these.hasNext && !those.hasNext
  }

  def canEqual(obj: Any): Boolean = true

  override def equals(obj: Any): Boolean = obj match {
    case that: GenerateByValues[Any] => (this canEqual that) && (this sameElements that)
    case _ => false
  }

  override lazy val hashCode: Int = scala.util.hashing.MurmurHash3.seqHash(values)

  override def toString: String = values.mkString("[", ",", "]") // todo: truncate
}
