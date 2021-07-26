package scala.knickknack.spandas

package object functions {
  object all {
    def apply(boolean: TraversableOnce[Boolean]): Boolean = boolean.forall { isTrue => isTrue }
  }

  object any {
    def apply(boolean: TraversableOnce[Boolean]): Boolean = boolean.exists { isTrue => isTrue }
  }

}
