package UniversalApplicationCompiler.helpers

case class TranslationContext(leaf: BitRange | Map[String, Int]):
  val children: scala.collection.mutable.Map[String, TranslationContext] = scala.collection.mutable.Map() //start with a map that is empty

  def search(referenceString: String): BitRange | Map[String, Int] =
    val references = referenceString.split(".")

    var currentTranslationContext: TranslationContext = this

    for reference <- references do
      currentTranslationContext = currentTranslationContext.children(reference)

    currentTranslationContext.leaf
