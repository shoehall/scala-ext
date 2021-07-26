package scala.knickknack.rld.rdd.cache

import java.io.{FileInputStream, ObjectInputStream}
import scala.collection.mutable.ArrayBuffer
import scala.knickknack.rld.{Session, zipAndSort}
import scala.reflect.ClassTag


/*
Memory: Compute 用于计算的内存
Memory: Cache   用于cache的内存, 满了之后会spill到buffer
Memory: Buffer  cache spill写入到Disk的缓冲区, 每满到一定程度会批量写入到disk
Disk: 硬盘cache
 */
trait Writer {
  def fileName: String

  var byteSize: Long = 0

  /**
   * @param value
   */
  def write(value: Any, partition: Int): Unit

}


abstract class Reader[T: ClassTag](session: Session, val name: String) {
  def read: Iterator[T]
}


class ShuffleReader[K: ClassTag : Ordering, V: ClassTag](
                                                          session: Session,
                                                          override val name: String,
                                                          val sorted: Boolean
                                                        )
  extends Reader[(K, V)](session, name) {
  def listPartsReader: Iterator[PartReader[(K, V)]] = {
    if (sorted) {
      new SortedPartReader[K, V](session, name)
      throw new UnsupportedOperationException
    } else
      throw new UnsupportedOperationException
  }

  override def read: Iterator[(K, V)] = listPartsReader.flatMap { part => part.read }

}


class PartReader[T: ClassTag](
                               session: Session,
                               override val name: String
                             ) extends Reader[T](session, name) {
  def listBlockReader: Array[Reader[T]] = throw new UnsupportedOperationException()

  override def read: Iterator[T] = listBlockReader.iterator.flatMap(_.read)
}


class SortedPartReader[K: ClassTag : Ordering, V: ClassTag](
                                                             session: Session,
                                                             override val name: String
                                                           ) extends PartReader[(K, V)](session, name) {
  override def read: Iterator[(K, V)] = zipAndSort(listBlockReader.map { reader => reader.read })
}


class UnSerializedCacheReader[T: ClassTag](
                                            session: Session,
                                            override val name: String
                                          ) extends Reader[T](session, name) {
  override def read: Iterator[T] = throw new UnsupportedOperationException()
}

class FileCacheReader[T: ClassTag](
                                    session: Session,
                                    override val name: String
                                  ) extends Reader[T](session, name) {
  def cacheUrl: String = name

  override def read: Iterator[T] = {
    new Iterator[ArrayBuffer[T]] {
      private lazy val ois = new ObjectInputStream(new FileInputStream(cacheUrl))
      private var buffer: ArrayBuffer[T] = _

      override def hasNext: Boolean =
        if (buffer == null) {
          val value = ois.readObject()
          if (value == null) {
            ois.close()
            false
          } else {
            buffer = value.asInstanceOf[ArrayBuffer[T]]
            true
          }
        } else
          true

      override def next(): ArrayBuffer[T] = if (hasNext) buffer else Iterator.empty.next()
    }.flatMap { v => v}
  }
}

