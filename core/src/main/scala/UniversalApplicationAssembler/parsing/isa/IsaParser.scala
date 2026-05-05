package UniversalApplicationAssembler.parsing.isa

import UniversalApplicationAssembler.parsing.yaml.YamlReader
import UniversalApplicationAssembler.parsing.{InstructionTemplate, TranslationNode, TranslationLeaf}
import org.snakeyaml.engine.v2.nodes.{Node, ScalarNode, MappingNode, SequenceNode}
import org.snakeyaml.engine.v2.api.LoadSettings

import java.io.InputStream

class IsaParser(val yamlConfigInputStream: InputStream):

  private val loadSettings: LoadSettings = LoadSettings.builder().build()

  val yamlNodes: Node = YamlReader.readYamlFile(yamlConfigInputStream)

  var instructions = Array[InstructionTemplate]()

  def parse(): TranslationNode =

    val currentTranslationContext = TranslationNode(-1)

    parseFirstPass(currentTranslationContext)
    currentTranslationContext

  //-----------------------------------------
  // FIRST PASS -> Gets all definitions and assignments
  //-----------------------------------------

  private def parseFirstPass(currentTranslationContext: TranslationNode): Unit = ???

  //Top-level parsers
  private def parseRecursivelyFirstPass(currentLevel: Node, currentTranslationContext: TranslationNode): Unit = ???

  private def parseLevelFirstPass(currentLevel: Node, currentTranslationContext: TranslationNode): Array[Node] = ???