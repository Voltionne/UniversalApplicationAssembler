package UniversalApplicationAssembler.internal.parsing.assembly

import UniversalApplicationAssembler.internal.parsing.isa.InstructionTemplate

class InstructionMapping(instructions: List[InstructionTemplate]):

  private val mapInstructions: Map[String, InstructionTemplate | List[InstructionTemplate]] =

    var temp: Map[String, InstructionTemplate | List[InstructionTemplate]] = Map.empty

    instructions.foreach { instructionTemplate =>
      if temp.contains(instructionTemplate.name) then //Instruction repeated -> multiple mappings

        temp(instructionTemplate.name) match
          case instructionTemplate: InstructionTemplate =>
            temp += (instructionTemplate.name -> List(instructionTemplate, instructionTemplate)) //This will overwrite the old key, essentially converting the entry in this list now.
          case l: List[?] =>
            val list = l.asInstanceOf[List[InstructionTemplate]] :+ instructionTemplate //Add the element to the end. INSTRUCTIONS DECLARED AFTERWARD WILL GET EVALUATED LATER THAN ITS FIRSTS VARIANTS
            temp += (instructionTemplate.name -> list) //Overwrite the old key

      else //New instruction
        temp += (instructionTemplate.name -> instructionTemplate) //Add it
    }

    temp

  def compileInstruction(parsedWrittenInstruction: Array[String]): String =

    /*
    Ok, there is a BIG problem with mutability right now. The thing is that when InstructionTemplate is called apply(), for compiling an instruction,
    it MODIFIES its internal state to fit the parameters. That means that if hypothetically you needed the "original" InstructionTemplate without the
    fields modified (for example, two instructions called the same where one maintains a field as "constant" (i.e. does not write to it except the writes
    performed at a higher level) and the other modifies it -> the first instruction may see the second instruction state instead of a clean state).

    This shouldn't be a problem in 99% of implementations (who tf is going to do what I just mentioned?), but is a thing to keep into consideration for
    future updates. Therefore, this is a TODO.

    Yooo, longest comment ever
     */

    //Calls to .apply always works because if no parameters, simply the for loop is skipped in InstructionTemplate

    val instruction = mapInstructions(parsedWrittenInstruction.head)

    instruction match
      case instructionTemplate: InstructionTemplate => //Single instruction
        instructionTemplate.apply(parsedWrittenInstruction.tail)
        instructionTemplate.compileInstruction

      case l: List[?] => //Multiple instructions
        val listInstructions = l.asInstanceOf[List[InstructionTemplate]]

        val firstInstruction = listInstructions.iterator //first instruction that does not fail apply (they are in written order of the YAML file, theoretically)
          .map { instruction =>
            util.Try(instruction.apply(parsedWrittenInstruction.tail)) match
              case util.Success(value) => instruction
              case other => None
          }
          .collectFirst {
            case instructionTemplate: InstructionTemplate => instructionTemplate
          }

        firstInstruction match
          case Some(instruction) => instruction.compileInstruction
          case None => throw new NoSuchElementException(s"Instruction \"${parsedWrittenInstruction.head}\" doesn't coincide with any format of such instruction!")
