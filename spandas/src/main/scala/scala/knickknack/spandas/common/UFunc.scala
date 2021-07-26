/*
 * 一元和二元操作的隐式转换
 * todo: 有些类型转换太多了, 现在是通过循环打印代码, 后期写个宏来处理.
 *
 */

package scala.knickknack.spandas.common

import scala.knickknack.spandas.index.Index

trait UFunc {
  /**
   * 一元操作
   * @tparam V 原类型
   * @tparam VR 操作符后的类型
   */
  trait UImpl[@specialized(Int, Double, Float) V, @specialized(Int, Double, Float) +VR] extends Serializable {
    def apply(v: V):VR
  }

  /**
   * 二元操作符
   * @tparam V1
   * @tparam V2
   * @tparam VR
   */
  trait UImpl2[@specialized(Int, Double, Float) V1, @specialized(Int, Double, Float) V2, @specialized(Int, Double, Float) +VR] extends Serializable {
    def apply(v: V1, v2: V2):VR
  }

  trait SelfTypeUImpl[@specialized(Int, Double, Float) V] extends UImpl[V, V]

}


/**
 * 减法操作及对应的隐式转换
 */
object MinusOps extends UFunc {
  implicit def unary_minus4_index_int = new UImpl[Index[Int], Index[Int]] {
    override def apply(v: Index[Int]): Index[Int] = v.map(-_)
  }

  implicit def unary_minus4_imseq_int = new UImpl[ImmutableSeq[Int], ImmutableSeq[Int]] {
    override def apply(v: ImmutableSeq[Int]): ImmutableSeq[Int] = v.map(-_)
  }

  implicit def index_int_minus_index_int = new UImpl2[ImmutableSeq[Int], ImmutableSeq[Int], ImmutableSeq[Int]] {
    override def apply(v: ImmutableSeq[Int], v2: ImmutableSeq[Int]): ImmutableSeq[Int] = {
      ImmutableSeq(v.values.zip(v2.values).map{ case (vv1, vv2) => vv1 - vv2}:_*)
    }
  }
}


// *   | numeric, string   |   \        | multiply   | MulOps
object MulOps extends UFunc {
  implicit def index_int_int_2_index_int = new UImpl2[ImmutableSeq[Int], Int, ImmutableSeq[Int]] {
    override def apply(v: ImmutableSeq[Int], v2: Int): ImmutableSeq[Int] = v.map(_ * v2)
  }

  implicit def index_int_index_int_2_index_int = new UImpl2[ImmutableSeq[Int], ImmutableSeq[Int], ImmutableSeq[Int]] {
    override def apply(v: ImmutableSeq[Int], v2: ImmutableSeq[Int]): ImmutableSeq[Int] = v.zip(v2).map{ case (vv1, vv2) => vv1 * vv2 }
  }
}

// todo: 此时会产生缺失值, 后期会有一个统一的缺失值处理方式
object DivOps extends UFunc {
  implicit def index_int_int_2_index_int = new UImpl2[ImmutableSeq[Int], Int, ImmutableSeq[Int]] {
    override def apply(v: ImmutableSeq[Int], v2: Int): ImmutableSeq[Int] = v.map(_ / v2)
  }

  implicit def index_int_index_int_2_index_int = new UImpl2[ImmutableSeq[Int], ImmutableSeq[Int], ImmutableSeq[Int]] {
    override def apply(v: ImmutableSeq[Int], v2: ImmutableSeq[Int]): ImmutableSeq[Int] = v.zip(v2).map{ case (vv1, vv2) => vv1 / vv2 }
  }
}


object ModOps extends UFunc {
  implicit def index_int_int_2_index_int = new UImpl2[ImmutableSeq[Int], Int, ImmutableSeq[Int]] {
    override def apply(v: ImmutableSeq[Int], v2: Int): ImmutableSeq[Int] = v.map(_ % v2)
  }

  implicit def index_int_index_int_2_index_int = new UImpl2[ImmutableSeq[Int], ImmutableSeq[Int], ImmutableSeq[Int]] {
    override def apply(v: ImmutableSeq[Int], v2: ImmutableSeq[Int]): ImmutableSeq[Int] = v.zip(v2).map{ case (vv1, vv2) => vv1 % vv2 }
  }
}

/**
 *
 */
object AndOps extends UFunc {
  implicit def index_bool_bool_2_index_bool = new UImpl2[ImmutableSeq[Boolean], Boolean, ImmutableSeq[Boolean]] {
    override def apply(v: ImmutableSeq[Boolean], v2: Boolean): ImmutableSeq[Boolean] = v.map{ _ && v2 }
  }

  implicit def index_bool_index_bool_2_index_bool = new UImpl2[ImmutableSeq[Boolean], ImmutableSeq[Boolean], ImmutableSeq[Boolean]] {
    override def apply(v: ImmutableSeq[Boolean], v2: ImmutableSeq[Boolean]): ImmutableSeq[Boolean] = v.zip(v2).map{ case (vv1, vv2) => vv1 && vv2 }
  }
}

object OrOps extends UFunc {
  implicit def index_bool_bool_2_index_bool = new UImpl2[ImmutableSeq[Boolean], Boolean, ImmutableSeq[Boolean]] {
    override def apply(v: ImmutableSeq[Boolean], v2: Boolean): ImmutableSeq[Boolean] = v.map{ _ || v2 }
  }

  implicit def index_bool_index_bool_2_index_bool = new UImpl2[ImmutableSeq[Boolean], ImmutableSeq[Boolean], ImmutableSeq[Boolean]] {
    override def apply(v: ImmutableSeq[Boolean], v2: ImmutableSeq[Boolean]): ImmutableSeq[Boolean] = v.zip(v2).map{ case (vv1, vv2) => vv1 || vv2 }
  }
}

object NotOps extends UFunc {
  implicit def unary_minus4_index_int = new UImpl[ImmutableSeq[Boolean], ImmutableSeq[Boolean]] {
    override def apply(v: ImmutableSeq[Boolean]): ImmutableSeq[Boolean] = v.map(!_)
  }
}

object XorOps extends UFunc {
  implicit def index_bool_bool_2_index_bool = new UImpl2[ImmutableSeq[Boolean], Boolean, ImmutableSeq[Boolean]] {
    override def apply(v: ImmutableSeq[Boolean], v2: Boolean): ImmutableSeq[Boolean] = v.map{ _ ^ v2 }
  }

  implicit def index_bool_index_bool_2_index_bool = new UImpl2[ImmutableSeq[Boolean], ImmutableSeq[Boolean], ImmutableSeq[Boolean]] {
    override def apply(v: ImmutableSeq[Boolean], v2: ImmutableSeq[Boolean]): ImmutableSeq[Boolean] = v.zip(v2).map{ case (vv1, vv2) => vv1 ^ vv2 }
  }
}