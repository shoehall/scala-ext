package scala.knickknack.rld.rdd

import java.util
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.knickknack.rld.rdd.cache.ShuffleReader
import scala.knickknack.rld.{Session, reservoirSampleAndCount}
import scala.reflect.{ClassTag, classTag}
import scala.util.hashing.byteswap32

/**
 *
 */
class MapRLD[T: ClassTag, U: ClassTag](session: Session, f: T => U, deps: RLD[T]) extends RLD[U](session, Seq(deps)) {
  override def makeIterator(): Iterator[U] = {
    deps.iterator().map(f)
  }
}

case class CollectionRLD[T: ClassTag](session: Session, collection: Seq[T]) extends RLD[T](session, Nil) {
  override def makeIterator(): Iterator[T] = collection.iterator
}

/*
沿用了spark的sort shuffle逻辑
 */
class SortPartitionRLD[K: ClassTag: Ordering, V: ClassTag](
                                                  session: Session,
                                                  deps: RLD[(K, V)],
                                                  sampleSize: Int = 1000,
                                                  partitions: Int = 10,
                                                  ascending: Boolean = true
                                                ) extends RLD[(K, V)](session, Seq(deps)) {
  private def determinePartitioner: Array[K] = {
    // 蓄水池抽样, 等比例抽1000个
    val (sketched, numItems) = reservoirSampleAndCount(deps.compute().map(_._1), sampleSize, 1123L)

    if (numItems == 0L) {
      Array.empty[K]
    } else {
      // If a partition contains much more than the average number of items, we re-sample from it
      // to ensure that enough items are collected from that partition.
      val candidates = ArrayBuffer.empty[(K, Float)]

      val sample = sketched
      val n = numItems

      // The weight is 1 over the sampling probability.
      val weight = (n.toDouble / sample.length).toFloat
      for (key <- sample) {
        candidates += ((key, weight))
      }

      SortPartitionRLD.determineBounds(candidates, math.min(partitions, candidates.size))
    }
  }

  val rangeBounds: Array[K] = determinePartitioner
  private val ordering = implicitly[Ordering[K]]

  private val binarySearch = SortPartitionRLD.makeBinarySearch[K]

  def getPartition(key: Any): Int = {
    val k = key.asInstanceOf[K]
    var partition = 0
    if (rangeBounds.length <= 128) {
      // If we have less than 128 partitions naive search
      while (partition < rangeBounds.length && ordering.gt(k, rangeBounds(partition))) {
        partition += 1
      }
    } else {
      // Determine which binary search method to use only once.
      partition = binarySearch(rangeBounds, k)
      // binarySearch either returns the match location or -[insertion point]-1
      if (partition < 0) {
        partition = -partition-1
      }
      if (partition > rangeBounds.length) {
        partition = rangeBounds.length
      }
    }

    if (ascending) {
      partition
    } else {
      rangeBounds.length - partition
    }
  }

  private def write(): Unit = {
    val writer = session.getWriter
    deps.compute().foreach {
      value =>
        writer.write(value, getPartition(value))
    }
  }

  private def read: Iterator[(K, V)] = {
    new ShuffleReader[K, V](session, "", true).read
  }

  override def makeIterator(): Iterator[(K, V)] = {
    write()
    read
  }
}

object SortPartitionRLD {
  /**
   * Determines the bounds for range partitioning from candidates with weights indicating how many
   * items each represents. Usually this is 1 over the probability used to sample this candidate.
   *
   * @param candidates unordered candidates with weights
   * @param partitions number of partitions
   * @return selected bounds
   */
  def determineBounds[K : Ordering : ClassTag](
                                                candidates: ArrayBuffer[(K, Float)],
                                                partitions: Int): Array[K] = {
    val ordering = implicitly[Ordering[K]]
    val ordered = candidates.sortBy(_._1)
    val numCandidates = ordered.size
    val sumWeights = ordered.map(_._2.toDouble).sum
    val step = sumWeights / partitions
    var cumWeight = 0.0
    var target = step
    val bounds = ArrayBuffer.empty[K]
    var i = 0
    var j = 0
    var previousBound = Option.empty[K]
    while ((i < numCandidates) && (j < partitions - 1)) {
      val (key, weight) = ordered(i)
      cumWeight += weight
      if (cumWeight >= target) {
        // Skip duplicate values.
        if (previousBound.isEmpty || ordering.gt(key, previousBound.get)) {
          bounds += key
          target += step
          j += 1
          previousBound = Some(key)
        }
      }
      i += 1
    }
    bounds.toArray
  }


  def makeBinarySearch[K : Ordering : ClassTag] : (Array[K], K) => Int = {
    // For primitive keys, we can use the natural ordering. Otherwise, use the Ordering comparator.
    classTag[K] match {
      case ClassTag.Float =>
        (l, x) => util.Arrays.binarySearch(l.asInstanceOf[Array[Float]], x.asInstanceOf[Float])
      case ClassTag.Double =>
        (l, x) => util.Arrays.binarySearch(l.asInstanceOf[Array[Double]], x.asInstanceOf[Double])
      case ClassTag.Byte =>
        (l, x) => util.Arrays.binarySearch(l.asInstanceOf[Array[Byte]], x.asInstanceOf[Byte])
      case ClassTag.Char =>
        (l, x) => util.Arrays.binarySearch(l.asInstanceOf[Array[Char]], x.asInstanceOf[Char])
      case ClassTag.Short =>
        (l, x) => util.Arrays.binarySearch(l.asInstanceOf[Array[Short]], x.asInstanceOf[Short])
      case ClassTag.Int =>
        (l, x) => util.Arrays.binarySearch(l.asInstanceOf[Array[Int]], x.asInstanceOf[Int])
      case ClassTag.Long =>
        (l, x) => util.Arrays.binarySearch(l.asInstanceOf[Array[Long]], x.asInstanceOf[Long])
      case _ =>
        val comparator = implicitly[Ordering[K]].asInstanceOf[java.util.Comparator[Object]]
        (l, x) => util.Arrays.binarySearch(l.asInstanceOf[Array[Object]], x.asInstanceOf[Object], comparator)
    }
  }

}
