package UniversalApplicationAssembler.parsing.isa

import UniversalApplicationAssembler.datatypes.{BitRange, Utils}
import UniversalApplicationAssembler.parsing.yaml.YamlReader
import UniversalApplicationAssembler.parsing.yaml.translation.{TranslationLeaf, TranslationNode}
import UniversalApplicationAssembler.parsing.isa.helpers.Conversions.toInstructionTemplate
import org.snakeyaml.engine.v2.nodes.{MappingNode, ScalarNode, SequenceNode}

import java.io.InputStream

class IsaParser(val yamlConfigInputStream: InputStream):

  private val yamlTopNode: MappingNode =
    YamlReader.readYamlFile(yamlConfigInputStream) match
      case mappingNode: MappingNode => mappingNode
      case other => throw new IllegalArgumentException(s"Expected top node to be a MappingNode, not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

  var instructions: List[InstructionTemplate] = List.empty

  def parse(): TranslationNode =

    val currentTranslationContext = TranslationNode(-1) //create the top translation node

    parseFirstPass(yamlTopNode, currentTranslationContext)
    parseSecondPass(yamlTopNode, currentTranslationContext)
    parseThirdPass(yamlTopNode, currentTranslationContext)

    currentTranslationContext

  //-----------------------------------------
  // FIRST PASS -> Resolve declarations
  //-----------------------------------------

  private def parseFirstPass(currentNode: MappingNode, currentTranslationContext: TranslationNode): Unit =

    currentNode.getValue.forEach { nodeTuple => //Iterate through sublevels

      val stringKey =
        nodeTuple.getKeyNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected key to be a ScalarNode (a string), not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

      if stringKey == "bits" then
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => currentTranslationContext.bits = BigInt(scalarNode.getValue) //Sets the number of bits
          case other => throw new IllegalArgumentException(s"Expected bits to be a ScalarNode (a BigInt), not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

      else if stringKey == "instructions" then
        () //Skip instructions -> left for second pass

      else
        //Match based on value, given that all special keys have already been revised
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => //This is assignment OR declaration
            val value = YamlReader.constructToScala(scalarNode)

            value match
              case s: String => //Declaration

                //Important checks to be performed to verify this is a good definition:
                //1. Is not a full path (definitions are always local) -> check if no dots in the name
                require(!stringKey.contains('.'))

                //There exists a BitRange in the scope that has the same name
                if currentTranslationContext.getScope.contains(stringKey) then
                  throw new IllegalArgumentException(s"Bit Range \"$stringKey\" is already defined!. ${YamlReader.getNodeLocation(scalarNode)}")
                else
                  //CREATE NEW BITRANGE
                  val split = s.split(':')

                  if split.length == 1 then
                    currentTranslationContext.changes(stringKey) = TranslationLeaf(
                      BitRange(split(0).toInt)
                    )
                  else if split.length == 2 then
                    currentTranslationContext.changes(stringKey) = TranslationLeaf(
                      BitRange(split(0).toInt, split(1).toInt)
                    )
                  else
                    throw new IllegalArgumentException(s"Expected 1 or 2 bit position indications, not ${split.length}! ${YamlReader.getNodeLocation(scalarNode)}")

              case i: BigInt => () //Assignment -> Ignore (left to second pass)
              case other => throw new IllegalArgumentException(s"Found not recognized value for assignment or declaration. ${YamlReader.getNodeLocation(scalarNode)}")

          case mappingNode: MappingNode => //This is sublevel OR assignment OR translation table

            if Helper.isSetMap(mappingNode) then //Assignment
              () //Ignore (left to second pass)
            else if Helper.isTranslationTable(mappingNode) then //Translation Table (i.e. declaration)

              if currentTranslationContext.getScope.contains(stringKey) then
                throw new IllegalArgumentException(s"Translation Table \"$stringKey\" is already defined!. ${YamlReader.getNodeLocation(mappingNode)}")
              else
                //CREATE NEW TRANSLATION TABLE
                val translationTable = YamlReader.constructToScala(mappingNode).asInstanceOf[Map[String, BigInt]] //This shouldn't fail because it is a translation table. Though, it is not very secure. May remake in the future
                currentTranslationContext.changes(stringKey) = TranslationLeaf(
                  translationTable
                )

            else //Sublevel

              //New sublevel -> explore but create first a new translation context
              val newTranslationContext = TranslationNode(currentTranslationContext.bits) //Propagate bits
              currentTranslationContext.addChild(newTranslationContext, stringKey)

              parseFirstPass(mappingNode, newTranslationContext)

          case other => throw new IllegalArgumentException(s"Expected all keys to lead to tables (except \"instructions\" key). ${YamlReader.getNodeLocation(other)}")
    }

  //-----------------------------------------
  // SECOND PASS -> Resolve assignments
  //-----------------------------------------
    
  private def parseSecondPass(currentNode: MappingNode, currentTranslationContext: TranslationNode): Unit =

    currentNode.getValue.forEach { nodeTuple =>

      val stringKey =
        nodeTuple.getKeyNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected key to be a ScalarNode (a string), not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

      if stringKey == "bits" || stringKey == "instructions" then
        () //Skip -> "bits" handled in 1st pass. "instructions handled in 3rd pass
      else
        //Match based on value, given that all special keys have already been revised
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => //This is assignment OR declaration
            val value = YamlReader.constructToScala(scalarNode)

            value match
              case s: String => () //Declaration -> Skip, handled in 1st pass
              case i: BigInt => //Assignment -> handle right now

                //Three options can happen here:
                //1. Making reference to a BitRange defined in this TranslationNode -> simply change its value
                //2. Making reference to a BitRange not defined in this TranslationNode BUT in scope -> add it to changes with the modified value
                //3. A full path -> add the full path to changes with the modified values

                if currentTranslationContext.changes.contains(stringKey) then //1st case (also can match in 2nd and 3rd case if somehow a re-assignment, this is expected)

                  currentTranslationContext.changes(stringKey).leaf match
                    case bitRange: BitRange =>
                      bitRange.setFullValue(i) //Modify directly
                    case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${YamlReader.getNodeLocation(scalarNode)}")

                else if currentTranslationContext.getScope.contains(stringKey) then //2nd case

                  val bitRange = currentTranslationContext.getScope(stringKey).leaf match
                    case bitRange: BitRange => bitRange
                    case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${YamlReader.getNodeLocation(scalarNode)}")

                  //Modify the value (as a copy)
                  val copyBitRange = bitRange.deepCopy()
                  copyBitRange.setFullValue(i)

                  //Add to current changes, with the modified value
                  currentTranslationContext.changes(stringKey) = TranslationLeaf(
                    copyBitRange
                  )

                else if currentTranslationContext.getTop.searchTranslationLeaf(stringKey).isDefined then //3rd case
                  val leaf = currentTranslationContext.getTop.searchTranslationLeaf(stringKey).get //Will 100% work, not the most idiomatic nevertheless

                  val bitRange = leaf.leaf match
                    case bitRange: BitRange => bitRange
                    case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${YamlReader.getNodeLocation(scalarNode)}")

                  //Modify the value (as a copy)
                  val copyBitRange = bitRange.deepCopy()
                  copyBitRange.setFullValue(i)

                  //Add to current changes, with the modified value
                  currentTranslationContext.changes(stringKey) = TranslationLeaf(
                    copyBitRange
                  )

                else
                  throw new IllegalArgumentException(s"Making reference to a non-existent variable \"$stringKey\". ${YamlReader.getNodeLocation(scalarNode)}")

              case other => throw new IllegalArgumentException(s"Found not recognized value for assignment or declaration. ${YamlReader.getNodeLocation(scalarNode)}")

          case mappingNode: MappingNode => //This is sublevel OR assignment OR translation table

            if Helper.isSetMap(mappingNode) then //Assignment
              val setMap = YamlReader.constructToScala(mappingNode).asInstanceOf[Map[String, Any]] //SHOULD 100% WORK (Not the most idiomatic, nevertheless). May change in the future


              //Three options can happen here:
              //1. Making reference to a BitRange defined in this TranslationNode -> simply change its value
              //2. Making reference to a BitRange not defined in this TranslationNode BUT in scope -> add it to changes with the modified value
              //3. A full path -> add the full path to changes with the modified values

              if currentTranslationContext.changes.contains(stringKey) then //1st case (also can match in 2nd and 3rd case if somehow a re-assignment, this is expected)

                currentTranslationContext.changes(stringKey).leaf match
                  case bitRange: BitRange =>
                    bitRange.setPartialValue(setMap) //Modify directly
                  case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${YamlReader.getNodeLocation(mappingNode)}")

              else if currentTranslationContext.getScope.contains(stringKey) then //2nd case

                val bitRange = currentTranslationContext.getScope(stringKey).leaf match
                  case bitRange: BitRange => bitRange
                  case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${YamlReader.getNodeLocation(mappingNode)}")

                //Modify the value (as a copy)
                val copyBitRange = bitRange.deepCopy()
                copyBitRange.setPartialValue(setMap)

                //Add to current changes, with the modified value
                currentTranslationContext.changes(stringKey) = TranslationLeaf(
                  copyBitRange
                )

              else if currentTranslationContext.getTop.searchTranslationLeaf(stringKey).isDefined then //3rd case
                val leaf = currentTranslationContext.getTop.searchTranslationLeaf(stringKey).get //Will 100% work, not the most idiomatic nevertheless

                val bitRange = leaf.leaf match
                  case bitRange: BitRange => bitRange
                  case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${YamlReader.getNodeLocation(mappingNode)}")

                //Modify the value (as a copy)
                val copyBitRange = bitRange.deepCopy()
                copyBitRange.setPartialValue(setMap)

                //Add to current changes, with the modified value
                currentTranslationContext.changes(stringKey) = TranslationLeaf(
                  copyBitRange
                )

              else
                throw new IllegalArgumentException(s"Making reference to a non-existent variable \"$stringKey\". ${YamlReader.getNodeLocation(mappingNode)}")

            else if Helper.isTranslationTable(mappingNode) then () //Declaration -> Skip, handled in 1st pass
            else //Sublevel

              if currentTranslationContext.children.contains(stringKey) then
                parseSecondPass(mappingNode, currentTranslationContext.children(stringKey))
              else
                throw new NoSuchElementException(s"Didn't found TranslationContext children with key \"$stringKey\". ${YamlReader.getNodeLocation(mappingNode)}")

          case other => throw new IllegalArgumentException(s"Expected all keys to lead to tables (except \"instructions\" key). ${YamlReader.getNodeLocation(other)}")
    }

  //-----------------------------------------
  // THIRD PASS -> Resolve instructions
  //-----------------------------------------

  private def parseThirdPass(currentNode: MappingNode, currentTranslationContext: TranslationNode): Unit =

    currentNode.getValue.forEach { nodeTuple =>

      val stringKey =
        nodeTuple.getKeyNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected key to be a ScalarNode (a string), not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

      if stringKey == "bits" then
        () //Skip -> Already handled during 1st pass
      else if stringKey == "instructions" then

        val sequenceNode = nodeTuple.getValueNode match
          case sequenceNode: SequenceNode => sequenceNode
          case other => throw new IllegalArgumentException(s"Expected \"instructions\" to be a SequenceNode, not ${other.getNodeType}! ${YamlReader.getNodeLocation(other)}")

        sequenceNode.getValue.forEach {
          case mappingNode: MappingNode => instructions = instructions :+ toInstructionTemplate(mappingNode, currentTranslationContext)
          case other => throw new IllegalArgumentException(s"Expected instructions to be a MappingNode, not ${other.getNodeType}! ${YamlReader.getNodeLocation(other)}")
        }

      else

        //Match based on value, given that all special keys have already been revised. Only done to find sublevels now
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => //This is assignment OR declaration
            () //Skip -> Already handled in 1st and 2nd pass
          case mappingNode: MappingNode => //This is sublevel OR assignment OR translation table

            if Helper.isSetMap(mappingNode) then () //Assignment -> Already handled in 2nd pass
            else if Helper.isTranslationTable(mappingNode) then () //Declaration -> Skip, handled in 1st pass
            else //Sublevel

              if currentTranslationContext.children.contains(stringKey) then
                parseThirdPass(mappingNode, currentTranslationContext.children(stringKey))
              else
                throw new NoSuchElementException(s"Didn't found TranslationContext children with key \"$stringKey\". ${YamlReader.getNodeLocation(mappingNode)}")
    }

/**
 * Small helper object that includes some snippets of code for checking fast
 */
object Helper:

  /**
   * Checks if a MappingNode is a set map (or assignment map)
   * @param mappingNode The mapping node to check
   * @return True or false depending on if it is or not a set map
   */
  def isSetMap(mappingNode: MappingNode): Boolean =

    val nodeAsScala = YamlReader.constructToScala(mappingNode)

    nodeAsScala match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
        val map = m.asInstanceOf[Map[String, Any]]
        Utils.isSetMap(map)
      case other => false

  /**
   * Checks if a MappingNode is a translation table
   * @param mappingNode The mapping node to check
   * @return True or false depending on if ti is or not a translation table
   */
  def isTranslationTable(mappingNode: MappingNode): Boolean =

    val nodeAsScala = YamlReader.constructToScala(mappingNode)

    nodeAsScala match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.forall(_.isInstanceOf[BigInt]) => true
      case other => false
