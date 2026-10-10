package tests

import java.io.{File, IOException}
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters.*

/**
 * Code-generation tests: run the compiler on every `.ic` file in a
 * directory (and its subdirectories), assemble and run the code it
 * generates, and compare the program's output to the matching
 * `.ic.expected` file.
 *
 * Run it from sbt:
 *
 * <pre>
 *   sbt "cgTests tests/pa3"             run every test in tests/pa3/
 *   sbt "cgTests -v tests/pa3"          also dump the source and full
 *                                       output of each failing test
 *   sbt "cgTests -opt tests/pa3"        pass extra flags (everything
 *                                       before the directory) to the
 *                                       compiler on every test
 * </pre>
 *
 * Every `.expected` file in the course has the same shape: the
 * expected compiler output, ending in a verdict line that is exactly
 * "Success." or "Failed.", optionally followed by a "# Output:" line
 * and the output the compiled program should print when run.  For
 * each test `foo.ic`:
 *
 *   1. The compiler runs in a fresh JVM; its output (stdout and
 *      stderr together) is saved in `foo.ic.output`, and its last
 *      non-blank line is compared to the expected verdict.
 *   2. If the expected verdict is "Failed." and the compiler agreed,
 *      the test passes -- a bad program correctly rejected.
 *   3. Otherwise the generated `foo.s` is assembled with
 *      `gcc -m64 -g foo.s libic64.a` (gcc's complaints, if any, are
 *      saved in `foo.ic.gcc-errors`).
 *   4. The resulting `foo.exe` is run, its output (stdout and stderr
 *      together) is saved in `foo.ic.result` and compared to the lines
 *      after "# Output:".  The program's exit status is ignored --
 *      run-time-error tests exit nonzero by design.
 *
 * `libic64.a` (the IC runtime library) must be in the project root,
 * next to build.sbt.
 */
object CgTests:

  def main(args: Array[String]): Unit =
    val verbose = args.contains("-v")
    args.filterNot(_ == "-v").toSeq match
      case rest :+ dir => run(Paths.get(dir), rest, verbose)
      case _ =>
        println("""Usage: sbt "cgTests [-v] [compiler flags ...] <dir>"""")
        System.exit(2)

  /** Run every test in `dir`, then report and exit. */
  private def run(dir: Path, flags: Seq[String], verbose: Boolean): Unit =
    if !Files.isDirectory(dir) then
      println(s"Not a directory: $dir")
      System.exit(2)

    if !Files.exists(Paths.get("libic64.a")) then
      println("libic64.a not found in the project root.")
      println("Download it from the course web site and place it next to build.sbt.")
      System.exit(2)

    val tests = icFiles(dir)
    if tests.isEmpty then println(s"No .ic files found in $dir")

    var passed = 0
    var failed = 0

    for test <- tests do
      println(s"$test...")

      val output = compile(test, flags, sibling(test, ".output"))

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
   * Check one test: verdict first, then -- for good programs --
   * assemble and run the generated code.  Returns None if the test
   * passed, or a description of the failure.
   */
  private def check(test: Path, output: String): Option[String] =
    val expectedFile = sibling(test, ".expected")
    if !Files.exists(expectedFile) then
      return Some(s"    No expected output file: $expectedFile")

    val (compilePart, runPart) = splitExpected(read(expectedFile))
    val expectedVerdict = lastLine(compilePart)
    val verdict = lastLine(output)

    if verdict != expectedVerdict then
      Some(s"    Compilation Failed.\n    Expected: $expectedVerdict\n    Got:      $verdict")
    else if expectedVerdict != "Success." then
      None // bad program, correctly rejected
    else
      assembleAndRun(test, runPart)

  /** Assemble foo.s, run foo.exe, and compare its output to `expectedRun`. */
  private def assembleAndRun(test: Path, expectedRun: String): Option[String] =
    val base = test.toString.stripSuffix(".ic")
    val gccErrors = sibling(test, ".gcc-errors")

    val gcc =
      try
        exec(Seq("gcc", "-m64", "-g", "-o", s"$base.exe", s"$base.s", "libic64.a"),
             gccErrors, Timeout)
      catch
        case _: IOException =>
          return Some("    gcc not found: a C toolchain is required to run these tests.")
    if gcc != 0 then
      return Some("    Assembly Failed (gcc).\n" +
        indent(read(gccErrors).linesIterator.take(8).mkString("\n")))

    val resultFile = sibling(test, ".result")
    exec(Seq(Paths.get(s"$base.exe").toAbsolutePath.toString), resultFile, RunTimeout)

    val got = normalize(read(resultFile))
    val want = normalize(expectedRun)
    if got == want then None
    else
      Some("    Test Run Failed.\n    Expected:\n" + indent(want) +
        "\n    Got:\n" + indent(got))

  /** Longest time (seconds) a compile or gcc run may take. */
  private val Timeout = 30

  /** Longest time (seconds) a compiled test program may run. */
  private val RunTimeout = 10

  /**
   * Run the compiler on one file in a fresh JVM, sending everything it
   * prints (stdout and stderr) to `outputFile`, and return that text.
   * A compiler that crashes or hangs makes that one test fail; it does
   * not stop the rest of the run.
   */
  private def compile(test: Path, flags: Seq[String], outputFile: Path): String =
    val javaBin = Paths.get(sys.props("java.home"), "bin", "java").toString
    exec(Seq(javaBin) ++ jvmFlags ++ Seq("-cp", classpath, "ic.Compiler", test.toString) ++ flags,
         outputFile, Timeout)
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
   * Run a command with stdout and stderr sent to `outputFile`; return
   * its exit status.  A command still running after `timeout` seconds
   * is killed and a note is appended to the file.
   */
  private def exec(command: Seq[String], outputFile: Path, timeout: Int): Int =
    val builder = new ProcessBuilder(command*)
    builder.redirectErrorStream(true)
    builder.redirectOutput(outputFile.toFile)
    val process = builder.start()
    if !process.waitFor(timeout, TimeUnit.SECONDS) then
      process.destroyForcibly().waitFor()
      write(outputFile, read(outputFile) + s"\nTimeout: still running after $timeout seconds.\n")
    process.exitValue()

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
      println("cgTests must run in a forked JVM: add `Test / run / fork := true` to build.sbt")
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
   * compiler output, expected run output).  Every accepted program in
   * a code-gen suite has the marker, even when its expected run
   * output is empty; a file with no marker is all compiler output --
   * an expected-to-fail test or a front-end suite.
   */
  private def splitExpected(text: String): (String, String) =
    val lines = text.linesIterator.toSeq
    val marker = lines.indexWhere(_.trim == OutputMarker)
    if marker < 0 then (text, "")
    else (lines.take(marker).mkString("\n"), lines.drop(marker + 1).mkString("\n"))

  /** Text with trailing blank lines removed, for whole-output comparison. */
  private def normalize(text: String): String =
    text.linesIterator.toSeq.reverse.dropWhile(_.trim.isEmpty).reverse.mkString("\n")

  /** Indent every line of text, for readable verbose output. */
  private def indent(text: String): String =
    text.linesIterator.map("        " + _).mkString("\n")
