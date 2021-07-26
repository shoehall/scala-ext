package scala.knickknack.rld

import scala.knickknack.rld.rdd.cache.{Reader, Writer}
import scala.reflect.ClassTag

class Session(env: Map[String, String]) {
  /**
   * 优先从缓存中获取数据, 如果没有或者丢失
   * @param classTag
   * @param makeIterator
   * @param cache 如果没有则会cache
   * @tparam T
   * @return
   */
  def getOrCompute[T](classTag: ClassTag[T], makeIterator: () => Iterator[T], cache: Boolean): Iterator[T] = {
    makeIterator()
  }

  def getWriter: Writer = throw new UnsupportedOperationException()

  def getReader[T]: Reader[T] = throw new UnsupportedOperationException()

  /**
   * 清理缓存
   * ----
   * 利用Weak Reference管理DAG中每个对象的cache, 配合gc一起使用, 清理弱引用的cache.
   */
  def clean(): Unit =
    throw new UnsupportedOperationException()

}
