package UniversalApplicationAssembler

import UniversalApplicationAssembler.parsing.Node

import scala.util.Using

class MainTest extends munit.FunSuite:

  test("First pass test") {

    Using.resource(getClass.getResourceAsStream("/testIsa2.yaml")) {stream =>
      val isaParser = IsaParser(stream, false)
      val node = isaParser.parse()

      println("Tree:")
      visualizeNodes(node)
    }
  }