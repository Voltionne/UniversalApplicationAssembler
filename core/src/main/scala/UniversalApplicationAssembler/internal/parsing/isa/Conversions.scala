package UniversalApplicationAssembler.internal.parsing.isa

import UniversalApplicationAssembler.internal.parsing.isa.Helper
import UniversalApplicationAssembler.internal.datatypes.BitRange
import UniversalApplicationAssembler.internal.parsing.yaml.YamlReader
import UniversalApplicationAssembler.internal.parsing.yaml.translation.{TranslationLeaf, TranslationNode}
import org.snakeyaml.engine.v2.nodes.{MappingNode, ScalarNode}

object Conversions:

  def toParametersDefinition(mappingNode: MappingNode): ParametersDefinition =

    val mappingNodeScala = YamlReader.constructToScala(mappingNode)

    mappingNodeScala match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.forall(_.isInstanceOf[List[Any]]) =>
        val map = m.asInstanceOf[Map[String, List[Any]]]

        require(map.contains("values") && map.contains("mappings") && map.size == 2, s"Bad parameters MappingNode. ${YamlReader.getNodeLocation(mappingNode)}") //Check that everything is alright

        val datatypes = map("values") match
          case l: List[?] if l.forall(_.isInstanceOf[String]) => l.asInstanceOf[List[String]]
          case other => throw new IllegalArgumentException(s"Expected \"values\" to be a list of strings! ${YamlReader.getNodeLocation(mappingNode)}")

        val mappings: List[ParameterMapping] = map("mappings").map {
          case s: String => SingleParameterMapping(s)
          case l: List[?] if l.forall(_.isInstanceOf[String]) => MultipleParameterMapping(l.asInstanceOf[List[String]])
          case other => throw new IllegalArgumentException(s"Expected mappings to be either a string or list of strings, not $other!")
        }

        ParametersDefinition(datatypes, mappings)

      case other => throw new IllegalArgumentException(s"Expected ParametersDefinition to be a Map with string keys and lists as values! ${YamlReader.getNodeLocation(mappingNode)}")

  /**
   * Parses an instruction stored as a MappingNode
   *
   * @param mappingNode        The MappingNode representing the instruction
   * @param translationContext The translation context where the instruction is defined
   * @return A InstructionTemplate representing the instruction
   */
  def toInstructionTemplate(mappingNode: MappingNode, translationContext: TranslationNode): InstructionTemplate =

    //Perform first some interesting checks
    require(mappingNode.getValue.stream().anyMatch { nodeTuple =>
      val stringKey =
        nodeTuple.getKeyNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected key to be a ScalarNode (a string), not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

      stringKey == "name"
    })

    var name: String = null
    var parametersDefinition: ParametersDefinition = ParametersDefinition(List[String](), List[ParameterMapping]()) //Empty, no parameters
    var fields: Map[String, BitRange] = Map.empty

    mappingNode.getValue.forEach { nodeTuple =>

      val stringKey =
        nodeTuple.getKeyNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected key to be a ScalarNode (a string), not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

      if stringKey == "name" then
        name = nodeTuple.getValueNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected name of instruction to be a ScalarNode (a string), not ${other.getNodeType}! ${YamlReader.getNodeLocation(other)}")
      else if stringKey == "parameters" then
        parametersDefinition = nodeTuple.getValueNode match
          case mappingNode: MappingNode => toParametersDefinition(mappingNode)
          case other => throw new IllegalArgumentException(s"Expected parameters to be a MappingNode, not ${other.getNodeType}! ${YamlReader.getNodeLocation(other)}")

      else //Must be BitRanges assignments AT MOST -> They count towards InstructionTemplate fields
        nodeTuple.getValueNode match
          case mappingNode: MappingNode => //HAS TO BE partial assignment

            if Helper.isSetMap(mappingNode) then
              val setMap = YamlReader.constructToScala(mappingNode).asInstanceOf[Map[String, Any]]

              //Procedure as following
              //Step 1: Get the BitRange
              //Step 2: Create a new one with the updated value. DON'T UPDATE THE TRANSLATION CONTEXT (as this is per instruction)
              //Step 3: The new one is a field

              //Step 1: Can be either in scope or full path
              val translationLeaf: TranslationLeaf =
                if translationContext.getScope.contains(stringKey) then
                  translationContext.getScope(stringKey)
                else if translationContext.getTop.searchTranslationLeaf(stringKey).isDefined then
                  translationContext.getTop.searchTranslationLeaf(stringKey).get
                else
                  throw new IllegalArgumentException(s"Variable \"$stringKey\" is not defined! ${YamlReader.getNodeLocation(mappingNode)}")

              //Step 2 & 3
              translationLeaf.leaf match
                case bitRange: BitRange =>

                  //Step 2:
                  val newBitRange = bitRange.deepCopy()
                  newBitRange.setPartialValue(setMap)

                  //Step 3:
                  //Note: using stringKey as the name of the field, this in some cases can be the full path. Should be ok as searching uses also the full path.
                  fields = fields + (stringKey -> newBitRange)

                case other => throw new IllegalArgumentException(s"Can only assign values to BitRange, not to $other")

            else
              throw new IllegalArgumentException(s"Expected assignment to be a set map, not $mappingNode. ${YamlReader.getNodeLocation(mappingNode)}")

          case scalarNode: ScalarNode => //HAS TO BE full assignment
            val value = YamlReader.constructToScala(scalarNode)

            value match
              case i: BigInt =>

                //Procedure as following
                //Step 1: Get the BitRange
                //Step 2: Create a new one with the updated value. DON'T UPDATE THE TRANSLATION CONTEXT (as this is per instruction)
                //Step 3: The new one is a field

                //Step 1: Can be either in scope or full path
                val translationLeaf: TranslationLeaf =
                  if translationContext.getScope.contains(stringKey) then
                    translationContext.getScope(stringKey)
                  else if translationContext.getTop.searchTranslationLeaf(stringKey).isDefined then
                    translationContext.getTop.searchTranslationLeaf(stringKey).get
                  else
                    throw new IllegalArgumentException(s"Variable \"$stringKey\" is not defined! ${YamlReader.getNodeLocation(scalarNode)}")

                //Step 2 & 3
                translationLeaf.leaf match
                  case bitRange: BitRange =>

                    //Step 2:
                    val newBitRange = bitRange.deepCopy()
                    newBitRange.setFullValue(i)

                    //Step 3:
                    //Note: using stringKey as the name of the field, this in some cases can be the full path. Should be ok as searching uses also the full path.
                    fields = fields + (stringKey -> newBitRange)

                  case other => throw new IllegalArgumentException(s"Can only assign values to BitRange, not to $other")

              case other => throw new IllegalArgumentException(s"Expected assignment to be an integer, not $other! ${YamlReader.getNodeLocation(scalarNode)}")
    }

    //Check that mappings are, in fact, BitRanges and set them correctly as fields. This will override other set fields.
    //There may be a problem if you use in parameters and also in the instruction the same variable. Currently not fixed.
    parametersDefinition.mappings.foreach {
      case singleParameterMapping: SingleParameterMapping =>

        //Search reference
        val translationLeaf: TranslationLeaf =
          if translationContext.getScope.contains(singleParameterMapping.mappingLocation) then
            translationContext.getScope(singleParameterMapping.mappingLocation)
          else if translationContext.getTop.searchTranslationLeaf(singleParameterMapping.mappingLocation).isDefined then
            translationContext.getTop.searchTranslationLeaf(singleParameterMapping.mappingLocation).get
          else
            throw new IllegalArgumentException(s"Variable \"$singleParameterMapping.mappingLocation\" is not defined!")

        translationLeaf.leaf match
          case bitRange: BitRange =>

            //Add as field
            val newBitRange = bitRange.deepCopy()
            fields = fields + (singleParameterMapping.mappingLocation -> newBitRange)

          case other => throw new IllegalArgumentException(s"Mapping of a parameter must be a BitRange, not a Translation Table!")

      case multipleParameterMapping: MultipleParameterMapping =>

        multipleParameterMapping.mappingLocations.foreach { mappingLocation =>
          //Search reference
          val translationLeaf: TranslationLeaf =
            if translationContext.getScope.contains(mappingLocation) then
              translationContext.getScope(mappingLocation)
            else if translationContext.getTop.searchTranslationLeaf(mappingLocation).isDefined then
              translationContext.getTop.searchTranslationLeaf(mappingLocation).get
            else
              throw new IllegalArgumentException(s"Variable \"$mappingLocation\" is not defined!")

          translationLeaf.leaf match
            case bitRange: BitRange =>

              //Add as field
              val newBitRange = bitRange.deepCopy()
              fields = fields + (mappingLocation -> newBitRange)

            case other => throw new IllegalArgumentException(s"Mapping of a parameter must be a BitRange, not a Translation Table!")
        }
    }

    InstructionTemplate(name, fields, parametersDefinition, translationContext)
