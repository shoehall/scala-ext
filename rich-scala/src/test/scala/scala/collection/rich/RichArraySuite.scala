package scala.collection.rich

import org.scalatest.FunSuite

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import scala.collection.mutable.WrappedArray
import scala.reflect.ClassTag
trait Slice {
  def iterator: Iterator[Int]
}

class RichArraySuite extends FunSuite {
  test("测试Array可以以Iterator和TransverableOnce的形式扩展其方法") {
    val values = Array(1, 2, 3, 4, 5)
    assert(values.argmax == 4)

    implicit class ArraySlice[T: ClassTag](value: Array[T]) {
      def apply(slice: Slice): Array[T] = {
        val bf = implicitly[CanBuildFrom[mutable.WrappedArray[T], T, mutable.WrappedArray[T]]]
        def builder = { // extracted to keep method size under 35 bytes, so that it can be JIT-inlined
          val b = bf(value)
          b.sizeHint(value)
          b
        }
        val b = builder
        for (i <- slice.iterator) b += value(i)
        b.result.array
      }
    }

    val intArray = Array(1, 2, 3)
    intArray(new Slice {
      override def iterator: Iterator[Int] = Array(2, 1).iterator
    }).foreach(println)

  }


  test("新建一个类可以用Seq[Int]类型索引") {
    implicit class ArraySlice[T: ClassTag](value: Array[T]) {
      def apply(slice: Slice): Array[T] = {
        val bf = implicitly[CanBuildFrom[mutable.WrappedArray[T], T, mutable.WrappedArray[T]]]
        def builder = { // extracted to keep method size under 35 bytes, so that it can be JIT-inlined
          val b = bf(value)
          b.sizeHint(value)
          b
        }
        val b = builder
        for (i <- slice.iterator) b += value(i)
        b.result.array
      }
    }

    trait CanTransform2Slice[T] {
      def transform(value: Seq[T]): Slice
    }

    class Tensor(value: Array[Double]) {
      def apply(slice: Slice): Tensor = new Tensor(value(slice))
      def apply[T](slice: Seq[T])(implicit tf: CanTransform2Slice[T]): Tensor =
        new Tensor(value(tf.transform(slice)))

    }

    object Tensor {
      implicit def seq_int_to_slice: CanTransform2Slice[Int] = new CanTransform2Slice[Int] {
        override def transform(value: Seq[Int]): Slice = new Slice {
          override def iterator: Iterator[Int] = value.iterator
        }
      }
    }

    val t = new Tensor(Array(0.1, 0.3, 0.4))


  }


}
