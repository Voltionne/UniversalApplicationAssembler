package UniversalApplicationCompiler

import org.yaml.snakeyaml.Yaml
import java.nio.file.{Files, Paths}
import UniversalApplicationCompiler.helpers.Conversions

/**
 * Interprets an ISA based on a YAML file.
 * @param yamlConfigPath the path of the YAML file
 * @param autoParse if true, the parsing process starts automatically. If false, IsaParser.parse() must be called.
 */
class IsaParser(val yamlConfigPath: String, var autoParse: Boolean = true):

  //loads the YAML data and converts it to Scala datatypes
  val yamlData: Any =
    val yaml = new Yaml()
    val raw = yaml.load(Files.newInputStream(Paths.get(yamlConfigPath)))
    Conversions.convertFromJava(raw)

  //Auto-parse during construction if autoParse is enabled.
  if autoParse then parse()

  def parse(): Unit =

    yamlData match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
        val map = m.asInstanceOf[Map[String, Any]]
        parseRecursively(map) //start the recursive parsing
      case other =>
        throw new IllegalArgumentException("Expected Map[String, Any]")

  //Top-level parsers
  private def parseRecursively(currentLevel: Map[String, Any]): Unit =

    val sublevels = parseLevel(currentLevel)

    for sublevel <- sublevels do
      parseRecursively(sublevel)

  private def parseLevel(currentLevel: Map[String, Any]): Array[Map[String, Any]] = ???

  //parse structures
  private def parseInstruction(): Unit = ???
  private def parseDefinitions(): Unit = ???