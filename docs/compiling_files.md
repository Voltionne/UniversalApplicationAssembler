# Compiling files

_updated for UAA >= v1.0.1_

The UAA api is designed to be straightforward to use and sensical. For simplicity, all api is concentrated in package `UniversalApplicationAssembler.api`.

## Reading a YAML ISA specification file

For parsing an ISA specification, the object `IsaParser` is used, under package `UniversalApplicationAssembler.api.parsing.isa`. This includes a function called `parse` which receives the YAML specification file path and returns an instance of class `InstructionMapping` (under the same package) which represents the ISA and its instructions.

The example below shows the procedure:

```scala worksheet
import UniversalApplicationAssembler.api.parsing.isa.{IsaParser, InstructionMapping}

import java.nio.file.Path

val pathToYAML: Path = Path.of("your/path/here")

val instructionMapping: InstructionMapping = IsaParser.parse(pathToYAML)
```

For debugging or more advanced users, IsaParser also includes function `debugParse`. This function does the same but returns also a tree of nodes (more exactly, the top node of that tree) that represents the internal organization of variables in the YAML file. This is an instance of `TranslationNode` under package `UniversalApplicationAssembler.internal.parsing.yaml.translation`. Nevertheless, note that this is for pure debugging and isn't technically public API (as the package name "internal" suggests), therefore it has no guarantees of continuity.

## Compiling code

Once the ISA specification has been parsed, it can be used to compile any file written in the recognized assembly. This is done thanks to the class `CustomAssembler`, under `UniversalApplicationAssembler.api.parsing.assembly`.

This class takes as constructor parameter a `InstructionMapping` representing the ISA and then becomes essentially an assembler that can compile any code based on that ISA and assembly specification.

After initialized the assembler, compiling is a trivial as calling either function `compileToBinary` or `compileToString`. Both functions take a source file path and an output file path, but they differ in the result they generate. The first one compiles to binary properly (more exactly, to an array of bytes padded right in case of not matching a byte multiple size). And the second one generates strings of 1s and 0s where each line is a compiled instruction. For this reason, for any serious compilation one should use `compileToBinary`, and the other function purely for debugging purposes.

The following example continues the previous one and shows how to do it:

```scala worksheet
// [previous example code ...]

import UniversalApplicationAssembler.api.parsing.assembly.CustomAssembler

val customAssembler = CustomAssembler(instructionMapping)

val sourcePath: Path = Path.of("your/path/here")
val outputPathBinary: Path = Path.of("your/path/here")
val outputPathString: Path = Path.of("your/path/here")

customAssembler.compileToBinary(sourcePath, outputPathBinary)
customAssembler.compileToString(sourcePath, outputPathString)

//Congratulations, you've made it!
```