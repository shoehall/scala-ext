package scala.time

import org.scalatest.FunSuite

import java.text.SimpleDateFormat
import java.util.TimeZone
import scala.time.duration.Duration

class DurationSuite extends FunSuite {
  test("duration parse") {
    println(Duration("1 hour"))

    println(Duration("1 hour") + Duration("180 days"))

    println(Duration("1 h"))
  }

  test("duration 整除 %/%") {
    assert(Duration("10 hours") %/% Duration("2 hours") == 5)
    assert(Duration("10 hours") %/% Duration("-2 hours") == -5)
    intercept[ArithmeticException](Duration("10 hours") %/% Duration("0 hours"))
    assert(Duration("10 hours") %/% Duration("20 hours") == 0)
    assert(Duration("10 hours") %/% Duration("20 minutes") == 30)
  }

  test("时区问题") {
    val isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    val date = isoFormat.parse("2010-05-23T09:01:02")
    println(date)
    // timestamp通常是没有timezone概念的, 只有具体到字符串的时候才需要timezone
  }


  test("隐式转换实现DSL风格的写法") {
    import Duration.DurationInt
    val mm = 3 hours

    println(mm)

    import org.joda.time.Interval
    val itv: Interval = null
    itv
  }

}
