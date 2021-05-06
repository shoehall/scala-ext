package scala.knickknack

import scala.collection.mutable
import scala.reflect.ClassTag

package object collection {

  implicit class PairFunctions4IteratorImpl[K, V](val iterator: Iterator[(K, V)])
                                                 (implicit kt: ClassTag[K], vt: ClassTag[V], ord: Ordering[K] = null) {
    def mapValues[R](f: V => R): Iterator[(K, R)] = iterator.map { case (key, value) => (key, f(value)) }

    def group: Iterator[(K, Array[V])] = new Iterator[(K, Array[V])] {
      // 结果元素, 会在next使用后还原为None
      private var values: Option[(K, Array[V])] = None

      def afterUsed(): Unit = {
        values = None
      }

      // 缓存
      private val memory_values = mutable.Buffer.empty[V]
      private var memory_key: Option[K] = None

      private def updateMemory(): Unit = {
        while (iterator.hasNext && values.isEmpty) {
          val (newGroup, value) = iterator.next()

          // 一个新组开始了, 此时生成结果元素
          if (memory_key.nonEmpty && memory_key.get != newGroup) {
            // 更新结果元素
            val res = new Array[V](memory_values.length)

            values = Some((memory_key.get, memory_values.toArray))

            // 清空缓存
            memory_values.clear()
          }

          // 更新
          memory_key = Some(newGroup)
          memory_values += value
        }
      }

      override def hasNext: Boolean = {
        if (iterator.hasNext) updateMemory()
        values.nonEmpty
      }

      override def next(): (K, Array[V]) = {
        val result = if (hasNext) {
          values.get
        } else Iterator.empty.next()
        afterUsed()
        result
      }
    }


  }

}
