package UniversalApplicationAssembler.internal.datatypes

import UniversalApplicationAssembler.internal.parsing.yaml.translation.TranslationNode

case class FullPath(identifiers: List[String])

object FullPath:

  /**
   * From a certain local path and the current translation context it constructs a FullPath
   * @param localPath The local path, as a string where each "." represents a scope separation
   * @param translationContext The current translation context
   * @return The FullPath the string represents
   */
  def apply(localPath: String, translationContext: TranslationNode): FullPath =

    require(localPath.head != '.' && localPath.last != '.', s"Path \"$localPath\" is not supported because it has trailing dots")

    val scopes = localPath.split('.')
    require(!scopes.isEmpty, s"Expected path \"$localPath\" to not be empty")

    val scopesExcludingLeaf = scopes.init

    //Iterate from the top to bottom to check if such path exists
    var current = translationContext.getTop
    for scope <- scopesExcludingLeaf do

      require(current.children.contains(scope), s"There is no scope called \"$scope\" in the context of parsing path \"$localPath\"")
      current = current.children(scope)

    //If nothing failed until here, this is a 100% valid path:
    FullPath(scopes.toList)