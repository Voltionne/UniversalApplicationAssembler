package UniversalApplicationAssembler

import UniversalApplicationAssembler.parsing.yaml.translation.TranslationNode

def visualizeNodes(node: TranslationNode, identifier: String = ""): Unit =

  println(" │" * (identifier.split('.').length - 1) + " ├" + identifier + " " + node.children.toString() + " BITS: " + node.bits.toString)

  for (key, node) <- node.children do
    visualizeNodes(node, identifier + "." + key)
