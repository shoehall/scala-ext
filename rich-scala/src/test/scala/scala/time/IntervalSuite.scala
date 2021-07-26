package scala.time

import org.scalatest.{FunSuite, ShouldMatchers}

import java.sql.Timestamp
import java.text.SimpleDateFormat
import scala.time.duration._
import scala.time.rich.RichTimestamp

class IntervalSuite extends FunSuite with ShouldMatchers {
  test("toDuration") {
    println(Interval.apply("2000-01-20 00:00:01", "2019-01-19 10:10:10").toDuration)
  }

  test("Interval %/%") {
    println(Interval.apply("2020-10-01 00:00:00", "2020-12-31 00:00:00"))
    println(Interval.apply("2020-10-01 00:00:00", "2020-12-31 00:00:00") %/% month)
    println(Interval.apply("2020-12-01 00:00:00", "2020-12-03 12:00:00") %/% year)
    println(Interval.apply("2020-12-01 00:00:00", "2020-12-03 12:00:00") %/% month)
    println(Interval.apply("2020-12-01 00:00:00", "2020-12-03 12:00:00") %/% day)
    println(Interval.apply("2020-12-01 00:00:00", "2020-12-03 12:00:00") %/% hour)
    println(Interval.apply("2020-12-01 00:00:00", "2020-11-27 12:00:00") %/% hour)
  }

  test("通过%/%实现时间分箱") {
    val s = "2020-10-01 00:00:00"
    val e = "2020-12-31 00:00:00"
    val smf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val ss = new Timestamp(smf.parse(s).getTime)
    println(month * (Interval.apply(s, e) %/% month) + ss)
  }

  def binning(phase: String, time: String, window: Int, unit: String): String = {
    val timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val s = new Timestamp(timeFormat.parse(phase).getTime)
    val e = new Timestamp(timeFormat.parse(time).getTime)
    val duration = Duration(window, unit)

    if(s < e)
      timeFormat.format(duration * (Interval(s, e) %/% duration) + s)
    else
      timeFormat.format(-duration * (Interval(e, s) %/% duration + 1) + s)
  }

  test("测试一般的时间分箱") {
    binning("2000-01-01 00:00:00", "1949-10-1 10:10:10", 1, "month") should be("1949-10-01 00:00:00")
    binning("2000-01-20 00:00:01", "1949-10-1 10:10:10", 1, "month") should be("1949-09-20 00:00:01")
    binning("2000-01-20 00:00:01", "0049-10-1 10:10:10", 1, "month") should be("0049-09-20 00:00:01")
    binning("2000-01-20 00:00:01", "0000-01-19 10:10:10", 1, "hour") should be("0001-01-19 10:00:01")
    binning("2000-01-20 00:00:01", "2019-01-19 10:10:10", 1, "day") should be("2019-01-19 00:00:01")

  }

}
