package UniversalApplicationAssembler

import UniversalApplicationAssembler.datatypes.{BitRange, isSetMap}
import org.yaml.snakeyaml.Yaml

import java.io.InputStream
import UniversalApplicationAssembler.helpers.Conversions
import UniversalApplicationAssembler.parsing.{InstructionTemplate, Leaf, Node}

/**
 * Interprets an ISA based on a YAML file.
 *
 * @param yamlConfigInputStream the input stream of the YAML config file.
 * @param autoParse if true, the parsing process starts automatically. If false, IsaParser.parse() must be called.
 */
class IsaParser(val yamlConfigInputStream: InputStream, var autoParse: Boolean = true):

  //loads the YAML data and converts it to Scala datatypes
  private val yamlData: Any =
    val yaml = new Yaml()
    val raw: Any = yaml.load(yamlConfigInputStream)
    Conversions.convertFromJava(raw)

  var instructions = Array[InstructionTemplate]()

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

    //Managing assignments
    for (key, value) <- currentLevel if key != "instructions" && key != "bits" && !sublevels.contains(key) do

      value match

        case s: String => () //This is a BitRange definition. Skipped
        case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.forall(_.isInstanceOf[BigInt]) => () //This is a translation table definition. Skipped

        case i: BigInt =>

          //Get the reference from the current scope
          currentScope.get(key) match
            case Some(Leaf(br: BitRange)) =>
              br.setFullValue(i)
            case Some(other) => throw new IllegalArgumentException("Not expected format")
            case None => //If reference not found, search from the top
              currentTranslationContext.getTop.searchLeaf(key) match
                case Leaf(br: BitRange) =>
                  br.setFullValue(i)
                case other => throw new IllegalArgumentException("Trying to set a value to a translation table!")

        case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
          val setMap = m.asInstanceOf[Map[String, Any]]

          //Get the reference from the current scope
          currentScope.get(key) match
            case Some(Leaf(br: BitRange)) =>
              br.setPartialValue(setMap)
            case Some(other) => throw new IllegalArgumentException("Not expected format")
            case None => //If reference not found, search from the top
              currentTranslationContext.getTop.searchLeaf(key) match
                case Leaf(br: BitRange) =>
                  br.setPartialValue(setMap)
                case other => throw new IllegalArgumentException("Trying to set a value to a translation table!")

        case other => () //Do nothing, theoretically handled earlier

    sublevels

  //-----------------------------------------
  // SECOND PASS -> Resolves references
  //-----------------------------------------
        
  private def parseSecondPass(currentTranslationContext: Node): Unit =

    yamlData match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
        val map = m.asInstanceOf[Map[String, Any]]
        parseRecursivelySecondPass(map, currentTranslationContext) //start the recursive parsing
      case other =>
        throw new IllegalArgumentException("Expected Map[String, Any]")

  private def parseRecursivelySecondPass(currentLevel: Map[String, Any], currentTranslationContext: Node): Unit =

    val sublevels = parseLevelSecondPass(currentLevel, currentTranslationContext)

    for sublevel <- sublevels do

      currentLevel(sublevel) match
        case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>

          //Go to the sublevel
          //VERY IMPORTANT NOTE: "instructions" and "bits" WILL FAIL, THEY ARE NOT MEANT TO BE PASSED HERE.
          val newTranslationContext = currentTranslationContext.children(sublevel)

          newTranslationContext match
            case n: Node => parseRecursivelySecondPass(m.asInstanceOf[Map[String, Any]], n)
            case other => throw new IllegalArgumentException("Tried to enter in a Leaf sublevel!")

        case other => throw new IllegalArgumentException("Expected Map[String, Any]")

  private def parseLevelSecondPass(currentLevel: Map[String, Any], currentTranslationContext: Node): Array[String] =

    var sublevels = Array[String]()

    for (key, value) <- currentLevel if key == "bits" do //Only skip "bits" level, as that is already set

      if key == "instructions" then

        value match
          case l: List[?] if l.forall {
            case m: Map[?, ?] => m.keys.forall(_.isInstanceOf[String])
            case _ => false
          } =>
            val instructions = l.asInstanceOf[List[Map[String, Any]]]

            for instruction <- instructions do
              parseInstruction(instruction, currentTranslationContext)

          case other => throw new IllegalArgumentException("Expected instructions to be as a List[Map[String, Any]]")

      else //As assignments and definitions are handled already -> do nothing with other things.

        //Find sublevels
        value match
          //check if at least one that is not BigInt, for not confusing with translation table
          case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.exists(!_.isInstanceOf[BigInt]) => //Either a setMap or a sublevel.
            val map = m.asInstanceOf[Map[String, Any]]

            if !isSetMap(map) then sublevels = sublevels :+ key

    sublevels

  //parse structures
  private def parseInstruction(instructionLevel: Map[String, Any], currentTranslationContext: Node): Unit =

    val instructionTemplate = InstructionTemplate(
      currentTranslationContext.bits.toInt,
      currentTranslationContext.getScope.collect {
        case (k, Leaf(bitRange: BitRange)) => k -> bitRange
      },
      Conversions.convertParametersMap {
        instructionLevel.get("parameters") match
          case Some(m: Map[?, ?]) if m.keys.forall(_.isInstanceOf[String]) =>
            val map = m.asInstanceOf[Map[String, Any]]
            map
          case Some(_) => throw new IllegalArgumentException("Incorrect format for parameters")
          case None => Map[String, Any]() //NO PARAMETERS
      },
      currentTranslationContext
    )

    instructions = instructions :+ instructionTemplate