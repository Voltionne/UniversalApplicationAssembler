package UniversalApplicationAssembler

import UniversalApplicationAssembler.api.config.assembly.AssemblerConfig
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

    val customAssembler = new CustomAssembler(instructionMapping)

    val outputDir = Files.createTempDirectory("uaa-test1-results")
    println(s"Temp path Test1: $outputDir")

    val inputStream0 = getClass.getResourceAsStream("/Test1_0.asm")
    val outputPathString0 = outputDir.resolve("Test1_0-string.txt")
    val outputPathBinary0 = outputDir.resolve("Test1_0-binary.txt")

    val inputStream1 = getClass.getResourceAsStream("/Test1_1.asm")
    val outputPathString1 = outputDir.resolve("Test1_1-string.txt")
    val outputPathBinary1 = outputDir.resolve("Test1_1-binary.txt")

    customAssembler.compileToString(inputStream0, outputPathString0)
    customAssembler.compileToBinary(inputStream0, outputPathBinary0)

    customAssembler.compileToString(inputStream1, outputPathString1)
    customAssembler.compileToBinary(inputStream1, outputPathBinary1)
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

    val customAssembler = new CustomAssembler(instructionMapping)

    val outputDir = Files.createTempDirectory("uaa-test2-results")
    println(s"Temp path Test2: $outputDir")

    val inputStream0 = getClass.getResourceAsStream("/Test2_0.asm")
    val outputPathString0 = outputDir.resolve("Test2_0-string.txt")
    val outputPathBinary0 = outputDir.resolve("Test2_0-binary.txt")

    customAssembler.compileToString(inputStream0, outputPathString0)
    customAssembler.compileToBinary(inputStream0, outputPathBinary0)
  }

  test("Test3") {
    /*
    Testing to compile a code for the UPC SISA CPU of IC course (1st semester)
     */

    val stream = getClass.getResourceAsStream("/Test3.yaml")

    val instructionMapping = IsaParser.parse(stream)

    val customAssembler = new CustomAssembler(instructionMapping)

    val outputDir = Files.createTempDirectory("uaa-test3-results")
    println(s"Temp path Test3: $outputDir")

    val inputStream0 = getClass.getResourceAsStream("/Test3_0.asm")
    val outputPathString0 = outputDir.resolve("Test3_0-string.txt")
    val outputPathBinary0 = outputDir.resolve("Test3_0-binary.txt")

    customAssembler.compileToString(inputStream0, outputPathString0)
    customAssembler.compileToBinary(inputStream0, outputPathBinary0)
  }