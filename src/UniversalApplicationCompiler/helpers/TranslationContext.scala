package UniversalApplicationCompiler.helpers

import UniversalApplicationCompiler.datatypes.BitVector

case class TranslationContext(leaf: BitRange | Map[String, BitVector]):
  val children: scala.collection.mutable.Map[String, TranslationContext] = scala.collection.mutable.Map() //start with a map that is empty

  def search(referenceString: String): BitRange | Map[String, BitVector] =
    val references = referenceString.split(".")

    var currentTranslationContext: TranslationContext = this

    for reference <- references do
      currentTranslationContext = currentTranslationContext.children(reference)

    currentTranslationContext.leaf
