package UniversalApplicationAssembler

import UniversalApplicationAssembler.parsing.{Node, Leaf, TranslationContext}

def visualizeNodes(node: Node, identifier: String = ""): Unit =

  println(" │" * (identifier.split('.').length - 1) + " ├" + identifier + " " + node.children.toString() + " BITS: " + node.bits.toString)

  val nodeChildren = node.children.collect {
    case (k, node: Node) => (k, node)
  }

  for (key, node) <- nodeChildren do
    visualizeNodes(node, identifier + "." + key)
