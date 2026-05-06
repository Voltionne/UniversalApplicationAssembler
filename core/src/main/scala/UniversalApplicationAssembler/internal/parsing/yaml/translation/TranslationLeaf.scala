package UniversalApplicationAssembler.internal.parsing.yaml.translation

import UniversalApplicationAssembler.internal.datatypes.BitRange

/**
 * Represents a Leaf of the TranslationContext tree, it has a parent but no children.
 *
 * @param leaf The leaf, which can be either a BitRange or a translation table (i.e. a map)
 */
case class TranslationLeaf(leaf: BitRange | Map[String, BigInt])
