package scala.knickknack.spandas.common

import org.scalatest.FunSuite

import scala.collection.mutable

class UFuncSuite extends FunSuite {
  val numericTypes: Array[String] = Array("Byte", "Char", "Short", "Int", "Long", "Float", "Double")

  def printUImpl1(name: String, typeName: String, supportedTypes: Array[Array[String]], fun: String) = {
    println(s"object ${name}Ops  extends UFunc {")
    println()
    supportedTypes.foreach {
      dTypes =>

        println(s"  implicit def ${name.toLowerCase}_${dTypes.mkString("_")} = new $typeName[${dTypes.mkString(", ")}] {")
        val params = dTypes.dropRight(1).zipWithIndex.map {
          case (nm, idx) =>
            if (idx == 0) s"v: $nm" else s"v${idx + 1}: $nm"
        }
        println(s"    override def apply(${params.mkString(", ")}): ${dTypes.last} = $fun")
        println(s"  }")
        println()
    }


    println("}")
  }

  test("NegativeOps") {
    val name = "Negative"
    val typeName = "UImpl"
    val supportedTypes = numericTypes.map {
      case s@("Char" | "Byte" | "Short") => Array(s, "Int")
      case other => Array(other, other)
    }
    val fun = "-v"
    printUImpl1(name, typeName, supportedTypes, fun)
  }

  test("Plus") {
    val name = "Plus"
    val typeName = "UImpl2"

    val levels = (numericTypes :+ "String").zipWithIndex.toMap
    val allTypes = numericTypes :+ "String"
    val collector = mutable.ArrayBuilder.make[Array[String]]()
    numericTypes.foreach {
      dType1 =>
        if (dType1 != "String") {
          allTypes.foreach {
            dType2 =>
              val l1 = levels(dType1)
              val l2 = levels(dType2)
              val ss = if (l1 <= 3 && l2 <= 3) {
                Array(dType1, dType2, allTypes(3))
              } else {
                Array(dType1, dType2, allTypes(scala.math.max(l1, l2)))
              }

              collector += ss
          }
        }
    }

    val supportedTypes = collector.result()

    val fun = "v + v2"

    printUImpl1(name, typeName, supportedTypes, fun)
  }


  test("查看是否符合语法") {

    object PlusOps  extends UFunc {

      implicit def plus_Byte_Byte_Int = new UImpl2[Byte, Byte, Int] {
        override def apply(v: Byte, v2: Byte): Int = v + v2
      }

      implicit def plus_Byte_Char_Int = new UImpl2[Byte, Char, Int] {
        override def apply(v: Byte, v2: Char): Int = v + v2
      }

      implicit def plus_Byte_Short_Int = new UImpl2[Byte, Short, Int] {
        override def apply(v: Byte, v2: Short): Int = v + v2
      }

      implicit def plus_Byte_Int_Int = new UImpl2[Byte, Int, Int] {
        override def apply(v: Byte, v2: Int): Int = v + v2
      }

      implicit def plus_Byte_Long_Long = new UImpl2[Byte, Long, Long] {
        override def apply(v: Byte, v2: Long): Long = v + v2
      }

      implicit def plus_Byte_Float_Float = new UImpl2[Byte, Float, Float] {
        override def apply(v: Byte, v2: Float): Float = v + v2
      }

      implicit def plus_Byte_Double_Double = new UImpl2[Byte, Double, Double] {
        override def apply(v: Byte, v2: Double): Double = v + v2
      }

      implicit def plus_Byte_String_String = new UImpl2[Byte, String, String] {
        override def apply(v: Byte, v2: String): String = v + v2
      }

      implicit def plus_Char_Byte_Int = new UImpl2[Char, Byte, Int] {
        override def apply(v: Char, v2: Byte): Int = v + v2
      }

      implicit def plus_Char_Char_Int = new UImpl2[Char, Char, Int] {
        override def apply(v: Char, v2: Char): Int = v + v2
      }

      implicit def plus_Char_Short_Int = new UImpl2[Char, Short, Int] {
        override def apply(v: Char, v2: Short): Int = v + v2
      }

      implicit def plus_Char_Int_Int = new UImpl2[Char, Int, Int] {
        override def apply(v: Char, v2: Int): Int = v + v2
      }

      implicit def plus_Char_Long_Long = new UImpl2[Char, Long, Long] {
        override def apply(v: Char, v2: Long): Long = v + v2
      }

      implicit def plus_Char_Float_Float = new UImpl2[Char, Float, Float] {
        override def apply(v: Char, v2: Float): Float = v + v2
      }

      implicit def plus_Char_Double_Double = new UImpl2[Char, Double, Double] {
        override def apply(v: Char, v2: Double): Double = v + v2
      }

      implicit def plus_Char_String_String = new UImpl2[Char, String, String] {
        override def apply(v: Char, v2: String): String = v + v2
      }

      implicit def plus_Short_Byte_Int = new UImpl2[Short, Byte, Int] {
        override def apply(v: Short, v2: Byte): Int = v + v2
      }

      implicit def plus_Short_Char_Int = new UImpl2[Short, Char, Int] {
        override def apply(v: Short, v2: Char): Int = v + v2
      }

      implicit def plus_Short_Short_Int = new UImpl2[Short, Short, Int] {
        override def apply(v: Short, v2: Short): Int = v + v2
      }

      implicit def plus_Short_Int_Int = new UImpl2[Short, Int, Int] {
        override def apply(v: Short, v2: Int): Int = v + v2
      }

      implicit def plus_Short_Long_Long = new UImpl2[Short, Long, Long] {
        override def apply(v: Short, v2: Long): Long = v + v2
      }

      implicit def plus_Short_Float_Float = new UImpl2[Short, Float, Float] {
        override def apply(v: Short, v2: Float): Float = v + v2
      }

      implicit def plus_Short_Double_Double = new UImpl2[Short, Double, Double] {
        override def apply(v: Short, v2: Double): Double = v + v2
      }

      implicit def plus_Short_String_String = new UImpl2[Short, String, String] {
        override def apply(v: Short, v2: String): String = v + v2
      }

      implicit def plus_Int_Byte_Int = new UImpl2[Int, Byte, Int] {
        override def apply(v: Int, v2: Byte): Int = v + v2
      }

      implicit def plus_Int_Char_Int = new UImpl2[Int, Char, Int] {
        override def apply(v: Int, v2: Char): Int = v + v2
      }

      implicit def plus_Int_Short_Int = new UImpl2[Int, Short, Int] {
        override def apply(v: Int, v2: Short): Int = v + v2
      }

      implicit def plus_Int_Int_Int = new UImpl2[Int, Int, Int] {
        override def apply(v: Int, v2: Int): Int = v + v2
      }

      implicit def plus_Int_Long_Long = new UImpl2[Int, Long, Long] {
        override def apply(v: Int, v2: Long): Long = v + v2
      }

      implicit def plus_Int_Float_Float = new UImpl2[Int, Float, Float] {
        override def apply(v: Int, v2: Float): Float = v + v2
      }

      implicit def plus_Int_Double_Double = new UImpl2[Int, Double, Double] {
        override def apply(v: Int, v2: Double): Double = v + v2
      }

      implicit def plus_Int_String_String = new UImpl2[Int, String, String] {
        override def apply(v: Int, v2: String): String = v + v2
      }

      implicit def plus_Long_Byte_Long = new UImpl2[Long, Byte, Long] {
        override def apply(v: Long, v2: Byte): Long = v + v2
      }

      implicit def plus_Long_Char_Long = new UImpl2[Long, Char, Long] {
        override def apply(v: Long, v2: Char): Long = v + v2
      }

      implicit def plus_Long_Short_Long = new UImpl2[Long, Short, Long] {
        override def apply(v: Long, v2: Short): Long = v + v2
      }

      implicit def plus_Long_Int_Long = new UImpl2[Long, Int, Long] {
        override def apply(v: Long, v2: Int): Long = v + v2
      }

      implicit def plus_Long_Long_Long = new UImpl2[Long, Long, Long] {
        override def apply(v: Long, v2: Long): Long = v + v2
      }

      implicit def plus_Long_Float_Float = new UImpl2[Long, Float, Float] {
        override def apply(v: Long, v2: Float): Float = v + v2
      }

      implicit def plus_Long_Double_Double = new UImpl2[Long, Double, Double] {
        override def apply(v: Long, v2: Double): Double = v + v2
      }

      implicit def plus_Long_String_String = new UImpl2[Long, String, String] {
        override def apply(v: Long, v2: String): String = v + v2
      }

      implicit def plus_Float_Byte_Float = new UImpl2[Float, Byte, Float] {
        override def apply(v: Float, v2: Byte): Float = v + v2
      }

      implicit def plus_Float_Char_Float = new UImpl2[Float, Char, Float] {
        override def apply(v: Float, v2: Char): Float = v + v2
      }

      implicit def plus_Float_Short_Float = new UImpl2[Float, Short, Float] {
        override def apply(v: Float, v2: Short): Float = v + v2
      }

      implicit def plus_Float_Int_Float = new UImpl2[Float, Int, Float] {
        override def apply(v: Float, v2: Int): Float = v + v2
      }

      implicit def plus_Float_Long_Float = new UImpl2[Float, Long, Float] {
        override def apply(v: Float, v2: Long): Float = v + v2
      }

      implicit def plus_Float_Float_Float = new UImpl2[Float, Float, Float] {
        override def apply(v: Float, v2: Float): Float = v + v2
      }

      implicit def plus_Float_Double_Double = new UImpl2[Float, Double, Double] {
        override def apply(v: Float, v2: Double): Double = v + v2
      }

      implicit def plus_Float_String_String = new UImpl2[Float, String, String] {
        override def apply(v: Float, v2: String): String = v + v2
      }

      implicit def plus_Double_Byte_Double = new UImpl2[Double, Byte, Double] {
        override def apply(v: Double, v2: Byte): Double = v + v2
      }

      implicit def plus_Double_Char_Double = new UImpl2[Double, Char, Double] {
        override def apply(v: Double, v2: Char): Double = v + v2
      }

      implicit def plus_Double_Short_Double = new UImpl2[Double, Short, Double] {
        override def apply(v: Double, v2: Short): Double = v + v2
      }

      implicit def plus_Double_Int_Double = new UImpl2[Double, Int, Double] {
        override def apply(v: Double, v2: Int): Double = v + v2
      }

      implicit def plus_Double_Long_Double = new UImpl2[Double, Long, Double] {
        override def apply(v: Double, v2: Long): Double = v + v2
      }

      implicit def plus_Double_Float_Double = new UImpl2[Double, Float, Double] {
        override def apply(v: Double, v2: Float): Double = v + v2
      }

      implicit def plus_Double_Double_Double = new UImpl2[Double, Double, Double] {
        override def apply(v: Double, v2: Double): Double = v + v2
      }

      implicit def plus_Double_String_String = new UImpl2[Double, String, String] {
        override def apply(v: Double, v2: String): String = v + v2
      }

    }

  }

}
