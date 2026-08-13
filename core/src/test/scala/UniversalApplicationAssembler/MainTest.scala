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
    val outputPathString = outputDir.resolve("test-string.txt")
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

  test("Galaicum16v1_1 ISA") {
    /*
    Test of parsing the YAML of the Galaicum16v1_1 ISA (of the Gala I CPU).
    This ISA is extremely simple yet very irregular, which is great for testing the compiler.
     */

    val stream = getClass.getResourceAsStream("/g1611_isa.yaml")

    val (instructionMapping, node) = IsaParser.debugParse(stream)

    println("Tree:")
    visualizeNodes(node)

    val customAssembler = CustomAssembler(instructionMapping)

    val inputStream = getClass.getResourceAsStream("/g1611_assembly.asm")

    val outputDir = Files.createTempDirectory("uaa-results-test3")
    println(s"Temp path G1611: $outputDir")

    val outputPathString = outputDir.resolve("test-string.txt")
    val outputPathBinary = outputDir.resolve("test-binary.txt")

    customAssembler.compileToString(inputStream, outputPathString)
    customAssembler.compileToBinary(inputStream, outputPathBinary)

  }