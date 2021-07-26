package scala.knickknack.spandas.common

import org.scalatest.FunSuite

class GenerateByValuesSuite extends FunSuite {
  class HasValues[T](override val values: T*) extends GenerateByValues[T]
  object HasValues {
    def apply[T](values: T*) = new HasValues[T](values:_*)
  }

  test("dType") {
    val index = new HasValues[String]("1", "0", "2", "3", "4")
    assert(index.dType == "string")
    assert(HasValues[Int](1, 0, 2, 3, 4).dType == "int")

  }

  /**
   * equal方法:
   * - 先比较类型, 再比较每个元素是否相等;
   * - 比较元素是否相等时不考虑元素类型
   * hashCode: 如果equal hash值应该相等
   *
   * note:
   * - 元素比较时, 需要注意float和double的表示方式不同, 比如scala中: 0.2d != 0.2f, 数值为整数/2的整数次方时才会相等,.
   */
  test("equal和hash方法") {
    assert(HasValues(1, 2, 3) == HasValues(1, 2, 3))

    val v1: HasValues[Int] = HasValues(1, 2, 3)
    val v2: HasValues[Double] = HasValues(1.0, 2.0, 3.0)
    val v3 = HasValues("1", "2", "3")
    assert(v1 == HasValues(1, 2, 3))
    assert(v1 != v3)
    assert(v1 != Array(1, 2, 3))
    assert(v1 == v2)
    assert(v1.hashCode == v2.hashCode)
    assert(0.2d != 0.2f)

  }

}
