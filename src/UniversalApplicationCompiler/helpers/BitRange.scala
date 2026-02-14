package UniversalApplicationCompiler.helpers

import scala.math._
import UniversalApplicationCompiler.helpers.Functions.gradientRange

case class BitRange(a: Int, b: Int):
  val bits: Int = abs(a - b) + 1
  var value: String = "?" * bits
  val endianness: String = if a > b then "little" else "big"

  def setPartialValue(setDict: Map[String, Any]): Unit =
    require(setDict contains "set")
    require(setDict("set").isInstanceOf[Int])

    require(setDict contains "bits")
    require(setDict("bits").isInstanceOf[String])

    (setDict("set"), setDict("bits")) match
      case (set: Int, bits: String) =>
        if bits contains ":" then

          //This means it specifies a whole range
          val parts = bits.split(":")

          require(parts.length == 2)
          require(parts(0).forall(p => p.isDigit) && parts(1).forall(p => p.isDigit)) //check that both numbers are numeric

          val bitsSet = abs(parts(0).toInt - parts(1).toInt) + 1

          require(set >= 0 && set < pow(2, bitsSet))

          val setValueBinArray: Array[Char] = set.toBinaryString.reverse.padTo(bitsSet, '0').reverse.toCharArray
          println(setValueBinArray.mkString)

          var valueAsArray: Array[Char] = value.toCharArray //convert to array temporally (because string is immutable)

          for (i, idx) <- gradientRange(parts(0).toInt, parts(1).toInt).zipWithIndex do
            if a > b then //LSB
              valueAsArray = valueAsArray.reverse

              valueAsArray(i - b) = setValueBinArray(idx)

              valueAsArray = valueAsArray.reverse

            else
              valueAsArray(i - a) = setValueBinArray(idx)
          
          value = valueAsArray.mkString

        else

          require(bits.forall(p => p.isDigit)) //check that bits is a digit
          require(set == 0 || set == 1) //check that set is a binary digit

          var valueAsArray: Array[Char] = value.toCharArray

          if a > b then //LSB

            valueAsArray = valueAsArray.reverse

            valueAsArray(bits.toInt - b) = set.toString.head

            valueAsArray = valueAsArray.reverse
          else
            valueAsArray(bits.toInt - a) = set.toString.head

          value = valueAsArray.mkString

  def setFullValue(value: Int): Unit =
    require(value >= 0 && value < pow(2, bits))
    this.value = value.toBinaryString.reverse.padTo(bits, '0').reverse

  def checkValue: Boolean = if value contains "?" then false else true
