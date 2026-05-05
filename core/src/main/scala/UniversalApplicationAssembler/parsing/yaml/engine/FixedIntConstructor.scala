package UniversalApplicationAssembler.parsing.yaml.engine

import org.snakeyaml.engine.v2.api.ConstructNode
import org.snakeyaml.engine.v2.nodes.{Node, ScalarNode}

class FixedIntConstructor extends ConstructNode:

  override def construct(node: Node): AnyRef =

    val rawValue = node.asInstanceOf[ScalarNode].getValue.replace("_", "")
    val isNegative = rawValue.startsWith("-")
    val unsignedValue = if (isNegative || rawValue.startsWith("+")) rawValue.substring(1) else rawValue

    val radix =
      if unsignedValue.startsWith("0b") then 2
      else if unsignedValue.startsWith("0x") then 16
      else 10

    val cleanString = radix match
      case 2 | 16 => unsignedValue.substring(2)
      case other => unsignedValue

    val parsed = java.math.BigInteger(cleanString, radix)
    val finalVal = if isNegative then java.math.BigInteger.valueOf(-1).multiply(parsed) else parsed

    //Skip down casting
    finalVal
