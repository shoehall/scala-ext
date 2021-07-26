package scala.knickknack.spandas.frame

import scala.collection.frame.Schema
import scala.collection.rich._
import scala.knickknack.spandas.common.CanBuildFrameFrom
import scala.knickknack.spandas.row.Row

/**
 * 一个轻量的Row操作接口
 * ----
 * - 数据不占用内存, 即用即消
 */
trait LightFrame extends Frame[LightFrame] with CanGroup[LightFrame, LightFrameGrouped] {
  override def groupBy(name: String): LightFrameGrouped = {
    val sc = this.schema
    val i = fieldIndex(name)
    val vv = values.map(row => (row(i), row))
    new LightFrameGrouped {
      override def schema: Schema = sc

      override def values: TraversableOnce[(Any, Row)] = vv
    }
  }
}

object LightFrame {
  def apply(rows: TraversableOnce[Row], sc: Schema): LightFrame =
    new LightFrame{
      override def values: TraversableOnce[Row] = rows
      override def schema: Schema = sc
    }


//  implicit def can_build_RowFrameLight_to_RowFrameLight  = new CanBuildFrameFrom[LightFrame, Row, LightFrame] {
//    override def apply(from: TraversableOnce[Row], schema: Schema): LightFrame = {
//      LightFrame(from, schema)
//    }
//  }
}

trait LightFrameGrouped extends FrameGrouped[LightFrame, DataFrame]{
  override def transform(fun: DataFrame => DataFrame): LightFrame = {
    val frames = groups.map {
      data =>
        fun(data)
    }.toIterator

    val (head, tastedFrames) = frames.taste()

    LightFrame(tastedFrames.flatMap(df => df.values), head.schema)
  }

  override def buildBlock(values: Array[Row]): DataFrame = {
    throw new UnsupportedOperationException()
  }
}

object LightFrameGrouped


