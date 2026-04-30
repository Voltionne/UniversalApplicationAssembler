package UniversalApplicationCompiler.helpers

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Represents a single translation scope that is inseparable (i.e. atomic), which can be either a BitRange or a translation table. For allowing recursive scopes, there is the children dictionary.
 *
 * @param leaf The BitRange or translation table that sits on this scope.
 */
//For representing the top translation context, simply set some random BitRange that will never be called, as search() will always search between the childs.
case class TranslationContext(leaf: BitRange | Map[String, BigInt]):
  val children: mutable.Map[String, TranslationContext] = mutable.Map.empty //start with a map that is empty
  var parent: Option[TranslationContext] = None

  /**
   * Adds a child to the TranslationContext
   * @param child the child, also a TranslationContext
   * @param childName The name of the child, used during searching
   */
  def addChild(child: TranslationContext, childName: String): Unit =
    if children.values.exists(_ eq child) then
      throw new IllegalArgumentException(s"Duplicate child!: ${child}")

    if child.parent.exists(_ != this) then
      throw new IllegalArgumentException("Child has already a parent!")

    children(childName) = child
    child.parent = Some(this)

  /**
   * Searches recursively through all children to return a certain nested TranslationContext
   *
   * @param referenceString the string that indicates the wanted TranslationContext, each level separated with a point.
   * @return The wanted TranslationContext
   */
  def search(referenceString: String): TranslationContext =
    val references = referenceString.split(".")

    var currentTranslationContext = this

    for reference <- references do
      currentTranslationContext = currentTranslationContext.children(reference)

    currentTranslationContext

  /**
   * Gets a map with all children and parents that can be seen in the scope of this certain TranslationContext
   * @return An immutable map with the leaves in this scope.
   */
  def getScope: Map[String, TranslationContext] =

    var currentTranslationContext = this

    @tailrec
    def recursiveCall(translationContext: Option[TranslationContext], current: Map[String, TranslationContext]): Map[String, TranslationContext] =
      translationContext match
        case None => current
        case Some(parent) =>
          val updated = current ++ parent.children

          recursiveCall(translationContext, updated)

    recursiveCall(parent, children.toMap)