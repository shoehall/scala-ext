package scala.io.jdbc

import java.util.Properties
import scala.collection.convert.decorateAsScala._
import scala.collection.mutable

/**
 * [[JdbcSource]]的工厂函数
 */
class JdbcSourceBuilder(val properties: Properties = new Properties()) extends HasDialect {

  import JdbcSourceBuilder._

  def this(params: Map[String, String]) =
    this({
      val p = new Properties()
      params.foreach {
        case (k, v) =>
          p.put(k, v)
      }
      p
    })

  def this(url: String, username: String, password: String, driver: String) =
    this(Map(JdbcSourceBuilder.Url_tag -> url, JdbcSourceBuilder.Username -> username, JdbcSourceBuilder.Password -> password, JdbcSourceBuilder.Driver -> driver))

  def this(url: String, username: String, password: String) =
    this(Map(JdbcSourceBuilder.Url_tag -> url, JdbcSourceBuilder.Username -> username, JdbcSourceBuilder.Password -> password))

  /* 通过ip port dbname构造, 需要指定dialect/或driver */
  def this(ip: String, port: String, dbname: String, username: String, password: String, driver: String) =
    this(Map(JdbcSourceBuilder.Ip_tag -> ip, JdbcSourceBuilder.Port -> port, JdbcSourceBuilder.Dbname -> dbname, JdbcSourceBuilder.Username -> username, JdbcSourceBuilder.Password -> password, JdbcSourceBuilder.Driver -> driver))

  def this(ip: String, port: String, dbname: String, username: String, password: String) =
    this(Map(JdbcSourceBuilder.Ip_tag -> ip, JdbcSourceBuilder.Port -> port, JdbcSourceBuilder.Dbname -> dbname, JdbcSourceBuilder.Username -> username, JdbcSourceBuilder.Password -> password))

  def setProperty(name: String, value: String): this.type = {
    properties.setProperty(name, value)
    this
  }

  def setProperty(property: Map[String, String]): this.type = {
    property.foreach {
      case (name, value) =>
        setProperty(name, value)
    }
    this
  }

  def setProperty(property: mutable.Map[String, String]): this.type = setProperty(property.toMap)

  def setProperty(property: java.util.Map[String, String]): this.type = setProperty(property.asScala)

  def setProperty(property: Properties): this.type = setProperty(property.asScala)


  def setUrl(value: String): this.type = setProperty(Url_tag, value)

  def setIp(value: String): this.type = setProperty(JdbcSourceBuilder.Ip_tag, value)

  def setPort(value: String): this.type = setProperty(Port, value)

  def setDbname(value: String): this.type = setProperty(Dbname, value)

  def setUsername(value: String): this.type = setProperty(Username, value)

  def setPassword(value: String): this.type = setProperty(Password, value)

  def setDriver(value: String): this.type = setProperty(Driver, value)

  def setDialect(value: String): this.type = setDialect(JdbcDialect(value))

  override def setDialect(value: JdbcDialect): this.type = {
    super.setDialect(value)
    setProperty(Dialect, value.name)
    this
  }

  private def contains(logic: String)(keys: String*): Boolean =
    if (logic == "any")
      keys.exists(name => properties.containsKey(name))
    else
      keys.forall(name => properties.containsKey(name))

  private def getProperty(name: String): String = properties.get(name).asInstanceOf[String]

  /**
   * assert the properties is valid for a jdbc.
   */
  private def validProperties(): Unit = {
    // 查找url
    if (!properties.containsKey(Url_tag)) {
      require(contains("all")(Ip_tag, Port, Dbname), "ip + port + dbname or an url is needed to connect database")
      require(contains("any")(Dialect, Driver), "dialect or driver of the database is needed when the url is not assigned")
      val ul =
        if (contains("any")(Dialect)) {
          if (getDialect == NoopDialect)
            setDialect(JdbcDialect(getProperty(Dialect)))
          require(getDialect != NoopDialect) // 如果set函数正确此时必然不为null
          // todo: 验证下端口的有效性 0-65535
          getDialect.pasteUrl(getProperty(Ip_tag), getProperty(Port), getProperty(Dbname))
        } else {
          setDialect(JdbcDialect.getDialectFromDriver(getProperty(Driver)))
          getDialect.pasteUrl(getProperty(Ip_tag), getProperty(Port), getProperty(Dbname))
        }
      setUrl(ul)
    }

    // 查找dialect
    if (getDialect == NoopDialect) {
      val dlt =
        if (contains("any")(Dialect))
          JdbcDialect(getProperty(Dialect))
        else {
          require(contains("any")(Url_tag, Driver), "The url or driver is needed when the dialect of the database is not assigned")
          if (contains("any")(Url_tag)) {
            JdbcDialect.getDialectFromUrl(getProperty(Url_tag))
          } else {
            JdbcDialect.getDialectFromDriver(getProperty(Driver))
          }
        }
      setDialect(dlt)
    }

    // 查找username
    require(contains("any")(Username), "username is needed to connect a database")
    // 查找password
    require(contains("any")(Password), "Password is needed to connect a database")
    // 查找driver
    if (!contains("any")(Driver)) {
      setDriver(getDialect.driver)
    }
  }

  def build(): JdbcSource = {
    validProperties()
    val url = getProperty(Url_tag)
    val username: String = getProperty(Username)
    val password: String = getProperty(Password)
    val driver: String = getProperty(Driver)

    new JdbcSource(url, username, password, driver).setDialect(getDialect)
  }

  def mysql(): JdbcSource = {
    setDialect(MySQLDialect)
    build()
  }

  def oracle(): JdbcSource = throw new UnsupportedOperationException()

  def postgresql(): JdbcSource = throw new UnsupportedOperationException()

  /* url构造 */
  def build(url: String, username: String, password: String, driver: String, dialect: JdbcDialect): JdbcSource =
    new JdbcSource(url, username, password, driver).setDialect(dialect)
}

object JdbcSourceBuilder {
  def apply(): JdbcSourceBuilder = new JdbcSourceBuilder()


  private[jdbc] val Url_tag: String = "url"
  private[jdbc] val Ip_tag: String = "ip"
  private[jdbc] val Port: String = "port"
  private[jdbc] val Dbname: String = "dbname"
  private[jdbc] val Username: String = "username"
  private[jdbc] val Password: String = "password"
  private[jdbc] val Driver: String = "driver"
  private[jdbc] val Dialect: String = "dialect"
}

