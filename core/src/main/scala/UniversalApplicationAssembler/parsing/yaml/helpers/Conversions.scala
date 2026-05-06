package UniversalApplicationAssembler.parsing.yaml.helpers

import UniversalApplicationAssembler.helpers.{MultipleParameterMapping, ParametersDefinition, SingleParameterMapping, ParameterMapping}
import UniversalApplicationAssembler.parsing.yaml.YamlReader
import org.snakeyaml.engine.v2.nodes.MappingNode

object Conversions:

  def toParametersDefinition(mappingNode: MappingNode): ParametersDefinition =

    val mappingNodeScala = YamlReader.constructToScala(mappingNode)

    mappingNodeScala match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.forall(_.isInstanceOf[List[Any]]) =>
        val map = m.asInstanceOf[Map[String, List[Any]]]

        require(map.contains("values") && map.contains("mappings") && map.size == 2, s"Bad parameters MappingNode. ${YamlReader.getNodeLocation(mappingNode)}") //Check that everything is alright

        val datatypes = map("values") match
          case l: List[?] if l.forall(_.isInstanceOf[String]) => l.asInstanceOf[List[String]]
          case other => throw new IllegalArgumentException(s"Expected \"values\" to be a list of strings! ${YamlReader.getNodeLocation(mappingNode)}")

        val mappings: List[ParameterMapping] = map("mappings").map {
          case s: String => SingleParameterMapping(s)
          case l: List[?] if l.forall(_.isInstanceOf[String]) => MultipleParameterMapping(l.asInstanceOf[List[String]])
          case other => throw new IllegalArgumentException(s"Expected mappings to be either a string or list of strings, not $other!")
        }

        ParametersDefinition(datatypes, mappings)

      case other => throw new IllegalArgumentException(s"Expected ParametersDefinition to be a Map with string keys and lists as values! ${YamlReader.getNodeLocation(mappingNode)}")
