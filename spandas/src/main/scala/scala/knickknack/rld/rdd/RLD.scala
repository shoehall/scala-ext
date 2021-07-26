package scala.knickknack.rld.rdd

import scala.knickknack.rld.Session
import scala.reflect.ClassTag

/**
 * A Resilient Light Dataset (LRD), which is a simple/local collection with the style of rdd of the spark.
 * - read only
 * @tparam T
 */
abstract class RLD[T: ClassTag](session: Session, deps: Seq[RLD[_]]) {
  /*
  基础函数
   */
  def makeIterator(): Iterator[T]

  def iterator(): Iterator[T] =
    session.getOrCompute(implicitly[ClassTag[T]], makeIterator, shouldCache)

  // 单个分区时compute和iterator一样
  def compute(): Iterator[T] = iterator()

  /*
  transformation算子
   */
  def map[U: ClassTag](f: T => U) = new MapRLD[T, U](session, f, this)

  /*
  action算子
   */
  def runJob[U: ClassTag](func: Iterator[T] => U): U = func(iterator())

  def count(): Int = runJob { itr: Iterator[T] => itr.size }
  def foreach(f: T => Unit): Unit = iterator().foreach(f)

  /*
  缓存
   */
  private var shouldCache: Boolean = false
  def cache(): this.type = {
    this.shouldCache = true
    this
  }

}
