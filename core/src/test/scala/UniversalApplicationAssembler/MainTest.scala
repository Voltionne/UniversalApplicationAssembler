package UniversalApplicationAssembler

import scala.util.Using

class MainTest extends munit.FunSuite:

  test("First pass test") {

    Using.resource(getClass.getResourceAsStream("/testIsa.yaml")) {stream =>
      val isaParser = IsaParser(stream, false)
      val node = isaParser.parse()
    }
  }