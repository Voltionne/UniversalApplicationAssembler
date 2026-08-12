package UniversalApplicationAssembler

import UniversalApplicationAssembler.api.parsing.assembly.CustomAssembler
import UniversalApplicationAssembler.api.parsing.isa.IsaParser

import java.nio.file.Files

class MainTest extends munit.FunSuite:

  test("Full test") {

    val stream = getClass.getResourceAsStream("/testIsa2.yaml")

    val (instructionMapping, node) = IsaParser.debugParse(stream)

    println("Tree:")
    visualizeNodes(node)

    val customAssembler = CustomAssembler(instructionMapping)

    val inputStream = getClass.getResourceAsStream("/assembly.asm")

    val outputDir = Files.createTempDirectory("uaa-results-test1")
    println(s"Temp path: $outputDir")
    val outputPathString = outputDir.resolve("test-string.text")
    val outputPathBinary = outputDir.resolve("test-binary.txt")

    customAssembler.compileToString(inputStream, outputPathString)
    customAssembler.compileToBinary(inputStream, outputPathBinary)
  }

  test("Test limits yaml") {

    val stream = getClass.getResourceAsStream("/testIsa3.yaml")

    val (instructionMapping, node) = IsaParser.debugParse(stream)

    println("Tree:")
    visualizeNodes(node)
  }