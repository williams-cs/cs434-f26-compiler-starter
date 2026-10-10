package ic.error;

/**
 * 9/19/13 -- Nathan Bricault and Ethan Gracer
 * 
 * LexicalError: thrown when Lexer encounters an invalid pattern 
 * from a source file, includes line number and message describing
 * the error that was encountered.
 */
class LexicalError(val line : Int, val col : Int, val message : String) extends Error(message) {
    
    /** Nice format for error message */
    override def toString() : String = {
        "Lexical error on line " + line.toString() + 
        ", character " + col.toString() + ": " + message;
    }
  
}