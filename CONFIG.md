
### Directory Structure

    CONFIG.md     this file
    README.md     Your write-up.
    build.sbt     the sbt build definition
    ./src         all source code
    ./test        scratch files for quick manual runs
    ./tests       the course test suites, one directory per milestone

### Building

**In VS Code (with Metals) or IntelliJ:**

- Open the project folder; the editor imports `build.sbt` automatically
  and rebuilds whenever you save a file.

**In a shell:**

- run `sbt compile` to build everything
- run `sbt test` to run the unit tests
- run `sbt "specTests <dir>"` to run the specification tests (see below)
- run `sbt assembly` to build `target/icc.jar` for manual scripted runs
- run `sbt clean` to remove generated files

### Running

**In VS Code / IntelliJ**: use the `run` code lens on `ic.Compiler`,
or create a Run Configuration with main class `ic.Compiler` and program
arguments like `test/test1.ic`.

**In a shell:** Run:

```
sbt "run test/test1.ic"
```

or, after `sbt assembly`:

```
java -jar target/icc.jar test/test1.ic
```

### Testing

`sbt test` runs the MUnit unit tests in `src/test/scala`.

To run the compiler on a whole directory of `.ic` test files and
compare its output to the matching `.ic.expected` files, use:

```
sbt "specTests tests/pa2-cp1"    # the suite for the current milestone
sbt "specTests -v tests/pa2"     # -v also dumps each failing test
```

The `tests/` directory holds one suite per milestone (`pa2-cp1`,
`pa2-cp2`, `pa2`); later assignments add more suites there.  Each test
runs in a fresh JVM; the runner reports how many tests passed and
failed, and leaves each test's output in a `.ic.output` file next to it.

From PA 3 on, `sbt "cgTests [flags] <dir>"` additionally assembles the
generated `.s` with `gcc` and `libic64.a` (place it next to build.sbt)
and checks the compiled program's runtime output.  Both runners live in
`src/test/scala/tests/` --- extend them as you like.
