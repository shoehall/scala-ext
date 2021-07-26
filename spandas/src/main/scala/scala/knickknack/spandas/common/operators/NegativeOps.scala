package scala.knickknack.spandas.common.operators

import scala.knickknack.spandas.common.UFunc

/**
 * 负数运算, 只支持: "Byte", "Char", "Short", "Int", "Float", "Long", "Double"
 */
object NegativeOps  extends UFunc {
  /* created by code (in test source UFuncSuite.scala):
    val name = "Negative"
    val typeName = "UImpl"
    val supportedTypes = numericTypes.map {
      case s@("Char" | "Byte" | "Short") => Array(s, "Int")
      case other => Array(other, other)
    }
    val fun = "-v"
    printUImpl1(name, typeName, supportedTypes, fun)
   */

  implicit def negative_Byte_Int: NegativeOps.UImpl[Byte, Int] = new UImpl[Byte, Int] {
    override def apply(v: Byte): Int = -v
  }

  implicit def negative_Char_Int = new UImpl[Char, Int] {
    override def apply(v: Char): Int = -v
  }

  implicit def negative_Short_Int = new UImpl[Short, Int] {
    override def apply(v: Short): Int = -v
  }

  implicit def negative_Int_Int = new UImpl[Int, Int] {
    override def apply(v: Int): Int = -v
  }

  implicit def negative_Float_Float = new UImpl[Float, Float] {
    override def apply(v: Float): Float = -v
  }

  implicit def negative_Long_Long = new UImpl[Long, Long] {
    override def apply(v: Long): Long = -v
  }

  implicit def negative_Double_Double = new UImpl[Double, Double] {
    override def apply(v: Double): Double = -v
  }

}


