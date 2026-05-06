package UniversalApplicationAssembler.internal.parsing.yaml

import scala.jdk.CollectionConverters.*

object Conversions:

  /**
   * Automatically converts a YAML file maps, sets, and lists from Java to Scala types. It calls recursively for maps.
   * @param value the value to be parsed
   * @return the converted type
   */
  def convertFromJava(value: Any): Any = value match

    case m: java.util.Map[?, ?] =>
      Map.from(m.asScala.view.map { case (k, y) => convertFromJava(k) -> convertFromJava(y) })
    case l: java.util.List[?] =>
      l.asScala.view.map(convertFromJava).toList
    case s: java.util.Set[?] =>
      s.asScala.view.map(convertFromJava).toSet
    case int32: java.lang.Integer => BigInt(int32)
    case int64: java.lang.Long => BigInt(int64)
    case bigint: java.math.BigInteger => BigInt(bigint) //Convert BigInteger of java to BigInt
    case other => other

  /**
   * Converts a string to a BigInt of Scala. Uses prefixes 0b for binary, 0x for hexadecimal and 0o for octal, if no prefixes is treated as decimal.
   *
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
