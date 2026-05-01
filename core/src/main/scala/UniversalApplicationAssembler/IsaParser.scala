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

  //Auto-parse during construction if autoParse is enabled.
  if autoParse then
    parse()

  def parse(): Node =

    val bits = -1

    //The top TranslationContext
    val currentTranslationContext = Node(bits)
    
    //execute the two passes
    parseFirstPass(currentTranslationContext)
    //parseSecondPass(currentTranslationContext)
    currentTranslationContext
    
  //-----------------------------------------
  // FIRST PASS -> Gets all definitions and assignments
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

          //Create new node to represent the sublevel
          val newTranslationContext = Node(currentTranslationContext.bits) //bits get propagated down initially
          currentTranslationContext.addChild(newTranslationContext, sublevel)
      
          parseRecursivelyFirstPass(m.asInstanceOf[Map[String, Any]], newTranslationContext)

        case other => throw new IllegalArgumentException("Expected Map[String, Any]")

  private def parseLevelFirstPass(currentLevel: Map[String, Any], currentTranslationContext: Node): Array[String] =

    var sublevels = Array[String]()

    for (key, value) <- currentLevel if key != "instructions" do //Skip "instructions" key as that will be checked in the second pass and declarations inside it are not allowed (in v0.2.0)!

      if key == "bits" then
        value match
          case i: BigInt => currentTranslationContext.bits = i
          case other => throw new IllegalArgumentException("Bits number is not an integer!")
      else
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

          case i: BigInt => () //This means it is an assignment, it skips it.
          case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) => //Either a setMap or a sublevel
            val map = m.asInstanceOf[Map[String, Any]]

            if !isSetMap(map) then sublevels = sublevels :+ key
            //if it is setMap, ignore until assignment

          case other => throw new IllegalArgumentException("Unrecognized construction")

    val currentScope = currentTranslationContext.getScope

    println(sublevels.mkString("Array(", ", ", ")"))

    //Managing assignments
    for (key, value) <- currentLevel if key != "instructions" && key != "bits" && !sublevels.contains(key) do

      value match

        case s: String => () //This is a BitRange definition. Skipped
        case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.forall(_.isInstanceOf[BigInt]) => () //This is a translation table definition. Skipped

        case i: BigInt =>

          currentScope(key) match
            case Leaf(l: BitRange) =>
              l.setFullValue(i)
            case other => throw new IllegalArgumentException("Trying to set a value to a translation table!")

        case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
          val setMap = m.asInstanceOf[Map[String, Any]]

          currentScope(key) match
            case Leaf(l: BitRange) =>
              l.setPartialValue(setMap)
            case other => throw new IllegalArgumentException("Trying to set a value to a translation table!")

        case other => () //Do nothing, theoretically handled earlier

    sublevels

  //-----------------------------------------
  // SECOND PASS -> Resolves references
  //-----------------------------------------
        
  private def parseSecondPass(currentTranslationContext: Node): Unit = ???

  private def parseRecursivelySecondPass(currentLevel: Map[String, Any], currentTranslationContext: Node): Unit = ???

  private def parseLevelSecondPass(currentLevel: Map[String, Any], currentTranslationContext: Node): Unit = ???

  //parse structures
  private def parseInstruction(): Unit = ???
  private def parseDefinitions(): Unit = ???