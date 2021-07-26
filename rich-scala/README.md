### 简介


### struct of ORM
- `scala.io.jdbc.mappers.Mapper`
- `rich resultSet: ResultSet => Iterator[T]`
- `rich statement: Statement => ResultSet`
- `JdbcSource: readAs[T]/write[T]()`

### 设计理念
- 工具函数: 功能简洁, 用法简洁

#### 现在的难点
- 层级要素之间的连接关系太复杂了, dialect/source/connection/statement/table/batch/SQL/result set/schema/row/Mapper/Wrapper/批处理vs单条灵活处理

#### 一些实践中的体会
- row和schema长期在一块, 可以构建一个LightFrame, 作为集合类


### 功能点
#### 1.rich collection    
- a rich pair iterator
- a rich traversable collection
- a slice method with style like python for seq
 
// todo: read需要加入一个connection, 方便便捷的操作, 手动控制关闭的按钮
// todo: preparedStatement加入一个频繁查询的接口

#### 2.jdbc
- a jdbc data source api
- an orm interface for read and write
- `jdbcSource.readAs[T]`
会自动创建一个connection和statement, 返回一个迭代器, 并且在迭代器迭代完毕时关闭connection和statement

```scala
import scala.io.jdbc.JdbcSource

val source: JdbcSource = null

// IteratorWithHook

// Reader
// conn.read(sql, autoClose: Boolean): ResultSet // statement
// val res = conn.prepareReader[P](sql, autoClose: Boolean).read(param: P*): RichResultSet
// res.schema
// res.result: ResultSet
// res.toIterator[T](implicit getter: ResultSet => T): Iterator[T]
// res.toRows: Iterator[Row] = toIterator[Row](Row.getter)
// res.toLightFrame: LightFrame
// res.toDataFrame: DataFrame

// source.createReader(sql) = getConnection.createReader(sql, true)
// source.read(sql) = {
//    createReader(sql).read()
// }
// conn.execute(sql): Unit
// conn.prepareExecutor(sql, autoClose: Boolean).execute(param: Tuple): Unit
// 
// source.read(sql, autoClose: Boolean = false).schema: Schema
// source.read(sql).result: ResultSet
// source.read(sql).getValues[T](getter: ResultSet => T)
// source.read(sql).getValuesAs[T](implicit val ev: ResultGetter[T]) = getValues[T](ev.getter)
// source.read(sql).rows = getValues[Row](Row.getter)
// source.read(sql).lightFrame: LightFrame
```

```scala
import scala.io.jdbc.JdbcSource
val source: JdbcSource = null
source.readAs[String]("select user from user;")
```
- `statement.readAs[T]`
根据statement读取数据, 不会创建和关闭statement. 
```scala
import java.sql.Statement
import scala.io.jdbc._
val statement: Statement = null
statement.readAs[String]
```
- 扩展性
如果想要扩展任何其他类型, 只需要引入一个隐式转换

```scala
import scala.io.jdbc.rich.RichStatement.ResultSetMapper

class Person()

implicit def transformPerson = new ResultSetMapper[Person] {
  override def makeGetter = ???
}
```

- 自动mapper为SQL语句
```scala

```


#### 3.datetime
- java.sql.Date等需要一个比较大小的方法
- a time delta
- arithmetic operation for time
- parse time with type of string

### roadmap


### 设计思想
- 工具方法, 尽量有一个工具函数式的调用, 然后才放在rich类中;


