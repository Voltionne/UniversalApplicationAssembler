package UniversalApplicationAssembler.helpers

/**
 * Nice functions to save coding time!
 */
object Functions:

  /**
   * Returns a clean integer numerical range iterator from "from" to "to"
   * @param from The initial value
   * @param to The final value
   * @return the range
   */
  def gradientRange(from: Int, to: Int): Range = if from <= to then from to to else from to to by -1