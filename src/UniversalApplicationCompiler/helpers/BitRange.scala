package UniversalApplicationCompiler.helpers

import scala.math._

case class BitRange(a: Int, b: Int):
  val bits: Int = abs(a - b) + 1
  val value: String = "0" * bits

  def setPartialValue(setDict: Map[String, Any]): Unit = ???
  def setFullValue(value: Int): Unit = ???
  def checkValue: Boolean = ???
