package scala.collection.rich

import org.scalatest.FunSuite

import java.util.Random
import scala.collection.mutable

class RichPairIteratorSuite extends FunSuite {
  val pairValues: Array[(String, Int)] = Array(
    ("A", 1),
    ("A", 2),
    ("A", 3),
    ("A", 4),
    ("A", 5),
    ("A", 6),
    ("B", 1),
    ("B", 2),
    ("B", 3),
    ("B", 4),
    ("B", 5),
    ("B", 6),
    ("B", 7),
    ("C", 1),
    ("C", 2),
    ("C", 3),
    ("D", 4),
    ("D", 5)
  )

  test("mapValues") {
    val res = mutable.ArrayBuilder.make[(String, Int)]()
    val iterator: Iterator[(String, Int)] = pairValues.iterator
    iterator.mapValues(v => v - 1).foreach { element => res += element }
    assert(res.result() sameElements pairValues.map { case (k, v) => (k, v - 1) })
  }

  test("group") {
    pairValues.iterator.group.foreach {
      case (key, values) =>
        println(key, values.mkString(","))
    }
  }

  test("一个组group") {
    val iterator2 = Array(("A", 1), ("A", 2)).iterator
    println(iterator2.hasNext)
    val grouped = iterator2.group
    println(grouped.hasNext)
    grouped.foreach {
      case (key, values) =>
        println(key, values.mkString(","))
    }
  }


  test("foldLeftByKey") {
    // 分组求和
    val res = pairValues.iterator.foldLeftByKey(0)(_ + _)
    println(res.hasNext)
    res.foreach(println)
    //    println(pairValues.map(_._2).sum)

    // 分组合并到Array
    pairValues.iterator.foldLeftByKey(Array.empty[Int])(_ :+ _).foreach {
      case (k, v) =>
        println(k, v.mkString(" | "))
    }

    // 分组抽样, 每组抽两个
    val n = 2
    val rd = new Random(123L)
    val res2 = pairValues.iterator.foldLeftByKey((Array.fill[(Int, Double)](n)(0, 0.0), 0.0, 0)) {
      case ((res, minValue, minIndex), value) =>
        val random = rd.nextDouble()
        if (random > minValue) {
          res(minIndex) = (value, random)

          var idx = -1
          val (newMinValue, newMinIdx) = res.foldLeft[(Double, Int)](random, minIndex) {
            case ((minVlu, minIdx), (_, vlu)) =>
              idx += 1
              if (minVlu > vlu) {
                (vlu, idx)
              } else
                (minVlu, minIdx)
          }

          (res, newMinValue, newMinIdx)
        } else
          (res, minValue, minIndex)
    }.values.flatMap {
      case (res, _, _) =>
        res
    }

    res2.foreach {
      println
    }
  }

  test("直接将Iterator[(K, V)]group为Iterator[Iterator[(K, V)]]") {
    /*
    结论, 不可能:
    首先要知道外Iterator hasNext才能迭代(前几组);
    但内Iterator迭代完才能知道外Iterator是否有hasNext(想象下最后一组)
     */
  }

  test("RichArray") {
    //    import scala.collection.rich.RichArray.richTraversableOnce
    def f(a: Iterator[Int]) = 0

    val values = Array(1, 2, 3)

    println(values.top(10))
    println(values.mkString(","))
  }

  test("查看Iterator的take") {
    val iterator: Iterator[Int] = new Iterator[Int] {
      val values = Array(1, 2, 3, 4, 5, 6, 7)

      var i = -1
      override def hasNext: Boolean = i < values.length - 1

      override def next(): Int = if(hasNext) {i += 1; values(i) } else Iterator.empty.next()
    }
    val iterator1: Iterator[Int] = iterator.take(5)
    iterator.foreach(println)
    println("=" * 80)
    iterator1.foreach(println)

    // 结论:
    // 通过Array构造的Iterator take和一般的Iterator take效果并不相同.
  }

  test("测试Iterator Map") {
    val iterator = Array((1, 2), (3, 4), (5, 6)).iterator

    val v1 = iterator.map(_._1)
    val v2 = iterator.map(_._2)

    v1.foreach(println)
    v2.foreach(println)


  }


}
