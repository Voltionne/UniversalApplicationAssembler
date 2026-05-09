package UniversalApplicationAssembler.internal.parsing.yaml.translation

/**
 * A helper that includes useful functions related to translation.
 */
object Translation:

  /**
   * Searches a variable in current scope or from path and returns it optionally.
   * @param nameOrPath The name of the variable or its path
   * @param translationContext The current translation context
   * @return The TranslationLeaf that contains the variable
   */
  def searchLeaf(nameOrPath: String, translationContext: TranslationNode): Option[TranslationLeaf] =

    if translationContext.getScope.contains(nameOrPath) then
      Some(translationContext.getScope(nameOrPath))
    else
      translationContext.getTop.searchTranslationLeaf(nameOrPath)
