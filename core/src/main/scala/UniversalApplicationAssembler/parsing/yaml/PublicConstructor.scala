package UniversalApplicationAssembler.parsing.yaml

import UniversalApplicationAssembler.helpers.Conversions
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.constructor.StandardConstructor
import org.snakeyaml.engine.v2.nodes.Node

/**
 * A wrapper around StandardConstructor to expose some extra methods that are usually not available publically.
 *
 * @param settings Configuration for the constructor
 */
class PublicConstructor(settings: LoadSettings) extends StandardConstructor(settings):

  /**
   * Converts a certain Node to its respective Scala 3 datatype.
   *
   * This is implemented in its own "PublicConstructor" class given that StandardConstructor does not expose a method to do it publically.
   * @param node The node to be converted
   * @return The datatype used
   */
  def constructToScala(node: Node): Any =
    val javaInstance = constructObject(node)
    Conversions.convertFromJava(javaInstance)
