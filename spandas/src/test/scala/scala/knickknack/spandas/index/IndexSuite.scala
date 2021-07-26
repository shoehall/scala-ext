package scala.knickknack.spandas.index

import breeze.linalg
import org.scalatest.FunSuite

import java.util.zip.ZipException
import scala.knickknack.spandas.common.{ImmutableSeq, Slice}
import scala.knickknack.spandas.functions.all
import scala.runtime.RichInt

class IndexSuite extends FunSuite {

  val stringValues: Array[String] = Array("1", "0", "2", "3", "4")
  val ofString: Index[String] = Index[String](stringValues: _*)
  val intValues = Seq(1, 0, 2, 3, 4)
  val floatValues = Seq(1.5f, 0.7f, 2.8f, 3.9f, 4.0f)
  val doubleValues = Seq(1.5d, 0.7d, 2.8d, 3.9d, 4.0d)

  val booleanValues: Array[Boolean] = Array(false, false, true, true, true)
  val ofBoolean: Index[Boolean] = Index(booleanValues: _*)
  val ofByte: Index[Byte] = Index(intValues.map(_.toByte): _*)
  val ofChar: Index[Char] = Index(intValues.map(_.toChar): _*)
  val ofShort: Index[Short] = Index(intValues.map(_.toShort): _*)
  val ofInt: Index[Int] = Index(intValues: _*)
  val ofLong: Index[Long] = Index(intValues.map(_.toLong): _*)
  val ofFloat: Index[Float] = Index(floatValues: _*)
  val ofDouble: Index[Double] = Index(doubleValues: _*)

  test("TraversableLike方法能够正常使用: map filter take") {
    val values = Array("1", "0", "2", "3", "4")
    val index = Index[String](values: _*)
    // 1)map
    val mapV = index.map(_ + "_immutableSeq")
    val values2 = values.map(_ + "_immutableSeq")
    val aa: Index[String] = Index.apply(values2: _*)
    assert(mapV == aa)
    // 2)filter
    val filterSeq: Index[String] = index.filter(s => s.toInt > 2)
    assert(filterSeq == Index("3", "4"))
    // 3)take
    val takeSeq: Index[String] = index.take(3)
    assert(takeSeq == Index("1", "0", "2"))
    // others..
  }

  test("索引") {
    val values = Array("1", "0", "2", "3", "4")
    val index = Index[String](values: _*)

    // 1)元素索引
    val _: String = index(0)
    assert(index(0) == values(0))
    // 2)Slice索引:
    val slice0: Index[String] = index.apply(Slice(0, 3)) // 类型变为了Index而不是ImmutableSeq
    assert(slice0 == index.take(3))
    // 2)Range索引
    val slice1: Index[String] = index.apply(Range(0, 3))
    assert(slice1 == index.take(3))
    // 4)Seq[Int]
    val slice2: Index[String] = index.apply(Seq(0, 1, 3))
    assert(slice2 == Index("1", "0", "3"))
    //    assert(immutableSeq.apply(Array(0, 1, 3)) == ImmutableSeq("1", "0", "3"))
    // 5)Seq[Boolean]
    val slice3: Index[String] = index(Seq(true, true, false, true, true))
    assert(slice3 == index(Seq(0, 1, 3, 4)))
    //    assert(immutableSeq(Array(true, true, false, true, false)) == immutableSeq.apply(Array(0, 1, 3)))

  }

  test("一元运算: !, not") {
    val notBoolean: Index[Boolean] = !ofBoolean
    assert(notBoolean == Index(booleanValues.map(v => !v): _*))
  }

  /**
   * 正运算:
   * - 会创建一个等于自身的类, 有clone的效果
   * - 适合任意的元素类型
   */
  test("一元运算: +, positive") {
    val s = +ofString
    assert(s == ofString)
  }

  /**
   * 负运算:
   * - 支持数值类型
   * - Byte, Char, Short在进行负数运算后会自动转为Int
   * todo: time delta类型完善后可以加入time delta类型
   */
  test("一元运算: -, negative") {
    // Byte
    val ng1: Index[Int] = ofByte.unary_-
    assert(ng1 == Index(intValues.map(-_): _*))
    // Char
    val ng2: Index[Int] = -ofChar
    assert(ng2 == Index(intValues.map(-_): _*))
    // Short
    val ng3: Index[Int] = -ofShort
    assert(ng3 == Index(intValues.map(-_): _*))
    // int
    val ng4: Index[Int] = -ofInt
    assert(ng4 == Index(intValues.map(-_): _*))
    // float
    val ng5: Index[Float] = -ofFloat
    assert(ng5.equals(Index(floatValues.map(-_): _*)))
    // long
    val ng6: Index[Long] = -ofLong
    assert(ng6 == Index(intValues.map(-_): _*))
    // double
    val ng7: Index[Double] = -ofDouble
    assert(ng7 == Index(doubleValues.map(-_): _*))
  }

  test("二元运算: zip map") {
    // zip map ImmutableSeq
    val z1: Index[String] = ofString.zipMap(ImmutableSeq("1", "0", "2", "3", "2")) { case (v1, v2) => v1 + "_" + v2 }
    assert(z1 == Index("1_1", "0_0", "2_2", "3_3", "4_2"))

    val z2: Index[String] = ofString.zipMap(Index("1", "0", "2", "3", "2")) { case (v1, v2) => v1 + "_" + v2 }
    assert(z2 == Index("1_1", "0_0", "2_2", "3_3", "4_2"))
    // length assert
    intercept[ZipException] {
      ofString.zipMap(ImmutableSeq("1", "0", "2", "3")) { case (v1, v2) => v1 + "_" + v2 }
    }

    // zip map Seq
    val z3: Index[Int] = ofInt.zipMap(intValues) { case (v1, v2) => v1 * v2 }
    val squared = Index(intValues.map(vvv => vvv * vvv): _*)
    assert(z3 == squared)
    // zip map Array
    val vv2 = intValues.toArray
    val z4: Index[Int] = ofInt.zipMap(vv2) { case (v1, v2: Int) => v1 * v2 }
    assert(z4 == squared)

    // zip map Range
    val z5: Index[Int] = ofInt.zipMap(vv2.indices) { case (v1, v2: Int) => v1 * v2 }
    assert(z5 == Index(0, 0, 4, 9, 16))

    // zip map one not traversable
    val z6: Index[Int] = Index(intValues: _*).zipMap(1) { case (v1, v2) => v1 + v2 }
    assert(z6 == Index(intValues.map(_ + 1): _*))
  }

  test("二元运算: ===") {
    // 1) === an Index
    val eq1: Index[Boolean] = ofString === ofString
    assert(eq1.forall(a => a))
    // 2) === a seq
    val eq2: Index[Boolean] = ofString === Seq("1", "2", "3", "4", "5")
    assert(eq2 == Index(true, false, false, false, false))
    // 3) === a range
    val eq3: Index[Boolean] = ofInt === Range(0, 5)
    assert(eq3 == Index(false, false, true, true, true))
    // 4) === an Array
    val eq4: Index[Boolean] = ofInt === Array.range(0, 5)
    assert(eq4 == Index(false, false, true, true, true))
    // 5) === other
    val eq5: Index[Boolean] = ofString === "3"
    assert(eq5 == Index(false, false, false, true, false))
    // 6) assert length equal if traversable
    intercept[ZipException] {
      ofString === Index("1", "2")
    }
  }

  test("二元运算: zip") {
    // 1)zip an index
    val z1: Index[(String, Int)] = ofString.zip(ofInt)
    assert(z1 == Index(stringValues.zip(intValues): _*))
    // 2)zip a seq
    val z2: Index[(String, String)] = ofString zip Seq("1", "2", "3", "4", "5")
    assert(z2 == Index(stringValues.zip(Seq("1", "2", "3", "4", "5")): _*))
    // 3)zip a range
    val z3 = ofString.zip(Range(0, 5))
    assert(z3 == Index(stringValues.zip(Range(0, 5)): _*))
    // 4)zip an array
    val z4 = ofString.zip(Array.range(0, 5))
    assert(z4 == z3)
    // 5)assert length
    intercept[ZipException] {
      ofString.zip(Array(1, 2))
    }
    // 6)zip other
    val z5: Index[(String, Boolean)] = ofString.zip(true)
    assert(z5 == Index(stringValues.map { v => (v, true) }: _*))
  }

  test("二元运算: Boolean => and or xor") {
    // 1)and an Index
    val ad1: Index[Boolean] = ofBoolean.&(Index(true, false, false, true, false))
    assert(ad1 == Index(false, false, false, true, false))
    intercept[ZipException] {
      ofBoolean and Index(true, false, true)
    }

    // 2)and a boolean
    assert((ofBoolean && true) == ofBoolean)
    println((ofBoolean ^ true) == Index(false, false, true, true, true))

  }

  /**
   * 二元运算:
   * - 支持数值类型, 支持String类型
   * - 低于Int的整数类型相加后为Int类型
   * - Int和Long相加为Long类型, Float和Double相加为Float类型
   * - 整形和浮点型相加为浮点型
   * - String可以和任何类型相加, 结果为String类型
   */
  test("二元运算: +") {
    val plus10 = ofInt + 1
    val plus11 = ofInt + ofInt

    assert(plus10 == Index(2, 1, 3, 4, 5))
    assert(plus11 == Index(2, 0, 4, 6, 8))

    val plus12: Index[Double] = ofInt + 1.0
    assert(plus12 == Index(2.0,1.0,3.0,4.0,5.0))

    val plus13: Index[Int] = ofChar + ofByte
    assert(plus13 == Index(2,0,4,6,8))

    // String和任何类型相加都是string
    val plus14: Index[String] = ofInt + "aa"
    assert(plus14 == Index("1aa", "0aa", "2aa", "3aa", "4aa"))

    val plus15: Index[String] = ofString + 1.0
    assert(plus15 == Index("11.0", "01.0", "21.0", "31.0", "41.0"))
  }


}
