package UniversalApplicationCompiler.helpers

sealed trait ParameterMapping
case class SingleParameterMapping(mappingLocation: String) extends ParameterMapping
case class MultipleParameterMapping(mappingLoctions: List[String]) extends ParameterMapping

/**
 * Represents the parameters of an instruction
 * @param values the datatypes of each of the parameters
 * @param mappings the fields where each parameter maps.
 */
case class ParametersDefinition(values: List[String], mappings: List[ParameterMapping]):
  require(values.length == mappings.length)

  /**
   * Returns how many parameters
   * @return length
   */
  def length: Int = values.length