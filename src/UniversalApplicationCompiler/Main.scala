package UniversalApplicationCompiler

import UniversalApplicationCompiler.helpers.BitRange

object Main:
  def main(args: Array[String]): Unit =
    val bitRange = BitRange(5, 0)
    println(bitRange.value)
    println(bitRange.checkValue)
    bitRange.setFullValue(15)
    println(bitRange.value)
    println(bitRange.checkValue)
