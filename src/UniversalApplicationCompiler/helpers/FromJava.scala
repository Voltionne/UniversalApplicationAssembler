package UniversalApplicationCompiler.helpers

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

/**
 * Convert things from Java to Scala (as Scala uses the JDK and some libraries use datatypes of Java)
 */
object FromJava:

  /**
   * Automatically converts a YAML file maps, sets, and lists from Java to Scala types.
   * @param value the value to be parsed
   * @return the converted type
   */
  def convert(value: Any): Any = value match
    
    case m: java.util.Map[?, ?] =>
      mutable.LinkedHashMap.from(m.asScala.view.map { case (k, y) => convert(k) -> convert(y) })
    case l: java.util.List[?] =>
      l.asScala.view.map(convert).toList
    case s: java.util.Set[?] =>
      s.asScala.view.map(convert).toSet
    case other => other

