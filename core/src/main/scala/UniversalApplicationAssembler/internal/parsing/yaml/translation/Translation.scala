package UniversalApplicationAssembler.internal.parsing.yaml.translation

import UniversalApplicationAssembler.internal.datatypes.Path

import scala.annotation.tailrec

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

  /**
   * Returns the full path, from top to bottom, of a certain variable in a certain translation context. NOTE: such variable is not checked if it is contained in the translation context.
   * @param name The name of the variable
   * @param translationContext The current translation context
   * @return The full path, a string separated with dots.
   */
  def getFullPath(name: String, translationContext: TranslationNode): String =

    var listPath: List[String] = List(name)

    @tailrec
    def recursiveCall(node: TranslationNode): Unit =
      val key = node.parent.flatMap { parent =>
        parent.children.collectFirst {
          case (key, child) if child eq node => key
        }
      }

      key match
        case Some(string) =>
          listPath = string :: listPath
          recursiveCall(node.parent.get) //SHOULD 100% RETURN, as if there is key, that means there is parent
        case None => () //Do nothing

    recursiveCall(translationContext)

    listPath.mkString(".")

  /**
   * Tries to search a certain leaf given its path. It takes into account the relation between the path and the current
   * translation context. That is, if the path is absolute, it searches from top node. If local, it searches from current
   * scope.
   *
   * Currently, relative paths are not supported (this is a UAA-wide limitation)
   *
   * If the leaf is not found (i.e. returns None) it should be considered UNREACHABLE from current context and given
   * that particular path. That means this function is also the authority on visibility. Nevertheless, in case of not
   * finding and even if introduced a relative path (which will fail 100%) it returns None without throwing error. The
   * caller is responsible for any necessary handling.
   * @param path The path to the leaf
   * @param translationContext The leaf requested, if found.
   * @return
   */
  def search(path: Path, translationContext: TranslationNode): Option[TranslationLeaf] =

    if path.isAbsolute then //Search from the top node
      ???
    else if path.isLocal then //Search from current scope
      ???
    else //Is relative -> None
      None
