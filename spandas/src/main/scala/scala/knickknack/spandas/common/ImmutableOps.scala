package scala.knickknack.spandas.common

/*
    | types             | unary fun  | binary fun | object
----|-------------------|------------|------------|---------
+   | numeric, string   |  self      | plus       | PlusOps
-   | numeric           | negative   | minus      | MinusOps
*   | numeric, string   |   \        | multiply   | MulOps
/   | numeric           |   \        | divide     | DivOps
%   | numeric           |   \        | mod        | ModOps
&   | bool              |   \        | and        | this
|   | bool              |   \        | or         | this
^   | bool              |   \        | xor        | this
!   | bool              |  not       |  \         | this
=== | all               |   \        |  zip equal | this
>   | all               |   \        |  zip gt    | this
>=  | all               |   \        |  zip gteq  | this
<   | all               |   \        |  zip lt    | this
<=  | all               |   \        |  zip lteq  | this

// todo: 等稳定了可以尝试用宏实现隐式转换
// TODO: 后期需要一个DELTA类型来实现时间的加减法和时间差的乘法
 */
trait ImmutableOps[+This] {
  def repr: This

//  // ALL TYPE
//  final def unary_+[TT >: This, That](implicit op: PlusOps.UImpl[TT, That]): That = op(repr)
//
//  // NUMERIC DELTA

  // NUMERIC + NUMERIC
  // STRING + STRING
  // TIMESTAMP + DELTA

//  final def +[TT >: This, B, That](that: B)(implicit op: PlusOps.UImpl2[TT, B, That]): That = op(repr, that)

  // coviarant
  //   final def +[B, That](that: B)(implicit op: MinusOps.UImpl2[This, B, That]) = op(repr, that)
  final def -[TT >: This, B, That](that: B)(implicit op: MinusOps.UImpl2[TT, B, That]): That = op(repr, that)

  final def *[TT >: This, B, That](that: B)(implicit op: MinusOps.UImpl2[TT, B, That]): That = op(repr, that)

  final def /[TT >: This, B, That](that: B)(implicit op: DivOps.UImpl2[TT, B, That]): That = op(repr, that)

  final def %[TT >: This, B, That](that: B)(implicit op: ModOps.UImpl2[TT, B, That]): That = op(repr, that)

//  final def and[TT >: This, B, That](that: B)(implicit op: AndOps.UImpl2[TT, B, That]): That = op(repr, that)
//  final def &[TT >: This, B, That](that: B)(implicit op: AndOps.UImpl2[TT, B, That]): That = and[TT, B, That](that)
//  final def &&[TT >: This, B, That](that: B)(implicit op: AndOps.UImpl2[TT, B, That]): That = and[TT, B, That](that)
//
//  final def or[TT >: This, B, That](that: B)(implicit op: OrOps.UImpl2[TT, B, That]): That = op(repr, that)
//  final def |[TT >: This, B, That](that: B)(implicit op: OrOps.UImpl2[TT, B, That]): That = or[TT, B, That](that)
//  final def ||[TT >: This, B, That](that: B)(implicit op: OrOps.UImpl2[TT, B, That]): That = or[TT, B, That](that)
//
//  final def xor[TT >: This, B, That](that: B)(implicit op: XorOps.UImpl2[TT, B, That]): That = op(repr, that)
//  final def ^[TT >: This, B, That](that: B)(implicit op: XorOps.UImpl2[TT, B, That]): That = xor[TT, B, That](that)
//
//  final def unary_![TT >: This, That](implicit op: NotOps.UImpl[TT, That]): That = op(repr)
}
