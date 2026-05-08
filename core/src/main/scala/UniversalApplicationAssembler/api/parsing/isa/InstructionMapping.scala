package UniversalApplicationAssembler.api.parsing.isa

import UniversalApplicationAssembler.internal.parsing.isa.InstructionTemplate

class InstructionMapping private[parsing] (instructions: List[InstructionTemplate]):

  private[parsing] val mapInstructions: Map[String, InstructionTemplate | List[InstructionTemplate]] =

    var temp: Map[String, InstructionTemplate | List[InstructionTemplate]] = Map.empty

    instructions.foreach { instructionTemplate =>
      if temp.contains(instructionTemplate.name) then //Instruction repeated -> multiple mappings

        temp(instructionTemplate.name) match
          case tempInstructionTemplate: InstructionTemplate =>
            temp += (tempInstructionTemplate.name -> List(tempInstructionTemplate, instructionTemplate)) //This will overwrite the old key, essentially converting the entry in this list now.
          case l: List[?] =>
            val list = l.asInstanceOf[List[InstructionTemplate]] :+ instructionTemplate //Add the element to the end. INSTRUCTIONS DECLARED AFTERWARD WILL GET EVALUATED LATER THAN ITS FIRSTS VARIANTS
            temp += (instructionTemplate.name -> list) //Overwrite the old key

      else //New instruction
        temp += (instructionTemplate.name -> instructionTemplate) //Add it
    }

    temp
