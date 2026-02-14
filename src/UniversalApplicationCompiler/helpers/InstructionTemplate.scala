package UniversalApplicationCompiler.helpers

import UniversalApplicationCompiler.helpers.Functions.gradientRange

case class InstructionTemplate(bits: Int, fields: Map[String, BitRange]):

  require: //Check that no bit collisions
    val usedBits: Array[Boolean] = Array.fill(bits)(false)

    //Hating Scala because no imperative nice for loop here
    fields.forall { field =>
      gradientRange(field(1).a, field(1).b).forall { idx =>
        if usedBits(idx) then false
        else {usedBits(idx) = true; true}
      }
    }

  def apply(): Unit = ???
