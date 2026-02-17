package UniversalApplicationCompiler.helpers

import UniversalApplicationCompiler.datatypes.BitVector
import UniversalApplicationCompiler.helpers.Functions.gradientRange
import UniversalApplicationCompiler.helpers.ParametersDefinition

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
      val leafTranslationContext = translationContext.search(this.parameters.values(idx))

      //Step 1: The translation
      leafTranslationContext match
        case bitRange: BitRange => //It is an immediate specification
          ???
        case m: Map[_, _] => //It is a map translation
          require(m.forall { case (k, y) => k.isInstanceOf[String] && y.isInstanceOf[BitVector] })
          val map = m.asInstanceOf[Map[String, BitVector]]

  def setPartialField(fieldName: String, setMap: Map[String, Any]): Unit = fields(fieldName).setPartialValue(setMap)
  def setFullField(fieldName: String, value: Int): Unit = fields(fieldName).setFullValue(value)
  def checkCompleteness: Boolean =
    fields.forall {
      case (fieldName, bitRange) =>
        bitRange.checkValue
    }

  def compileInstruction: String = ???
