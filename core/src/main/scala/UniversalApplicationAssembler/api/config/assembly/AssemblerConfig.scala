package UniversalApplicationAssembler.api.config.assembly

/**
 * Stores the configuration of the assembler
 * @param startAddress The starting address of the code (i.e. the PC of the first instruction), by default is zero
 * @param useRelativeAddresses If tags use relative offsets addresses instead of absolute addresses, making code
 *                             location-agnostic. By default, is true
 */
case class AssemblerConfig(startAddress: Int = 0, useRelativeAddresses: Boolean = true)
