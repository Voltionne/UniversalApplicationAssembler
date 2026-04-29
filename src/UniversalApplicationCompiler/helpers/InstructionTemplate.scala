package UniversalApplicationCompiler.helpers

import scala.math.*
import UniversalApplicationCompiler.helpers.Functions.gradientRange
import UniversalApplicationCompiler.helpers.ParametersDefinition
import UniversalApplicationCompiler.helpers.{SingleParameterMapping, MultipleParameterMapping}

case class InstructionTemplate(bits: Int, fields: Map[String, BitRange], parameters: ParametersDefinition):

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

  def apply(translationContext: TranslationContext, parameters: Array[String]): Unit =
    require(parameters.length == this.parameters.length)

    for idx <- parameters.indices do

      //get the TranslationContext leaf of the datatype
      val leafTranslationContext = translationContext.search(this.parameters.values(idx)).leaf

      //Step 1: The translation
      leafTranslationContext match
        case bitRange: BitRange => //It is an immediate specification

          val immediateValue = Casts.stringToBigInt(parameters(idx)) //convert the string to immediate BigInt

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

        case m: Map[_, _] => //It is a map translation
          require(m.forall { case (k, y) => k.isInstanceOf[String] && y.isInstanceOf[BigInt] })
          val map = m.asInstanceOf[Map[String, BigInt]]

          val translatedBigInt = map(parameters(idx))

          this.parameters.mappings(idx) match

            case SingleParameterMapping(s: String) =>
              setFullField(s, translatedBigInt)
            case MultipleParameterMapping(l: List[String]) => throw new IllegalArgumentException("Currently don't support multiple mappings in case of translation table!")

  def setPartialField(fieldName: String, setMap: Map[String, Any]): Unit = fields(fieldName).setPartialValue(setMap)
  def setFullField(fieldName: String, value: BigInt): Unit = fields(fieldName).setFullValue(value)
  def checkCompleteness: Boolean =
    fields.forall {
      case (fieldName, bitRange) =>
        bitRange.checkValue
    }

  def compileInstruction: String =
    //Not done because already done in BitRange
    //require(checkCompleteness)

    val compiledInstructionArray = ("?" * bits).toCharArray

    val compiledResults: List[String] = List()

    for (fieldName, bitRange) <- fields do
      compiledResults +: bitRange.compile(bits)
    
    compiledResults.map(_.toList).transpose.map {
      column => column.find(_ != 'P').getOrElse('P')
    }.mkString