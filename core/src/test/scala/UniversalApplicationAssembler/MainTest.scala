package UniversalApplicationAssembler

import UniversalApplicationAssembler.api.parsing.{IsaParser, CustomAssembler}
import UniversalApplicationAssembler.internal.parsing.assembly.InstructionMapping

import java.nio.file.{Path, Files}
import scala.util.Using

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

    println("Instructions:")
    for instruction <- isaParser.instructions do
      println(instruction)

    //create the mapping:
    val instructionMapping = InstructionMapping(isaParser.instructions)

    val customAssembler = CustomAssembler(instructionMapping)

    val inputPath = Helper.getResourcePath("/assembly.asm")
    
    val outputDir = Files.createTempDirectory("uaa-results")
    val outputPathString = outputDir.resolve("test-string.text")
    val outputPathBinary = outputDir.resolve("test-binary.txt")
    
    customAssembler.compileToString(inputPath, outputPathString)
    customAssembler.compileToBinary(inputPath, outputPathBinary)
  }