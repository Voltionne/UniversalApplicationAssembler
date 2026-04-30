package UniversalApplicationCompiler

import org.yaml.snakeyaml.Yaml

import java.nio.file.{Files, Paths}
import UniversalApplicationCompiler.helpers.{BitRange, Conversions, TranslationContext, Node, Leaf}

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
  var bits: Int = -1

  //Auto-parse during construction if autoParse is enabled.
  if autoParse then
    parse()
    
  def parse(): Unit =
    parseFirstPass()
    parseSecondPass()
    
  //-----------------------------------------
  // FIRST PASS
  //-----------------------------------------  

  private def parseFirstPass(): Unit =

    //The top TranslationContext
    val currentTranslationContext = Node() //Literally none, this is the top level.

    yamlData match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
        val map = m.asInstanceOf[Map[String, Any]]
        parseRecursively(map, currentTranslationContext) //start the recursive parsing
      case other =>
        throw new IllegalArgumentException("Expected Map[String, Any]")
        
  //Top-level parsers
  private def parseRecursively(currentLevel: Map[String, Any], currentTranslationContext: Node): Unit =

    val sublevels = parseLevel(currentLevel, currentTranslationContext)

    for sublevel <- sublevels do
      
      currentLevel(sublevel) match
        case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
          
          val newTranslationContext = Node()
          currentTranslationContext.addChild(newTranslationContext, sublevel)
      
          parseRecursively(m.asInstanceOf[Map[String, Any]], newTranslationContext)

        case other => throw new IllegalArgumentException("Expected Map[String, Any]")

  private def parseLevel(currentLevel: Map[String, Any], currentTranslationContext: Node): Array[String] = ???

  //-----------------------------------------
  // SECOND PASS
  //-----------------------------------------
        
  private def parseSecondPass(): Unit = ???

  //parse structures
  private def parseInstruction(): Unit = ???
  private def parseDefinitions(): Unit = ???