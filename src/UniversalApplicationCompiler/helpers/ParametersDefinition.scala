package UniversalApplicationCompiler.helpers

/**
 * Represents the parameters of a function
 * @param values the datatypes of each of the parameters
 * @param mappings the fields where each parameter maps.
 */
case class ParametersDefinition(values: List[String], mappings: List[String | List[String]]):
  require(values.length == mappings.length)
  
  def length: Int = values.length