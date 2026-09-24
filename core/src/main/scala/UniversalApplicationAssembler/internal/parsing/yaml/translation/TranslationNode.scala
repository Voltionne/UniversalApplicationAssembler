package UniversalApplicationAssembler.internal.parsing.yaml.translation

import UniversalApplicationAssembler.internal.datatypes.Path

import scala.annotation.tailrec
import scala.collection.mutable

case class TranslationNode(var bits: BigInt):

  /**
   * Represents the parent of the node
   */
  var parent: Option[TranslationNode] = None

  /**
   * Represents all sublevels. Paths are in canonical form
   */
  val children: mutable.Map[Path, TranslationNode] = mutable.Map.empty

  /**
   * Represents the variables that have changed since the parent. Paths are in canonical form
   */
  val changes: mutable.Map[Path, TranslationLeaf] = mutable.Map.empty

  /**
   * Represents the name of the node
   */
  var name: String = ""

  /**
   * Adds a child to this node, which can be either another Node or a Leaf.
   *
   * @param child     The child to add.
   * @param childName The name to identify the child inside the children map.
   */
  def addChild(child: TranslationNode, childName: String): Unit =

    val childPath = Path(childName, this).toCanonical

    if children.contains(childPath) then
      throw new IllegalArgumentException(s"Duplicate child \"$childName\"!: $child")

    if child.parent.exists(_ != this) then
      throw new IllegalArgumentException("Child has already a parent!")

    children(childPath) = child
    child.parent = Some(this)
    child.name = childName

  /**
   * Tries to get a certain child, by name
   * @param childName The name of the child
   * @return True if it has that child
   */
  def getChild(childName: String): Option[TranslationNode] =

    val childPath = Path(childName, this).toCanonical

    children.get(childPath)

  /**
   * Returns the top node, searching recursively through parents
   *
   * @return The top node
   */
  def getTop: TranslationNode =

    @tailrec
    def recursiveCall(current: TranslationNode): TranslationNode =
      current.parent match
        case None => current
        case Some(parent) => recursiveCall(parent)

    recursiveCall(this)

  /**
   * Returns the canonical path from the top node to this node
   * @return the canonical path
   */
  def getPath: Path =

    parent match
      case None => Path.empty
      case Some(parent) =>
        parent.getPath :+ name
