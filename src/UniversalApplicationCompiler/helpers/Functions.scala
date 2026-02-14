package UniversalApplicationCompiler.helpers

object Functions {
  def gradientRange(from: Int, to: Int): Range =
    if from <= to then from to to else from to to by -1
}
