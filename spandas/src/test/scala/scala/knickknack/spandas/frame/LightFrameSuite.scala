package scala.knickknack.spandas.frame

import org.scalatest.FunSuite

import scala.reflect.ClassTag

class LightFrameSuite extends FunSuite {
  test("语法特性") {
    val frame: LightFrame = null

//    val frame1: LightFrame = frame.transform(row => row)
    val frame2: LightFrame = frame.groupBy("c1").transform { df: DataFrame => df }

    val uu: LightFrameGrouped = frame.groupBy("c1")
    frame.groupBy("cc1")
  }

  test("测试一下spark风格的transform和action操作") {


  }

}
