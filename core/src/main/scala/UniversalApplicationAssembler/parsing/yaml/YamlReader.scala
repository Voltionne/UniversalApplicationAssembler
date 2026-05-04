package UniversalApplicationAssembler.parsing.yaml

import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.nodes.{Node, ScalarNode}

import java.io.InputStream

object YamlReader:

  private val settings: LoadSettings = LoadSettings.builder().build() //Default arguments are nice enough

  def readYamlFile(yamlConfigInputStream: InputStream): Node =
    val compose = new Compose(settings)
    compose.composeInputStream(yamlConfigInputStream).orElseThrow()