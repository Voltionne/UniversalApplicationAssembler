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

  /**
   * Reads a YAML file and returns it as a tree of nodes.
   * @param yamlConfigInputStream The input stream of the file
   * @return The top node of the tree
   */
  def readYamlFile(yamlConfigInputStream: InputStream): Node =
    val compose = new Compose(settings)
    compose.composeInputStream(yamlConfigInputStream).orElseThrow()