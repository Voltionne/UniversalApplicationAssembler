package UniversalApplicationAssembler

import UniversalApplicationAssembler.api.parsing.assembly.CustomAssembler
import UniversalApplicationAssembler.api.parsing.isa.IsaParser

import java.nio.file.Files

class MainTest extends munit.FunSuite:

  test("Test0") {

    val stream = getClass.getResourceAsStream("/Test0.yaml")

    val (instructionMapping, node) = IsaParser.debugParse(stream)

    println("Tree:")
    visualizeNodes(node)
  }

  test("Test1") {

    val stream = getClass.getResourceAsStream("/Test1.yaml")

    val (instructionMapping, node) = IsaParser.debugParse(stream)

    println("Tree:")
    visualizeNodes(node)

    val customAssembler = CustomAssembler(instructionMapping)

    val inputStream = getClass.getResourceAsStream("/Test1.asm")

    val outputDir = Files.createTempDirectory("uaa-test1-results")
    println(s"Temp path Test1: $outputDir")
    val outputPathString = outputDir.resolve("test-string.txt")
    val outputPathBinary = outputDir.resolve("test-binary.txt")

    customAssembler.compileToString(inputStream, outputPathString)
    customAssembler.compileToBinary(inputStream, outputPathBinary)
  }

  test("Test2") {
    /*
    Test of parsing the YAML of the Galaicum16v1_1 ISA (of the Gala I CPU).
    This ISA is extremely simple yet very irregular, which is great for testing the compiler.
     */

    val stream = getClass.getResourceAsStream("/Test2.yaml")

    val (instructionMapping, node) = IsaParser.debugParse(stream)

    println("Tree:")
    visualizeNodes(node)

    val customAssembler = CustomAssembler(instructionMapping)

    val inputStream = getClass.getResourceAsStream("/Test2.asm")

    val outputDir = Files.createTempDirectory("uaa-test2-results")
    println(s"Temp path Test2: $outputDir")

    val outputPathString = outputDir.resolve("test-string.txt")
    val outputPathBinary = outputDir.resolve("test-binary.txt")

    customAssembler.compileToString(inputStream, outputPathString)
    customAssembler.compileToBinary(inputStream, outputPathBinary)

  }