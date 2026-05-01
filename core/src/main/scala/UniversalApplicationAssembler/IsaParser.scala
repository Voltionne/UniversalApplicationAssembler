package UniversalApplicationAssembler

import UniversalApplicationAssembler.datatypes.BitRange
import org.yaml.snakeyaml.Yaml

import java.io.InputStream
import UniversalApplicationAssembler.helpers.Conversions
import UniversalApplicationAssembler.parsing.{Leaf, Node}

/**
 * Interprets an ISA based on a YAML file.
 *
 * @param yamlConfigInputStream the input stream of the YAML config file.
 * @param autoParse if true, the parsing process starts automatically. If false, IsaParser.parse() must be called.
 */
class IsaParser(val yamlConfigInputStream: InputStream, var autoParse: Boolean = true):

  //loads the YAML data and converts it to Scala datatypes
  val yamlData: Any =
    val yaml = new Yaml()
    val raw: Any = yaml.load(yamlConfigInputStream)
    Conversions.convertFromJava(raw)
  var bits: Int = -1

  //Auto-parse during construction if autoParse is enabled.
  if autoParse then
    parse()
    
  def parse(): Node =

    //The top TranslationContext
    val currentTranslationContext = Node()
    var bits = -1
    
    //execute the two passes
    parseFirstPass(currentTranslationContext)
    //parseSecondPass(currentTranslationContext)
    currentTranslationContext
    
  //-----------------------------------------
  // FIRST PASS
  //-----------------------------------------  

  private def parseFirstPass(currentTranslationContext: Node): Unit =

    yamlData match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
        val map = m.asInstanceOf[Map[String, Any]]
        parseRecursivelyFirstPass(map, currentTranslationContext) //start the recursive parsing
      case other =>
        throw new IllegalArgumentException("Expected Map[String, Any]")
        
  //Top-level parsers
  private def parseRecursivelyFirstPass(currentLevel: Map[String, Any], currentTranslationContext: Node): Unit =

    val sublevels = parseLevelFirstPass(currentLevel, currentTranslationContext)

    for sublevel <- sublevels do
      
      currentLevel(sublevel) match
        case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
          
          val newTranslationContext = Node()
          currentTranslationContext.addChild(newTranslationContext, sublevel)
      
          parseRecursivelyFirstPass(m.asInstanceOf[Map[String, Any]], newTranslationContext)

        case other => throw new IllegalArgumentException("Expected Map[String, Any]")

  private def parseLevelFirstPass(currentLevel: Map[String, Any], currentTranslationContext: Node): Array[String] =

    val sublevels = Array[String]()

    val currentScope = currentTranslationContext.getScope

    for (key, value) <- currentLevel if key != "instructions" && key != "bits" do //Skip "instructions" key as that will be checked in the second pass and declarations inside it are not allowed (in v0.2.0)!

      //Will only match for definitions, NO REFERENCES.
      value match
        case s: String => //This is a BitRange definition

          //Add the new definition
          currentTranslationContext.addChild(
            {
             val split = s.split(':')
             Leaf(BitRange(split(0).toInt, split(1).toInt))
            },
            key
          )

        case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.forall(_.isInstanceOf[BigInt]) => //This is a translation table definition
          val map = m.asInstanceOf[Map[String, BigInt]]

          //Add the new definition
          currentTranslationContext.addChild(
            Leaf(map),
            key
          )

        //Assume that it is a sublevel -> add as possible sublevels.
        case other => sublevels ++ key

    sublevels

  //-----------------------------------------
  // SECOND PASS
  //-----------------------------------------
        
  private def parseSecondPass(currentTranslationContext: Node): Unit = ???

  private def parseRecursivelySecondPass(currentLevel: Map[String, Any], currentTranslationContext: Node): Unit = ???

  private def parseLevelSecondPass(currentLevel: Map[String, Any], currentTranslationContext: Node): Unit = ???

  //parse structures
  private def parseInstruction(): Unit = ???
  private def parseDefinitions(): Unit = ???