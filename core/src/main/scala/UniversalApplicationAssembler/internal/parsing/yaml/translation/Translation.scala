package UniversalApplicationAssembler.internal.parsing.yaml.translation

import UniversalApplicationAssembler.internal.datatypes.Path

import scala.annotation.tailrec

/**
 * A helper that includes useful functions related to translation.
 */
object Translation:

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

    //Searches first on scope, if there is some change, then on absolute if it fails.

    if path.isAbsolute then //It may adopt, yet, the local value first.
      val possibleA = searchLocal(path, translationContext)

      val possibleB = searchAbsolute(path, translationContext)

      if possibleA.isEmpty then
        possibleB
      else
        possibleA

    else if path.isLocal then //Search from current scope
      searchLocal(path, translationContext)

    else //Is relative -> None
      None

  private def searchAbsolute(path: Path, translationContext: TranslationNode): Option[TranslationLeaf] =

    val finalContext = path.identifiers.init.foldLeft(Option(translationContext.getTop)) {
      (current, childName) => current.flatMap(_.getChild(childName))
    }

    finalContext.flatMap(_.changes.get(path))

  private def searchLocal(path: Path, translationContext: TranslationNode): Option[TranslationLeaf] =

    var currentTranslationContext = this

    @tailrec
    def recursiveCall(node: Option[TranslationNode], current: Map[String, TranslationLeaf]): Map[String, TranslationLeaf] =
      node match
        case None => current
        case Some(parent) =>

          val parentLeaves = parent.changes

          //Get only the proper name
          val updated = parentLeaves.map((path, leaf) => path.toCanonical.base.last -> leaf) ++ current

          recursiveCall(parent.parent, updated.toMap)

    val visibleChildren = recursiveCall(Some(translationContext), Map.empty)

    visibleChildren.get(path.identifiers.last)