package UniversalApplicationAssembler.api.parsing

import UniversalApplicationAssembler.internal.parsing.assembly.InstructionMapping
import UniversalApplicationAssembler.internal.parsing.assembly.AssemblyParser

import java.nio.file.{Path, Files}
import java.nio.charset.StandardCharsets


/**
 * Represents a custom assembler that acts over certain instructions
 * @param instructionMapping Represents the instructions supported on this ISA.
 */
class CustomAssembler(instructionMapping: InstructionMapping):

  /**
   * Compiles a source file to a string representation of "1" and "0", for debugging purposes
   * @param sourceFile The path of the source file
   * @param outputFile The path of the output file
   */
  def compileToString(sourceFile: Path, outputFile: Path): Unit =

    val assemblyFile = Files.readString(sourceFile)

    val instructions = AssemblyParser.parseToList(assemblyFile)

    if instructions.nonEmpty then

      var compiledCode = ""

      for instruction <- instructions do
        compiledCode += instructionMapping.compileInstruction(instruction) + "\n"

      Files.writeString(outputFile, compiledCode, StandardCharsets.UTF_8)

    else
      Files.writeString(outputFile, "", StandardCharsets.UTF_8) //write empty

  /**
   * Compiles a source file to a binary file
   * @param sourceFile The path of the source file
   * @param outputFile The path of the output file
   */
  def compileToBinary(sourceFile: Path, outputFile: Path): Unit =

    val assemblyFile = Files.readString(sourceFile)

    val instructions = AssemblyParser.parseToList(assemblyFile)

    if instructions.nonEmpty then

      var compiledCode = ""

      for instruction <- instructions do
        compiledCode += instructionMapping.compileInstruction(instruction)

      //Write the binary directly
      Files.write(outputFile, AssemblyParser.bitsToBytes(compiledCode))

    else
      Files.writeString(outputFile, "", StandardCharsets.UTF_8) //write empty