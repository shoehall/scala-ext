package scala.collection

import scala.reflect.ClassTag

package object rich {
  implicit def richTraversableOnce[A: ClassTag](values: TraversableOnce[A]): RichTraversableOnce[A] = new RichTraversableOnce[A](values)
  // extension for Array by treat it as a TraversableOnce.
  implicit def richArrayWithIterator[A: ClassTag](values: Array[A]): RichTraversableOnce[A] = new RichTraversableOnce[A](values)

  // extension for pair iterator, like Iterator[(K, V)]
  implicit def richPairIterator[K: ClassTag, V: ClassTag](iterator: Iterator[(K, V)]): RichPairIterator[K, V] = new RichPairIterator[K, V](iterator)

  // extension for Array by its' iterator.
  implicit def richPairArrayWithIterator[K: ClassTag, V: ClassTag](values: Array[(K, V)]): RichPairIterator[K, V] = new RichPairIterator[K, V](values.iterator)

  implicit def rich_Iterator[K: ClassTag](values: Iterator[K]): RichIterator[K] = new RichIterator[K](values)


}
