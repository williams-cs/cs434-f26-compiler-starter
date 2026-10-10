package ic.lex

import slex.{LexError, LexSpec}
import ic.error.LexicalError

/**
 * The IC lexer, driven by the `ic.slex` spec file: the spec plus the
 * TokenKind enum IS the whole lexer.  This class is the host shim --
 * it loads the spec once and converts slex's LexError into the
 * compiler's LexicalError.  (The parser takes the richer spec-token
 * stream from `Lexer.specTokens` instead.)
 */
class Lexer(source: String):

  // The input is scanned one token per next() call, so every token
  // before a bad lexeme is produced before that lexeme's
  // LexicalError is thrown.
  private val scan = Lexer.spec.scan(source)

  /** The next token, or None at the end of the input. */
  def next(): Option[Token] =
    try scan.next().map(t => Token(t.kind, t.text, t.pos))
    catch case LexError(msg, pos) => throw LexicalError(pos.line, pos.col, msg)

  /** Lex the whole input. */
  def tokenize(): IndexedSeq[Token] =
    val toks = Vector.newBuilder[Token]
    var t = next()
    while t.isDefined do
      toks += t.get
      t = next()
    toks.result()

object Lexer:

  /** The spec-driven rule table (loaded once per JVM). */
  val spec: LexSpec[TokenKind] = LexSpec.load[TokenKind]("ic.slex")

  /** Lex a whole source, spec-token form (kind, text, pos, value),
   *  converting lexical errors as next() does. */
  def specTokens(source: String): IndexedSeq[scup.Token[TokenKind]] =
    try spec.tokenize(source)
    catch case LexError(msg, pos) => throw LexicalError(pos.line, pos.col, msg)
