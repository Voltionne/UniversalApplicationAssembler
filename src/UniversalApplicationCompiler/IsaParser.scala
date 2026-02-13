package UniversalApplicationCompiler

import org.yaml.snakeyaml.Yaml
import java.nio.file.{Files, Paths}
import UniversalApplicationCompiler.helpers.FromJava

class IsaParser(val yamlConfigPath: String):
  val yamlData: Any =
    val yaml = new Yaml()
    val raw = yaml.load(Files.newInputStream(Paths.get(yamlConfigPath)))
    FromJava.convert(raw)
