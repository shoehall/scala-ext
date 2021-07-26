package scala.knickknack.spandas.functions

import org.scalatest.FunSuite

class functionsSuite extends FunSuite {
  test("all") {
    // empty is true.
    // python:
    assert(all(Array.emptyBooleanArray))
  }

  test("any") {
    // empty is false
    assert(!any(Array.emptyBooleanArray))
  }

}
