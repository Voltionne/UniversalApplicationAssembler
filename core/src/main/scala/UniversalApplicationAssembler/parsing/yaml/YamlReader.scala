package UniversalApplicationAssembler.parsing.yaml

import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.nodes.Node

import java.io.InputStream

/**
 * Provides methods for reading a YAML file using SnakeYAML-engine
 */
object YamlReader:

  private val settings: LoadSettings = LoadSettings.builder().build() //Default arguments are nice enough

  private val publicConstructor: PublicConstructor = PublicConstructor(settings)

  /**
   * Reads a YAML file and returns it as a tree of nodes.
   * @param yamlInputStream The input stream of the file
   * @return The top node of the tree
   */
  def readYamlFile(yamlInputStream: InputStream): Node =
    val compose = new Compose(settings)
    compose.composeInputStream(yamlInputStream).orElseThrow()

  /**
   * Converts a certain YAML node into a Scala datatype
   * @param node The node to convert
   * @return The value as a Scala datatype.
   */
  def constructToScala(node: Node): Any = publicConstructor.constructToScala(node)