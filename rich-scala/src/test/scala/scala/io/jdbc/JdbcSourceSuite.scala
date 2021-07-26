package scala.io.jdbc

import org.scalatest.{FunSuite, durations}

import java.sql.{Connection, PreparedStatement, ResultSet, Statement, Timestamp}
import java.text.SimpleDateFormat
import java.util.Random
import scala.io.jdbc.rich.RichStatement.richStatement
import scala.reflect.ClassTag

case class ResData(emiplatname: String, emiplatnat: String, lat: Double, lon: Double, hgt: Double, obtmet: String, newRF: Double, newPri: Double, locTim: Timestamp)

class JdbcSourceSuite extends FunSuite {
  test("JdbcSourceBuilder构建MySQL连接") {
    // need a mysql env
    val ip = "localhost"
    val port = "3306"
    val username = "root"
    val password = "123456"
    val dbname = "mysql"

    val source: JdbcSource = JdbcSourceBuilder()
      .setIp(ip)
      .setPort(port)
      .setDbname(dbname)
      .setUsername(username)
      .setPassword(password)
      .mysql()

    source.withStatement {
      statement =>
        val rs = statement.executeQuery("select * from user")

        while (rs.next()) {
          println(rs.getString("User"))
          println(rs.getString(1)) // jdbc的schema从1开始
          println(rs.getInt("max_updates") + 1) // get什么对数据库中原本的类型并不敏感
        }
    }
  }

  // need a mysql env
  test("RichStatement read") {
    // 此处需要import rich类

    // todo: 多试一些类型
    val ip = "localhost"
    val port = "3306"
    val username = "root"
    val password = "123456"
    val dbname = "mysql"

    val source: JdbcSource = JdbcSourceBuilder()
      .setIp(ip)
      .setPort(port)
      .setDbname(dbname)
      .setUsername(username)
      .setPassword(password)
      .mysql()

    source.withStatement {
      statement =>
        statement.readAs[String]("select user from user;").foreach(println)
    }

    source.withStatement {
      statement =>
        statement.readAs[Long]("select max_updates from user").foreach(println)
    }

  }


  test("JdbcSource read") {
    // need a mysql env
    val ip = "localhost"
    val port = "3306"
    val username = "root"
    val password = "123456"
    val dbname = "mysql"

    val source: JdbcSource = JdbcSourceBuilder()
      .setIp(ip)
      .setPort(port)
      .setDbname(dbname)
      .setUsername(username)
      .setPassword(password)
      .mysql()

    source.readAs[(String, Long)]("select user, max_updates from user;").foreach(println)

  }


  test("尝试下type class") {
    trait ValueWrapper[T] {
      def to(preparedStatement: PreparedStatement, value: T, index: Int): Unit

      def to(preparedStatement: PreparedStatement, value: T): Unit = to(preparedStatement, value, defaultIndex)

      def defaultIndex: Int = 1

      def toPrepareWithSize(preparedStatement: PreparedStatement, value: T, index: Int, size: Int): Unit = throw new UnsupportedOperationException

    }


    import shapeless._
    object ValueWrapper {
      implicit def canReadAsString = new ValueWrapper[String] {
        override def to(preparedStatement: PreparedStatement, value: String, index: Int): Unit = {
          println(s"setString($index)")
        }
      }

      implicit def canReadAsInt = new ValueWrapper[Int] {
        override def to(preparedStatement: PreparedStatement, value: Int, index: Int): Unit = {
          println(s"setInt($index)")
        }
      }

      implicit val hNilRandom = new ValueWrapper[HNil] {
        override def to(preparedStatement: PreparedStatement, value: HNil, index: Int): Unit = {
          println(s"Nil($defaultIndex)")
        }

        override def defaultIndex: Int = 0

        override def toPrepareWithSize(preparedStatement: PreparedStatement, value: HNil, index: Int, size: Int): Unit = {
          println(s"Nil(${size - index})")
        }
      }

      implicit def hListRandom[T, HL <: HList](implicit tRandom: ValueWrapper[T],
                                               hListRandom: ValueWrapper[HL]) = new ValueWrapper[::[T, HL]] {
        override def to(preparedStatement: PreparedStatement, value: ::[T, HL]): Unit = {
          toPrepareWithSize(preparedStatement, value, 0, defaultIndex)
        }

        override def toPrepareWithSize(preparedStatement: PreparedStatement, value: ::[T, HL], index: Int, size: Int): Unit = {
          tRandom.to(preparedStatement, value.head, size - defaultIndex + 1)
          hListRandom.toPrepareWithSize(preparedStatement, value.tail, 0, size)
        }

        override def to(preparedStatement: PreparedStatement, value: T :: HL, index: Int): Unit = throw new UnsupportedOperationException
      }

      implicit def caseClassRandom[T, HL <: HList](implicit gen: Generic.Aux[T, HL], hListRandom: ValueWrapper[HL]): ValueWrapper[T] = new ValueWrapper[T] {
        override def to(preparedStatement: PreparedStatement, value: T, index: Int): Unit = throw new UnsupportedOperationException

        override def to(preparedStatement: PreparedStatement, value: T): Unit = {
          hListRandom.to(preparedStatement, gen.to(value))
        }
      }

    }


    val preparedStatement: PreparedStatement = null

    def readAs[T: ValueWrapper](value: T) = {
      implicitly[ValueWrapper[T]].to(preparedStatement, value)
    }

    class User(val name: String, val age: Int, val weight: Double) {
      override def toString: String = s"User($name, $age, $weight)"
    }

    //    readAs[String::Int::Double::HNil]("张三"::31:: 73.4::HNil)

  }


  test("use shapeless to create type classes2") {
    // 尝试传递字段的顺序index

    import shapeless._

    class User(val name: String, val age: Int, val weight: Double) {
      def ff: Boolean = false

      override def toString: String = s"User($name, $age, $weight)"
    }

    trait ValueWrapper[T] {
      def typeName: String

      def to(idx: Int, value: T): Unit = println(s"$typeName($idx)")

      def to(value: T): Unit = to(hintIndex, value)

      def to(size: Option[Int], value: T): Unit = throw new UnsupportedOperationException()

      def from(): T = from(hintIndex)

      def from(size: Option[Int]): T = throw new UnsupportedOperationException

      def from(idx: Int): T

      def hintIndex: Int = 1
    }

    object ValueWrapper {

      implicit val hNilRandom = new ValueWrapper[HNil] {
        override def to(value: HNil): Unit = {
          println(s"HNil($hintIndex)")
        }

        override def typeName: String = "HNil"

        override def to(idx: Int, value: HNil): Unit = {
          println(s"HNil($idx)")
        }

        override def to(size: Option[Int], value: HNil): Unit = {
          to(value)
        }

        override def hintIndex: Int = 0

        override def from(idx: Int): HNil = HNil

        override def from(size: Option[Int]): HNil = HNil
      }

      implicit def hListRandom[T, HL <: HList](implicit tRandom: ValueWrapper[T],
                                               hListRandom: ValueWrapper[HL]) = new ValueWrapper[::[T, HL]] {
        override def typeName: String = s"${tRandom.typeName}::{${hListRandom.typeName}}"

        override def to(value: T :: HL): Unit = {
          to(Some(hintIndex), value)
        }

        override def to(size: Option[Int], value: T :: HL): Unit = {
          tRandom.to(size.get - hintIndex + 1, value.head)
          hListRandom.to(size, value.tail)
        }

        override def hintIndex: Int = hListRandom.hintIndex + 1

        override def from(idx: Int): T :: HL = throw new UnsupportedOperationException

        override def from(size: Option[Int]): T :: HL = {
          ::(tRandom.from(1), hListRandom.from(size))
        }

        override def from(): T :: HL = {
          ::(tRandom.from(1), hListRandom.from(Some(hintIndex)))
        }
      }

      implicit def caseClassRandom[T, HL <: HList](implicit gen: Generic.Aux[T, HL], hListRandom: ValueWrapper[HL]): ValueWrapper[T] = new ValueWrapper[T] {
        override def to(value: T): Unit = {
          hListRandom.to(gen.to(value))
        }

        override def typeName: String = hListRandom.typeName

        override def from(idx: Int): T = throw new UnsupportedOperationException()

        override def from(): T = {
          gen.from(hListRandom.from())
        }
      }

      /*
      基础元素类型
       */
      implicit def stringRandom = new ValueWrapper[String] {
        override def typeName: String = "String"

        override def from(idx: Int): String = "张三"
      }

      implicit def intRandom = new ValueWrapper[Int] {
        override def typeName: String = "Int"

        override def from(idx: Int): Int = 31
      }

      implicit def doubleRandom: ValueWrapper[Double] = new ValueWrapper[Double] {
        override def typeName: String = "Double"

        override def from(idx: Int): Double = 75.4
      }

      //      implicit def userRandom = new Random[User] {
      //        override def random(): User = new User("张三", 21, 76.0)
      //
      //        override def typeName: String =
      //      }

    }

    //    ValueWrapper[Int :: String :: HNil]()

    val user = new User("张三", 31, 75.6)

    def writeAs[T](value: T)(implicit random: ValueWrapper[T]) = {
      random.to(value)
    }

    writeAs[User](user)
    //    println(user)
    writeAs[(String, Int, Double)](("张三", 31, 75.6))
    println()
    println("read")

    def readAs[T]()(implicit random: ValueWrapper[T]) = {
      random.from()
    }

    println(readAs[User]())

  }

  test("test some features of prepared statement") {
    // need a mysql env
    val ip = "localhost"
    val port = "3306"
    val username = "root"
    val password = "123456"
    val dbname = "data"

    val source: JdbcSource = JdbcSourceBuilder()
      .setIp(ip)
      .setPort(port)
      .setDbname(dbname)
      .setUsername(username)
      .setPassword(password)
      .mysql()

    source.withStatement {
      statement: Statement =>
        statement.execute("drop table if exists user;")
        statement.execute("create table user(name varchar(256), age int, weight double);")
    }

    // todo: 在RichStatement中添加
    // drop table
    // create table

    source.withPreparedStatement(s"insert into user(name, age, weight) values(?, ?, ?)") {
      statement: PreparedStatement =>
        /*
        statement.setString(1, "name")
        statement.setString(2, "age")
        statement.setString(3, "weight") // not run: syntax SQL near 'name', 'age', 'weight'
        */
        statement.setObject(5 - 3, null) // null can use setObject
        statement.setString(4 - 3, null) // null can use setString, and must started with 1
        //        statement.setInt(5 - 3, 32) // null can not use setInt
        //        statement.setInt(5 - 3, 23) // can set duplicate
        statement.setDouble(6 - 3, 73.4)
        statement.execute()
    }


    source.withStatement {
      statement: Statement =>
        val rs = statement.executeQuery("select * from user;")
        rs.next()
        println(rs.getInt("weight"))

    }

    /*
    结论:
    1)所有的parameter必须为值类型, 不能以?作为字段或表达式的占位符, preparedStatement会自动格式化为SQL语法;
    2)所有的parameter位置不能为空, 可以重复set;
    3)setObject/setString可以为null, setInt等则不能直接插入, 是由于编译器的原因;
     */
  }


  test("交通赛数据, 手动prepared statement") {
    val url = "jdbc:postgresql://localhost:5432/gis"
    val user = "postgres"
    val password = "123456"
    val driver = "org.postgresql.sefon.Driver"

    val source = JdbcSourceBuilder()
      .setUrl(url)
      .setUsername(user)
      .setPassword(password)
      .setDriver(driver)
      .build()

    val path = "E:\\studio\\data\\交通赛数据_上\\20140803_train.txt"

    val countries = Array("中国", "美国", "日本", "韩国", "德国", "俄罗斯", "英国", "法国", "以色列", "未知")

    val smf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

    val rd = new Random(1123L)

    println(new Timestamp(System.currentTimeMillis()))
    val vv = scala.io.Source.fromFile(path).getLines().map {
      string =>
        string.split(",") match {
          case Array(id, lat, long, obtMet, time) =>
            ResData(id, countries(id.toDouble.toInt % 10), lat.toDouble, long.toDouble, 0.0, obtMet, (obtMet.toDouble + 1) * 1300000, rd.nextInt(10) * (rd.nextInt(10).toDouble), new Timestamp(smf.parse(time).getTime))
        }
    }

    // rich prepared statement
    // rich statement

    import scala.io.jdbc.rich.RichPreparedStatement._
    source.withPreparedStatement(""" insert into "comemiresdata" ("emiplatname","emiplatnat","lat","lon","hgt","obtmet","newRF","newPri","locTim") values (?,?,?,?,?,?,?,?,?); """.stripMargin) {
      statement: PreparedStatement =>
        val ss = statement.setBatchSize(10000)
        var i = 0
        vv.foreach {
          value =>
            ss.executeInBatch {
              statement: PreparedStatement =>
                statement.setString(1, value.emiplatname)
                statement.setString(2, value.emiplatnat)
                statement.setDouble(3, value.lat)
                statement.setDouble(4, value.lon)
                statement.setDouble(5, value.hgt)
                statement.setString(6, value.obtmet)
                statement.setDouble(7, value.newRF)
                statement.setDouble(8, value.newPri)
                statement.setTimestamp(9, value.locTim)
                statement.addBatch()
            }
        }
    }

    println(new Timestamp(System.currentTimeMillis()))
    //    source.write("comemiresdata", vv, 10000)

    // 53040000
    // 2021-07-02 14:22:56.512
    // 2021-07-02 14:43:26.420
    // 00:19:29.808


  }


  test("交通赛数据, 自动mapper") {
    val url = "jdbc:postgresql://localhost:5432/gis"
    val user = "postgres"
    val password = "123456"
    val driver = "org.postgresql.sefon.Driver"

    val source = JdbcSourceBuilder()
      .setUrl(url)
      .setUsername(user)
      .setPassword(password)
      .setDriver(driver)
      .build()

    val path = "E:\\studio\\data\\交通赛数据_上\\20140804_train.txt"

    val countries = Array("中国", "美国", "日本", "韩国", "德国", "俄罗斯", "英国", "法国", "以色列", "未知")

    val smf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

    val rd = new Random(1123L)

    println(new Timestamp(System.currentTimeMillis()))
    val vv = scala.io.Source.fromFile(path).getLines().map {
      string =>
        string.split(",") match {
          case Array(id, lat, long, obtMet, time) =>
            ResData(id, countries(id.toDouble.toInt % 10), lat.toDouble, long.toDouble, 0.0, obtMet, (obtMet.toDouble + 1) * 1300000, rd.nextInt(10) * (rd.nextInt(10).toDouble), new Timestamp(smf.parse(time).getTime))
        }
    }

    // rich prepared statement
    // rich statement

    import scala.io.jdbc.rich.RichPreparedStatement._
    val conn: Connection = source.getConnection
    /*

        val preparedStatement = conn.prepareStatement(""" insert into "comemiresdata" ("emiplatname","emiplatnat","lat","lon","hgt","obtmet","newRF","newPri","locTim") values (?,?,?,?,?,?,?,?,?); """)

        val mapper = implicitly[JdbcMapper[ResData]]
        val wrap = (statement: PreparedStatement, index: Int, value:ResData) => mapper.wrap(statement, 1, value)
        wrap(preparedStatement, 1, vv.next())*/


    scala.io.jdbc.write(conn, "comemiresdata", vv, 10000)

    //    source.write("comemiresdata", vv, 10000)

    println(new Timestamp(System.currentTimeMillis()))
    //    source.write("comemiresdata", vv, 10000)

    // 108527541
    // 53040000
    // 55487541
    // 2021-07-02 15:45:03.005
    // 2021-07-02 16:06:54.217  00:21:51.215


  }

  test("ss") {
    val path = "E:\\studio\\data\\交通赛数据_上\\20140810_train.txt"
  }


  test("同时带有类型和不带类型的create") {


  }

  test("轨迹问题架构设计") {
    trait Param[T]

    trait HasParams {
      def setParam(param: Param[_]): this.type

      def getParam[T]: T

      // 敏感参数, 当前参数与历史执行批次不同时会引发reset操作
      def setSensitiveParams(params: String*): this.type

      def getSensitiveParams: Array[Param[_]]
    }

    trait CanSafeRun {
      def safeRun[T: ClassTag](body: => Unit)(rollBack: => Unit): Unit =
        try {
          body
        } catch {
          case _: Throwable =>
            rollBack
        }
    }

    trait Task extends HasParams with CanSafeRun {
      def stage: String

      def run(): Unit =
        while (!taskToolBox.isLatest) {
          if (taskToolBox.batchIndex == 0)
            rerunList.foreach { task => task.reset() }
          safeRun {
            runTask(taskToolBox.startTime, taskToolBox.endTime)
            taskToolBox.update()
          } {
            rollBackList.foreach {
              task =>
                task.rollBack()
            }
          }
        }


      protected def runTask(startTime: Timestamp, endTime: Timestamp): Unit

      def rerun(): Unit

      def rollBack(): Unit

      def reset(): Unit

      var rerunList: Array[Task]
      var taskToolBox: TaskToolBox
      var rollBackList: Array[Task]

      def afterAll(): Unit
    }

    trait TaskToolBox {
      def startTime: Timestamp

      def endTime: Timestamp

      def isLatest: Boolean

      def batchIndex: Int

      def update(): Unit
    }

    val task1: Task = throw new Exception("")
    task1.setParam(null)

    val task2: Task = null
    val task3: Task = null
    val task4: Task = null
    val hook: Task = null

    task1.rerunList = Array(task2)
    task2.rerunList = Array(task3, task4)
    task3.rerunList = Array(hook)
    hook
  }


}
