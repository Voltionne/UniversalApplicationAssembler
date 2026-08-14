package UniversalApplicationAssembler.internal.datatypes

import UniversalApplicationAssembler.internal.parsing.yaml.YamlReader
import org.snakeyaml.engine.v2.nodes.MappingNode

import scala.math.*

/**
 * Represents a partial assignment to a certain bit range. A map that includes "set" which indicates the value to be set and "bits" which indicates what bits does it affect the set, as a string in format "a:b" (SystemVerilog style)
 * @param value The value to be assigned
 * @param a The left-most bit position
 * @param b The right-most bit position
 * @param local Whether the bit positions represent the real positions in the end instruction (false) or local to the bit range (true)
 */
case class PartialAssignment(value: BigInt, a: Int, b: Int, local: Boolean):
  val bits: Int = abs(a - b) + 1

object PartialAssignment:
  // Constants that represents the keys. For changing them fast in case of needed.
  private val VALUE_KEY = "set"
  private val BITS_KEY = "bits"
  private val BITS_LOCAL_KEY = "bits_local"

  /**
   * Checks if a certain mapping node follows the format of a partial assignment map or not
   * @param mappingNode The mapping node to check
   * @return True if it follows the format, false if not
   */
  def isPartialAssignment(mappingNode: MappingNode): Boolean =
    val nodeAsScala = YamlReader.constructToScala(mappingNode)

    nodeAsScala match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
        val map = m.asInstanceOf[Map[String, Any]]
        isPartialAssignment(map)
      case other => false

  /**
   * Checks if a certain map follows the format of a partial assignment map or not
   * @param assignmentMap The map to check
   * @return True if it follows the format, false if not
   */
  def isPartialAssignment(assignmentMap: Map[String, Any]): Boolean =
    val option1 = (assignmentMap contains VALUE_KEY) && assignmentMap(VALUE_KEY).isInstanceOf[BigInt] && (assignmentMap contains BITS_KEY) && assignmentMap(BITS_KEY).isInstanceOf[String] && assignmentMap.size == 2
    val option2 = (assignmentMap contains VALUE_KEY) && assignmentMap(VALUE_KEY).isInstanceOf[BigInt] && (assignmentMap contains BITS_LOCAL_KEY) && assignmentMap(BITS_LOCAL_KEY).isInstanceOf[String] && assignmentMap.size == 2
    option1 || option2

  /**
   * Convert from a map to PartialAssignment. NOT RECOMMENDED AS THIS LOSES NICE ERROR MESSAGES
   * @param map The map
   * @return The partial assignment that this map represents
   */
  def apply(map: Map[String, Any]): PartialAssignment =
  
    require(map.size == 2, s"Expected partial assignment map to have only exactly 2 elements!.")

    require(map contains VALUE_KEY, s"Partial assignment map doesn't have \"$VALUE_KEY\" key!.")
    require(map(VALUE_KEY).isInstanceOf[BigInt], s"Partial assignment \"$VALUE_KEY\" field is not a BigInt!.")

    if map contains BITS_KEY then
      require(map(BITS_KEY).isInstanceOf[String], s"Expected \"$BITS_KEY\" key to be an string!.")

      val bits = map(BITS_KEY).asInstanceOf[String]

      val parts = bits.split(":")

      if parts.length == 1 then
        require(parts(0).forall(p => p.isDigit)) //check that the number is numeric
        PartialAssignment(map(VALUE_KEY).asInstanceOf[BigInt], parts(0).toInt, parts(0).toInt, false)
      else if parts.length == 2 then
        require(parts(0).forall(p => p.isDigit) && parts(1).forall(p => p.isDigit)) //check that both numbers are numeric
        PartialAssignment(map(VALUE_KEY).asInstanceOf[BigInt], parts(0).toInt, parts(1).toInt, false)
      else
        throw new IllegalArgumentException(s"Expected bit positions to be 1 or 2 numbers separated by \":\".")

    else if map contains BITS_LOCAL_KEY then
      require(map(BITS_LOCAL_KEY).isInstanceOf[String], s"Expected \"$BITS_LOCAL_KEY\" key to be an string!.")

      val bits = map(BITS_LOCAL_KEY).asInstanceOf[String]

      val parts = bits.split(":")

      if parts.length == 1 then
        require(parts(0).forall(p => p.isDigit)) //check that the number is numeric
        PartialAssignment(map(VALUE_KEY).asInstanceOf[BigInt], parts(0).toInt, parts(0).toInt, true)
      else if parts.length == 2 then
        require(parts(0).forall(p => p.isDigit) && parts(1).forall(p => p.isDigit)) //check that both numbers are numeric
        PartialAssignment(map(VALUE_KEY).asInstanceOf[BigInt], parts(0).toInt, parts(1).toInt, true)
      else
        throw new IllegalArgumentException(s"Expected bit positions to be 1 or 2 numbers separated by \":\".")

    else
      throw new IllegalArgumentException(s"Partial assignment map doesn't have \"$BITS_KEY\" nor \"$BITS_LOCAL_KEY\" key!.")

  /**
   * Convert from a MappingNode to PartialAssignment
   * @param mappingNode The mapping node
   * @return The partial assignment that the mapping node represents
   */
  def apply(mappingNode: MappingNode): PartialAssignment =
    val nodeAsScala = YamlReader.constructToScala(mappingNode)

    //Yeah, very hell of long if-else
    nodeAsScala match
      case m: Map[?, ?] =>

        require(m.keys.forall(_.isInstanceOf[String]), s"Expected all keys to be strings in partial assignment map. ${YamlReader.getNodeLocation(mappingNode)}")
        require(m.size == 2, s"Expected partial assignment map to have only exactly 2 elements!. ${YamlReader.getNodeLocation(mappingNode)}")
        val map = m.asInstanceOf[Map[String, Any]]

        require(map contains VALUE_KEY, s"Partial assignment map doesn't have \"$VALUE_KEY\" key!. ${YamlReader.getNodeLocation(mappingNode)}")
        require(map(VALUE_KEY).isInstanceOf[BigInt], s"Partial assignment \"$VALUE_KEY\" field is not a BigInt!. ${YamlReader.getNodeLocation(mappingNode)}")

        if map contains BITS_KEY then
          require(map(BITS_KEY).isInstanceOf[String], s"Expected \"$BITS_KEY\" key to be an string!. ${YamlReader.getNodeLocation(mappingNode)}")

          val bits = map(BITS_KEY).asInstanceOf[String]

          val parts = bits.split(":")

          if parts.length == 1 then
            require(parts(0).forall(p => p.isDigit)) //check that the number is numeric
            PartialAssignment(map(VALUE_KEY).asInstanceOf[BigInt], parts(0).toInt, parts(0).toInt, false)
          else if parts.length == 2 then
            require(parts(0).forall(p => p.isDigit) && parts(1).forall(p => p.isDigit)) //check that both numbers are numeric
            PartialAssignment(map(VALUE_KEY).asInstanceOf[BigInt], parts(0).toInt, parts(1).toInt, false)
          else
            throw new IllegalArgumentException(s"Expected bit positions to be 1 or 2 numbers separated by \":\". ${YamlReader.getNodeLocation(mappingNode)}")

        else if map contains BITS_LOCAL_KEY then
          require(map(BITS_LOCAL_KEY).isInstanceOf[String], s"Expected \"$BITS_LOCAL_KEY\" key to be an string!. ${YamlReader.getNodeLocation(mappingNode)}")

          val bits = map(BITS_LOCAL_KEY).asInstanceOf[String]

          val parts = bits.split(":")

          if parts.length == 1 then
            require(parts(0).forall(p => p.isDigit)) //check that the number is numeric
            PartialAssignment(map(VALUE_KEY).asInstanceOf[BigInt], parts(0).toInt, parts(0).toInt, true)
          else if parts.length == 2 then
            require(parts(0).forall(p => p.isDigit) && parts(1).forall(p => p.isDigit)) //check that both numbers are numeric
            PartialAssignment(map(VALUE_KEY).asInstanceOf[BigInt], parts(0).toInt, parts(1).toInt, true)
          else
            throw new IllegalArgumentException(s"Expected bit positions to be 1 or 2 numbers separated by \":\". ${YamlReader.getNodeLocation(mappingNode)}")

        else
          throw new IllegalArgumentException(s"Partial assignment map doesn't have \"$BITS_KEY\" nor \"$BITS_LOCAL_KEY\" key!. ${YamlReader.getNodeLocation(mappingNode)}")

      case other =>
        throw new IllegalArgumentException(s"Expected partial assignment to be a map, not ${mappingNode.getNodeType}. ${YamlReader.getNodeLocation(mappingNode)}")