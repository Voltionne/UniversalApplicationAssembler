# Universal Application Assembler (UAA) v0.1

import os as _os
import yaml as _yaml

from helpers import Value as _Value
from helpers import InstructionTemplate as _InstructionTemplate
from helpers import TranslationContext as _TranslationContext

class Assembler:

    def __init__(self, source_file: _os.PathLike = None, auto_update: bool = True):

        """
        Creates an instance of the assembler.
        
        :param source_file: A .toml file specifying the ISA specification, by default none is specified (can add a source later setting Assembler.source manually)
        :type source_file: _os.PathLike
        :param auto_update: If after adding a source file it automatically updates the assembler to count it.
        :type auto_update: bool
        """

        #general variables
        self.auto_update = auto_update

        #the famous source
        self.source = source_file

        self.bits = None
        self.instructions: dict[str, _InstructionTemplate] = {}
        self.translation_context: _TranslationContext = None


    @property
    def source(self):

        return self._source
    
    @source.setter
    def source(self, source_file: _os.PathLike):

        self._source = source_file

        if self.auto_update:
            self.update()

    def update(self):

        """
        Refreshes the assembler so that all sources are taken in to account
        """

        assert self.source, f"No source defined!"

        self.global_values = []
        self.instructions: dict[str, _InstructionTemplate] = {}

        with open(self.source) as file:

            yaml_config = _yaml.safe_load(file) #read the config

            self._preparser(yaml_config)

    def _preparser(self, yaml_config):

        """
        INTERNAL FUNCTION. Does all the assertions for starting correctly the configuration
        
        :param yaml_config: The yaml config
        """

        assert isinstance(yaml_config, dict), "Config file yaml is not a dictionary!"

        pass