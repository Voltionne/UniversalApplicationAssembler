package UniversalApplicationAssembler.internal.datatypes

import UniversalApplicationAssembler.internal.helpers.Functions.gradientRange

import scala.math.*

/**
 * Represents a range of bits, using SystemVerilog notation. Therefore, it can be little-endian, big-endian, and any size.
 * @param a The left-most digit index
 * @param b The right-most digit index
 */
case class BitRange(a: Int, b: Int):
  val bits: Int = abs(a - b) + 1
  var value: String = "?" * bits
  val endianness: String = if a >= b then "little" else "big" //NOTE: 1 bit defaults to LITTLE ENDIAN. VERY IMPORTANT TODO: fix

  def this(a: Int) =
    this(a, a)

  /**
   * Makes a deep copy of the BitRange, including the internal bit states
   * @return The copy of this BitRange
   */
  def deepCopy(): BitRange =
    val bitRange = this.copy()
    bitRange.value = value
    bitRange

  /**
   * Sets partially the value of the BitRange based on a PartialAssignment.
   *
   * @param partialAssignment The partial assignment to use
   */
  def setPartialValue(partialAssignment: PartialAssignment): Unit =

    if partialAssignment.local then

      val setValueBinArray: Array[Char] = partialAssignment.value.toString(2).reverse.padTo(partialAssignment.bits, '0').reverse.toCharArray

      var valueAsArray: Array[Char] = value.toCharArray //convert to array temporally (because string is immutable)

      for (i, idx) <- gradientRange(partialAssignment.a, partialAssignment.b).zipWithIndex do
        if a > b then //LSB
          valueAsArray = valueAsArray.reverse

          valueAsArray(i) = setValueBinArray(idx)

          valueAsArray = valueAsArray.reverse

        else
          valueAsArray(i) = setValueBinArray(idx)

      value = valueAsArray.mkString
    else

      val setValueBinArray: Array[Char] = partialAssignment.value.toString(2).reverse.padTo(partialAssignment.bits, '0').reverse.toCharArray

      var valueAsArray: Array[Char] = value.toCharArray //convert to array temporally (because string is immutable)

      for (i, idx) <- gradientRange(partialAssignment.a, partialAssignment.b).zipWithIndex do
        if a > b then //LSB
          valueAsArray = valueAsArray.reverse

          valueAsArray(i - b) = setValueBinArray(idx)

          valueAsArray = valueAsArray.reverse

        else
          valueAsArray(i - a) = setValueBinArray(idx)

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
  def checkValue: Boolean = if value.contains("?") then false else true

  /**
   * Puts the BitRange on its placed (based on its indexes "a" and "b") and fills other places with "P" meaning placeholder. It is meant to allow the overlapping and combination of different BitRanges as the same final number.
   * @param bits The number of bits to pad.
   * @return A string representing the final representation of the BitRange
   */
  def compile(bits: Int): String =
    require(checkValue, s"$value")

    //"P" stands for placeholder

    if endianness == "little" then
      (value + "P" * b).reverse.padTo(bits, 'P').reverse
    else //big
      ("P" * a + value).padTo(bits, 'P')

object BitRange:
  def apply(a: Int): BitRange = new BitRange(a)