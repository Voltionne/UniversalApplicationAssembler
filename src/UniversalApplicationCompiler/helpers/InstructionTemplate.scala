package UniversalApplicationCompiler.helpers

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

  def apply(translationContext: TranslationContext, parameters: Array[String]): Unit = ???

  def setPartialField(fieldName: String, setMap: Map[String, Any]): Unit = fields(fieldName).setPartialValue(setMap)
  def setFullField(fieldName: String, value: Int): Unit = fields(fieldName).setFullValue(value)
  def checkCompleteness: Boolean =
    fields.forall {
      case (fieldName, bitRange) =>
        bitRange.checkValue
    }

  def compileInstruction: String = ???
