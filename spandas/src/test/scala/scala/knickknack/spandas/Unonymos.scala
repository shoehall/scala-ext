package scala.knickknack.spandas

object Unonymos {
  def main(args: Array[String]): Unit = {
    import scala.math.{abs, max}

    class Series[F](values: Seq[F]) {
      def >%>[T](fun: F => T): Series[T] = new Series[T](values.map(fun))

      override def toString: String = values.mkString("[", ", ", "]")
    }
    val series = new Series[Int](Seq(1, 3, 5, 7, 9))

    series >%> abs >%>(max(7, _))
//    series >%> abs >%> max(7, _)  // not compiled

    Runtime.getRuntime.totalMemory()
    Runtime.getRuntime.freeMemory()
    Runtime.getRuntime.maxMemory()
  }

//  def compile(): Unit = {
//    import scala.math.{abs, max}
//
//    class Series[F](values: Array[F]) {
//      def >%>[T](fun: F => T): Series[T] = new Series[T](values.map(fun))
//    }
//    val series = new Series[Int](Array(1, 3, 5, 7, 9))
//
//    series.>%>(abs).>%>(((x$1) => max(7, x$1)));
//    ((x$2) => series.>%>(abs).>%>(max(7, x$2)))
//
//  }

}
