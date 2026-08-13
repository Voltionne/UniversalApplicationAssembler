package UniversalApplicationAssembler.internal.datatypes

import UniversalApplicationAssembler.internal.parsing.yaml.YamlReader
import org.snakeyaml.engine.v2.nodes.MappingNode

case class SymbolMap(map: Map[String, BigInt]):

  /**
   * Get the translation of a symbol as big int.
   * @param symbol The symbol to translate
   * @return The translated symbol
   */
  def apply(symbol: String): BigInt =
    map(symbol)

object SymbolMap:

  /**
   * Checks if a certain mapping node follows the format of a symbol map
   * @param mappingNode The mapping node to check
   * @return True if it follows the format, false if not
   */
  def isSymbolMap(mappingNode: MappingNode): Boolean =
    val nodeAsScala = YamlReader.constructToScala(mappingNode)

    nodeAsScala match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.forall(_.isInstanceOf[BigInt]) => true
      case other => false

  /**
   * Checks if a certain map follows the format of a symbol map
   * @param map The map to check
   * @return True if it follows the format, false if not
   */
  def isSymbolMap(map: Map[Any, Any]): Boolean =

    if map.keys.forall(_.isInstanceOf[String]) && map.values.forall(_.isInstanceOf[BigInt]) then
      true
    else
      false

  /**
   * Convert from a MappingNode to a SymbolMap
   * @param mappingNode The mapping node
   * @return The symbol map that the mapping node represents
   */
  def apply(mappingNode: MappingNode): SymbolMap =
    val nodeAsScala = YamlReader.constructToScala(mappingNode)

    nodeAsScala match
      case m: Map[?, ?] =>
        require(m.keys.forall(_.isInstanceOf[String]), s"Expected all keys to be strings in symbol map. ${YamlReader.getNodeLocation(mappingNode)}")
        require(m.values.forall(_.isInstanceOf[BigInt]), s"Expected all values to be BigInt in symbol map. ${YamlReader.getNodeLocation(mappingNode)}")
        SymbolMap(m.asInstanceOf[Map[String, BigInt]]) //Should work directly
      case other =>
        throw new IllegalArgumentException(s"Expected symbol map to be a map, not ${mappingNode.getNodeType}. ${YamlReader.getNodeLocation(mappingNode)}")