# Compiling files

_updated for PyUAA 0.1.x_

Compiling a file with UAA is trivial. First of all simply import the assembler

```
import UniversalApplicationAssembler as UAA
```

The "UAA" shorthand is recommended to use when importing, to avoid long names.

After this simply create an instance of the Assembler and pass it the path of the YAML configuration file:

```
import UniversalApplicationAssembler as UAA

assembler = UAA.Assembler("path/to/config/yaml.yaml")
```

If auto_update argument is left as true, this will automatically start the parsing of the ISA. Then, compiling any file is a simple as calling the function compile_code, passing the path of the assembly source file and the path of destination:

```
assembler.compile_code("my_source.asm", "compiled_code.bin")
```

As of versions 0.1.x, currently the output file is string output where each line are the binary representation of each instruction. Real binary output will be added in a future update.