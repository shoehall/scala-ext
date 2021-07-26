package scala.time.duration

import org.joda.time.{DateTime, Period}

import java.sql.Timestamp
import java.text.{DateFormat, SimpleDateFormat}
import scala.time.rich.RichTimestamp
import java.util.concurrent.TimeUnit.MILLISECONDS

class Interval(val start: Timestamp, val end: Timestamp) {

  override def toString: String = s"[$start, $end)"

  def toDuration: Duration = {
    val s = new DateTime(start.getTime)
    val e = new DateTime(end.getTime)
    val period = new Period(s, e)

    Duration(period.getYears, period.getMonths, period.getDays, period.getHours, period.getMinutes, period.getSeconds + period.getMillis * 0.001)
  }

  def toStandardDuration: FiniteDuration = {
    FiniteDuration(end.getTime - start.getTime, MILLISECONDS)
  }

  def %/%(duration: Duration): Long = {
    if(duration.canBeFinite) {
      toStandardDuration.toMillis / duration.finite.toMillis
    } else {
      var i = 0
      val (s, e, factor) = if(start <= end) (start, end, 1) else (end, start, -1)
      while((duration * i + s) <= e) {
        i += 1
      }

      (i - 1) * factor
    }
  }
}

object Interval {
  def apply(start: Timestamp, end: Timestamp) = new Interval(start, end)

  // 后面写一个智能的formatter替代
  private val formatter: DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def apply(start: String, end: String): Interval =
    apply(new Timestamp(formatter.parse(start).getTime), new Timestamp(formatter.parse(end).getTime))
}
