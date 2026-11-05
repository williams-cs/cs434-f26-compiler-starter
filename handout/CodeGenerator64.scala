package ic.cg;

import ic.ast.Program
import java.io.PrintWriter
import scala.collection.mutable.HashMap
import ic.ast.Program
import java.io.FileWriter


/**
 * A x86_64 CodeGenerator generates the assembly code for a program,
 * after each MethodDecl has been annotated with TACLists.  There is
 * some basic functionality for dealing with string constants and
 * their labels.
 * <p>
 * Feel free to change this code in any way that you find useful,
 * or to ignore it completely and roll your own code generator from 
 * scratch.
 * <p> 
 * This code assumes that your top-level AST node is called
 * Program.  You may need to change this to match your own AST
 * hierarchy.  Usage:
 *
 * <pre>
 *   val cg = new CodeGenerator("file.ic", program);
 *   cg.generate();
 * </pre>
 */
class CodeGenerator64(icFileName : String, val program : Program) {

    // The .s file goes next to the source file, whatever directory
    // the compiler was run from.
    val asmFile = {
        val src = new java.io.File(icFileName);
        val stem = src.getName.substring(0, src.getName.lastIndexOf('.'));
        val dir = src.getParentFile;
        (if (dir == null) new java.io.File(stem + ".s") else new java.io.File(dir, stem + ".s")).getPath
    }
    val out = new PrintWriter(new FileWriter(asmFile));        

    /**
     * The main method to call when you wish to perform the
     * translation.
     */
    def generate() = {

        out.println("# File " + asmFile);
        out.println();
        out.println();

        generateVTables();
        generateCode();
        generateErrorHandlers();

        // TODO: Provide the real information for main.

        val classForMain = "Simple";
        val sizeOfMainObject = 8;
        val offsetOfMainInDV = 0;
        generateMain(classForMain, sizeOfMainObject, offsetOfMainInDV);

        generateStringConstants();

        // Suppress the linker's executable-stack warning on Linux.  This ELF-only
        // directive is rejected by the macOS assembler, so emit it only there.
        if (System.getProperty("os.name").toLowerCase.contains("linux")) {
            out.println()
            out.println("# Suppress executable stack warning from linker")
            out.println(".section .note.GNU-stack,\"\",@progbits")
            out.println()
        }

        out.close();
    }

    /**
     * Print the VTable for all the classes.  You'll need to change this.
     *
     */
    protected def generateVTables() = { 

        // TODO: You must implement this method 

        out.println("# ----------------------------");                
        out.println("# VTables");                

        out.println();
        out.println(".data");
        out.println(".align 8");
        out.println();

        out.println("_Simple_DV:");
        out.println("      .quad _Simple_main");
        out.println("");
        out.println("");
        out.println("");
    }


    /**
     * Generate Simple.main method.  You'll need to change this.
     */
    protected def generateCode() = { 

        // TODO: You must implement this method 

        out.println("# ----------------------------");
        out.println("# Code.  Just the Simple main method for now...");
        out.println("");
        out.println(".text");
        out.println(".align 8");
        out.println("_Simple_main:");
        out.println("");
        out.println("     # prologue");
        out.println("     pushq %rbp                    ");
        out.println("     movq %rsp, %rbp                ");
        out.println("     subq $8, %rsp                ");
        out.println("");
        out.println("     # store 1234 into a local variable.");
        out.println("     movq $1234, %rcx                     ");
        out.println("     movq %rcx, -8(%rbp)");
        out.println("");
        out.println("     # print that local var...");
        out.println("     movq -8(%rbp), %rdi");
        out.println("     call __LIB_printi");
        out.println("");
        out.println("     # ... and a new line");
        val lab = labelForStringConstant("\na string \n containing \t\t quotes and escape characters: \"moo\"\n");
        out.println(s"     movq ${lab}(%rip), %rdi\n"); 
        out.println("     call __LIB_println");
        out.println("        ");
        out.println("     # epilogue");
        out.println("     movq %rbp, %rsp                ");
        out.println("     popq %rbp                      ");
        out.println("     ret                           ");
        out.println("");
        out.println("");
        out.println("");
    }

    /**
     * Print out the assembly code to print run-time errors and
     * exit gracefully.  You should jump to these labels on
     * run-time check failure.  You should not need to change this
     * method.
     */
    protected def generateErrorHandlers() = {
        out.println("# ----------------------------");
        out.println("# Error handling.  Jump to these procedures when a run-time check fails.");
        out.println("");
        out.println(".data");
        out.println(".align 8");
        out.println("");
        out.println(".quad 23");
        out.println("  strNullPtrErrorChars:     .ascii \"Null pointer violation.\"");
        out.println("strNullPtrError: .quad strNullPtrErrorChars");
        out.println("");
        out.println(".quad 23");
        out.println("  strArrayBoundsErrorChars: .ascii \"Array bounds violation.\"");
        out.println("strArrayBoundsError: .quad strArrayBoundsErrorChars");
        out.println("");
        out.println(".quad 21");
        out.println("  strArraySizeErrorChars:   .ascii \"Array size violation.\"");
            out.println("strArraySizeError: .quad strArraySizeErrorChars");
        out.println("");
        out.println(".quad 22");
        out.println("  divByZeroErrorChars:      .ascii \"Divide by 0 violation.\"");
        out.println("divByZeroError: .quad divByZeroErrorChars");
        out.println("");

        out.println(".text");
        out.println(".align 8");
        out.println("labelNullPtrError:");
        out.println("    movq strNullPtrError(%rip), %rdi");
        out.println("    call __LIB_println");
        out.println("    movq $1, %rdi");
        out.println("    call __LIB_exit");
        out.println("");
        out.println(".align 8");
        out.println("labelArrayBoundsError:");
        out.println("    movq strArrayBoundsError(%rip), %rdi");
        out.println("    call __LIB_println");
        out.println("    movq $1, %rdi");
        out.println("    call __LIB_exit");
        out.println("");
        out.println(".align 8");
        out.println("labelArraySizeError:");
        out.println("    movq strArraySizeError(%rip), %rdi");
        out.println("    call __LIB_println");
        out.println("    movq $1, %rdi");
        out.println("    call __LIB_exit");
        out.println("");
        out.println(".align 8");
        out.println("labelDivByZeroError:");
        out.println("    movq divByZeroError(%rip), %rdi");
        out.println("    call __LIB_println");
        out.println("    movq $1, %rdi");
        out.println("    call __LIB_exit");
        out.println("");
        out.println("");
        out.println("");
    }

    /**
     * Generate the __ic_main stub that creates and calls main on
     * the right object.  You should not need to change this
     * method.
     * @param className                name of the class containing main
     * @param objectSize               size of objects of that class
     * @param indexOfMainInVTable      index on that class's vtable for main
     */
    protected def generateMain(className : String, objectSize : Int, indexOfMainInVTable : Int) = {
        out.println("# The main entry point.  Allocate object and invoke main on it.");
        out.println("");
        out.println(".text");
        out.println(".align 8");
        out.println(".globl __ic_main");
        out.println("__ic_main:");
        out.println("       pushq %rbp                        # prologue");
        out.println("       movq %rsp,%rbp                ");
        out.println("       pushq %rdi                        # o.main(args) -> push args");
        out.println("");
        out.println(s"       movq $$${objectSize}, %rdi                 # o = new $className\n"); 
        out.println("       call __LIB_allocateObject   ");
        out.println(s"       leaq _${className}_DV(%rip), %rdi       \n");
        out.println("       movq %rdi, (%rax)");
        out.println("       pushq %rax                        # o.main(args) -> push o");
        out.println("       movq (%rax), %rax            ");
        out.println(s"       call *${indexOfMainInVTable * 8}(%rax)                   # main is at offset $indexOfMainInVTable in vtable\n"); 
        out.println("       addq $16, %rsp                ");
        out.println("       movq $0, %rax                     # __ic_main always returns 0");
        out.println("");
        out.println("       movq %rbp,%rsp                    # epilogue");
        out.println("       popq %rbp                    ");
        out.println("       ret                         ");
        out.println("");
        out.println("");
        out.println("");
    }


    /********************** String Constants *********************/


    /** A map from string constant to the assembly code label in
     * the data segment where that constant is stored.  See the
     * labelForStringConstant method.
     */
    protected val stringConstantsToLabel : HashMap[String,String] = new HashMap[String,String]();

    /**
     * Return a unique label for a string constant.  After
     * translating all code and getting labels for all string
     * constants, the code generator will print out a data segment
     * containing the labels and string constants.  The string may
     * contain only the following escape characters: \n, \r, \t.
     */
    protected def labelForStringConstant(stringConstant : String) : String = {
            stringConstantsToLabel.get(stringConstant) match {
            case None => { 
                val label = "_str" + stringConstantsToLabel.size;
                stringConstantsToLabel.put(stringConstant, label);
                label;
            }
            case Some(label) => {
                label;
            }
            }
    }


    /**
     * Iterate over all used string constants and print them into
     * a data segment.  You should not need to change this method.
     */
    protected def generateStringConstants() = {
        out.println("# ----------------------------");                
        out.println("# String Constants");                

        out.println();
        out.println(".data");
        out.println(".align 8");
        out.println();

        for ((str, label) <- stringConstantsToLabel) {
            val len = str.length();
            out.println(s".quad $len");
            // NOTE: backslashes must be escaped first, or the backslashes
            // introduced by the other replacements would be escaped again.
            val escapedString =
                    "\"" + str.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"").replace("\r", "\\r") + "\"";
            out.println(s"  ${label}Chars:\t.ascii ${escapedString}");
            out.println(s"${label}:\t.quad ${label}Chars");
        }
        out.println("");
        out.println("");
        out.println("");
    }
}

