package ic;

/**
	*
	* A few useful utility messages for doing the following:
	* <ul>
	* <li> assert that conditions are true.  Use this liberally.
	*
	* <li> print debugging messages when the program is running,
	*   provided that Util.debug has been set to true.
	* </ul>
	*
	* Feel free to add other utility routines to this class as well.
	*/
object Util {


	/**
		* Set this to true to print out messages with the debug() methods.
		* Otherwise, those methods do nothing.
		*/
	var debug = false;

	/**
		* Print a message to the terminal, if debugging
		* messages has been enabled by setting {@link Util#debug}.
		*/
	def debug(message : String) : Unit = {
		debug("%s", message);
	}

	/**
		* Print a message to the terminal using printf-style arguments,
		* if debugging messages has been enabled by setting
		* {@link Util#debug}.
		* <p>
		* See {@link java.io.PrintStream#printf(String, Object...)} for more details.
		*/
	def debug(format : String, args : Object*) : Unit = {
		if (debug) {
			System.out.printf("[" + format + "]\n", args.toArray:_*);
		}
	}


	/******************************************************************/


}
