package UniversalApplicationAssembler.internal.parsing.yaml

import UniversalApplicationAssembler.internal.engine.{FixedIntConstructor, FixedScalarResolver, PublicConstructor}
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.api.{ConstructNode, LoadSettings}
import org.snakeyaml.engine.v2.nodes.{Node, Tag}
import org.snakeyaml.engine.v2.resolver.ScalarResolver
import org.snakeyaml.engine.v2.schema.CoreSchema

import java.io.InputStream
import java.util

/**
 * Provides methods for reading a YAML file using SnakeYAML-engine
 */
object YamlReader:

  private val settings: LoadSettings =

    val coreSchema: CoreSchema = new CoreSchema(): //Add overwritten ScalarNode resolver
      override def getScalarResolver: ScalarResolver = FixedScalarResolver(true)

    val constructs = new util.HashMap[Tag, ConstructNode]()

    constructs.put(Tag.INT, new FixedIntConstructor()) //Add overwritten Tag.INT constructor

    LoadSettings.builder().setTagConstructors(constructs).setSchema(coreSchema).build()


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
  def constructToScala(node: Node): Any = publicConstructor.constructToScala(node) //This exposes the publicConstructor

  /**
   * Returns a pretty text with the location of a node in the original YAML file. Useful for debugging.
   * @param node The node to find its location.
   * @param addExtraText Whatever add an introduction or return the plain location directly. True by default.
   * @return The location of the node.
   */
  def getNodeLocation(node: Node, addExtraText: Boolean = true): String =
    val mark = node.getStartMark.orElseThrow()

    if addExtraText then
      s"Location: line ${mark.getLine + 1}, column ${mark.getColumn + 1}"
    else
      s"line ${mark.getLine + 1}, column ${mark.getColumn + 1}"