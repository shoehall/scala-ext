package scala.collection.rich

import scala.collection.mutable
import scala.reflect.ClassTag


class RichIterator[T: ClassTag](iterator: Iterator[T]) {
  def taste(from: Int, to: Int): (Array[T], Iterator[T]) = {
    val headCollector = mutable.ArrayBuilder.make[T]()
    val tasteCollector = mutable.ArrayBuilder.make[T]()
    val i = 0
    while (i < to && iterator.hasNext) {
      val value = iterator.next()
      if (i >= from) {
        tasteCollector += value
      } else
        headCollector += value
    }
    (tasteCollector.result(), headCollector.result().iterator ++ tasteCollector.result().iterator ++ iterator)
  }

  def taste(n: Int): (Array[T], Iterator[T]) = taste(0, n)

  def taste(): (T, Iterator[T]) = {
    val (sliced, it) = taste(1)
    val head = if (sliced.isEmpty)
      throw new NoSuchElementException("next on empty iterator")
    else {
      sliced.head
    }
    (head, it)
  }
}


class RichPairIterator[K, V](iterator: Iterator[(K, V)])(implicit kt: ClassTag[K], vt: ClassTag[V], ord: Ordering[K] = null) extends Iterator[(K, V)] {
  override def hasNext: Boolean = iterator.hasNext

  override def next(): (K, V) = iterator.next()

  def keys: Iterator[K] = iterator.map(_._1)

  def values: Iterator[V] = iterator.map(_._2)

  def mapValues[R](f: V => R): Iterator[(K, R)] = iterator.map { case (key, value) => (key, f(value)) }

  // todo: 后面改一下Generic方法
  // 根据key进行分组聚合操作
  def combineByKey[R0, R1](z: R0)(op: (R0, V) => R0, con: R0 => R1): Iterator[(K, R1)] = new Iterator[(K, R1)] {
    // 生产者, 负责向缓冲区中提供数据
    private val provider = new Provider4IteratorFold[K, V, R0](iterator, z, op)

    // 缓冲区
    private val memory = new Memory4Iterator[K, V, R0](provider)

    // 消费者, 负责从缓冲区中消费结果
    override def hasNext: Boolean = memory.hasNext0

    override def next(): (K, R1) = {
      val (k, v) = memory.next0()
      (k, con(v))
    }
  }

  // 根据key进行分组聚合操作
  def foldLeftByKey[R](z: R)(op: (R, V) => R): Iterator[(K, R)] = combineByKey[R, R](z)(op, (v: R) => v)

  def foldByKey[R](z: R)(op: (R, V) => R): Iterator[(K, R)] = foldLeftByKey[R](z)(op)

  def reduceByKey(op: (V, V) => V): Iterator[(K, (Boolean, V))] = foldByKey((true, null.asInstanceOf[V])) {
    case ((isFirst, res), value) =>
      if(isFirst)
        (false, value)
      else
        (false, op(res, value))
  }

  def group: Iterator[(K, Array[V])] = combineByKey[mutable.ArrayBuilder[V], Array[V]](mutable.ArrayBuilder.make[V]())(
    (res, value) => res += value,
    res => {
      val result = res.result()
      res.clear()
      result
    }
  )
}


private class Provider4IteratorFold[K, V, R](iterator: Iterator[(K, V)], z: R, op: (R, V) => R) extends Serializable {
  // key, 当前key
  var key: Option[K] = None
  // next head, 下一组的头部数据, 和Iterator中的数据一样作为输入材料
  var nextHead: Option[(K, V)] = None

  def hasNext0: Boolean = nextHead.nonEmpty || iterator.nonEmpty

  def next0(): (K, V) = if (nextHead.nonEmpty) {
    val value = nextHead.get
    nextHead = None
    value
  } else {
    iterator.next()
  }

  /**
   * 检查key是否和之前的一组
   * ----
   * 如果发现不是会有动作:
   * 1)将key变更为None, 下一组从0开始
   * 2)将数据计入下一个组的开头
   *
   * @param k key
   * @param v value
   * @return 此处返回两个信息: 如果是None表示还是和以前一组, 否则返回之前组key
   */
  def checkKey(k: K, v: V): Option[K] = {
    if (key.isEmpty) {
      key = Some(k)
      None
    } else {
      if (key.get != k) {
        val presentKey = Some(key.get)
        key = None
        nextHead = Some((k, v))
        presentKey
      } else
        None
    }
  }

  def provide(memory: Memory4Iterator[K, V, R]): Unit =
    if (memory.isEmpty) {
      var res = z
      var presentKey: Option[K] = None
      var currentKey: Option[K] = None
      while (hasNext0 && presentKey.isEmpty) {
        val (nextKey, nextValue) = next0()
        currentKey = Some(nextKey)
        presentKey = checkKey(nextKey, nextValue)
        if (presentKey.isEmpty)
          res = op(res, nextValue)
      }

      if (presentKey.nonEmpty) {
        memory.update(presentKey.get, res)
      } else {
        if (currentKey.nonEmpty)
          memory.update(currentKey.get, res)
      }
    }
}

private class Memory4Iterator[K, V, R](provider: Provider4IteratorFold[K, V, R]) extends Serializable {
  // 1)缓冲结果
  var values: Option[(K, R)] = None

  def isEmpty: Boolean = values.isEmpty

  def update(k: K, v: R): Unit = {
    values = Some((k, v))
  }

  def hasNext0: Boolean = {
    if (isEmpty)
      provider.provide(this)
    !isEmpty
  }

  def next0(): (K, R) = {
    if (hasNext0) {
      val res = values.get
      clear()
      res
    } else
      Iterator.empty.next()
  }

  def clear(): Unit = {
    values = None
  }

}


