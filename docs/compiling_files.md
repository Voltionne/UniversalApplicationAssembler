# Compiling files

_updated for UAA 1.0.x_

The UAA api is designed to be straightforward to use and sensical. Everything used for compiling is usually under `UniversalApplicationAssembler.api.parsing` package.

## Reading a YAML ISA specification file

For reading a file, the class `IsaParser` is used, under the package mentioned just before. This class takes the path to the YAML file, and parsing it is as simple as calling `parse()` function. The example below shows the procedure

```scala worksheet
import UniversalApplicationAssembler.api.parsing.IsaParser

import java.nio.file.Path

val pathToYAML: Path = Path.of("your/path/here")

val isaParser = IsaParser(pathToYAML)

val translationNode = isaParser.parse()
```

As it can be seen from the example, the function `parse()` of `IsaParser` returns a translation node. This represents the top node of the tree that the function has built to analyze the ISA specification, its usage in the public api is purely for debugging. The exact datatype of this node is the class `UniversalApplicationAssembler.internal.parsing.yaml.translation.TranslationNode`

## Compiling code

Once the ISA specification has been parsed, it can be used to compile any file written in the recognized assembly. This is done thanks to the class `CustomAssembler`, also under `UniversalApplicationAssembler.api.parsing`.

`CustomAssembler` takes as input an object of type `InstructionMapping` which represents the instructions specified in the ISA. For creating such object, the instructions that generated the `IsaParser` when parsing (stored in variable `instructions`) are passed to this class. In this case, this is the only time some internal class is ever referenced, as `InstructionMapping` is not under the api package namespace currently (this may change in future versions), it is `UniversalApplicationAssembler.internal.parsing.assembly`.

After initialized the assembler, compiling is a trivial as calling either function `compileToBinary` or `compileToString`. Both functions take a source file path and an output file path, but they differ in the result they generate. The first one compiles to binary properly (more exactly, to an array of bytes padded right in case of not matching a byte multiple size). And the second one generates strings of 1s and 0s where each line is a compiled instruction. For this reason, for any serious compilation one should use `compileToBinary`, and the other function purely for debugging purposes.

The following example continues the previous one and shows how to do it:

```scala worksheet
// [previous example code ...]

import UniversalApplicationAssembler.api.parsing.CustomAssembler
import UniversalApplicationAssembler.internal.parsing.assembly.InstructionMapping

val instructionMapping = InstructionMapping(isaParser.instructions)

val customAssembler = CustomAssembler(instructionMapping)

val sourcePath: Path = Path.of("your/path/here")
val outputPathBinary: Path = Path.of("your/path/here")
val outputPathString: Path = Path.of("your/path/here")

customAssembler.compileToBinary(sourcePath, outputPathBinary)
customAssembler.compileToString(sourcePath, outputPathString)

//Congratulations, you've made it!
```