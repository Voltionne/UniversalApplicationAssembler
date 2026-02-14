# Specifying an ISA

_updated for PyUAA 0.1.x_

This document treats how to specify an ISA for the assembler to compile.

The whole advantage of UAA is it can target any ISA by simply writing a YAML file that describes it, NO BACKEND NEEDED! But for doing this it is important to note what is the exact format and expected shape of the YAML file for the assembler to understand your ISA. It will be supposed that the reader knows how to write in YAML.

# Basic tables

There are 2 top tables that are recognized by the parser: "format" and "parameters". And only the first one is really mandatory. All the names of the tables are as written and case sensitive.

## format table

This table specifies the format and all instructions.

It is expected that this table has an entry called "bits" which indicates with an integer how many bits does each instruction have. There must also be a subtable called "definitions" which includes as keys the name of all fields used up in later instruction definitions and what their bit mapping is, specified as an string following the format of "a:b", where a and b are the range of bits affected, counting from zero from LSB. Following there is a basic example of how it would look:

```
format:

    bits: 16

    definitions: 
        opcode: "15:12"
        Rd3: "11:9"
        Rs5-1: "8:6"
        Rs5-2: "5:3"
        func3: "2:0"

        Imm6: "5:0"
        Imm12: "11:0"
```

There is no problem that some fields "collide" in the sense they use the same bits, these are the definitions of the fields ACROSS ALL FORMATS. Then each instruction and/or format decides what fields are used and what fields are not.

From here you can define AS MANY SUBTABLES AS YOU WANT, you can organize the YAML file as you want. The only condition is that there is a "instructions" subtable down somewhere to define the instructions. The following subsections will describe what can be specified in each of the subtables and how does the "instructions" subtable look.

### In-between tables

Each in-between table is ignored by the assembler and acts as pure organizator. These subtables, though, can write to fields partially or totally to form kind of a heirarchical organization. This written value will affect all subtables inside the table and is only removable if overwritten. Unwritten values have unknow value.

For writing to a partial field, simply open a key with the name of a field that was previously defined in "definitions" and then assign it a integer value for full writes or a subtable that has keys "set" to indicate the integer value and "bits" to indicate what bits are modified only, using the "a:b" notation or "a" if only changing a single bit, for partial writes. Now some examples:

```
format:

    bits: 16

    definitions: 
        opcode: "15:12"
        Rd3: "11:9"
        Rs5-1: "8:6"
        Rs5-2: "5:3"
        func3: "2:0"

        Imm6: "5:0"
        Imm12: "11:0"

    format_a: #subtable with any name, will be ignored

        opcode: 0b01 #this will perform a full write to opcode, setting it to 0b0001 for ALL subtables of format_a from here (or until overwritten).

        #everything here has opcode=0b0001

    format_b: #another subtable

        Imm12: {set: 0b111, bits: "5:3"} #sets bits 5:3 to 111, this is a partial write, also affects to ALL subtables of subtable2 from here (or until overwritten).
        opcode: {set: 0b1, bits: "2"} #opcode is partially written to 0b?1??.

        #everything here has Imm12=0b??????111??? and opcode=0b?1??

        subtable2:

            #here Imm12 is still 0b??????111???
```

This is powerful because it allows to organize the YAML file by formats and even subformats/families without having an strange shape.

### instructions subtable

This is the only subtable that has meaning inside format (apart from bits and definitions). It specifies the instructions.

It is a list of tables where each table contain the mandatory "name" field (which is the mmenoic the assembler will use to recognize the instruction) and optionally the parameters and field writes (like the ones shown in the previous section).

In case the instruction has parameters, they are specified by a table called "parameters" which has keys "values" and "mapping". The "values" key contains a list of the type of each parameter in the order they would be written in assembly, this "type" will be defined and clarified when explained in the "parameters" top table. In the other hand, the "mapping" key includes a list where each element is an string that tells to what field it should the parameter map or a list of strings where it tells where should the bits map from low to high, in that order.

Let's see an example (because this is hard to explain):

```
format:

    bits: 16

    definitions: 
        opcode: "15:12"
        Rd3: "11:9"
        Rs5-1: "8:6"
        Rs5-2: "5:3"
        func3: "2:0"

        Imm12: "11:0"

    format_a: #subtable with any name, will be ignored

        opcode: 0b01 #this will perform a full write to opcode, setting it to 0b0001 for ALL subtables of format_a from here (or until overwritten).

        #everything here has opcode=0b0001

        instructions:

          - name: "ADD"

            parameters:
                values: ["reg.int", "reg.int", "reg.int"]
                mapping: ["Rs5-1", "Rs5-2", "Rd5"]

            func3: 0b000

          - name: "ADD_IMM"

            parameters:
                values: ["reg.int", "imm6", "reg.int"]
                mapping: ["Rs5-2", ["Rs5-1", "func3"], "Rd5"]

    format_b: #another subtable

        Imm12: {set: 0b111, bits: "5:3"} #sets bits 5:3 to 111, this is a partial write, also affects to ALL subtables of subtable2 from here (or until overwritten).
        opcode: {set: 0b1, bits: "2"} #opcode is partially written to 0b?1??.

        #everything here has Imm12=0b??????111??? and opcode=0b?1??

        subtable2:

            Imm12: 0b0 #Imm12 is fully overwritten

            instructions:

              - name: "NOP"
                Imm12: {set: 0b1, bits:"3"}
                opcode: 0b0000 #opcode is also overwritten        
```

In this example, it can be seen, first of all, that there can be multiple "instructions" subtables in different places. Each of them adds more instructions to the ISA. In the case of the instruction "ADD" it can be see how func3 is set to 0b000 and there are 3 parameters specified. These three parameters are of type "reg.int" and each of them map to Rs5-1, Rs5-2, and Rd5 fields, respectively.

In the case of "ADD_IMM" it gets more interesting, because the second parameter is of type "imm6" which in this example is defined as an immediate of 6 bits. The mapping of this field, as there is no 6 bits one defined, consists on splitting the value and putting the lower bits on Rs5-1 (more exactly, the lower 3 bits, as Rs5-1 is 3 bits wide) and the higher bits in func3 (the upper 3 bits). Note that these field are not continuous, they are in "8:6" and "2:0", so the immediate will end up fragmented in the final encoding. This is the simplest way of splitting an immediate in multiple non-contiguous fields.

In the "NOP" instruction it can be seen a partial write of Imm12 happening in action, writing a high bit on position 3, and the overwrite of the opcode.

## parameters table

The parameters table defines all types of parameters that exist in the ISA and the assembler must recognize and translate to their binary values.

In this case the shape of this table is totally free, but there a two basic important constructs that must be known.

### Immediate specification

For specifying an immediate, simply put the name of the type as key and put and write a string following the format "a:0", where a is the number of bits minus 1 (this is to be consistent with "a:b" format, but starting from zero).

```
parameters:

    imm6: "5:0"

    other_immediates:

        aaaa: "11:0"
```

In this example, there are defined 2 parameter types: "imm6" and "other_immediates.aaaa". The first one is a 6 bit immediate, and the second one a 12 bit immediate. Here it can also be noted that if there are subtables they act as a namespace for the parameters types, separated by points. Now you could put "imm6" or "other_immediates.aaaa" as values in the parameter list of an instruction.

### Translation specification

Sometimes you need to translate a written notation to a value. For example, tell the assembler to translate "r0" to 0 or "PC" to 7, for implementing registers or extra arguments easily.

For making a translation table simply create a subtable where each key is the string to translate which maps to an integer value.

```
parameters:

    reg:

        int:

            r0: 0
            r1: 1
            r2: 2
            r3: 3
            r4: 4
            r5: 5
            r6: 6
            PC: 7
```

In this particular example, a parameter type called "reg.int" means the assembler will only accept "r0", "r1", "r2", "r3", ... "PC" as inputs and will translate that to the numerical value provided. Note that using "reg.int.r0", or another translation, directly as parameter type will result in error.

# Clarity Note

This file has to be HEAVILY rewritten. Literally this is probably the worst docs written, take them as mere temporal guidelines for at least remembering what should be done. The examples are correct, though.