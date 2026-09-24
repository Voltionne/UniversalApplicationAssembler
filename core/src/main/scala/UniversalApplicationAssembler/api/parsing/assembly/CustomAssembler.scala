package UniversalApplicationAssembler.api.parsing.assembly

import UniversalApplicationAssembler.api.config.assembly.AssemblerConfig
import UniversalApplicationAssembler.api.parsing.isa.InstructionMapping
import UniversalApplicationAssembler.internal.parsing.isa.InstructionTemplate

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

/**
 * Represents a custom assembler that acts over certain instructions
 * @param instructionMapping Represents the instructions supported on this ISA
 * @param assemblerConfig The configuration of the assembler (see the class for documentation)
 */
class CustomAssembler(instructionMapping: InstructionMapping, assemblerConfig: AssemblerConfig):

  /**
   * Represents a custom assembler that acts over certain instructions. This constructor uses default configuration
   * @param instructionMapping Represents the instructions supported on this ISA
   */
  def this(instructionMapping: InstructionMapping) =
    this(instructionMapping, AssemblerConfig())

  /**
   * Compiles an assembly string to a string representation of "1" and "0" for debugging purposes
   * @param source The string of the source file
   * @param outputFile The path of the output file
   */
  def compileToString(source: String, outputFile: Path): Unit =
    val assemblyFile = CustomAssembler.preprocessFile(source) //Preprocess: i.e. delete comments

    val (tags, finalAssemblyFile) = CustomAssembler.getTags(assemblyFile)

    val instructions = CustomAssembler.parseToList(finalAssemblyFile)

    if instructions.nonEmpty then

      var compiledCode = ""

      for (instruction, instructionIdx) <- instructions.zipWithIndex do

        for idx <- instruction.indices do
          if instruction(idx).head == '@' then //This is a tag

            if assemblerConfig.useRelativeAddresses then
              instruction(idx) = (tags(instruction(idx)) - instructionIdx).toString
            else
              instruction(idx) = (tags(instruction(idx)) + assemblerConfig.startAddress).toString

        compiledCode += compileInstruction(instruction) + "\n"

      Files.writeString(outputFile, compiledCode, StandardCharsets.UTF_8)

    else
      Files.writeString(outputFile, "", StandardCharsets.UTF_8) //write empty

  /**
   * Compiles a source file to a string representation of "1" and "0", for debugging purposes
   *
   * @param sourceFile The path of the source file
   * @param outputFile The path of the output file
   */
  def compileToString(sourceFile: Path, outputFile: Path): Unit =
    compileToString(Files.readString(sourceFile), outputFile)

  /**
   * Compiles a source assembly input stream to a string representation of "1" and "0", for debugging purposes
   *
   * @param sourceInputStream The input stream of the source file
   * @param outputFile The path of the output file
   */
  def compileToString(sourceInputStream: InputStream, outputFile: Path): Unit =
    compileToString(String(sourceInputStream.readAllBytes()), outputFile)

  /**
   * Compiles an assembly string to a binary file
   *
   * @param source The string of the source file
   * @param outputFile The path of the output file
   */
  def compileToBinary(source: String, outputFile: Path): Unit =
    val assemblyFile = CustomAssembler.preprocessFile(source) //Preprocess: i.e. delete comments

    val (tags, finalAssemblyFile) = CustomAssembler.getTags(assemblyFile)

    val instructions = CustomAssembler.parseToList(finalAssemblyFile)

    if instructions.nonEmpty then

      var compiledCode = ""

      for (instruction, instructionIdx) <- instructions.zipWithIndex do

        for idx <- instruction.indices do
          if instruction(idx).head == '@' then //This is a tag

            if assemblerConfig.useRelativeAddresses then
              instruction(idx) = (tags(instruction(idx)) - instructionIdx).toString
            else
              instruction(idx) = (tags(instruction(idx)) + assemblerConfig.startAddress).toString

        compiledCode += compileInstruction(instruction)

      //Write the binary directly
      Files.write(outputFile, CustomAssembler.bitsToBytes(compiledCode))

    else
      Files.writeString(outputFile, "", StandardCharsets.UTF_8) //write empty

  /**
   * Compiles a source file to a binary file
   * @param sourceFile The path of the source file
   * @param outputFile The path of the output file
   */
  def compileToBinary(sourceFile: Path, outputFile: Path): Unit =
    compileToBinary(Files.readString(sourceFile), outputFile)

  /**
   * Compiles a source assembly input stream to a binary file
   *
   * @param sourceInputStream The input stream of the source file
   * @param outputFile The path of the output file
   */
  def compileToBinary(sourceInputStream: InputStream, outputFile: Path): Unit =
    compileToBinary(String(sourceInputStream.readAllBytes()), outputFile)

  private def compileInstruction(parsedWrittenInstruction: Array[String]): String =

    /*
    Ok, there is a BIG problem with mutability right now. The thing is that when InstructionTemplate is called apply(), for compiling an instruction,
    it MODIFIES its internal state to fit the parameters. That means that if hypothetically you needed the "original" InstructionTemplate without the
    fields modified (for example, two instructions called the same where one maintains a field as "constant" (i.e. does not write to it except the writes
    performed at a higher level) and the other modifies it -> the first instruction may see the second instruction state instead of a clean state).

    This shouldn't be a problem in 99% of implementations (who tf is going to do what I just mentioned?), but is a thing to keep into consideration for
    future updates. Therefore, this is a TODO.

    Yooo, longest comment ever
     */

    //Calls to .apply always works because if no parameters, simply the for loop is skipped in InstructionTemplate

    val instruction = instructionMapping(parsedWrittenInstruction.head)

    instruction match
      case instructionTemplate: InstructionTemplate => //Single instruction
        instructionTemplate.apply(parsedWrittenInstruction.tail)
        instructionTemplate.compileInstruction

      case l: List[?] => //Multiple instructions
        val listInstructions = l.asInstanceOf[List[InstructionTemplate]]

        val firstInstruction = listInstructions.iterator //first instruction that does not fail apply (they are in written order of the YAML file, theoretically)
          .map { instruction =>
            util.Try(instruction.apply(parsedWrittenInstruction.tail)) match
              case util.Success(value) => Some(instruction)
              case util.Failure(exception) => None
          }
          .collectFirst {
            case Some(instructionTemplate: InstructionTemplate) => instructionTemplate
          }

        firstInstruction match
          case Some(instruction) => instruction.compileInstruction
          case None => throw new NoSuchElementException(s"Instruction \"${parsedWrittenInstruction.head}\" doesn't coincide with any format of such instruction!")

/**
 * Helper functions of CustomAssembler
 */
private object CustomAssembler:

  private def preprocessFile(assemblyFile: String): String =
    val singleComment = "//.*"
    val fixed = assemblyFile.replaceAll(singleComment, "")

    val multilineComment = "/\\*[\\S\\s]*?\\*/"
    fixed.replaceAll(multilineComment, "")

  /**
   * Separates instructions and operands of an assembly file represented as a String.
   *
   * @param assemblyFile The string representing the assembly file
   * @return Each instruction separated into opcode and operands.
   */
  private def parseToList(assemblyFile: String): List[Array[String]] =

    //Instruction format: (May change in the future)
    //[OPERAND] [PARAM1], [PARAM2], [PARAM3] [...]

    val assemblyLines = assemblyFile.split("\n").map(_.trim).filter(_.nonEmpty)
    var temp: List[Array[String]] = List.empty

    for assemblyLine <- assemblyLines do
      val parts = assemblyLine.split("\\s+", 2)

      if parts.length == 1 then
        temp = parts :: temp
      else
        val opcode = parts(0)
        val operands = parts(1).split(",").map(_.trim)
        temp = (opcode +: operands) :: temp

    //Prepends everything for performance, now reverse. This is O(n)
    temp.reverse

  /**
   * Gets the tags of the assembly file
   *
   * @param assemblyFile The assembly file string
   * @return The tags extracted and the assembly file with the tags extracted
   */
  private def getTags(assemblyFile: String): (Map[String, BigInt], String) =

    val assemblyLines = assemblyFile.split("\n").map(_.trim).filter(_.nonEmpty)

    var instructionCount: BigInt = 0
    var tags = Map.empty[String, BigInt]

    //If a line starts by "@" it is a tag
    for assemblyLine <- assemblyLines do
      if assemblyLine.head == '@' then
        tags += (assemblyLine -> instructionCount)
      else
        instructionCount += 1

    val tagsRemover = "\n@.*"
    (tags, assemblyFile.replaceAll(tagsRemover, "\n"))

  /**
   * Converts a string representing a sequence of 0s and 1s to an array of bytes.
   *
   * @param bits The string representing the single bits
   * @return An array of bytes
   */
  private def bitsToBytes(bits: String): Array[Byte] =

    //Right-padding for multiple of 8, for bytes.
    val padded =
      val mod = bits.length % 8
      if mod == 0 then bits
      else "0" * (8 - mod) + bits

    padded.grouped(8)
      .map { byteStr =>
        Integer.parseInt(byteStr, 2).toByte
      }
      .toArray

