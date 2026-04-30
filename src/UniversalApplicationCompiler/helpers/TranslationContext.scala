package UniversalApplicationCompiler.helpers

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Groups all the classes used to create the TranslationContext tree
 */
sealed trait TranslationContext:
  var parent: Option[Node] = None //Parent will always be a Node, as they are the only ones who can have children.

/**
 * Represents a node of the TranslationContext tree, it has children which can be other Nodes or Leaves.
 */
case class Node() extends TranslationContext:
  //As it is not the final Node, it can have children
  val children: mutable.Map[String, TranslationContext] = mutable.Map.empty

  def addChild(child: TranslationContext, childName: String): Unit =
    if children.values.exists(_ eq child) then
      throw new IllegalArgumentException(s"Duplicate child!: ${child}")

    if child.parent.exists(_ != this) then
      throw new IllegalArgumentException("Child has already a parent!")

  def search(referenceString: String): TranslationContext =
    val references = referenceString.split(".")

    var currentTranslationContext: TranslationContext = this

    for reference <- references do

      currentTranslationContext match
        case n: Node => currentTranslationContext = n.children(reference)
        case other => throw new IllegalArgumentException("Expected a Node, not a Leaf!")

    currentTranslationContext

  def getScope: Map[String, TranslationContext] =

    var currentTranslationContext = this

    @tailrec
    def recursiveCall(node: Option[Node], current: Map[String, TranslationContext]): Map[String, TranslationContext] =
      node match
        case None => current
        case Some(parent) =>
          val updated = current ++ parent.children

          recursiveCall(node, updated)

    recursiveCall(parent, children.toMap)

/**
 * Represents a Leaf of the TranslationContext tree, it has a parent but no children.
 * @param leaf The leaf, which can be either a BitRange or a translation table (i.e. a map)
 */
case class Leaf(leaf: BitRange | Map[String, BigInt]) extends TranslationContext