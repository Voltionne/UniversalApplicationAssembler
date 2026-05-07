package UniversalApplicationAssembler.internal.parsing.assembly


object AssemblyParser:

  /**
   * Separates instructions and operands of an assembly file represented as a String.
   * @param assemblyFile The string representing the assembly file
   * @return Each instruction separated into opcode and operands.
   */
  def parseToList(assemblyFile: String): List[Array[String]] =
  
    //Instruction format: (May change in the future)
    //[OPERAND] [PARAM1], [PARAM2], [PARAM3] [...]

    val assemblyLines = assemblyFile.split("\n")
    var temp: List[Array[String]] = List.empty

    for assemblyLine <- assemblyLines do
      val parts = assemblyLine.split("\\s+", 2)

      if parts.length == 1 then
        temp = parts :: temp
      else
        val opcode = parts(0)
        val operands = parts(1).split(",").map(_.trim)
        temp = (opcode +: operands) :: temp

    //Prepends everything for performance, now reverse. This is O(n)
    temp.reverse
