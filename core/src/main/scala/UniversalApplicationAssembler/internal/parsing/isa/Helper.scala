package UniversalApplicationAssembler.internal.parsing.isa

import UniversalApplicationAssembler.internal.datatypes.PartialAssignment
import UniversalApplicationAssembler.internal.parsing.yaml.YamlReader
import org.snakeyaml.engine.v2.nodes.MappingNode

/**
 * Small helper object that includes some snippets of code for checking fast
 */
object Helper:

  /**
   * Checks if a MappingNode is a translation table
   * @param mappingNode The mapping node to check
   * @return True or false depending on if ti is or not a translation table
   */
  def isTranslationTable(mappingNode: MappingNode): Boolean =

    val nodeAsScala = YamlReader.constructToScala(mappingNode)

    nodeAsScala match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.forall(_.isInstanceOf[BigInt]) => true
      case other => false
