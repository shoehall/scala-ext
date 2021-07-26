package scala.collection.frame

trait CastAs[T] {
  def cast(value: Any): T
}

object CastAs {
  implicit def castAsString: CastAs[String] = new CastAs[String] {
    override def cast(value: Any): String = if(value == null) null else value.toString
  }

  implicit def castAsBoolean: CastAs[Boolean] = new CastAs[Boolean] {
    override def cast(value: Any): Boolean = if(value == null) null.asInstanceOf[Boolean] else value.toString.toBoolean
  }

  implicit def castAsInt: CastAs[Int] = new CastAs[Int] {
    override def cast(value: Any): Int =
      if(value == null)
        throw new NullPointerException("null could not cast as Int")
      else
        value.toString.toDouble.toInt
  }

}
