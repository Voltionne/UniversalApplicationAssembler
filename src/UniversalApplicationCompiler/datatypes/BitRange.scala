package UniversalApplicationCompiler.datatypes

import UniversalApplicationCompiler.helpers.Functions.gradientRange

import scala.math.*

/**
 * Represents a range of bits, using SystemVerilog notation. Therefore, it can be little-endian, big-endian, and any size.
 * @param a The left-most digit index
 * @param b The right-most digit index
 */
case class BitRange(a: Int, b: Int):
  val bits: Int = abs(a - b) + 1
  var value: String = "?" * bits
  val endianness: String = if a > b then "little" else "big"

  /**
   * Sets partially the value of the BitRange based on a setMap.
   * @param setMap A map that includes "set" which indicates the value to be set and "bits" which indicates what bits does it affect the set, as a string in format "a:b" (SystemVerilog style)
   */
  def setPartialValue(setMap: Map[String, Any]): Unit =
    require(setMap contains "set")
    require(setMap("set").isInstanceOf[Int])

    require(setMap contains "bits")
    require(setMap("bits").isInstanceOf[String])

    (setMap("set"), setMap("bits")) match
      case (set: BigInt, bits: String) =>
        if bits contains ":" then

          //This means it specifies a whole range
          val parts = bits.split(":")

          require(parts.length == 2)
          require(parts(0).forall(p => p.isDigit) && parts(1).forall(p => p.isDigit)) //check that both numbers are numeric

          val bitsSet = abs(parts(0).toInt - parts(1).toInt) + 1

          require(set >= 0 && set < (BigInt(1) << bitsSet))

          val setValueBinArray: Array[Char] = set.toString(2).reverse.padTo(bitsSet, '0').reverse.toCharArray

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

  /**
   * Set the full value of the BitRange
   * @param value the value to be set
   */
  def setFullValue(value: BigInt): Unit =
    require(value >= 0 && value < (BigInt(1) << bits))
    this.value = value.toString(2).reverse.padTo(bits, '0').reverse

  /**
   * Tests if the BitRange has all its bits positions declared correctly as 0 or 1, i.e. no placeholder values.
   * @return true or false depending on the result of the check
   */
  def checkValue: Boolean = if value contains "?" then false else true

  /**
   * Puts the BitRange on its placed (based on its indexes "a" and "b") and fills other places with "P" meaning placeholder. It is meant to allow the overlapping and combination of different BitRanges as the same final number.
   * @param bits The number of bits to pad.
   * @return A string representing the final representation of the BitRange
   */
  def compile(bits: Int): String =
    require(checkValue)

    //"P" stands for placeholder

    if endianness == "little" then
      (value + "P" * b).reverse.padTo(bits, 'P').reverse
    else //big
      ("P" * a + value).padTo(bits, 'P')