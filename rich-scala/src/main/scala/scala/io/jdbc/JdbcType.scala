package scala.io.jdbc

/**
 * JDBC Type
 *
 * @param sqlType the sql type in the [[java.sql.Types]]
 */
class JdbcType(val sqlType: Int) {
  def toString(dialect: JdbcDialect): String = dialect.getSQLTypeName(this)

  override def toString: String = NoopDialect.getSQLTypeName(this)

  private val properties = scala.collection.mutable.Map.empty[String, Any]

  def set(name: String, value: Any): Unit = properties.put(name, value)

  def getAs[T](name: String, default: T): T =
    if(properties contains name)
      properties(name).asInstanceOf[T]
    else
      default

}

object JdbcType {
  def apply(jdbcType: Int): JdbcType = new JdbcType(jdbcType)
  val PRECISION: String = "PRECISION"
  val SCALE: String = "SCALE"
}
