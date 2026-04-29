package UniversalApplicationCompiler.helpers

/**
 * Represents a single translation scope that is inseparable (i.e. atomic), which can be either a BitRange or a translation table. For allowing recursive scopes, there is the children dictionary.
 * @param leaf The BitRange or translation table that sits on this scope.
 */
case class TranslationContext(leaf: BitRange | Map[String, BigInt]):
  val children: scala.collection.mutable.Map[String, TranslationContext] = scala.collection.mutable.Map() //start with a map that is empty

  /**
   * Searches recursively through all children to return a certain nested TranslationContext
   *
   * @param referenceString the string that indicates the wanted TranslationContext, each level separated with a point.
   * @return The wanted TranslationContext
   */
  def search(referenceString: String): TranslationContext =
    val references = referenceString.split(".")

    var currentTranslationContext: TranslationContext = this

    for reference <- references do
      currentTranslationContext = currentTranslationContext.children(reference)

    currentTranslationContext