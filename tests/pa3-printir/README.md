# PA 3 `-printIR` checkpoint tests

Six small programs for exercising `-printIR`, each paired with the
course reference compiler's output in its `.ic.expected` file:

- `arith.ic` — straight-line arithmetic and temporaries
- `ifelse.ic` — an if/else and its labels
- `loop.ic` — a while loop
- `call.ic` — parameter passing, return values, nested calls
- `objects.ic` — field offsets, field access, method dispatch
- `array.ic` — array allocation, indexing, `.length`

**The `.expected` files here will not match your compiler.** Your TAC
instruction set, temporary names, and printing format are your own
design, so there is no common expected output. Treat these files as one
example of a reasonable `-printIR` format (including the field/frame
offsets the checkpoint asks for), and eyeball your compiler's output on
these programs. Once you are happy with your IR, replace the
`.expected` files with your own verified output — and extend the
`check` method in `SpecTests.scala` to compare full output rather than
just the last line — and the suite will catch IR regressions from then
on.
