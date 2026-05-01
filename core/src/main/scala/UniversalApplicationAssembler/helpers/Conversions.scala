package UniversalApplicationAssembler.helpers

import scala.jdk.CollectionConverters.*
import UniversalApplicationAssembler.helpers.{ParameterMapping, SingleParameterMapping, MultipleParameterMapping}

/**
 * Converts things to other things, to simplify coding
 */
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
    case int32: Integer => BigInt(int32)
    case int64: Long => BigInt(int64)
    case bigint: java.math.BigInteger => bigint.asInstanceOf[BigInt] //Convert BigInteger of java to BigInt
    case other => other

  /**
   * Converts a map that represents the parameters of instructions to the case class ParameterDefinition
   * @param map the map that represents the parameters of an instruction
   * @return ParametersDefinition class instance
   */
  def convertParametersMap(map: Map[String, Any]): ParametersDefinition =

    val values: List[String] = map.get("Values") match
      case Some(l: List[_]) =>
        if l.forall(_.isInstanceOf[String]) then l.asInstanceOf[List[String]]
        else throw new IllegalArgumentException("values must be a list of Strings")
      case Some(_) => throw new IllegalArgumentException("values must be a list")
      case None => throw new IllegalArgumentException("missing 'values'")

    val mappings: List[ParameterMapping] = map.get("mappings") match
      case Some(l: List[_]) =>
        if l.forall {
          case _: String => true
          case l: List[_] => l.forall(_.isInstanceOf[String])
          case _ => false
        } then
          val temporalList = l.asInstanceOf[List[String | List[String]]]
          
          temporalList.map {
            case s: String => SingleParameterMapping(s)
            case l: List[String] => MultipleParameterMapping(l)
          }
          
        else throw new IllegalArgumentException("mappings must be a list that contains either Strings or lists of Strings")
      case Some(_) => throw new IllegalArgumentException("mappings must be a list")
      case None => throw new IllegalArgumentException("missing 'mappings'")

    ParametersDefinition(values, mappings)

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