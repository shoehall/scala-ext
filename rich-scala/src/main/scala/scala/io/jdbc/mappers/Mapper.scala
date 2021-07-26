package scala.io.jdbc.mappers

import shapeless.nonGeneric

import scala.io.jdbc.JdbcDialect
import scala.reflect.runtime.universe._


/**
 * 类型T的wrap和unwrap方法的type class
 *
 * @tparam T 泛型
 */
trait Mapper[T] {
  def dataType: String = this.getClass.getSimpleName

  def getOrNull[F](value: Option[F]): F = if(value.isEmpty) null.asInstanceOf[F] else value.get
}

object Mapper extends JdbcMappers {
  /**
   * get field struct of type T
   *
   * @tparam T type T
   * @return field struct
   */
  def getStructOf[T: JdbcMapper]: Seq[PrimitiveStruct] = {
    val dDLs = implicitly[JdbcMapper[T]].toDDL.flatten
    var i = -1
    dDLs.map {
      ddl =>
        i += 1
        if (ddl.fieldName == null)
          ddl.concatFieldName(s"_${i}")
        else
          ddl
    }
  }

  def getDDLOf[T: JdbcMapper](dialect: JdbcDialect): String = getStructOf[T].map(_.toString(dialect)).mkString(",")
  def getDDLOf[T: JdbcMapper]: String = getStructOf[T].mkString(",")

  object utils {
    def isAnonOrRefinement(sym: Symbol): Boolean = {
      val nameStr = sym.name.toString
      nameStr.contains("$anon") || nameStr == "<refinement>"
    }

    def isNonGeneric(sym: Symbol): Boolean = {
      def check(sym: Symbol): Boolean = {
        // See https://issues.scala-lang.org/browse/SI-7424
        sym.typeSignature // force loading method's signature
        sym.annotations.foreach(_.tree.tpe) // force loading all the annotations

        sym.annotations.exists(_.tree.tpe =:= typeOf[nonGeneric])
      }

      // See https://issues.scala-lang.org/browse/SI-7561
      check(sym) ||
        (sym.isTerm && sym.asTerm.isAccessor && check(sym.asTerm.accessed)) ||
        sym.overrides.exists(isNonGeneric)
    }


    def isCaseAccessorLike(sym: TermSymbol): Boolean =
      !isNonGeneric(sym) && sym.isPublic && (if (sym.owner.asClass.isCaseClass) sym.isCaseAccessor else sym.isAccessor)

    def fieldsOf(tpe: Type): List[(TermName, Type)] = {
      val tSym = tpe.typeSymbol
      if (tSym.isClass && isAnonOrRefinement(tSym)) Nil
      else
        tpe.decls.toList collect {
          case sym: TermSymbol if isCaseAccessorLike(sym) =>
            (sym.name.toTermName, sym.typeSignatureIn(tpe).finalResultType)
        }
    }
  }


}

