package UniversalApplicationCompiler

import org.yaml.snakeyaml.Yaml
import java.nio.file.{Files, Paths}
import UniversalApplicationCompiler.helpers.Conversions

class IsaParser(val yamlConfigPath: String, var autoParse: Boolean = true):
  val yamlData: Any =
    val yaml = new Yaml()
    val raw = yaml.load(Files.newInputStream(Paths.get(yamlConfigPath)))
    Conversions.convertFromJava(raw)

  //Auto-parse during construction if autoParse is enabled.
  if autoParse then parse()

  def parse(): Unit = ???

  //Top-level parsers
  private def parseRecursively(): Unit = ???
  private def parseLevel(): Array[String] = ???

  //parse structures
  private def parseInstruction(): Unit = ???
  private def parseDefinitions(): Unit = ???