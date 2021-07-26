package scala.collection.frame

import scala.reflect.runtime.universe.TypeTag

class Schema(val fields: Array[Field]) {
  lazy val index4names: Map[String, Int] = fields.map(_.name).zipWithIndex.toMap
  def fieldIndex(name: String): Int = index4names(name)

  def toDDL: String = fields.map(field => s"${field.name} ${field.dType.name}").mkString(",")
  override def toString: String = s"Schema($toDDL)"

  def add(field: Field) = new Schema(fields :+ field)
}

object Schema {
  def apply(fields: Array[Field]) = new Schema(fields)

  def buildBy[T <: Product : TypeTag]: Schema = throw new UnsupportedOperationException()
}

trait HasSchema {
  def schema: Schema
  def fieldIndex(name: String): Int = schema.fieldIndex(name)
}

