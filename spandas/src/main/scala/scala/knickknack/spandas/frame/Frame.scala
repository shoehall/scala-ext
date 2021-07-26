package scala.knickknack.spandas.frame

import scala.collection.frame.{HasSchema, Schema}
import scala.knickknack.spandas.row.Row
import scala.knickknack.spandas.common.{CanBuildFrameFrom, Series}
import scala.collection.rich._
/*
          DataFrameLight        DataFrame         DataFrameLightGrouped    DataFrameGrouped
 values   Iterator[Row]         Array[Row]        Iterator[(Any, Row)]     Array[(Any, DataFrame)]
 apply    row: Row => T         row: Row => T     DataFrameLight => DL     DataFrame => DF
 需要:    Iterator[Row], Row    Array[Row], Row   DataFrameLight           DataFrame
 */

// 结构化数据模型
// - transform/apply
/**
 * 结构化数据模型, 行数据模型
 * - transform/map 等行处理方法
 * - todo: 列处理
 * - aggregate, 沿用spark和scala的风格, 不采用接收python函数的形式
 * @tparam Repr  自身类型
 */
trait Frame[Repr] extends HasSchema {
  // 留个类型变量给子类调用
  type This = Repr

  def repr: Repr = this.asInstanceOf[Repr]

  def values: TraversableOnce[Row]

  /**
   * 行变换
   * @param fun
   * @param ev
   * @tparam T 需要是一个能够根据类型隐式转换得到Schema的类型
   */
//  def transform[T, That](fun: Row => T)(implicit bf: CanBuildFrameFrom[This, Row, That], rowBuilder: Row.CanBuildBy[T, Row], schemaBuilder: Schema.CanBuildBy[T, Schema]): That = {
//    val vv = values.map(row => rowBuilder(fun(row)))
////    bf.apply(vv, schemaBuilder.apply())
//    throw new UnsupportedOperationException
//  }

  def aggregate[R0, R1](z: R0)(op: (R0, Row) => R0): Iterator[R0] =
    throw new UnsupportedOperationException()

  def aggregate[T, R](op: Series[T] => Series[R]): Repr =
    throw new UnsupportedOperationException()

  def reduce(op: (Row, Row) => Row): Repr =
    throw new UnsupportedOperationException()

  def sample(n: Int): Repr = throw new UnsupportedOperationException()
}

object Frame

/**
 * 结构化数据的分组类型
 * @tparam F 对应的原Frame类型
 */
trait FrameGrouped[F, Block] extends HasSchema {

  def values: TraversableOnce[(Any, Row)]

  def transform(fun: Block => Block): F

  def aggregate[R0, R1](z: R0)(op: (R0, Row) => R0)(con: R0 => R1): Iterator[(Any, R1)] = {
    values.toIterator.combineByKey[R0, R1](z: R0)(op: (R0, Row) => R0, con: R0 => R1)
  }

  def reduce(op: (Row, Row) => Row): F =
    throw new UnsupportedOperationException()

  def sample(n: Int): F = throw new UnsupportedOperationException()

  def buildBlock(values: Array[Row]): Block

  def groups: TraversableOnce[Block] = groupedRows.map{ case (_, v) => buildBlock(v) }

  def groupedRows: TraversableOnce[(Any, Array[Row])] = values.toIterator.group
}

trait CanGroup[Repr, Grouped] {
  def groupBy(name: String): Grouped
}


