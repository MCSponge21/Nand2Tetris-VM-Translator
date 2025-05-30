# Hack VM Translator

---

## Project Overview

This repository contains a **Virtual Machine (VM) Translator** for the Hack platform, developed as part of the "Elements of Computing Systems" (Nand2Tetris) course, specifically covering **Projects 7 and 8**.

This program serves as a critical layer in the compilation pipeline, translating the **Hack VM intermediate language** into **Hack Assembly code**. It effectively bridges the gap between a high-level, stack-based virtual machine and the low-level, register-oriented Hack assembly language, which can then be assembled into binary machine code by the Hack Assembler (Project 6).

## Features

This VM Translator fully implements the translation of all Hack VM commands, providing robust and efficient assembly output:

* **Comprehensive VM Command Set:** Handles all standard VM commands, including:
    * **Arithmetic/Logical Operations:** `add`, `sub`, `neg`, `eq`, `gt`, `lt`, `and`, `or`, `not`.
    * **Memory Access Commands:** `push` and `pop` for all VM segments (`constant`, `local`, `argument`, `this`, `that`, `static`, `pointer`, `temp`).
    * **Control Flow Commands:** `label`, `goto`, `if-goto`.
    * **Function Calling Convention:** `function`, `call`, and `return` commands, correctly managing the call stack, segment pointers, and return addresses.
* **Modular Design:** The translator is structured to handle parsing and code generation for each VM command type independently.
* **Multi-File Translation:** Capable of processing entire directories of `.vm` files, automatically concatenating their output into a single `.asm` file and ensuring `Sys.vm` is translated first for proper bootstrapping.
* **Robust Label Management:** Incorporates mechanisms to generate unique labels for arithmetic jumps and function call return addresses, preventing conflicts in the final assembly output.
* **Initial Program Setup:** Generates essential bootstrap code for the Hack VM, initializing the stack pointer and setting up the execution environment.

## How to Use

To translate your Hack VM code (`.vm` files) into Hack Assembly (`.asm` files):

1.  **Compile the Java code:**
    ```bash
    javac VM.java
    ```

2.  **Run the translator:**
    * **For a single `.vm` file:**
        ```bash
        java VM <path/to/your/file.vm>
        ```
        Example: `java VM ./FunctionCalls/SimpleFunction/SimpleFunction.vm`
        This will create `SimpleFunction.asm` in the same directory.

    * **For a directory containing multiple `.vm` files:**
        ```bash
        java VM <path/to/your/directory>
        ```
        Example: `java VM ./StackArithmetic/StackTest`
        This will create `StackTest.asm` inside the `StackArithmetic/StackTest` directory.

The generated `.asm` file can then be loaded into the Hack Assembler (Project 6) to produce executable binary code for the Hack CPU.

## Project Context

This VM Translator is a foundational component of the Nand2Tetris curriculum. It represents the transformation of a high-level, stack-oriented VM language (which the Jack compiler targets) into low-level assembly instructions for the Hack computer. This project highlights the power of **abstraction** in computer architecture and software design, showing how a complex system can be built layer by layer.
