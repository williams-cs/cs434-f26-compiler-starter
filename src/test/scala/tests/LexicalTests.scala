package tests

import ic.lex._
import ic.error._

class LexicalTests extends munit.FunSuite {

  test("Sanity Check") {
    val lexer = new Lexer("()")
    assertEquals(lexer.next().map(_.kind), Some(TokenKind.LPAREN))
    assertEquals(lexer.next().map(_.kind), Some(TokenKind.RPAREN))
    assertEquals(lexer.next(), None)
  }

  test("Keywords and identifiers") {
    val toks = new Lexer("class Cow moo").tokenize()
    assertEquals(toks.map(_.kind).toList,
      List(TokenKind.CLASS, TokenKind.CLS_ID, TokenKind.ID))
  }

  test("Operators use maximal munch") {
    val toks = new Lexer("<= < == = !=!").tokenize()
    assertEquals(toks.map(_.kind).toList,
      List(TokenKind.LEQ, TokenKind.LT, TokenKind.EQEQ, TokenKind.EQ,
           TokenKind.NOTEQ, TokenKind.NOT))
  }

  test("Int literals are range checked") {
    assertEquals(new Lexer("2147483647").tokenize().head.intValue, 2147483647)
    intercept[LexicalError] { new Lexer("2147483648").tokenize() }
    intercept[LexicalError] { new Lexer("007").tokenize() }
  }

  test("String literals process escapes") {
    val toks = new Lexer("\"moo\\n\"").tokenize()
    assertEquals(toks.head.kind, TokenKind.STRINGLIT)
    assertEquals(toks.head.text, "moo\n")
  }

  test("Unterminated strings and comments are errors") {
    intercept[LexicalError] { new Lexer("\"moo").tokenize() }
    intercept[LexicalError] { new Lexer("/* moo").tokenize() }
  }

  test("Comments are skipped") {
    val toks = new Lexer("a // moo\n /* bar \n baz */ b").tokenize()
    assertEquals(toks.map(_.text).toList, List("a", "b"))
    assertEquals(toks(1).pos.line, 3)
  }

  test("Invalid characters are lexical errors") {
    intercept[LexicalError] { new Lexer("#").tokenize() }
    intercept[LexicalError] { new Lexer("a & b").tokenize() }
  }
}
