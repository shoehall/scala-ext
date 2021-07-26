package scala.time.duration

import java.sql.{Date, Timestamp}
import java.text.DecimalFormat
import java.util.{Calendar, StringTokenizer}

/**
 * 存续时间
 */
trait Duration {
  // 绝对时间
  def getTime: Long = throw new UnsupportedOperationException()

  def years: Int

  def months: Int

  def days: Int

  def hours: Int

  def minutes: Int

  def seconds: Double

  private val secondsFormat: DecimalFormat = {
    val format = new DecimalFormat("0.00####")
    val dfs = format.getDecimalFormatSymbols
    dfs.setDecimalSeparator('.')
    format.setDecimalFormatSymbols(dfs)
    format
  }

  def %/%(duration: Duration): Long = {
    finite %/% duration.finite
  }

  def canBeFinite: Boolean = years == 0 && months == 0

  def finite: FiniteDuration = {
    require(years == 0, "year is not a finite time unit")
    require(months == 0, "month is not a finite time unit")

    if (seconds != 0) {
      if (seconds.toInt != seconds) {
        val millis = (days * 24 * 60 * 60 + hours * 60 * 60 + minutes * 60 + seconds) * 1000
        FiniteDuration(millis.toInt, "milliseconds")
      } else
        FiniteDuration(days * 24 * 60 * 60 + hours * 60 * 60 + minutes * 60 + seconds.toInt, "seconds")
    } else if (minutes != 0)
      FiniteDuration(days * 24 * 60 + hours * 60 + minutes, "minutes")
    else if (hours != 0)
      FiniteDuration(days * 24 + hours, "hours")
    else
      FiniteDuration(days, "days")
  }

  def unary_- : Duration = {
    val year0 = this.years
    val month0 = this.months
    val day0 = this.days
    val hour0 = this.hours
    val minute0 = this.minutes
    val second0 = this.seconds
    new Duration {
      override def years: Int = -year0

      override def months: Int = -month0

      override def days: Int = -day0

      override def hours: Int = -hour0

      override def minutes: Int = -minute0

      override def seconds: Double = -second0
    }
  }

  /**
   * Returns the stored interval information as a string.
   *
   * @return String represented interval
   */
  override def toString: String =
    years + " years " + months + " mons " + days + " days " + hours + " hours " + minutes + " mins " + secondsFormat.format(seconds) + " secs"


  /**
   * Rolls this interval on a given calendar.
   *
   * @param cal Calendar instance to add to
   */
  private def add(cal: Calendar): Calendar = {
    // Avoid precision loss
    // Be aware postgres doesn't return more than 60 seconds - no overflow can happen
    val microseconds = (seconds * 1000000.0).toInt
    val milliseconds = (microseconds + (if (microseconds < 0) -500 else 500)) / 1000

    cal.add(Calendar.MILLISECOND, milliseconds)
    cal.add(Calendar.MINUTE, minutes)
    cal.add(Calendar.HOUR, hours)
    cal.add(Calendar.DAY_OF_MONTH, days)
    cal.add(Calendar.MONTH, months)
    cal.add(Calendar.YEAR, years)
    cal
  }

  /**
   * Rolls this interval on a given date.
   *
   * @param date Date instance to add to
   */
  def add(date: Date): Date = {
    val cal = Calendar.getInstance()
    cal.setTime(date)
    add(cal)
    new Date(cal.getTime.getTime)
  }

  def add(timestamp: Timestamp): Timestamp = {
    val cal = Calendar.getInstance()
    cal.setTime(timestamp)
    add(cal)
    new Timestamp(cal.getTime.getTime)
  }

  /**
   * Add this interval's value to the passed interval. This is backwards to what I would expect, but
   * this makes it match the other existing add methods.
   *
   * @param duration duration to add
   */
  def add(duration: Duration): Duration =
    Duration(
      years + duration.years,
      months + duration.months,
      days + duration.days,
      hours + duration.hours,
      minutes + duration.minutes,
      seconds + duration.seconds
    )

  def +(date: Timestamp): Timestamp = add(date)

  def +(date: Date): Date = add(date)

  def +(duration: Duration): Duration = add(duration)

  /**
   * Scale this interval by an integer factor. The server can scale by arbitrary factors, but that
   * would require adjusting the call signatures for all the existing methods like getDays() or
   * providing our own justification of fractional intervals. Neither of these seem like a good idea
   * without a strong use case.
   *
   * @param factor scale factor
   */
  def scale(factor: Int): Duration = Duration(years * factor, months * factor, days * factor, hours * factor, minutes * factor, seconds * factor)
  def scale(factor: Long): Duration = scale(factor.toInt)

  def *(factor: Int): Duration = scale(factor)
  def *(factor: Long): Duration = scale(factor)

  /**
   * Returns whether an object is equal to this one or not.
   *
   * @param obj Object to compare with
   * @return true if the two intervals are identical
   */
  override def equals(obj: Any): Boolean = {
    obj match {
      case null => false
      case du: Duration =>
        years == du.years && months == du.months && days == du.days && hours == du.hours && minutes == du.minutes &&
          java.lang.Double.doubleToLongBits(seconds) == java.lang.Double.doubleToLongBits(du.seconds)
      case _ => false
    }

  }


  override def hashCode(): Int = {
    (
      (
        (
          (
            (
              (
                7 * 31 + java.lang.Double.doubleToLongBits(seconds).toInt
                ) * 31 + minutes
              ) * 31 + hours
            ) * 31 + days
          ) * 31 + months
        ) * 31 + years
      ) * 31
  }

  override def clone(): AnyRef = super.clone()

}


object Duration {
  def apply(length: Int, unit: String): Duration = {
    unit match {
      case "year" | "years" => Duration(length, 0, 0, 0, 0, 0)
      case "month" | "months" => Duration(0, length, 0, 0, 0, 0)
      case _ => FiniteDuration(length, unit)
    }
  }

  /**
   * Initialize a interval with a given interval string representation.
   *
   * @param duration String representated duration (e.g. '3 years 2 mons')
   */
  def apply(duration: String): Duration = {
    var value = duration
    val ISOFormat = !value.startsWith("@")

    // Just a simple '0'
    if (!ISOFormat && value.length() == 3 && value.charAt(2) == '0') {
      Duration(0, 0, 0, 0, 0, 0.0)
    } else {
      var years = 0
      var months = 0
      var days = 0
      var hours = 0
      var minutes = 0
      var seconds = 0.0

      var valueToken: String = null
      value = value.replace('+', ' ').replace('@', ' ')
      val st = new StringTokenizer(value)
      var i = 1
      while (st.hasMoreElements) {
        val token = st.nextToken()

        if ((i & 1) == 1) {
          val endHours = token.indexOf(':')
          if (endHours == -1) {
            valueToken = token
          } else {
            // This handles hours, minutes, seconds and microseconds for
            // ISO intervals
            val offset = if (token.charAt(0) == '-') 1 else 0

            hours = nullSafeIntGet(token.substring(offset + 0, endHours))
            minutes = nullSafeIntGet(token.substring(endHours + 1, endHours + 3))

            // Pre 7.4 servers do not put second information into the results
            // unless it is non-zero.
            val endMinutes = token.indexOf(':', endHours + 1)
            if (endMinutes != -1) {
              seconds = nullSafeDoubleGet(token.substring(endMinutes + 1))
            }

            if (offset == 1) {
              hours = -hours
              minutes = -minutes
              seconds = -seconds
            }

            valueToken = null
          }
        } else {
          // This handles years, months, days for both, ISO and
          // Non-ISO intervals. Hours, minutes, seconds and microseconds
          // are handled for Non-ISO intervals here.
          if (token.startsWith("year")) {
            years = nullSafeIntGet(valueToken)
          } else if (token.startsWith("mon")) {
            months = nullSafeIntGet(valueToken)
          } else if (token.startsWith("day")) {
            days = nullSafeIntGet(valueToken)
          } else if (token.startsWith("hour")) {
            hours = nullSafeIntGet(valueToken)
          } else if (token.startsWith("min")) {
            minutes = nullSafeIntGet(valueToken)
          } else if (token.startsWith("sec")) {
            seconds = nullSafeDoubleGet(valueToken)
          }
        }

        i += 1
      }

      if (!ISOFormat && value.endsWith("ago")) {
        // Inverse the leading sign
        Duration(-years, -months, -days, -hours, -minutes, -seconds)
      } else
        Duration(years, months, days, hours, minutes, seconds)
    }

  }


  def apply(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Double): Duration = new Duration {
    override def years: Int = year

    override def months: Int = month

    override def days: Int = day

    override def hours: Int = hour

    override def minutes: Int = minute

    override def seconds: Double = second
  }

  /**
   * Returns integer value of value or 0 if value is null.
   *
   * @param value integer as string value
   * @return integer parsed from string value
   * @throws NumberFormatException if the string contains invalid chars
   */
  @throws[NumberFormatException]
  def nullSafeIntGet(value: String): Int = if (value == null) 0 else Integer.parseInt(value)


  /**
   * Returns double value of value or 0 if value is null.
   *
   * @param value double as string value
   * @return double parsed from string value
   * @throws NumberFormatException if the string contains invalid chars
   */
  @throws[NumberFormatException]
  def nullSafeDoubleGet(value: String): Double = if (value == null) 0.0 else java.lang.Double.parseDouble(value)

  trait DurationConversions {
    implicit val postfixOps: languageFeature.postfixOps = language.postfixOps

    def length: Int

    def milliseconds: Duration = Duration(0, 0, 0, 0, 0, length * 0.001)

    def seconds: Duration = Duration(0, 0, 0, 0, 0, length)

    def minutes: Duration = Duration(0, 0, 0, 0, length, 0)

    def hours: Duration = Duration(0, 0, 0, length, 0, 0)

    def days: Duration = Duration(0, 0, length, 0, 0, 0)

    def months: Duration = Duration(0, length, 0, 0, 0, 0)

    def years: Duration = Duration(length, 0, 0, 0, 0, 0)
  }

  implicit class DurationInt(override val length: Int) extends DurationConversions

}
