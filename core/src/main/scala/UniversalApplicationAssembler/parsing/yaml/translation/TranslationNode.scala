package UniversalApplicationAssembler.parsing.yaml.translation

import scala.annotation.tailrec
import scala.collection.mutable

case class TranslationNode(var bits: BigInt):

  /**
   * Represents the parent of the node
   */
  val parent: Option[TranslationNode] = None

  /**
   * Represents all sublevels
   */
  val children: mutable.Map[String, TranslationNode] = mutable.Map.empty

  /**
   * Represents the variables that have changed since the parent
   */
  val changes: mutable.Map[String, TranslationLeaf] = mutable.Map.empty

  def getTop: TranslationNode =

    @tailrec
    def recursiveCall(current: TranslationNode): TranslationNode =
      current.parent match
        case None => current
        case Some(parent) => recursiveCall(parent)

    recursiveCall(this)
    

