package scala.knickknack.spandas.common


import java.sql.Timestamp
import java.util.zip.ZipException
import scala.collection.generic.{CanBuildFrom, GenericCompanion, GenericTraversableTemplate, TraversableFactory}
import scala.collection.mutable.ListBuffer
import scala.collection.{Traversable, TraversableLike, mutable}
import scala.knickknack.spandas.common.ImmutableSeq.{SliceIterator, WrapIterator}
import scala.knickknack.spandas.common.Slice.{formatSide, isIndexValid}
import scala.knickknack.spandas.common.operators.{NegativeOps, PlusOps}

//trait ImmutableSeq[T] extends ImmutableSeqLike[T, ImmutableSeq[T]]
trait ImmutableSeq[T] extends Traversable[T]
  with GenerateByValues[T]
  with GenericTraversableTemplate[T, ImmutableSeq]
  with TraversableLike[T, ImmutableSeq[T]]
  /*with ImmutableOps[ImmutableSeq[T]]*/ {
  def values: Seq[T]

  override def companion: GenericCompanion[ImmutableSeq] = ImmutableSeq

  override def foreach[U](f: T => U): Unit = values.foreach(f)

  /**
   * 按位置索引元素
   * ----
   * 1)支持负数索引
   * 2)越界会抛出异常
   *
   * @param i
   * @return
   */
  override def apply(i: Int): T = {
    if (!isIndexValid(i, size))
      throw new IndexOutOfBoundsException(s"The index $i is out of bound")
    values(formatSide(i, size))
  }

  def length: Int = size

  /*  def apply[That](range: Range)(implicit bf: CanBuildFrom[This, T, That]): That = {
      val b = bf(repr)
      b.sizeHint(range.size)
      for (x <- range) b += apply(x)
      b.result
    }

    def apply[That](range: SeqInt)(implicit bf: CanBuildFrom[This, T, That]): That = {
      val b = bf(repr)
      b.sizeHint(range.value.size)
      for (x <- range.value) b += apply(x)
      b.result
    }

    def apply[That](range: SeqBoolean)(implicit bf: CanBuildFrom[This, T, That]): That = {
      val b = bf(repr)
      var i = 0
      for (x <- range.value) {
        if (x)
          b += apply(i)
        i += 1
      }
      b.result
    }*/

  //  def fill[B, That](b0: B)(implicit bf: CanBuildFrom[This, B, That]): That = {
  //    val b = bf(repr)
  //    b.sizeHint(size)
  //    foreach {
  //      _ =>
  //        b += b0
  //    }
  //    b.result
  //  }

  /**
   * 类似python的slice功能
   * ----
   * 1)支持负数作为索引
   * 2)支持切片
   * 3)支持
   * 1)越界后不会抛出异常
   *
   * @param slice
   * @return
   */
  def apply[That](slice: SliceIterator)(implicit bf: CanBuildFrom[Self, T, That]): That = {
    val b = bf(repr)
    for (x <- slice.iterator(size)) {
      b += apply(x)
    }
    b.result
  }

  /*
  一元运算
    | types             | unary fun  |
----|-------------------|------------|
+   | numeric, string   |  self      |
-   | numeric           | negative   |
!   | bool              |  not       |
   */

  def unary_![That](implicit bf: CanBuildFrom[Self, Boolean, That], ev: T =:= Boolean): That = map(!_)(bf)

  def unary_-[R, That](implicit ev: NegativeOps.UImpl[T, R], bf: CanBuildFrom[Self, R, That]): That = map(ev.apply)(bf)

  def unary_+[That](implicit bf: CanBuildFrom[Self, T, That]): That = map(v => v)

  /*
  二元运算
   */
  def zipMap[PT, Elem, R, That](other: WrapIterator[PT, Elem])(f: (T, Elem) => R)(implicit bf: CanBuildFrom[Self, R, That]): That = {
    val b = bf(repr)
    b.sizeHint(length)

    var i = 0
    other.iterator(size).foreach {
      x =>
        if (i >= length)
          throw new ZipException("Lengths of two traversable values must match for binary function.")
        b += f(apply(i), x)
        i += 1
    }
    if (i < length)
      throw new ZipException("Lengths of two traversable values must match for binary function.")
    b.result
  }

  // 实现序列之间的等于方法, 返回一个新的序列
  def ===[PT, Elem, That](other: WrapIterator[PT, Elem])(implicit bf: CanBuildFrom[Self, Boolean, That]): That =
    zipMap[PT, Elem, Boolean, That](other) { case (v1, v2) => v1 == v2 }(bf)

  def zip[PT, Elem, That](other: WrapIterator[PT, Elem])(implicit bf: CanBuildFrom[Self, (T, Elem), That]): That =
    zipMap[PT, Elem, (T, Elem), That](other) { case (v1, v2) => (v1, v2) }(bf)

  def and[PT, That](other: WrapIterator[PT, Boolean])(implicit bf: CanBuildFrom[Self, Boolean, That], ev: T =:= Boolean): That =
    zipMap[PT, Boolean, Boolean, That](other) { case (v1, v2) => v1 && v2 }(bf)

  def &[PT, That](other: WrapIterator[PT, Boolean])(implicit bf: CanBuildFrom[Self, Boolean, That], ev: T =:= Boolean): That =
    and[PT, That](other)(bf, ev)

  def &&[PT, That](other: WrapIterator[PT, Boolean])(implicit bf: CanBuildFrom[Self, Boolean, That], ev: T =:= Boolean): That =
    and[PT, That](other)(bf, ev)

  def or[PT, That](other: WrapIterator[PT, Boolean])(implicit bf: CanBuildFrom[Self, Boolean, That], ev: T =:= Boolean): That =
    zipMap[PT, Boolean, Boolean, That](other) { case (v1, v2) => v1 || v2 }(bf)

  def |[PT, That](other: WrapIterator[PT, Boolean])(implicit bf: CanBuildFrom[Self, Boolean, That], ev: T =:= Boolean): That =
    or[PT, That](other)(bf, ev)

  def ||[PT, That](other: WrapIterator[PT, Boolean])(implicit bf: CanBuildFrom[Self, Boolean, That], ev: T =:= Boolean): That =
    or[PT, That](other)(bf, ev)

  def xor[PT, That](other: WrapIterator[PT, Boolean])(implicit bf: CanBuildFrom[Self, Boolean, That], ev: T =:= Boolean): That =
    zipMap[PT, Boolean, Boolean, That](other) { case (v1, v2) => v1 ^ v2 }(bf)

  def ^[PT, That](other: WrapIterator[PT, Boolean])(implicit bf: CanBuildFrom[Self, Boolean, That], ev: T =:= Boolean): That =
    xor[PT, That](other)(bf, ev)

  def +[PT, Elem, R, That](other: WrapIterator[PT, Elem])(implicit canOp: PlusOps.UImpl2[T, Elem, R], bf: CanBuildFrom[Self, R, That]): That =
    zipMap[PT, Elem, R, That](other){ case (v1, v2) => canOp(v1, v2) }(bf)


}


object ImmutableSeq extends TraversableFactory[ImmutableSeq] with UFunc {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, ImmutableSeq[A]] = new GenericCanBuildFrom[A]

  // 此处也更新了newBuilder方法
  def newBuilder[A] = new ListBuffer[A] mapResult (x => new ImmutableSeq[A] {
    override def values: Seq[A] = x
  })

  /**
   * 类似于python中的slice类型
   */
  trait SliceIterator extends Serializable {
    def iterator(size: Int): Iterator[Int]
  }

  implicit def seq_int_to_slice(value: Seq[Int]) = new SliceIterator {
    override def iterator(size: Int): Iterator[Int] = value.iterator.map(i => formatSide(i, size))
  }

  implicit def seq_bool_to_slice(value: Seq[Boolean]) = new SliceIterator {
    override def iterator(size: Int): Iterator[Int] = new Iterator[Int] {
      private val iterator = value.iterator
      private var nextIndex = -1

      private var findNextTrue: Boolean = false

      override def hasNext: Boolean = {
        while (!findNextTrue && iterator.hasNext) {

          nextIndex += 1
          if (iterator.next())
            findNextTrue = true
        }
        findNextTrue
      }

      override def next(): Int =
        if (hasNext) {
          findNextTrue = false
          nextIndex
        } else
          Iterator.empty.next()
    }
  }


  trait WrapIterator[PR, Elem] {
    def iterator(size: Int): TraversableOnce[Elem]
  }

  implicit def array_2_WrapIterator[T](value: Array[T]) = new WrapIterator[Array[T], T] {
    override def iterator(size: Int): TraversableOnce[T] =
      value.iterator
  }

  implicit def traversableOnce_2_WrapIterator[T](value: TraversableOnce[T]) = new WrapIterator[Array[T], T] {
    override def iterator(size: Int): TraversableOnce[T] =
      value
  }

  implicit def any_2_WrapIterator[T](value: T) = new WrapIterator[T, T] {
    override def iterator(size: Int): TraversableOnce[T] = Iterator.fill(size)(value)
  }


}
