package ic

import ic.lex.{Lexer, Token, TokenKind}
import ic.parser.ICParser
import ic.error.{LexicalError, SyntaxError}
import java.io.IOException

/**
 * The main class for the IC Compiler.  The Compiler
 * expects the name of the file to process on the command line,
 * as in:
 * <pre>
 *   sbt "run test/test1.ic"      (or: java -jar target/icc.jar test/test1.ic)
 * </pre>
 * An optional flag <tt>-d</tt> can be provided, as in:
 * <pre>
 *   sbt "run -d test/test1.ic"
 * </pre>
 * In this case, messages created by calling {@link ic.Util#debug(String)}
 * or {@link ic.Util#debug(String, Object...)} will be printed to the
 * terminal.  If <tt>-d</tt> is not provided, these messages will
 * be silently ignored.
 */
object Compiler {

  def main(args: Array[String]): Unit = {

    var n = 0;

    // If first command line argument is -d, turn on debugging
    if (args.length > 0 && args(n).equals("-d")) {
      Util.debug = true;
      n = 1;
    }

    // Get name of file
    if (args.length == n) {
      System.out.println("No file given.");
    } else {
      val file = args(n);

      // example of debug message: This message will only be printed if
      // you provide "-d" on the command line.
      Util.debug("Processing %s...", file);

      // TODO: finish me.  Parse the program, build your AST, and then
      // add symbol table construction and semantic checks.  Print
      // "Success." if all checks pass and "Failed." (after the error
      // message) if any error is thrown.
      try {
        val source = scala.io.Source.fromFile(file).mkString
        val result = ICParser.parse(source)
        println(result)
        println("Success.")
      } catch {
        case e: IOException => println(e); println("Failed.")
        case e: LexicalError => println(e); println("Failed.")
        case e: SyntaxError => println(e); println("Failed.")
      }
    }
  }
}
