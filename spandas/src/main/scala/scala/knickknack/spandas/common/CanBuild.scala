package scala.knickknack.spandas.common

import scala.collection.frame.Schema


trait CanBuild {
  trait CanBuildBy[V1, VR] {
    def apply(v: V1): VR
    def apply(): VR
  }
}


trait CanBuildFrameFrom[-From, T, +To] {
  def apply(from: TraversableOnce[T], schema: Schema): To
}