package scala.knickknack.spandas.common

import scala.knickknack.spandas.common.ImmutableSeq.SliceIterator


trait Slice extends SliceIterator {

  import Slice.formatSide

  def from: Option[Int] = None

  def to: Option[Int] = None

  def step: Int = 1

  require(step > 0, "the step of slice must be positive")

  def range(size: Int): Range =
    Range(scala.math.max(formatSide(from, size, 0), 0), scala.math.min(formatSide(to, size, size), size))

  def iterator(size: Int): Iterator[Int] = range(size).iterator
}

class RangeSlice(f: Int, t: Int, override val step: Int = 1) extends Slice {
  override def from: Option[Int] = Some(f)

  override def to: Option[Int] = Some(t)
}

object Slice {
  def apply(f: Int, t: Int) = new RangeSlice(f, t)

  def apply(f: Int, t: Int, step: Int) = new RangeSlice(f, t, step)

  /**
   * 判定范围[-size, size)是否正确
   * @param index
   * @param size
   * @return
   */
  def isIndexValid(index: Int, size: Int): Boolean = index >= -size && index < size

  /**
   *
   * @param index
   * @param size
   * @return 当超出界限[-size, size)时返回-1, 否则返回合法的索引值
   */
  def formatSide(index: Int, size: Int): Int = {
    if (!isIndexValid(index, size))
      -1
    else if (index < 0)
      index + size
    else
      index
  }

  // todo: until时inclusive是否要考虑以下?
  def formatSide(side: Option[Int], size: Int, default: Int): Int =
    if (side.isEmpty)
      default
    else
      formatSide(side.get, size)

}

class ::: extends Slice

case object ::: extends ::: {
  def to(t: Int): Slice = new Slice {
    override def to: Option[Int] = Some(t)
  }
}
