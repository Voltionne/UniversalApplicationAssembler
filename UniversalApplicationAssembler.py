# Universal Application Assembler (UAA) v0.1

import os as _os
import yaml as _yaml

from helpers import Value as _Value
from helpers import InstructionTemplate as _InstructionTemplate
from helpers import TranslationContext as _TranslationContext
from helpers import is_number_colon_number as _is_number_colon_number

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

        #parser specifications
        self.bits: int = None
        self.instructions: dict[str, _InstructionTemplate] = {}
        self.translation_context: _TranslationContext = None
        self.definitions: dict[str, tuple[str, _Value]] = {}

        #the famous source (specified the last because it can autotriger update)
        self.source = source_file 

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

        self.global_values: set = set()
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

        # ----------------------------------------------------
        # CHECK FORMAT KEY
        # ----------------------------------------------------
        # This key is mandatory. It should include at least a bits specification and definitions specification

        assert "format" in yaml_config, f"Expected key \"format\" in yaml configuration file!"
        assert "bits" in yaml_config["format"], f"Expected bit specification inside the \"format\" key of the yaml configuration file!"
        assert "definitions" in yaml_config["format"], f"Expected definitions specification inside the \"format\" key of the yaml configuration file!"

        #CHECK BITS
        assert isinstance(yaml_config["format"]["bits"], int), f"Expected bits to be an integer number, not {type(yaml_config["format"]["bits"])}"
        self.bits = yaml_config["format"]["bits"]

        #CHECK DEFINITIONS
        assert isinstance(yaml_config["format"]["definitions"], dict), f"Expected definitions to be a dictionary, not {type(yaml_config["format"]["definitions"])}"

        definitions_dict = yaml_config["format"]["definitions"] #a shortcut to the definitions dict

        for definition_name in definitions_dict:

            #check that type and format is correct
            assert isinstance(definitions_dict[definition_name], str), f"Expected each definition to be a string, not type {type(definition_name)}"

            assert _is_number_colon_number(definitions_dict[definition_name]) or definitions_dict[definition_name].isnumeric(), f"Expected each definition to follow the format \"n:n\" or \"n\""

            if _is_number_colon_number(definitions_dict[definition_name]):

                parts = definitions_dict[definition_name].split(":")
                
                parts[0] = int(parts[0])
                parts[1] = int(parts[1])

                bits = abs(parts[0] - parts[1]) + 1 #calculate number of bits

                self.definitions[definition_name] = (definitions_dict[definition_name], _Value(bits)) #add the definition to the definitions dict.

            else: #a regular str

                self.definitions[definition_name] = (definitions_dict[definition_name], _Value()) #add the definition to the definitions dict.

        # ----------------------------------------------------
        # CHECK PARAMETERS KEY
        # ----------------------------------------------------

        if "parameters" in yaml_config: #there is parameters specification -> translation context

            assert isinstance(yaml_config["parameters"], dict), f"Expected key \"parameters\" to be a dict, not {type(yaml_config["parameters"])}!"

            self.translation_context = _TranslationContext(translation_dict=yaml_config["parameters"])

    def _parse_one_level(self, current_level: dict):

        assert isinstance(current_level, dict), f"Expected current_level to be a dict, not {type(current_level)}"

        for key in current_level:

            #Check if some definition is mentioned
            for definition_key in self.definitions:

                if key == definition_key:
                    self.global_values.add(key) #add the definition mentioned as globally affected.

                    assert isinstance(current_level[definition_key], dict) or isinstance(current_level[definition_key], int), f"Expected definition mention to be dict or int, not {type(current_level[definition_key])}!"

                    if isinstance(current_level[definition_key], dict): #partial value set
                        self.definitions[definition_key][1].set_partial_value(current_level[definition_key])
                    else: #is a integer -> full value set
                        self.definitions[definition_key][1].set_full_value(current_level[definition_key])
                        
            #check the presence of certain special keys
            if key == "instructions": #this level specifies instructions -> LAST LEVEL OF RECURSION
                pass

                