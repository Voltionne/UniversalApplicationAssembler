package UniversalApplicationAssembler.api.parsing.isa

import UniversalApplicationAssembler.internal.datatypes.{BitRange, PartialAssignment, SymbolMap}
import UniversalApplicationAssembler.internal.parsing.isa.InstructionTemplate
import UniversalApplicationAssembler.internal.parsing.yaml.YamlReader.{constructToScala, getNodeLocation, getStringFromInputStream, getStringFromPath, nodeifyYamlFile}
import UniversalApplicationAssembler.internal.parsing.yaml.translation.{TranslationLeaf, TranslationNode}
import org.snakeyaml.engine.v2.nodes.{MappingNode, ScalarNode, SequenceNode}

import java.io.InputStream
import java.nio.file.Path


object IsaParser:

  /**
   * Parses the YAML configuration as a string
   * @param yamlConfig The YAML configuration string
   * @return A representation of the instructions in the ISA.
   */
  def parse(yamlConfig: String): InstructionMapping =

    val yamlTopNode: MappingNode =
      nodeifyYamlFile(yamlConfig) match
        case mappingNode: MappingNode => mappingNode
        case other => throw new IllegalArgumentException(s"Expected top node to be a MappingNode, not ${other.getNodeType}. ${getNodeLocation(other)}")

    val currentTranslationContext = TranslationNode(-1) //create the top translation node

    parseFirstPass(yamlTopNode, currentTranslationContext)
    parseSecondPass(yamlTopNode, currentTranslationContext)
    val instructions = parseThirdPass(yamlTopNode, currentTranslationContext, List.empty)

    InstructionMapping(instructions)

  /**
   * Parses the YAML as file path.
   *
   * @param yamlConfigPath The path of the configuration YAML file
   * @return A representation of the instructions in the ISA.
   */
  def parse(yamlConfigPath: Path): InstructionMapping =
    parse(getStringFromPath(yamlConfigPath))

  /**
   * Parses the YAML as an input stream.
   *
   * @param yamlConfigInputStream The input stream of the YAML
   * @return A representation of the instructions in the ISA.
   */
  def parse(yamlConfigInputStream: InputStream): InstructionMapping =
    parse(getStringFromInputStream(yamlConfigInputStream))

  /**
   * Parses the YAML configuration as a string and also returns the TranslationNode top node for debugging.
   *
   * @param yamlConfig The YAML configuration string
   * @return A tuple with a representation of the instructions in the ISA (InstructionMapping) and the top node of the result of building a node tree of variables (TranslationNode)
   */
  def debugParse(yamlConfig: String): (InstructionMapping, TranslationNode) =

    val yamlTopNode: MappingNode =
      nodeifyYamlFile(yamlConfig) match
        case mappingNode: MappingNode => mappingNode
        case other => throw new IllegalArgumentException(s"Expected top node to be a MappingNode, not ${other.getNodeType}. ${getNodeLocation(other)}")

    val currentTranslationContext = TranslationNode(-1) //create the top translation node

    parseFirstPass(yamlTopNode, currentTranslationContext)
    parseSecondPass(yamlTopNode, currentTranslationContext)
    val instructions = parseThirdPass(yamlTopNode, currentTranslationContext, List.empty)

    (InstructionMapping(instructions), currentTranslationContext)

  /**
   * Parses the YAML as file path and also returns the TranslationNode top node for debugging.
   *
   * @param yamlConfigPath The path of the configuration YAML file
   * @return A tuple with a representation of the instructions in the ISA (InstructionMapping) and the top node of the result of building a node tree of variables (TranslationNode)
   */
  def debugParse(yamlConfigPath: Path): (InstructionMapping, TranslationNode) =
    debugParse(getStringFromPath(yamlConfigPath))

  /**
   * Parses the YAML as an input stream and also returns the TranslationNode top node for debugging.
   *
   * @param yamlConfigInputStream The input stream of the YAML
   * @return A tuple with a representation of the instructions in the ISA (InstructionMapping) and the top node of the result of building a node tree of variables (TranslationNode)
   */
  def debugParse(yamlConfigInputStream: InputStream): (InstructionMapping, TranslationNode) =
    debugParse(getStringFromInputStream(yamlConfigInputStream))

  //-----------------------------------------
  // FIRST PASS -> Resolve declarations
  //-----------------------------------------

  private def parseFirstPass(currentNode: MappingNode, currentTranslationContext: TranslationNode): Unit =

    currentNode.getValue.forEach { nodeTuple => //Iterate through sublevels

      val stringKey =
        nodeTuple.getKeyNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected key to be a ScalarNode (a string), not ${other.getNodeType}. ${getNodeLocation(other)}")

      if stringKey == "bits" then
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => currentTranslationContext.bits = BigInt(scalarNode.getValue) //Sets the number of bits
          case other => throw new IllegalArgumentException(s"Expected bits to be a ScalarNode (a BigInt), not ${other.getNodeType}. ${getNodeLocation(other)}")

      else if stringKey == "instructions" then
        () //Skip instructions -> left for third pass

      else
        //Match based on value, given that all special keys have already been revised
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => //This is assignment OR declaration
            val value = constructToScala(scalarNode)

            value match
              case s: String => //Declaration

                //Important checks to be performed to verify this is a good definition:
                //1. Is not a full path (definitions are always local) -> check if no dots in the name
                require(!stringKey.contains('.'), s"Declarations cannot be full path, only local path!. ${getNodeLocation(scalarNode)}")

                //2. There doesn't exist a BitRange in the scope that has the same name
                require(!currentTranslationContext.getScope.contains(stringKey), s"Bit Range \"$stringKey\" is already defined!. ${getNodeLocation(scalarNode)}")

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
                  throw new IllegalArgumentException(s"Expected 1 or 2 bit position indications, not ${split.length}! ${getNodeLocation(scalarNode)}")

              case i: BigInt => () //Assignment -> Ignore (left to second pass)
              case other => throw new IllegalArgumentException(s"Found not recognized value for assignment or declaration. ${getNodeLocation(scalarNode)}")

          case mappingNode: MappingNode => //This is sublevel OR assignment OR symbol map

            if PartialAssignment.isPartialAssignment(mappingNode) then //Assignment
              () //Ignore (left to second pass)
            else if SymbolMap.isSymbolMap(mappingNode) then //Translation Table (i.e. declaration)

              //Check that it isn't defined already
              require(!currentTranslationContext.getScope.contains(stringKey), s"Translation Table \"$stringKey\" is already defined!. ${getNodeLocation(mappingNode)}")

              //CREATE NEW TRANSLATION TABLE
              val translationTable = constructToScala(mappingNode).asInstanceOf[Map[String, BigInt]] //This shouldn't fail because it is a translation table. Though, it is not very secure. May remake in the future
              currentTranslationContext.changes(stringKey) = TranslationLeaf(
                translationTable
              )

            else //Sublevel

              //New sublevel -> explore but create first a new translation context
              val newTranslationContext = TranslationNode(currentTranslationContext.bits) //Propagate bits
              currentTranslationContext.addChild(newTranslationContext, stringKey)

              parseFirstPass(mappingNode, newTranslationContext)

          case other => throw new IllegalArgumentException(s"Expected all keys to lead to tables (except \"instructions\" key). ${getNodeLocation(other)}")
    }

  //-----------------------------------------
  // SECOND PASS -> Resolve assignments
  //-----------------------------------------
    
  private def parseSecondPass(currentNode: MappingNode, currentTranslationContext: TranslationNode): Unit =

    currentNode.getValue.forEach { nodeTuple =>

      val stringKey =
        nodeTuple.getKeyNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected key to be a ScalarNode (a string), not ${other.getNodeType}. ${getNodeLocation(other)}")

      if stringKey == "bits" || stringKey == "instructions" then
        () //Skip -> "bits" handled in 1st pass. "instructions handled in 3rd pass
      else
        //Match based on value, given that all special keys have already been revised
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => //This is assignment OR declaration
            val value = constructToScala(scalarNode)

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
                    case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${getNodeLocation(scalarNode)}")

                else if currentTranslationContext.getScope.contains(stringKey) then //2nd case

                  val bitRange = currentTranslationContext.getScope(stringKey).leaf match
                    case bitRange: BitRange => bitRange
                    case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${getNodeLocation(scalarNode)}")

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
                    case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${getNodeLocation(scalarNode)}")

                  //Modify the value (as a copy)
                  val copyBitRange = bitRange.deepCopy()
                  copyBitRange.setFullValue(i)

                  //Add to current changes, with the modified value
                  currentTranslationContext.changes(stringKey) = TranslationLeaf(
                    copyBitRange
                  )

                else
                  throw new IllegalArgumentException(s"Making reference to a non-existent variable \"$stringKey\". ${getNodeLocation(scalarNode)}")

              case other => throw new IllegalArgumentException(s"Found not recognized value for assignment or declaration. ${getNodeLocation(scalarNode)}")

          case mappingNode: MappingNode => //This is sublevel OR assignment OR translation table

            if PartialAssignment.isPartialAssignment(mappingNode) then //Assignment
              val setMap = constructToScala(mappingNode).asInstanceOf[Map[String, Any]] //SHOULD 100% WORK (Not the most idiomatic, nevertheless). May change in the future


              //Three options can happen here:
              //1. Making reference to a BitRange defined in this TranslationNode -> simply change its value
              //2. Making reference to a BitRange not defined in this TranslationNode BUT in scope -> add it to changes with the modified value
              //3. A full path -> add the full path to changes with the modified values

              if currentTranslationContext.changes.contains(stringKey) then //1st case (also can match in 2nd and 3rd case if somehow a re-assignment, this is expected)

                currentTranslationContext.changes(stringKey).leaf match
                  case bitRange: BitRange =>
                    bitRange.setPartialValue(setMap) //Modify directly
                  case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${getNodeLocation(mappingNode)}")

              else if currentTranslationContext.getScope.contains(stringKey) then //2nd case

                val bitRange = currentTranslationContext.getScope(stringKey).leaf match
                  case bitRange: BitRange => bitRange
                  case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${getNodeLocation(mappingNode)}")

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
                  case m: Map[?, ?] => throw new IllegalArgumentException(s"Cannot assign a value to a Translation Table! ${getNodeLocation(mappingNode)}")

                //Modify the value (as a copy)
                val copyBitRange = bitRange.deepCopy()
                copyBitRange.setPartialValue(setMap)

                //Add to current changes, with the modified value
                currentTranslationContext.changes(stringKey) = TranslationLeaf(
                  copyBitRange
                )

              else
                throw new IllegalArgumentException(s"Making reference to a non-existent variable \"$stringKey\". ${getNodeLocation(mappingNode)}")

            else if SymbolMap.isSymbolMap(mappingNode) then () //Declaration -> Skip, handled in 1st pass
            else //Sublevel

              if currentTranslationContext.children.contains(stringKey) then
                parseSecondPass(mappingNode, currentTranslationContext.children(stringKey))
              else
                throw new NoSuchElementException(s"Didn't found TranslationContext children with key \"$stringKey\". ${getNodeLocation(mappingNode)}")

          case other => throw new IllegalArgumentException(s"Expected all keys to lead to tables (except \"instructions\" key). ${getNodeLocation(other)}")
    }

  //-----------------------------------------
  // THIRD PASS -> Resolve instructions
  //-----------------------------------------

  private def parseThirdPass(currentNode: MappingNode, currentTranslationContext: TranslationNode, currentInstructions: List[InstructionTemplate]): List[InstructionTemplate] =
    
    var instructions: List[InstructionTemplate] = currentInstructions

    currentNode.getValue.forEach { nodeTuple =>

      val stringKey =
        nodeTuple.getKeyNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected key to be a ScalarNode (a string), not ${other.getNodeType}. ${getNodeLocation(other)}")

      if stringKey == "bits" then
        () //Skip -> Already handled during 1st pass
      else if stringKey == "instructions" then

        val sequenceNode = nodeTuple.getValueNode match
          case sequenceNode: SequenceNode => sequenceNode
          case other => throw new IllegalArgumentException(s"Expected \"instructions\" to be a SequenceNode, not ${other.getNodeType}! ${getNodeLocation(other)}")

        sequenceNode.getValue.forEach {
          case mappingNode: MappingNode => instructions = instructions :+ InstructionTemplate(mappingNode, currentTranslationContext)
          case other => throw new IllegalArgumentException(s"Expected instructions to be a MappingNode, not ${other.getNodeType}! ${getNodeLocation(other)}")
        }

      else

        //Match based on value, given that all special keys have already been revised. Only done to find sublevels now
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => //This is assignment OR declaration
            () //Skip -> Already handled in 1st and 2nd pass
          case mappingNode: MappingNode => //This is sublevel OR assignment OR translation table

            if PartialAssignment.isPartialAssignment(mappingNode) then () //Assignment -> Already handled in 2nd pass
            else if SymbolMap.isSymbolMap(mappingNode) then () //Declaration -> Skip, handled in 1st pass
            else //Sublevel

              if currentTranslationContext.children.contains(stringKey) then
                instructions = parseThirdPass(mappingNode, currentTranslationContext.children(stringKey), instructions)
              else
                throw new NoSuchElementException(s"Didn't found TranslationContext children with key \"$stringKey\". ${getNodeLocation(mappingNode)}")
    }
    
    instructions
