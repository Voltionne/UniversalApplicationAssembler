package UniversalApplicationAssembler.internal.datatypes

import UniversalApplicationAssembler.internal.parsing.yaml.translation.TranslationNode

case class Path(identifiers: List[String], base: List[String]):

  /**
   * Checks if the path is absolute
   * @return True if absolute
   */
  def isAbsolute: Boolean = base.isEmpty

  /**
   * Converts the path to absolute
   * @return The path as absolute
   */
  def getAbsolute: Path =
    Path(base ++ identifiers, List.empty)

  /**
   * Checks if the path is fully local
   * @return True if local
   */
  def isLocal: Boolean = identifiers.length == 1

  /**
   * Converts the path to local
   * @return The path as local
   */
  def getLocal: Path =
    Path(List(identifiers.last), base ++ identifiers.init)

  /**
   * Checks if the path is relative
   * @return True if relative
   */
  def isRelative: Boolean =
    !isAbsolute && !isLocal

  /**
   * Converts the whole path to a base
   * @return The path as a base
   */
  def rebase: Path =
    Path(List.empty, base ++ identifiers)

  /**
   * Returns the path but one scope lower
   * @return The path one scope lower
   */
  def moveDown: Path =
    Path(identifiers.tail, base :+ identifiers.head)

  def +(other: Path): Path =
    require(base == other.base, s"Trying to add two paths with different bases!: Path 1: $this Path 2: $other")

    Path(identifiers ++ other.identifiers, base)

  def :+(other: String): Path =
    Path(identifiers :+ other, base)

  override def equals(other: Any): Boolean =

    other match
      case other: Path =>
        rebase.base == other.rebase.base
      case _ => false

  override def hashCode(): Int =
    rebase.base.hashCode()

object Path:

  /**
   * Builds a Path from a string path and the current translation context. Conserves the base of the path (i.e. if the path was absolute
   * it will continue to be it, the same if local).
   * @param stringPath The string that represents the path
   * @param currentTranslationContext The current translation context, used as base
   * @return The path the String path represents
   */
  def apply(stringPath: String, currentTranslationContext: TranslationNode): Path =

    /*
    2 cases:
    1. The path is absolute
    2. The path is local
     */

    if stringPath contains '.' then //100% absolute

      require(stringPath.head != '.' && stringPath.last != '.', s"Absolute path \"$stringPath\" contains trailing dots!")

      val scopes = stringPath.split('.')

      Path(scopes.toList, List.empty)
    else //100% local

      Path(List(stringPath), currentTranslationContext.getPath.identifiers)

  /**
   * Returns an empty path
   * @return An empty path
   */
  def empty: Path =
    Path(List.empty, List.empty)