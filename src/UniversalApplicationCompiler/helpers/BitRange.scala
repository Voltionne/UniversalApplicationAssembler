package UniversalApplicationCompiler.helpers

import scala.math._

case class BitRange(initialPos: Int, finalPos: Int) {
  val bits: Int = abs(initialPos - finalPos) - 1
}
