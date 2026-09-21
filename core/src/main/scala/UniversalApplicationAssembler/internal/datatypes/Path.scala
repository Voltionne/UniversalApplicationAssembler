package UniversalApplicationAssembler.internal.datatypes

import UniversalApplicationAssembler.internal.parsing.yaml.translation.TranslationNode

/**
 * Represents a path inside a translation tree. It can be local, absolute or relative.
 * It includes members to manipulate between them and perform comparisons.
 *
 * It is best used with the "Translation" helper object functions
 * @param base The base of reference of the path
 * @param identifiers The path from the base of reference
 */
case class Path(base: List[String], identifiers: List[String]):

  /**
   * Checks if the path is absolute. A path is absolute if its base is the root and everything is identifiers from there
   * @return True if absolute
   */
  def isAbsolute: Boolean = base.isEmpty

  /**
   * Converts this path to absolute
   * @return The path as absolute
   */
  def toAbsolute: Path =
    Path(List.empty, base ++ identifiers)

  /**
   * Checks if the path is local. A path is local if it includes only one identifier
   * @return
   */
  def isLocal: Boolean = identifiers.length == 1

  /**
   * Converts this path to local
   * @return The path as local
   */
  def toLocal: Path =
    Path(base ++ identifiers.init, List(identifiers.last))

  /**
   * Checks if this path is relative. A path is relative if it is not local nor absolute, refers to an offset.
   * @return
   */
  def isRelative: Boolean =
    !isAbsolute && !isLocal

  /**
   * Checks whether a path is in canonical form.
   * Canonical form is when it has no identifiers (points to nothing) and only base
   * @return True if canonical
   */
  def isCanonical: Boolean = identifiers.isEmpty

  /**
   * Converts this path to its canonical form
   * @return The path as canonical
   */
  def toCanonical: Path =
    Path(base ++ identifiers, List.empty)

  /**
   * Appends a certain string to the path
   * @param other The sting to append
   * @return The path with the subdirectory appended
   */
  def :+(other: String): Path =
    Path(base, identifiers :+ other)

  //Equality based on canonical form
  override def equals(other: Any): Boolean =

    other match
      case other: Path =>
        toCanonical.base == other.toCanonical.base
      case _ => false

  override def hashCode(): Int =
    toCanonical.base.hashCode()

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
