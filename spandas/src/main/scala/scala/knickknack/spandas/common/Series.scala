package scala.knickknack.spandas.common

trait Series[T] extends ImmutableSeq[T] {
  // todo: 加入update方法
  // todo: 加入Index
}

object Series {
  def apply[T](values: T*): Series[T] = throw new UnsupportedOperationException()
}
