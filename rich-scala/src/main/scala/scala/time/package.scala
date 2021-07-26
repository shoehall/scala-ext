package scala

import scala.time.duration.Duration

package object time {
  implicit class IntMulDuration(int: Int) {
    def *(duration: Duration): Duration = duration * int
  }
}
