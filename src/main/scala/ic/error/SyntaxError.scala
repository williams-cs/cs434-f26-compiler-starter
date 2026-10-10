package ic.error

import scup.Pos

/**
 * SyntaxError: thrown by the parser when the token stream does not
 * match the IC grammar.  Reports what the parser was expecting and the
 * position of the token where the error was detected.
 */
class SyntaxError(val expected: List[String], val pos: Pos) extends Error {

  override def toString(): String = {
    "Syntax error at " + pos + ": expected " + expected.mkString(" or ")
  }
}
