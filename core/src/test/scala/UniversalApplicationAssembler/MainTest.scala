package UniversalApplicationAssembler

import UniversalApplicationAssembler.api.parsing.{CustomAssembler, IsaParser}
import UniversalApplicationAssembler.internal.parsing.assembly.InstructionMapping

import java.nio.file.{Files, Path}

object Helper:

  def getResourcePath(name: String): Path =

    val url = getClass.getResource(name)
    java.nio.file.Paths.get(url.toURI)


class MainTest extends munit.FunSuite:

  test("Full test") {

    val path = Helper.getResourcePath("/testIsa2.yaml")
    val isaParser = IsaParser(path)

    val node = isaParser.parse()

    println("Tree:")
    visualizeNodes(node)

    val whiteList = List("WUPP", "WLOW", "ADD")

    println("Instructions:")
    for instruction <- isaParser.instructions if whiteList.contains(instruction.name) do
      println(s"Instruction: ${instruction.name}. Fields: ${instruction.fields}")
      for idx <- instruction.parameters.datatypes.indices do
        println(s"  - dt: ${instruction.parameters.datatypes(idx)}")
        println(s"  - ma: ${instruction.parameters.mappings(idx)}")

    //create the mapping:
    val instructionMapping = InstructionMapping(isaParser.instructions)

    val customAssembler = CustomAssembler(instructionMapping)

    val inputPath = Helper.getResourcePath("/assembly.asm")

    val outputDir = Files.createTempDirectory("uaa-results")
    println(s"Temp path: $outputDir")
    val outputPathString = outputDir.resolve("test-string.text")
    val outputPathBinary = outputDir.resolve("test-binary.txt")

    customAssembler.compileToString(inputPath, outputPathString)
    customAssembler.compileToBinary(inputPath, outputPathBinary)
  }