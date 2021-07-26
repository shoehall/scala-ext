package scala.time

import java.sql.Timestamp
import java.util.Date

package object rich {
  implicit class RichTimestamp(timestamp: Timestamp) extends Comparable[Date]{
    override def compareTo(o: Date): Int = {
      (timestamp, o) match {
        case (null, null) => 0
        case (null, _) => -1
        case (_, null) => 1
        case _ =>
          java.lang.Long.compare(timestamp.getTime, o.getTime)
      }
    }

    def <(o: Date): Boolean = compareTo(o) < 0
    def <=(o: Date): Boolean = compareTo(o) <= 0
    def >(o: Date): Boolean = compareTo(o) > 0
    def >=(o: Date): Boolean = compareTo(o) >= 0
  }

}
