# Universal Application Assembler (UAA) v0.1

import os as _os
import yaml as _yaml

from helpers import Value as _Value

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

