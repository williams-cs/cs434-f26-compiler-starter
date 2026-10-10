# IC Compiler

#### Group Members



## Overview

...

## Building and Testing

    sbt compile                     compile everything
    sbt test                        run the MUnit unit tests
    ./icc.sh <file.ic> [flags]      run the compiler on one file
    sbt "specTests tests/pa2-cp1"   run a test suite (add -v for details)

`./icc.sh` is a thin wrapper around `sbt "run ..."` that hides sbt's
log messages; it takes the same arguments as the compiler: an
optional `-d` first, then the file, then any other flags (e.g.
`-printAST`). Once your compiler generates assembly (PA 3), it also
links any freshly generated `.s` with `libic64.a` into a runnable
`.exe`.

The starter also includes the `cgTests` runner used from PA 3 on:
`sbt "cgTests <dir>"` additionally assembles, links, and runs each
accepted test program (it needs `libic64.a` in the project root).
More on that later...
