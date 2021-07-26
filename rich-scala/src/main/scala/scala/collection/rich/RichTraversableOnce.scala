package scala.collection.rich

import scala.reflect.ClassTag


class RichTraversableOnce[+A](values: TraversableOnce[A])
                             (implicit kt: ClassTag[A], ord: Ordering[A] = null) extends Serializable {
  def argmax[B >: A](implicit cmp: Ordering[B]): Int = {
    if (values.isEmpty)
      throw new UnsupportedOperationException("empty.max")
    var maxValue: B = null.asInstanceOf[B]
    var maxIndex = -1
    var i = 0
    values.foreach {
      value =>
        if(maxIndex < 0 || cmp.lt(maxValue, value)) { // update
          maxValue = value
          maxIndex = i
        }
        i += 1
    }
    if(maxIndex < 0)
      throw new UnsupportedOperationException("empty.max")
    else
      maxIndex
  }

  def maxAndArg[B >: A](implicit cmp: Ordering[B]): (B, Int) = {
    if (values.isEmpty)
      throw new UnsupportedOperationException("empty.max")
    var maxValue: B = null.asInstanceOf[B]
    var maxIndex = -1
    var i = 0
    values.foreach {
      value =>
        if(maxIndex < 0 || cmp.lt(maxValue, value)) { // update
          maxValue = value
          maxIndex = i
        }
        i += 1
    }
    if(maxIndex < 0)
      throw new UnsupportedOperationException("empty.max")
    else
      (maxValue, maxIndex)
  }

  def top[B >: A](n: Int)(implicit cmp: Ordering[B]): Array[B] = throw new UnsupportedOperationException()

  def argtop[B >: A](n: Int)(implicit cmp: Ordering[B]): Array[Int] = throw new UnsupportedOperationException()

  def topAndArg[B >: A](n: Int)(implicit cmp: Ordering[B]): Array[(B, Int)] = throw new UnsupportedOperationException()

  // zip fun
}

