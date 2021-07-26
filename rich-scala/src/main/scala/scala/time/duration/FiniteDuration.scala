package scala.time.duration

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.TimeUnit._
class FiniteDuration(val length: Long, val unit: TimeUnit) extends Duration {

  override def unary_- : Duration = new FiniteDuration(-length, unit)

  override def toString: String = s"FiniteDuration($length, $unit)"

  def toNanos: Long = unit.toNanos(length)
  def toMicros: Long = unit.toMicros(length)
  def toMillis: Long = unit.toMillis(length)
  def toSeconds: Long = unit.toSeconds(length)
  def toMinutes: Long = unit.toMinutes(length)
  def toHours: Long = unit.toHours(length)
  def toDays: Long = unit.toDays(length)
  def toUnit(u: TimeUnit): Double = toNanos.toDouble / NANOSECONDS.convert(1, u)

  override def %/%(duration: Duration): Long = {
    val duf = duration.finite
    if(unit.ordinal() <= duf.unit.ordinal())
      length / unit.convert(duf.length, duf.unit)
    else
      duf.unit.convert(length, unit) / duf.length
  }

  override def finite: FiniteDuration = this

  override def years: Int = 0

  override def months: Int = 0

  override def days: Int = if(unit == DAYS) length.toInt else 0

  override def hours: Int = if(unit == HOURS) length.toInt else 0

  override def minutes: Int = if(unit == MINUTES) length.toInt else 0

  override def seconds: Double = if(unit == SECONDS) length.toInt else 0
}

object FiniteDuration {
  def apply(length: Long, unit: TimeUnit) = new FiniteDuration(length, unit)
  def apply(length: Long, unit: String): FiniteDuration = unit match {
    case "day" | "days" => new FiniteDuration(length, DAYS)
    case "hour" | "hours" => new FiniteDuration(length, HOURS)
    case "minute" | "minutes" => new FiniteDuration(length, MINUTES)
    case "second" | "seconds" => new FiniteDuration(length, SECONDS)
    case "millis" | "millisecond" | "milliseconds" => new FiniteDuration(length, MILLISECONDS)
    case other =>
      throw new UnsupportedOperationException(s"does not support unit $other")
  }

}