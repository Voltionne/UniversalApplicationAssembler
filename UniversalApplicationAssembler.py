# Universal Application Assembler (UAA) v0.1

import os as _os
import yaml as _yaml

from helpers import Value as _Value
from helpers import InstructionTemplate as _InstructionTemplate

class Assembler:

    def __init__(self, source_file: _os.PathLike = None, auto_update: bool = True):

        """
        Creates an instance of the assembler.
        
        :param source_file: A .toml file specifying the ISA specification, by default none is specified (can add sources later using add_source())
        :type source_file: _os.PathLike
        :param auto_update: If after adding a source file it automatically updates the assembler to count it.
        :type auto_update: bool
        """

        self.sources = []
        self.auto_update = auto_update

        if source_file:
        
            self.sources.append(source_file)

            if auto_update:

                self.update()

    def add_source(self, file: _os.PathLike):

        """
        Adds a source .toml file for a certain ISA specification.
        
        :param file: the filepath of the .toml file
        :type file: _os.PathLike
        """

        self.sources.append(file)

        if self.auto_update:

            self.update()

    def update(self):
        
        """
        Refreshes the assembler so that all sources are taken in to account
        """

        for source in self.sources:

            self.global_values = [] #all the global values that are set.
            self.instructions: dict[str, _InstructionTemplate] = {} #all the instruction templates that are set.

            with open(source) as file:

                yaml_config = _yaml.safe_load(file) #read the file

                self._preparser(yaml_config)

    def _preparser(self, yaml_config):

        """
        INTERNAL FUNCTION. Does all the assertions for starting correctly the configuration
        
        :param yaml_config: The yaml config
        """

        #starts the parsing
        assert isinstance(yaml_config, dict), "Config file yaml is not a dictionary!"

        assert "format" in yaml_config, "\"format\" key is not in the ISA configuration. It is needed for knowing the formats and instructions!"
        assert "definitions" in yaml_config["format"], "\"definitions\" key is not in the ISA format configuration. It is needed to refer to concepts!"
        assert "bits" in yaml_config["format"], "\"bits\" key is not in ISA format configuration. It is needed to know the bit size of instructions!"

        #get the number of bits of each instruction
        self.bits = yaml_config["format"]["bits"]
        assert isinstance(self.bits, int), "\"bits\" must be an integer!"

        #get all definitions used for parsing formats
        self.definitions = yaml_config["format"]["definitions"]
        assert isinstance(self.definitions, dict), "\"definitions\" must be a dictionary will all definitions!"

        #convert definitions to values
        for key in self.definitions:

            definition = self.definitions[key]
            self.definitions[key] = _Value()
            self.definitions[key].create_from_definition(definition)

    def _parse_one_level(self, current_level) -> list:

        """
        INTERNAL FUNCTION. Parses just one dictionary level of the yaml config file and returns possible sublevels.
        
        :param current_level: The current level
        :return: possible sublevels
        :rtype: list
        """

        assert isinstance(current_level, dict), f"current_level \"{current_level}\" is not a dict!"

        for key in current_level:
            
            #check for a certain keyword presence
            if key == "instructions": #this level defines instructions -> LAST LEVEL OF RECURSION
                
                #do assertion checks
                instructions = current_level["instructions"]

                assert isinstance(instructions, list), f"Expected instructions to be a list, not {type(instructions)}!"

                for instruction in instructions:
                    pass

                return [] #return an empty list -> nothing more to iterate

            #Check if some definition is mentioned
            for def_key in self.definitions:

                if key == def_key:
                    self.global_values.append(def_key) #now it is a globally defined field!
