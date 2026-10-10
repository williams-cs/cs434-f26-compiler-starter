package tests

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters.*

/**
 * Specification tests: run the compiler on every `.ic` file in a
 * directory (and its subdirectories) and compare what it printed
 * against the matching `.ic.expected` file.
 *
 * Run it from sbt:
 *
 * <pre>
 *   sbt "specTests tests/pa1"        run every test in tests/pa1/
 *   sbt "specTests -v tests/pa1"     also dump the source and the
 *                                    full output of each failing test
 * </pre>
 *
 * Every `.expected` file in the course has the same shape: the
 * expected compiler output, ending in a verdict line that is exactly
 * "Success." or "Failed.", optionally followed by a "# Output:" line
 * and the output the compiled program should print when run
 * (code-generation suites only).
 *
 * Each test is compiled in a fresh JVM, so a compiler that crashes,
 * hangs, or calls System.exit spoils only that one test.  For each
 * test `foo.ic` the compiler's output (stdout and stderr together) is
 * saved in `foo.ic.output`, and its **last non-blank line** is
 * compared to the verdict in `foo.ic.expected` -- the last non-blank
 * line before the "# Output:" marker, or of the whole file when
 * there is none.  So a test passes when the compiler accepts or
 * rejects the file as it should, no matter what tokens it printed
 * along the way; anything after "# Output:" is cgTests' business,
 * not ours.  Once your lexer prints tokens, you may well want to
 * compare more than the verdict -- that is a small change to `check`
 * below.
 *
 * You should not need to modify this file to complete PA 1, but it is
 * yours: extend it as your test suite grows.
 */
object SpecTests:

  def main(args: Array[String]): Unit =
    val verbose = args.contains("-v")
    args.filterNot(_ == "-v") match
      case Array(dir) => run(Paths.get(dir), verbose)
      case _ =>
        println("""Usage: sbt "specTests [-v] <dir>"""")
        System.exit(2)

  /** Run every test in `dir`, then report and exit. */
  private def run(dir: Path, verbose: Boolean): Unit =
    if !Files.isDirectory(dir) then
      println(s"Not a directory: $dir")
      System.exit(2)

    val tests = icFiles(dir)
    if tests.isEmpty then println(s"No .ic files found in $dir")

    var passed = 0
    var failed = 0

    for test <- tests do
      println(s"$test...")

      // Run the compiler, catching everything it prints
      val output = compile(test, sibling(test, ".output"))

      check(test, output) match
        case None =>
          passed += 1
        case Some(complaint) =>
          failed += 1
          println(complaint)
          if verbose then
            println("    Code:")
            println(indent(read(test)))
            println("    Full Output:")
            println(indent(output))
            println()

    println(s"# PASSED: $passed")
    println(s"# FAILED: $failed")

    if failed != 0 then System.exit(1)

  /**
   * Compare a test's output to its `.expected` file.  Returns None if
   * the test passed, or a description of the mismatch if it failed.
   */
  private def check(test: Path, output: String): Option[String] =
    val expectedFile = sibling(test, ".expected")
    if !Files.exists(expectedFile) then
      Some(s"    No expected output file: $expectedFile")
    else
      val (compilePart, _) = splitExpected(read(expectedFile))
      val expected = lastLine(compilePart)
      val actual = lastLine(output)
      if expected == actual then None
      else Some(s"    Expected: $expected\n    Got:      $actual")

  /** Longest time (seconds) one compile may run before we give up on it. */
  private val Timeout = 30

  /**
   * Run the compiler on one file in a fresh JVM, sending everything it
   * prints (stdout and stderr) to `outputFile`, and return that text.
   * A compiler that crashes or hangs makes that one test fail; it does
   * not stop the rest of the run.
   */
  private def compile(test: Path, outputFile: Path): String =
    val javaBin = Paths.get(sys.props("java.home"), "bin", "java").toString
    val command = Seq(javaBin) ++ jvmFlags ++ Seq("-cp", classpath, "ic.Compiler", test.toString)
    val builder = new ProcessBuilder(command*)
    builder.redirectErrorStream(true)
    builder.redirectOutput(outputFile.toFile)
    val process = builder.start()
    if !process.waitFor(Timeout, TimeUnit.SECONDS) then
      process.destroyForcibly().waitFor()
      write(outputFile, read(outputFile) + s"\nTimeout: compiler still running after $Timeout seconds.\n")
    read(outputFile)

  /**
   * Flags for the fresh JVMs.  JDK 24 and later print warnings whenever
   * Scala's lazy-val runtime touches sun.misc.Unsafe; allow it quietly
   * so the noise does not pollute the captured test output.
   */
  private val jvmFlags: Seq[String] =
    if Runtime.version().feature() >= 23 then Seq("--sun-misc-unsafe-memory-access=allow")
    else Seq()

  /**
   * The classpath for the fresh JVMs: this JVM's own classpath, which
   * is the project classpath because build.sbt sets
   * `Test / run / fork := true`.
   */
  private lazy val classpath: String =
    val cp = sys.props("java.class.path")
    // An unforked run sees only sbt's launcher jar here, which would
    // leave the fresh JVMs unable to find the compiler.
    if cp.split(File.pathSeparator).length < 2 then
      println("specTests must run in a forked JVM: add `Test / run / fork := true` to build.sbt")
      System.exit(2)
    cp

  /** All .ic files under dir, in sorted order. */
  private def icFiles(dir: Path): Seq[Path] =
    val stream = Files.walk(dir)
    try
      stream
        .iterator()
        .asScala
        .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".ic"))
        .toSeq
        .sortBy(_.toString)
    finally stream.close()

  /** Read a whole file as UTF-8 text. */
  private def read(file: Path): String =
    new String(Files.readAllBytes(file), UTF_8)

  /** Write text to a file as UTF-8. */
  private def write(file: Path, text: String): Unit =
    Files.write(file, text.getBytes(UTF_8))

  /** The file next to `test` with `suffix` added: foo.ic -> foo.ic.output */
  private def sibling(test: Path, suffix: String): Path =
    test.resolveSibling(test.getFileName.toString + suffix)

  /** The last line of text that is not blank, or "" if there is none. */
  private def lastLine(text: String): String =
    text.linesIterator.map(_.trim).filter(_.nonEmpty).toSeq.lastOption.getOrElse("")

  /** The marker separating expected compiler output from expected run output. */
  private val OutputMarker = "# Output:"

  /**
   * Split an expected file at its "# Output:" line into (expected
   * compiler output, expected run output).  A file with no marker is
   * all compiler output -- the program is not meant to be run.
   */
  private def splitExpected(text: String): (String, String) =
    val lines = text.linesIterator.toSeq
    val marker = lines.indexWhere(_.trim == OutputMarker)
    if marker < 0 then (text, "")
    else (lines.take(marker).mkString("\n"), lines.drop(marker + 1).mkString("\n"))

  /** Indent every line of text, for readable verbose output. */
  private def indent(text: String): String =
    text.linesIterator.map("        " + _).mkString("\n")
