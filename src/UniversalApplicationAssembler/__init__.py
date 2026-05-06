"""
Create easily custom assemblers specifying ISAs with a YAML configuration file
"""
import warnings

warnings.simplefilter("always", DeprecationWarning)
warnings.warn(
    "PyUAA is not longer maintained past version 0.1.2! It will recieve more support nor bug fixes"
    "It has been rewritten into \"UniversalApplicationAssembler\" in Scala",
    DeprecationWarning,
    stacklevel=2,
)

from .compiler import Assembler
version = "0.1.2"