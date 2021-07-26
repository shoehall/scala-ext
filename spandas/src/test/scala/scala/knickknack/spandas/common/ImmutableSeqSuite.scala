package scala.knickknack.spandas.common

import org.scalatest.FunSuite

import java.util.zip.ZipException
import scala.collection.mutable
import scala.knickknack.spandas.frame.DataFrame

class ImmutableSeqSuite extends FunSuite {
  test("TraversableLike方法能够正常使用: map filter take") {
    val values = Array("1", "0", "2", "3", "4")
    val immutableSeq = ImmutableSeq[String](values: _*)
    // 1)map
    val mapV = immutableSeq.map(_ + "_immutableSeq")
    val values2 = values.map(_ + "_immutableSeq")
    val aa: ImmutableSeq[String] = ImmutableSeq.apply(values2: _*)
    assert(mapV == aa)
    // 2)filter
    val filterSeq: ImmutableSeq[String] = immutableSeq.filter(s => s.toInt > 2)
    assert(filterSeq == ImmutableSeq("3", "4"))
    // 3)take
    val takeSeq: ImmutableSeq[String] = immutableSeq.take(3)
    assert(takeSeq == ImmutableSeq("1", "0", "2"))
    // others..
  }

  test("索引") {
    val values = Array("1", "0", "2", "3", "4")
    val immutableSeq = ImmutableSeq[String](values: _*)
    // 1)元素索引
    assert(immutableSeq(0) == values(0))
    // 2)Slice索引
    assert(immutableSeq.apply(Slice(0, 3)) == immutableSeq.take(3))
    // 2)Range索引
    val x = Range(0, 3)
    assert(immutableSeq.apply(Range(0, 3)) == immutableSeq.take(3))
    // 4)Seq[Int]
    //    assert(immutableSeq.apply(Array(0, 1, 3)) == ImmutableSeq("1", "0", "3"))
    assert(immutableSeq.apply(Seq(0, 1, 3)) == ImmutableSeq("1", "0", "3"))
    // 5)Seq[Boolean]
    assert(immutableSeq(Seq(true, true, false, true, false)) == immutableSeq(Seq(0, 1, 3)))
    //    assert(immutableSeq(Array(true, true, false, true, false)) == immutableSeq.apply(Array(0, 1, 3)))

  }

  test("一元运算") {
    val values = Array("1", "0", "2", "3", "4")
    val index = ImmutableSeq[String](values: _*)
    assert(index(0) == values(0))

//    val intIndex: ImmutableSeq[Int] = ImmutableSeq(1, 2, 3)
//    -intIndex
//    println(-intIndex)
//
//    val boolean = ImmutableSeq(true, false, true, false)
//    !boolean
//
//    val intImmutableSeq: ImmutableSeq[Int] = ImmutableSeq(1, 2, 3)
//    import scala.knickknack.spandas.common.MinusOps._
//    -intImmutableSeq
  }

  test("二元运算") {
    val values = Array("1", "0", "2", "3", "4")
    val seq = ImmutableSeq[String](values: _*)

    // 1. ==运算
    assert(seq == seq)
    assert(seq != ImmutableSeq("1", "0", "2", "3"))

    // --------------分界线: 此处开始函数变为, 接受一个可遍历的类型, 返回的是序列类型
    // 2.zipMap
    // zip map ImmutableSeq
    val zipMap: ImmutableSeq[String] = seq.zipMap(ImmutableSeq("1", "0", "2", "3", "2")) { case (v1, v2) => v1 + "_" + v2 }
    assert(zipMap == ImmutableSeq("1_1", "0_0", "2_2", "3_3", "4_2"))
    intercept[ZipException] {
      seq.zipMap(ImmutableSeq("1", "0", "2", "3")) { case (v1, v2) => v1 + "_" + v2 }
    }

    // zip map Seq
    val vv = Seq(1, 0, 2, 3, 4)
    println(ImmutableSeq(vv: _*).zipMap(vv) { case (v1, v2) => v1 * v2 })

    // zip map Array
    val vv2 = vv.toArray

    //
    println(ImmutableSeq(vv: _*).zipMap(vv2) { case (v1, v2: Int) => v1 * v2 })

    // zip ArrayBuffer
    println(ImmutableSeq(vv: _*).zipMap(vv2.indices) { case (v1, v2: Int) => v1 * v2 })

    // zip map 一个元素
    val addOne = ImmutableSeq(vv: _*).zipMap(1) { case (v1, v2) => v1 + v2 }
    assert(addOne == ImmutableSeq(vv.map(_ + 1): _*))

    // 2. ===运算
//    val equals1: ImmutableSeq[Boolean] = seq === Seq("1", "1", "2", "3", "4")
//    val equals2: ImmutableSeq[Boolean] = seq === Array("1", "1", "2", "3", "4")
//
//    assert(equals1 == ImmutableSeq(true, false, true, true, true))
//    assert(equals2 == ImmutableSeq(true, false, true, true, true))
//    assert(seq === "3" == ImmutableSeq(false, false, false, true, false))

    // 3.and 运算

  }

}
