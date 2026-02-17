package UniversalApplicationCompiler.helpers

import UniversalApplicationCompiler.datatypes.BitVector

object Casts:

  /**
   * Converts a string to a BigInt of Scala. Uses prefixes 0b for binary, 0x for hexadecimal and 0o for octal, if no prefixes is treated as decimal.
   * @param string The string in question to be converted
   * @return BigInt that represents the value
   */
  def stringToBigInt(string: String): BigInt =

    val stringWithoutSign: String = if string(0) == '-' then string.drop(1) else string //Negative sign
    val sign: String = if string(0) == '-' then "-" else ""

    if stringWithoutSign.slice(0, 2) == "0b" then
      BigInt(sign + stringWithoutSign.drop(2), 2)
    else if stringWithoutSign.slice(0, 2) == "0x" then
      BigInt(sign + stringWithoutSign.drop(2), 16)
    else if stringWithoutSign.slice(0, 2) == "0o" then
      BigInt(sign + stringWithoutSign.drop(2), 8)
    else
      BigInt(sign + stringWithoutSign)