package UniversalApplicationAssembler.parsing.yaml.engine

import org.snakeyaml.engine.v2.nodes.Tag
import org.snakeyaml.engine.v2.resolver.CoreScalarResolver

import java.util.regex.Pattern

class FixedScalarResolver(supportMerge: Boolean) extends CoreScalarResolver(supportMerge):

  private val BINARY_PATTERN = Pattern.compile("^[-+]?0b[0-1_]+$")

  private val HEX_PATTERN = Pattern.compile("^[-+]?0x[0-9a-fA-F_]+$")

  override def addImplicitResolvers(): Unit =
    super.addImplicitResolvers()

    addImplicitResolver(Tag.INT, BINARY_PATTERN, "-+0")
    addImplicitResolver(Tag.INT, HEX_PATTERN, "-+0")
