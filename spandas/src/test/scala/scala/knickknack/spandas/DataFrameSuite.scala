package scala.knickknack.spandas

import org.scalatest.FunSuite

import scala.knickknack.spandas.frame.{DataFrame, LightFrame}
import scala.reflect.ClassTag

/**
 * 语法效果:
 * # 初始化
 * val df: DataFrame = null
 * ## 单数索引
 * df.iCol('a'): Series[Any]  // 列索引: python中的icol
 * df.iColAs[T]('a'): Series[T]
 * df.iRow(0)  // 行索引: irow
 * df.get(0, 'a') // 值索引
 * df.getAs[T](0, 'a') // 值索引
 *
 * ## 复数索引
 * df.col(Array('a', 'b')): DataFrame // 列索引
 * df.ix(Array('a', 'b')): DataFrame  // 行索引
 * df.loc(Array(0), Array('a', 'b')): DataFrame // 值索引
 * """
 *
 *
 * 思考:
 * 在进行行列选择的时候, 需要两个函数分别返回: DataFrame类型和Series类型
 * Row/DataFrame不能带类型, 否则太多了
 * 考虑下是否结合下R语言的管道符和dplyr的一些语法设计
 *
 * 设计原则:
 * - 方法在难以确定类型时留一个类型转换的接口, 否则在调用时还要推断元素类型.
 *
 */
class DataFrameSuite extends FunSuite {
  test("语法风格") {
    class AA[F] {
      def >%>[T: ClassTag](fun: F => T): AA[T] = throw new UnsupportedOperationException
    }

    val aa = new AA[Int]()

    val f1: Int => Double = {(a: Int) => a.toDouble}
    val f2: (Double, Int) => Double = {(a: Double, b: Int) => a}

    val f3: Double => Double = f2(_, 1)

    def f4(f: Double => Double) = {
      0.0
    }

    aa >%> f1 >%> f3 >%>(f2(_: Double, 1))

    case class DF() {
      def >%>(fun: DF => DF): DF = throw new UnsupportedOperationException()

      def >[Any](value: Any): DF = throw new UnsupportedOperationException()
    }

    val df = DF()

    def abs(df: DF): DF = null

    def merge(df1: DF, df2: DF, method: String): DF = null

    val df2: DF = null
    val mm = df >%> abs >%>(merge(_, df2, "inner"))

  }


  test("测试") {
    Seq.empty[Int]
    val df: DataFrame = null

    // df()
    // df("")
    // df(1) = row
    //    df.rows: Iterator[Row]

    val values = Seq("a", 1, "b", 2, "c", 3)
    val mm1 = Seq.range(0, values.length, 2).map { i => values(i) }
    mm1.foreach {
      case string: String =>
        println("String", string)
      case int: Int =>
        println("Int", int)
    }
  }

}
