package scala.io.jdbc

import scala.io.jdbc.SQL.Mode
import scala.io.jdbc.SQL.Mode.{Noop, Prepared, Wrapped}

class SQL(val parts: Seq[String], val args: SQLArg[_]*) extends HasDialect {
  def this(stem: String) = this(Seq(stem))

  assert(parts.nonEmpty, "The sql should not be empty.")

  assert(parts.length - args.length == 1, "The length of parts should be one more than the length of args")

  /**
   * 两个SQL拼接在一起
   *
   * @param other 另一个SQL
   * @return
   */
  def merge(other: SQL): SQL = {
    require(getDialect == other.getDialect, s"The merge option is illegal when dialects of SQLs are not same: ${(getDialect, other.getDialect)}")
    val newParts = (parts.dropRight(1) :+ (parts.last + other.parts.head)) ++ other.parts.drop(1)
    new SQL(newParts, args.toSeq ++ other.args.toSeq: _*)
  }

  def +(other: SQL): SQL = merge(other)

  def ++(other: SQL): SQL = merge(other)

  private def formatArg(arg: SQLArg[_], mode: Mode): String = arg match {
    case v: SQLValue[_] =>
      mode match {
        case Noop => v.toString()
        case Wrapped => v.toString(getDialect)
        case Prepared => Array.fill(v.wrapper.toDDL.flatten.length)("?").mkString(",")
      }
    case v: SeqSQLValue[_] =>
      v.value.get.map {
        value =>
          formatArg(value, mode)
      }.mkString(",")
    case _ =>
      arg.toString(getDialect)
  }

  def toString(mode: Mode): String = {
    val stringBuilder = new StringBuilder()
    var i = 0
    while (i < parts.length - 1) {
      stringBuilder append parts(i)
      stringBuilder append formatArg(args(i), mode)

      i += 1
    }
    stringBuilder append parts(i)
    stringBuilder.result()
  }


  /**
   * convert to sql expression
   *
   * @return
   */
  def sql: String = toString(Mode.Wrapped)

  /**
   * to PreparedStatement SQL expression
   *
   * @return
   */
  def prepare: String = toString(Mode.Prepared)

  /**
   * toString, not wrap parameter values
   *
   * @return
   */
  override def toString: String = toString(Wrapped)


}

object SQL {
  def apply(parts: Seq[String], args: SQLArg[_]*) = new SQL(parts, args: _*)

  def apply(part: String) = new SQL(Seq(part))

  implicit def string_to_sql(value: String): SQL = SQL(value)

  class Mode(val mode: String) {
    override def toString: String = mode
  }

  object Mode {
    implicit def string2Mode(mode: String): Mode = mode match {
      case Noop.mode => Noop
      case Wrapped.mode => Wrapped
      case Prepared.mode => Prepared
      case other =>
        throw new UnsupportedOperationException(s"illegal mode '$other', does not support mode except: [${Noop.mode}, ${Wrapped.mode}, ${Prepared.mode}]")
    }

    object Noop extends Mode("Noop")

    object Wrapped extends Mode("SQLExpr")

    object Prepared extends Mode("Prepared")

  }

}
