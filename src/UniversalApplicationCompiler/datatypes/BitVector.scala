package UniversalApplicationCompiler.datatypes

final case class BitVector private(value: BigInt, width: Int):

  def +(other: BitVector): BitVector =
    require(width == other.width)
    BitVector(value + other.value, width=width)

  def -(other: BitVector): BitVector =
    require(width == other.width)
    BitVector(value - other.value, width=width)

  def *(other: BitVector): BitVector =
    require(width == other.width)
    BitVector(value * other.value, width = width)

  def /(other: BitVector): BitVector =
    require(width == other.width)
    BitVector(value / other.value, width = width)

object BitVector:
  def apply(value: BigInt, width: Int): BitVector =
    require(width > 0)
    val normalizedValue: BigInt = value & ((BigInt(1) << width) - 1)
    new BitVector(normalizedValue, width)
