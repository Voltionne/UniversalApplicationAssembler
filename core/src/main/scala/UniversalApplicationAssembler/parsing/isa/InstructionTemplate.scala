package UniversalApplicationAssembler.parsing.isa

import UniversalApplicationAssembler.datatypes.BitRange
import UniversalApplicationAssembler.helpers.{Conversions, MultipleParameterMapping, ParametersDefinition, SingleParameterMapping}
import UniversalApplicationAssembler.parsing.yaml.translation.{TranslationLeaf, TranslationNode}
import UniversalApplicationAssembler.helpers.Functions.gradientRange

/**
 * Represents a full ISA instruction. It is meant to be applied over a string that represents an instruction to compile it to binary instantly.
 *
 * @param fields The fields of the instruction, it is a map where each key is the name of the field and a BitRange represents it.
 * @param parameters A class that represents the parameters of the instruction.
 * @param translationContext The translation context node/scope where this instruction was declared.
 */
case class InstructionTemplate(name: String, fields: Map[String, BitRange], parameters: ParametersDefinition, translationContext: TranslationNode):

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

      val leafTranslation: TranslationLeaf =
        if translationContext.getScope.contains(this.parameters.datatypes(idx)) then //case 1
          translationContext.getScope(this.parameters.datatypes(idx))
        else if translationContext.getTop.searchTranslationLeaf(this.parameters.datatypes(idx)).isDefined then //case 2
          translationContext.getTop.searchTranslationLeaf(this.parameters.datatypes(idx)).get
        else
          throw new NoSuchElementException(s"Didn't found variable ${this.parameters.datatypes(idx)}")

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

    val compiledInstructionArray = ("?" * bits).toCharArray

    val compiledResults: List[String] = List()

    for (fieldName, bitRange) <- fields do
      compiledResults +: bitRange.compile(bits) //This calls checkValue on each BitRange

    compiledResults.map(_.toList).transpose.map {
      column => column.find(_ != 'P').getOrElse('P')
    }.mkString
      
