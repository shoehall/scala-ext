package scala.knickknack.spandas.index

import scala.collection.TraversableLike
import scala.collection.generic.{CanBuildFrom, GenericTraversableTemplate, TraversableFactory}
import scala.collection.mutable.ListBuffer
import scala.knickknack.spandas.common.{ImmutableOps, ImmutableSeq, UFunc}

class Index[T](override val values: T*) extends ImmutableSeq[T]
  with GenericTraversableTemplate[T, Index]
  with TraversableLike[T, Index[T]]
  with ImmutableOps[Index[T]] {
  override def companion = Index
}

object Index extends TraversableFactory[Index] with UFunc {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, Index[A]] =
    ReusableCBF.asInstanceOf[GenericCanBuildFrom[A]]

  def newBuilder[A] = new ListBuffer[A] mapResult (x => new Index[A](x: _*))

}
