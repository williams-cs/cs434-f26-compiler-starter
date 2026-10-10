name := "icc"

scalaVersion := "3.3.6"

libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test

Compile / run / mainClass := Some("ic.Compiler")
assembly / mainClass := Some("ic.Compiler")
assembly / assemblyOutputPath := target.value / "icc.jar"

Compile / scalacOptions ++= Seq("-deprecation")

// Specification tests:  sbt "specTests [-v] <dir>"
// Runs the compiler on every .ic file in <dir> and compares the
// output to the .ic.expected files.  See src/test/scala/tests/SpecTests.scala.
lazy val specTests = inputKey[Unit]("Run the compiler on all .ic tests in a directory.")
specTests := (Test / runMain).partialInput(" tests.SpecTests").evaluated

// Code-generation tests:  sbt "cgTests [-v] [compiler flags ...] <dir>"
// Compiles every .ic file in <dir>, assembles the generated code with
// gcc and libic64.a, runs it, and compares the runtime output to the
// .ic.expected files.  See src/test/scala/tests/CgTests.scala.
lazy val cgTests = inputKey[Unit]("Compile, assemble, and run all .ic tests in a directory.")
cgTests := (Test / runMain).partialInput(" tests.CgTests").evaluated

// The test runners fork a fresh JVM per compile and read this JVM's
// classpath to do so, so the runner itself must be forked too.
Test / run / fork := true

// JDK 24+ prints warnings when Scala's lazy-val runtime touches
// sun.misc.Unsafe; silence them in the forked test runner.  (The
// runners pass the same flag to the per-test JVMs they spawn.)
Test / run / javaOptions ++=
	(if (java.lang.Runtime.version().feature() >= 23) Seq("--sun-misc-unsafe-memory-access=allow") else Seq())

// Spec files: assemble the .slex/.scup action blocks at build time
// (Path A; see project/SpecGen.scala), and keep the editor wiring
// current on every sbt load.
Compile / sourceGenerators += Def.task {
  scup.SpecGen.generateAll(
    baseDirectory.value, (Compile / sourceManaged).value / "specgen")
}.taskValue

Global / onLoad := (Global / onLoad).value.andThen { s =>
  scup.VscodeSetup(new java.io.File(".")); s
}

// The unit-test suites exercise the spec-loading host code, whose
// spec paths are project-root relative; forked tests always run in
// the project root.
Test / fork := true

// Spec files also ride in the jar as resources, so an assembled jar
// (or a grader running it from another directory) is self-contained.
Compile / unmanagedResources ++= (baseDirectory.value * ("*.slex" | "*.scup")).get
