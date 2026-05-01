package UniversalApplicationAssembler.parsing

import UniversalApplicationAssembler.datatypes.BitRange

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Groups all the classes used to create the TranslationContext tree.
 */
sealed trait TranslationContext:
  var parent: Option[Node] = None //Parent will always be a Node, as they are the only ones who can have children.

/**
 * Represents a node of the TranslationContext tree, it has children which can be other Nodes or Leaves.
 */
case class Node(var bits: BigInt) extends TranslationContext:
  //As it is not the final Node, it can have children
  val children: mutable.Map[String, TranslationContext] = mutable.Map.empty

  /**
   * Adds a child to this node, which can be either another Node or a Leaf.
   * @param child The child to add.
   * @param childName The name to identify the child inside the children map.
   */
  def addChild(child: TranslationContext, childName: String): Unit =
    if children.contains(childName) then
      throw new IllegalArgumentException(s"Duplicate child!: ${child}")

    if child.parent.exists(_ != this) then
      throw new IllegalArgumentException("Child has already a parent!")
      
    children(childName) = child
    child.parent = Some(this)

  /**
   * Searches a certain Leaf between all the children, recursively.
   * @param referenceString The reference string that identifies the wanted leaf, separated each level with a dot.
   * @return The wanted leaf.
   */
  def searchLeaf(referenceString: String): Leaf =
    val references = referenceString.split(".")

    var currentTranslationContext: TranslationContext = this

    for reference <- references do

      currentTranslationContext match
        case n: Node => currentTranslationContext = n.children(reference)
        case other => throw new IllegalArgumentException("Expected a Node, not a Leaf! For finding a subreference")
    
    currentTranslationContext match
      case n: Node => throw new IllegalArgumentException("Expected a Leaf, not a Node! For returning")
      case l: Leaf => l

  /**
   * Gets all visible Leaves from the scope in this current Node.
   * @return A map with all the leaves
   */
  def getScope: Map[String, Leaf] =

    var currentTranslationContext = this

    @tailrec
    def recursiveCall(node: Option[Node], current: Map[String, Leaf]): Map[String, Leaf] =
      node match
        case None => current
        case Some(parent) =>

          val parentLeaves = parent.children.collect {
            case (k, leaf: Leaf) => k -> leaf
          }

          val updated = parentLeaves ++ current

          recursiveCall(parent.parent, updated.toMap)

    val thisLeaves = children.collect {
      case (k, leaf: Leaf) => k -> leaf
    }

    recursiveCall(parent, thisLeaves.toMap)

  /**
   * Returns the top node, searching recursively through parents
   * @return The top node
   */
  def getTop: Node =
    
    @tailrec
    def recursiveCall(current: Node): Node =
      current.parent match
        case None => current
        case Some(parent) => recursiveCall(parent)
        
    recursiveCall(this)

/**
 * Represents a Leaf of the TranslationContext tree, it has a parent but no children.
 * @param leaf The leaf, which can be either a BitRange or a translation table (i.e. a map)
 */
case class Leaf(leaf: BitRange | Map[String, BigInt]) extends TranslationContext