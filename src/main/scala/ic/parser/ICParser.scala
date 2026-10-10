package ic.parser

import scup.{GrammarSpec, ParseError}
import ic.lex.Lexer
import ic.error.SyntaxError

/**
 * The IC parser, driven by the `ic.scup` spec file at the root of
 * this project -- that is where your grammar work goes; this object
 * is the host, provided in full.  It loads the grammar against
 * ic.slex's token binding, feeds it the lexer's token stream, and
 * converts scup's ParseError into the compiler's SyntaxError.
 *
 * TODO: once your grammar builds AST nodes (give your start rule a
 * %type), change this to return your root node type:
 *
 *   def parse(source: String): ProgramNode =
 *     ...
 *     try g.parseAs[ProgramNode](toks)
 */
object ICParser:

  private lazy val g = GrammarSpec.load("ic.scup", Lexer.spec.binding)

  /**
   * Parse a whole IC program from source text.  Throws LexicalError
   * for bad tokens and SyntaxError if the token stream doesn't match
   * the grammar.
   */
  def parse(source: String): Unit =
    val toks = Lexer.specTokens(source)
    try
      g.parseAll(toks)
      ()
    catch case e: ParseError => throw SyntaxError(e.expected, e.pos)
