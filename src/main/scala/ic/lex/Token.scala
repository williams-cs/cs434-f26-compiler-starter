package ic.lex

import scup.{Pos, Tokenish}

/**
 * The different kinds of tokens in IC.
 */
enum TokenKind:
  // keywords
  case CLASS, EXTENDS, RETURN, NEW, IF, ELSE, WHILE, BREAK, CONTINUE,
       LENGTH, LIBRARY, THIS, VOID, INT, BOOLEAN, STRING, NULL

  // identifiers and literals
  case ID, CLS_ID, INTLIT, STRINGLIT, BOOLLIT

  // punctuation and operators
  case LBRACE, RBRACE, LPAREN, RPAREN, LBRACKET, RBRACKET, SEMI, COMMA, DOT,
       PLUS, MINUS, MULT, DIV, MOD,
       LT, LEQ, GT, GEQ, EQEQ, NOTEQ, AND, OR, NOT, EQ

/**
 * A token produced by the Lexer.
 *
 *  - kind: which kind of token this is.
 *  - text: the token's text.  For most tokens this is exactly the
 *    characters matched in the source; for STRINGLIT it is the string's
 *    *contents*, with escape sequences like \n already processed.
 *  - pos:  where the token started in the source.
 *
 * Extending scup.Tokenish (which just requires pos) is what lets
 * scup parsers consume a sequence of Tokens.
 */
case class Token(kind: TokenKind, text: String, pos: Pos) extends Tokenish:

  /** The line on which this token appeared. */
  def line: Int = pos.line

  /** The value of an INTLIT token. */
  def intValue: Int = text.toInt

  /** The value of a BOOLLIT token. */
  def boolValue: Boolean = text == "true"

  override def toString: String = s"[$kind,$text,$line]"
