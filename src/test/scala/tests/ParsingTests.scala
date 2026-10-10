package tests

import ic.parser.ICParser
import ic.error._

/**
 * Sample Unit Tests for the IC parser.
 *
 * Add tests as you extend the grammar: parse small programs and check
 * the AST that comes back, and use intercept[SyntaxError] to check
 * that bad programs are rejected.
 *
 * Run these with `sbt test`.
 */
class ParsingTests extends munit.FunSuite {

  // The stub grammar accepts just the keyword `class`.  Replace this
  // test as soon as you extend the grammar.
  test("Sanity Check") {
    ICParser.parse("class")
  }

  test("Bad programs raise SyntaxError") {
    intercept[SyntaxError] {
      ICParser.parse("void")
    }
  }
}
