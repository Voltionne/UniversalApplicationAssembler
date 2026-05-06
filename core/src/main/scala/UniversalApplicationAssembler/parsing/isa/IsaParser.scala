package UniversalApplicationAssembler.parsing.isa

import UniversalApplicationAssembler.datatypes.{BitRange, Utils}
import UniversalApplicationAssembler.parsing.yaml.YamlReader
import UniversalApplicationAssembler.parsing.yaml.translation.{TranslationLeaf, TranslationNode}
import org.snakeyaml.engine.v2.nodes.{MappingNode, Node, ScalarNode}

import java.io.InputStream

class IsaParser(val yamlConfigInputStream: InputStream):

  private val yamlTopNode: MappingNode =
    YamlReader.readYamlFile(yamlConfigInputStream) match
      case mappingNode: MappingNode => mappingNode
      case other => throw new IllegalArgumentException(s"Expected top node to be a MappingNode, not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

  def parse(): TranslationNode =

    val currentTranslationContext = TranslationNode(-1) //create the top translation node

    parseRecursivelyFirstPass(yamlTopNode, currentTranslationContext)

    currentTranslationContext

  //-----------------------------------------
  // FIRST PASS -> Resolve declarations
  //-----------------------------------------

  private def parseRecursivelyFirstPass(currentNode: MappingNode, currentTranslationContext: TranslationNode): Unit =

    currentNode.getValue.forEach { nodeTuple => //Iterate through sublevels

      val stringKey =
        nodeTuple.getKeyNode match
          case scalarNode: ScalarNode => scalarNode.getValue
          case other => throw new IllegalArgumentException(s"Expected key to be a ScalarNode (a string), not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

      if stringKey == "bits" then
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => currentTranslationContext.bits = BigInt(scalarNode.getValue) //Sets the number of bits
          case other => throw new IllegalArgumentException(s"Expected bits to be a ScalarNode (a BigInt), not ${other.getNodeType}. ${YamlReader.getNodeLocation(other)}")

      else if stringKey == "instructions" then
        () //Skip instructions -> left for second pass

      else
        //Match based on value, given that all special keys have already been revised
        nodeTuple.getValueNode match
          case scalarNode: ScalarNode => //This is assignment OR declaration
            val value = YamlReader.constructToScala(scalarNode)

            value match
              case s: String => //Declaration

                //Important checks to be performed to verify this is a good definition:
                //1. Is not a full path (definitions are always local) -> check if no dots in the name
                require(!stringKey.contains('.'))

                //There exists a BitRange in the scope that has the same name
                if currentTranslationContext.getScope.contains(stringKey) then
                  throw new IllegalArgumentException(s"Bit Range \"$stringKey\" is already defined!. ${YamlReader.getNodeLocation(scalarNode)}")
                else
                  //CREATE NEW BITRANGE
                  val split = s.split(':')

                  if split.length == 1 then
                    currentTranslationContext.changes(stringKey) = TranslationLeaf(
                      BitRange(split(0).toInt)
                    )
                  else if split.length == 2 then
                    currentTranslationContext.changes(stringKey) = TranslationLeaf(
                      BitRange(split(0).toInt, split(1).toInt)
                    )
                  else
                    throw new IllegalArgumentException(s"Expected 1 or 2 bit position indications, not ${split.length}! ${YamlReader.getNodeLocation(scalarNode)}")

              case i: BigInt => () //Assignment -> Ignore (left to second round)
              case other => throw new IllegalArgumentException(s"Found not recognized value for assignment or declaration. ${YamlReader.getNodeLocation(scalarNode)}")

          case mappingNode: MappingNode => //This is sublevel OR assignment OR translation table

            if Helper.isSetMap(mappingNode) then //Assignment
              () //Ignore (left to second round)
            else if Helper.isTranslationTable(mappingNode) then //Translation Table (i.e. declaration)

              if currentTranslationContext.getScope.contains(stringKey) then
                throw new IllegalArgumentException(s"Translation Table \"$stringKey\" is already defined!. ${YamlReader.getNodeLocation(mappingNode)}")
              else
                //CREATE NEW TRANSLATION TABLE
                val translationTable = YamlReader.constructToScala(mappingNode).asInstanceOf[Map[String, BigInt]] //This shouldn't fail because it is a translation table. Though, it is not very secure. May remake in the future
                currentTranslationContext.changes(stringKey) = TranslationLeaf(
                  translationTable
                )

            else //Sublevel

              //New sublevel -> explore but create first a new translation context
              val newTranslationContext = TranslationNode(currentTranslationContext.bits) //Propagate bits
              currentTranslationContext.addChild(newTranslationContext, stringKey)

              parseRecursivelyFirstPass(mappingNode, currentTranslationContext)

          case other => throw new IllegalArgumentException(s"Expected all keys to lead to tables (except \"instructions\" key). ${YamlReader.getNodeLocation(other)}")
    }

/**
 * Small helper object that includes some snippets of code for checking fast
 */
object Helper:

  /**
   * Checks if a MappingNode is a set map (or assignment map)
   * @param mappingNode The mapping node to check
   * @return True or false depending on if it is or not a set map
   */
  def isSetMap(mappingNode: MappingNode): Boolean =

    val nodeAsScala = YamlReader.constructToScala(mappingNode)

    nodeAsScala match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) =>
        val map = m.asInstanceOf[Map[String, Any]]
        Utils.isSetMap(map)
      case other => false

  /**
   * Checks if a MappingNode is a translation table
   * @param mappingNode The mapping node to check
   * @return True or false depending on if ti is or not a translation table
   */
  def isTranslationTable(mappingNode: MappingNode): Boolean =

    val nodeAsScala = YamlReader.constructToScala(mappingNode)

    nodeAsScala match
      case m: Map[?, ?] if m.keys.forall(_.isInstanceOf[String]) && m.values.forall(_.isInstanceOf[BigInt]) => true
      case other => false
