package UniversalApplicationAssembler.internal.parsing.isa

import UniversalApplicationAssembler.internal.datatypes.Path

sealed trait ParameterMapping
case class SingleParameterMapping(mappingLocation: Path) extends ParameterMapping
case class MultipleParameterMapping(mappingLocations: List[Path]) extends ParameterMapping

/**
 * Represents the parameters of an instruction
 * @param datatypes the datatypes of each of the parameters
 * @param mappings the fields where each parameter maps.
 */
case class ParametersDefinition(datatypes: List[Path], mappings: List[ParameterMapping]):
  require(datatypes.length == mappings.length)

  /**
   * Returns how many parameters
   * @return length
   */
  def length: Int = datatypes.length