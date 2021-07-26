package scala.knickknack

import java.util.Random
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.util.Random

package object rld {
  def reservoirSampleAndCount[T: ClassTag](
                                            input: Iterator[T],
                                            k: Int,
                                            seed: Long = new java.util.Random().nextLong()
                                          )
  : (Array[T], Long) = {
    val reservoir = new Array[T](k)
    // Put the first k elements in the reservoir.
    var i = 0
    while (i < k && input.hasNext) {
      val item = input.next()
      reservoir(i) = item
      i += 1
    }

    // If we have consumed all the elements, return them. Otherwise do the replacement.
    if (i < k) {
      // If input size < k, trim the array to return only an array of input size.
      val trimReservoir = new Array[T](i)
      System.arraycopy(reservoir, 0, trimReservoir, 0, i)
      (trimReservoir, i)
    } else {
      // If input size > k, continue the sampling process.
      var l = i.toLong
      val rand = new XORShiftRandom(seed)
      while (input.hasNext) {
        val item = input.next()
        l += 1
        // There are k elements in the reservoir, and the l-th element has been
        // consumed. It should be chosen with probability k/l. The expression
        // below is a random long chosen uniformly from [0,l)
        val replacementIndex = (rand.nextDouble() * l).toLong
        if (replacementIndex < k) {
          reservoir(replacementIndex.toInt) = item
        }
      }
      (reservoir, l)
    }
  }


  def zipAndSort[K, T](values: Array[Iterator[(K, T)]])(implicit ordering: Ordering[K]): Iterator[(K, T)] =
    new Iterator[(K, T)] {
      private var value: Option[(K, T)] = None

      private val pool = new {
        val iterators: mutable.Buffer[Iterator[(K, T)]] = values.filter(_.hasNext).toBuffer
        val buffer: mutable.Buffer[(K, T)] = iterators.map(_.next())

        def findMinAndIndex(): ((K, T), Int) = {
          var min_k: K = null.asInstanceOf[K]
          var res: (K, T) = (null.asInstanceOf[K], null.asInstanceOf[T])
          var min_idx = 0
          var idx = 0

          for ((k, v) <- buffer) {
            if (idx <= 0) {
              min_k = k
              res = (k, v)

              min_idx = 0
            } else {
              if (ordering.lt(k, min_k)) {
                min_k = k
                res = (k, v)

                min_idx = idx
              }
            }

            idx += 1
          }

          (res, min_idx)
        }

        def hasNext: Boolean = buffer.nonEmpty

        def next(): (K, T) = {
          val (minValue, minIndex) = findMinAndIndex()

          if (iterators(minIndex).hasNext) {
            buffer(minIndex) = iterators(minIndex).next()
          } else {
            iterators.remove(minIndex)
            buffer.remove(minIndex)
          }

          minValue
        }
      }

      override def hasNext: Boolean = value.nonEmpty || pool.hasNext

      override def next(): (K, T) = if (value.nonEmpty) {
        value.get
      } else if (pool.hasNext) {
        val valuePop = pool.next()
        value = Some(valuePop)
        valuePop
      } else
        Iterator.empty.next()
    }


}
