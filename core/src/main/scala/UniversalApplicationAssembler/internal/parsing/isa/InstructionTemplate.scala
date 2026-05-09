package UniversalApplicationAssembler.internal.parsing.isa

import UniversalApplicationAssembler.internal.helpers.Functions.gradientRange
import UniversalApplicationAssembler.internal.datatypes.{BitRange, Utils}
import UniversalApplicationAssembler.internal.parsing.yaml.{Conversions, YamlReader}
import UniversalApplicationAssembler.internal.parsing.yaml.translation.{TranslationLeaf, TranslationNode, Translation}
import org.snakeyaml.engine.v2.nodes.MappingNode

/**
 * Represents a full ISA instruction. It is meant to be applied over a string that represents an instruction to compile it to binary instantly.
 *
 * @param fields The fields of the instruction, it is a map where each key is the name of the field and a BitRange represents it.
 * @param parameters A class that represents the parameters of the instruction.
 * @param translationContext The translation context node/scope where this instruction was declared.
 * @param originNode The node where the instruction is defined
 */
case class InstructionTemplate(name: String, fields: Map[String, BitRange], parameters: ParametersDefinition, translationContext: TranslationNode, originNode: MappingNode):

  val bits: Int = translationContext.bits.toInt

  //Checks for fields
  require: //Check that no bit collisions
    val usedBits: Array[Boolean] = Array.fill(bits)(false)

    //Hating Scala because no imperative nice for loop here
    fields.forall { field =>
      gradientRange(field(1).a, field(1).b).forall { idx =>
        if usedBits(idx) then false
        else {usedBits(idx) = true; true}
      }
    }

  /**
   * Given the parameters and a certain translation context it sets up the fields accordingly.
   *
   * @param parameters A list that includes the parameters used.
   */
  def apply(parameters: Array[String]): Unit =
    require(parameters.length == this.parameters.length)
    
    for idx <- parameters.indices do

      //Get the leaf translation context. 2 cases:
      //1. In this scope (either in changes of this current context or some parent)
      //2. Full pathDeleted "bits" argument

      val leafTranslation: TranslationLeaf = Translation.searchLeaf(this.parameters.datatypes(idx), translationContext)
        .getOrElse(throw new NoSuchElementException(s"Didn't found datatype ${this.parameters.datatypes(idx)}"))

      leafTranslation.leaf match
        case bitRange: BitRange => //It is an immediate specification

          val immediateValue = Conversions.stringToBigInt(parameters(idx))

          this.parameters.mappings(idx) match
            case SingleParameterMapping(s: String) =>
              setFullField(s, immediateValue)
            case MultipleParameterMapping(mappings: List[String]) =>

              var bitsDone = 0 //counts how many bits already done

              for mapping <- mappings.reverse do //iterates over the mappings, reversed to do first the LSB

                val mappingBits = fields(mapping).bits

                val mask: BigInt = (BigInt(1) << mappingBits) - 1

                val finalValue = (immediateValue >> bitsDone) & mask

                setFullField(mapping, finalValue)

                //increase the bits done to do the other iterations correctly
                bitsDone += mappingBits

        case m: Map[?, ?] => //It is a map translation
          val map = m.asInstanceOf[Map[String, BigInt]] //Should 100% work

          val translatedBigInt = map(parameters(idx))

          this.parameters.mappings(idx) match

            case SingleParameterMapping(s: String) =>
              setFullField(s, translatedBigInt)
            case MultipleParameterMapping(l: List[String]) => throw new IllegalArgumentException("Currently don't support multiple mappings in case of translation table!")

  /**
   * Sets a value partially of a certain field
   *
   * @param fieldName The name of the field
   * @param setMap    A map that includes "set" which indicates the value to be set and "bits" which indicates what bits does it affect the set, as a string in format "a:b" (SystemVerilog style)
   */
  def setPartialField(fieldName: String, setMap: Map[String, Any]): Unit = fields(fieldName).setPartialValue(setMap)

  /**
   * Sets the whole value of a certain field
   *
   * @param fieldName The name of the field
   * @param value     The value to be set
   */
  def setFullField(fieldName: String, value: BigInt): Unit = fields(fieldName).setFullValue(value)

  /**
   * Check whether all the bits of the instruction are set to a certain defined value and not a placeholder value.
   *
   * @return true or false depending on the result of the check
   */
  def checkCompleteness: Boolean =
    fields.forall {
      case (fieldName, bitRange) =>
        bitRange.checkValue
    }

  /**
   * Combine all fields to finally produce the end instruction
   *
   * @return A binary string that represents the compiled instruction
   */
  def compileInstruction: String =
    //Not done because already done in BitRange
    //require(checkCompleteness)

    var compiledInstructionArray = ("P" * bits).toCharArray.map(_.toString)

    for (fieldName, bitRange) <- fields do

      val compiledBitRange = bitRange.compile(bits).toCharArray //This calls checkValue on each BitRange

      compiledInstructionArray = compiledInstructionArray.zip(compiledBitRange).map { case (x, y) => s"$x$y"}

    val result = compiledInstructionArray.zipWithIndex.map { (bit, idx) =>
      bit.find(char => char == '0' || char == '1') match
        case Some(value) => value
        case None => throw new IllegalArgumentException(s"Bit in array idx $idx was not set!")
    }

    result.mkString


/**
 * Object for fast constructing InstructionObjects from other things
 */
object InstructionTemplate:

  /**
   * Constructs an InstructionTemplate from the MappingNode that defined it and its current translation context
   * @param mappingNode The MappingNode that defines the InstructionTemplate
   * @param translationContext The current translation context, where this instruction was defined
   * @return The constructed InstructionTemplate
   */
  def apply(mappingNode: MappingNode, translationContext: TranslationNode): InstructionTemplate =

    require(translationContext.bits > 0, s"Required a defined number of bits that is positive for defining instructions! (Currently bits is set to \"${translationContext.bits}\". ${YamlReader.getNodeLocation(mappingNode)})")

    val instructionAsMap = YamlReader.constructToScala(mappingNode)

    instructionAsMap match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
        val map = m.asInstanceOf[Map[String, Any]]

        require(map.contains("name"), s"Instruction MUST have a name! ${YamlReader.getNodeLocation(mappingNode)}")

        //Variables needed for creating the instruction
        val name = map("name") match
          case s: String => s
          case other => throw new IllegalArgumentException(s"Expected name of instruction to be a string. ${YamlReader.getNodeLocation(mappingNode)}")

        var fields: Map[String, BitRange] = Map.empty
        var parametersDefinition: ParametersDefinition = ParametersDefinition(List.empty, List.empty)

        for (key, value) <- map if key != "name" do

          if key == "parameters" then //The famous parameters
            value match
              case params: Map[?, ?] if params.keys.forall(_.isInstanceOf[String]) && params.values.forall(_.isInstanceOf[List[Any]]) =>
                val parameters = params.asInstanceOf[Map[String, List[Any]]]

                val (parametersConstructed, fieldsConstructed) = constructParameters(parameters, mappingNode, translationContext)

                parametersDefinition = parametersConstructed //Set the parameters
                fields ++= fieldsConstructed //Add the fields found

              case other => throw new IllegalArgumentException(s"Expected parameters of a function to be a map with keys being strings and values be lists, not ${other}. ${YamlReader.getNodeLocation(mappingNode)}")
          else //100% an assignment

            value match
              case sm: Map[?, ?] if sm.keys.forall(_.isInstanceOf[String]) => //Partial assignment
                val setMap = sm.asInstanceOf[Map[String, Any]]

                if Utils.isSetMap(setMap) then

                  //Procedure as following
                  //Step 1: Get the BitRange
                  //Step 2: Create a new one with the updated value. DON'T UPDATE THE TRANSLATION CONTEXT (as this is per instruction)
                  //Step 3: The new one is a field

                  //Step 1:
                  val translationLeaf: TranslationLeaf = Translation.searchLeaf(key, translationContext)
                    .getOrElse(throw new IllegalArgumentException(s"Variable \"$key\" is not defined! ${YamlReader.getNodeLocation(mappingNode)}"))

                  //Step 2 & 3
                  translationLeaf.leaf match
                    case bitRange: BitRange =>

                      //Step 2:
                      val newBitRange = bitRange.deepCopy()
                      newBitRange.setPartialValue(setMap)

                      //Step 3:
                      //Note: using stringKey as the name of the field, this in some cases can be the full path. Should be ok as searching uses also the full path.
                      fields = fields + (key -> newBitRange)

                    case other => throw new IllegalArgumentException(s"Can only assign values to BitRange, not to ${other.getClass}")

                else
                  throw new IllegalArgumentException(s"Expected map to be a set map for variable \"$key\". ${YamlReader.getNodeLocation(mappingNode)}")

              case i: BigInt => //Full assignment

                //Procedure as following
                //Step 1: Get the BitRange
                //Step 2: Create a new one with the updated value. DON'T UPDATE THE TRANSLATION CONTEXT (as this is per instruction)
                //Step 3: The new one is a field

                //Step 1:
                val translationLeaf: TranslationLeaf = Translation.searchLeaf(key, translationContext)
                  .getOrElse(throw new IllegalArgumentException(s"Variable \"$key\" is not defined! ${YamlReader.getNodeLocation(mappingNode)}"))

                //Step 2 & 3
                translationLeaf.leaf match
                  case bitRange: BitRange =>

                    //Step 2:
                    val newBitRange = bitRange.deepCopy()
                    newBitRange.setFullValue(i)

                    //Step 3:
                    //Note: using stringKey as the name of the field, this in some cases can be the full path. Should be ok as searching uses also the full path.
                    fields = fields + (key -> newBitRange)

                  case other => throw new IllegalArgumentException(s"Can only assign values to BitRange, not to ${other.getClass}. ${YamlReader.getNodeLocation(mappingNode)}")

              case other => throw new IllegalArgumentException(s"Only assignments inside instructions! Not $other! ${YamlReader.getNodeLocation(mappingNode)}")

        InstructionTemplate(name, fields, parametersDefinition, translationContext, mappingNode)

      case other => throw new IllegalArgumentException(s"Expected MappingNode to be a Map and also for all keys to be a string. ${YamlReader.getNodeLocation(mappingNode)}")

  private def constructParameters(map: Map[String, List[Any]], originNode: MappingNode, translationContext: TranslationNode): (ParametersDefinition, Map[String, BitRange]) =

    require(map.contains("values") && map.contains("mappings") && map.size == 2, s"Bad parameters MappingNode. ${YamlReader.getNodeLocation(originNode)}") //Check that everything is alright

    val datatypes = map("values") match
      case l: List[?] if l.forall(_.isInstanceOf[String]) => l.asInstanceOf[List[String]]
      case other => throw new IllegalArgumentException(s"Expected \"values\" to be a list of strings! ${YamlReader.getNodeLocation(originNode)}")

    val mappings: List[ParameterMapping] = map("mappings").map {
      case s: String => SingleParameterMapping(s)
      case l: List[?] if l.forall(_.isInstanceOf[String]) => MultipleParameterMapping(l.asInstanceOf[List[String]])
      case other => throw new IllegalArgumentException(s"Expected mappings to be either a string or list of strings, not $other!")
    }

    val parametersDefinition = ParametersDefinition(datatypes, mappings)
    var newFields: Map[String, BitRange] = Map.empty

    for (datatype, mapping) <- parametersDefinition.datatypes.zip(parametersDefinition.mappings) do

      if translationContext.getScope.contains(datatype) then () //Nice! It exists
      else if translationContext.getTop.searchTranslationLeaf(datatype).isDefined then () //Nice! It exists
      else
        throw new NoSuchElementException(s"Didn't found datatype \"$datatype\" specified in parameters. ${YamlReader.getNodeLocation(originNode)}")

      //Get the locations of mapping
      val locations = mapping match
        case SingleParameterMapping(location: String) => List(location)
        case MultipleParameterMapping(locations: List[String]) => locations

      //Check locations exist
      for location <- locations do
        val translationLeaf: TranslationLeaf = Translation.searchLeaf(location, translationContext)
          .getOrElse(throw new IllegalArgumentException(s"Didn't found mapping location \"$location\" specified in parameters. ${YamlReader.getNodeLocation(originNode)}"))

        translationLeaf match
          case TranslationLeaf(leaf: BitRange) =>
            newFields += (location -> leaf)
          case other => throw new IllegalArgumentException(s"Expected a BitRange as mapping, not ${other.getClass}! ${YamlReader.getNodeLocation(originNode)}")

    (parametersDefinition, newFields)