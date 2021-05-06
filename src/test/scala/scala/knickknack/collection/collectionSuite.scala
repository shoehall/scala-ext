package scala.knickknack.collection

import org.scalatest.FunSuite

class collectionSuite extends FunSuite {
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
    val iterator = pairValues.iterator
    iterator.mapValues(v => v - 1).foreach(println)
  }

  test("group") {
    pairValues.iterator.group.foreach {
      case (key, values) =>
        println(key, values.mkString(","))
    }
  }

  test("一个组") {
    val iterator2 = Array(("A", 1), ("A", 2)).iterator
    println(iterator2.hasNext)
    val grouped = iterator2.group
    println(grouped.hasNext)
    grouped.foreach {
      case (key, values) =>
        println(key, values.mkString(","))
    }



  }

}
