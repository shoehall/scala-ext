package scala.time

package object duration {
  val year = Duration(1, 0, 0, 0, 0, 0.0)
  val month = Duration(0, 1, 0, 0, 0, 0.0)
  val day = Duration(0, 0, 1, 0, 0, 0.0)
  val hour = Duration(0, 0, 0, 1, 0, 0.0)
  val minute = Duration(0, 0, 0, 0, 1, 0.0)
  val second = Duration(0, 0, 0, 0, 0, 1.0)
  val millisecond = Duration(0, 0, 0, 0, 0, 0.001)
}
