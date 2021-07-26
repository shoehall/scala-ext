package scala.collection.frame

trait Row {
  val values: Array[Any]

  def length: Int = values.length

  def update(index: Int, value: Any): Unit = values.update(index, value)

  def update(name: String, value: Any): Unit = update(fieldIndex(name), value)

  def toSeq: Seq[Any] = values
  override def toString: String = values.mkString("[", ",", "]")

  def schema: Schema = throw new UnsupportedOperationException("schema is not defined.")

  def fieldIndex(name: String): Int = schema.fieldIndex(name)

  def apply(i: Int): Any = values(i)

  def apply(name: String): Any = apply(fieldIndex(name))

  def get(name: String): Any = apply(name)

  def get(columnIndex: Int): Any = apply(columnIndex)

  def getAs[T](i: Int): T = get(i).asInstanceOf[T]

  def getAs[T](columnName: String): T = getAs[T](fieldIndex(columnName))

  /**
   * 提取元素并转为某种类型, 用于类型之间的转换
   *
   * @param i index
   * @tparam T type T
   * @return
   */
  def castAs[T: CastAs](i: Int): T = implicitly[CastAs[T]].cast(get(i))

  def castAs[T: CastAs](name: String): T = castAs[T](fieldIndex(name))
}

object Row {
  def empty: Row = new Row {
    override val values: Array[Any] = Array.empty[Any]
  }

  def merge(rows: Row*) = {

  }

  def apply(values0: Any*): Row = new Row {
    override val values: Array[Any] = values0.toArray
  }

  def fromSeq(values0: Seq[Any]): Row = apply(values0:_*)
}

case class RowWithSchema(override val values: Array[Any], override val schema: Schema) extends Row
