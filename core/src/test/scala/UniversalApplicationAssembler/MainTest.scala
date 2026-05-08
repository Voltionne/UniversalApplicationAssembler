package UniversalApplicationAssembler

import UniversalApplicationAssembler.api.parsing.assembly.CustomAssembler
import UniversalApplicationAssembler.api.parsing.isa.{InstructionMapping, IsaParser}

import java.nio.file.{Files, Path}

object Helper:

  def getResourcePath(name: String): Path =

    val url = getClass.getResource(name)
    java.nio.file.Paths.get(url.toURI)


class MainTest extends munit.FunSuite:

  test("Full test") {

    val path = Helper.getResourcePath("/testIsa2.yaml")

    val (instructionMapping, node) = IsaParser.debugParse(path)

    println("Tree:")
    visualizeNodes(node)

    val customAssembler = CustomAssembler(instructionMapping)

    val inputPath = Helper.getResourcePath("/assembly.asm")

    val outputDir = Files.createTempDirectory("uaa-results")
    println(s"Temp path: $outputDir")
    val outputPathString = outputDir.resolve("test-string.text")
    val outputPathBinary = outputDir.resolve("test-binary.txt")

    customAssembler.compileToString(inputPath, outputPathString)
    customAssembler.compileToBinary(inputPath, outputPathBinary)
  }