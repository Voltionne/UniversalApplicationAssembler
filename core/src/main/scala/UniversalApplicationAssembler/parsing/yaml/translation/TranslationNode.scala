package UniversalApplicationAssembler.parsing.yaml.translation

import scala.annotation.tailrec
import scala.collection.mutable

case class TranslationNode(var bits: BigInt):

  /**
   * Represents the parent of the node
   */
  var parent: Option[TranslationNode] = None

  /**
   * Represents all sublevels
   */
  val children: mutable.Map[String, TranslationNode] = mutable.Map.empty

  /**
   * Represents the variables that have changed since the parent
   */
  val changes: mutable.Map[String, TranslationLeaf] = mutable.Map.empty

  /**
   * Adds a child to this node, which can be either another Node or a Leaf.
   *
   * @param child     The child to add.
   * @param childName The name to identify the child inside the children map.
   */
  def addChild(child: TranslationNode, childName: String): Unit =
    if children.contains(childName) then
      throw new IllegalArgumentException(s"Duplicate child!: $child. $childName")

    if child.parent.exists(_ != this) then
      throw new IllegalArgumentException("Child has already a parent!")

    children(childName) = child
    child.parent = Some(this)

  /**
   * Searches a certain Leaf between all the children, recursively.
   *
   * @param path The reference string that identifies the wanted leaf, separated each level with a dot.
   * @return The wanted leaf.
   */
  def searchTranslationLeaf(path: String): Option[TranslationLeaf] =
    val pathSplit = path.split('.')

    val optionTranslationNode = pathSplit.init.foldLeft(Option(this)) { (current, key) =>
      current.flatMap(node => node.children.get(key))
    }

    optionTranslationNode.flatMap(node => node.changes.get(pathSplit.last))

  /**
   * Gets all visible Leaves from the scope in this current Node.
   * @return A map with all the leaves
   */
  def getScope: Map[String, TranslationLeaf] =

    var currentTranslationContext = this

    @tailrec
    def recursiveCall(node: Option[TranslationNode], current: Map[String, TranslationLeaf]): Map[String, TranslationLeaf] =
      node match
        case None => current
        case Some(parent) =>

          val parentLeaves = parent.changes

          val updated = parentLeaves ++ current

          recursiveCall(parent.parent, updated.toMap)

    recursiveCall(parent, changes.toMap)

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


