package scala.knickknack.spandas.frame

import scala.collection.frame.{HasSchema, Schema}
import scala.knickknack.spandas.row.Row


trait DataFrame extends Frame[DataFrame] with CanGroup[DataFrame, DataFrameGrouped] {
  def light: LightFrame =
    throw new UnsupportedOperationException()

  override def groupBy(name: String): DataFrameGrouped =
    throw new UnsupportedOperationException()
}

object DataFrame {
  def apply(values: Array[Row]): DataFrame = throw new UnsupportedOperationException()

  def apply(values: Array[Row], schema: Schema): DataFrame = throw new UnsupportedOperationException()

}

trait DataFrameGrouped extends HasSchema


