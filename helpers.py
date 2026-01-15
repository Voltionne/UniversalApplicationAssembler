from warnings import warn as _warn

class TranslationContext:

    """
    Represents a parameter translation system
    """

    def __init__(self, translation_dict: dict):
        self.translation_dict = translation_dict

    def translate(self, parameter_type: str, value: str) -> int:

        """
        Docstring for translate
        
        :param parameter_type: The type of the parameters, represented as a string separated by commas
        :type parameter_type: str
        :param value: The value itself of the paramater
        :type value: str
        :return: The value that represents the parameter
        :rtype: int
        """

        parts = parameter_type.split(".")

        current_level = self.translation_dict

        #accesses the translation
        for part in parts:

            assert part in current_level, f"\"{part}\" is not a key inside {current_level}"

            current_level = current_level[part]

        if isinstance(current_level, dict): #it is a translator
            
            assert value in current_level, f"Translator {current_level} has not key \"{value}\""

            assert isinstance(current_level[value], int), f"Expected the translator to give an integer, not {type(current_level[value])}"

            return current_level[value]

        elif isinstance(current_level, str): #it is a immediate specification

            assert is_number_colon_number(current_level) or current_level.isnumeric(), f"Parameter immediate with argument \"{current_level}\" is not recognized"

            bits = 0
            if is_number_colon_number(current_level):
                parts = current_level.split(":")

                bits = abs(parts[0] - parts[1]) + 1 #calculate number of bits

            else: #isnumeric()

                bits = 1

            if value.isnumeric():

                assert int(value) >= 0 and int(value) < 2**bits, f"immediate value {value} does surpass the limit of {bits} bit[s]!"

                return int(value)
            
            else: #check if it is a certain base
                if value[:2] == "0x":
                    
                    val = int(value[2:], 16)

                    assert val >= 0 and val < 2**bits, f"immediate value {value} does surpass the limit of {bits} bit[s]!"

                    return val

                elif value[:2] == "0o":
                    
                    val = int(value[2:], 8)

                    assert val >= 0 and val < 2**bits, f"immediate value {value} does surpass the limit of {bits} bit[s]!"

                    return val
                
                elif value[:2] == "0b":
                    
                    val = int(value[2:], 2)

                    assert val >= 0 and val < 2**bits, f"immediate value {value} does surpass the limit of {bits} bit[s]!"

                    return val
                
                else:
                    raise ValueError(f"\"{value}\" not recognized as a number!")
                
        elif isinstance(current_level, int): #literal number
            return current_level
        else:
            raise TypeError(f"translator in not a dict, str, or int. It is a {type(current_level)}")

class InstructionTemplate:

    """
    Represents a full ISA instruction
    """

    def __init__(self, bits: int , mappings: dict[str, tuple[str, "Value"]] = None, parameters: dict = None):

        """
        Creates a new InstructionTemplate
        
        :param bits: Description
        :type bits: int
        :param mappings: The mapping of Values to bit positions (optional -> can be later specified calling define_mappings)
        :type mappings: dict[str, tuple[str, "Value"]]
        :param parameters: the parameters of the instruction, represented using a dict with values and mapping
        :type parameters: dict
        """

        #dict -> FIELD_NAME: (BIT_RANGE, VALUE)

        self.bits = bits

        if mappings:
            self.define_mappings(mappings)

        if parameters:
            self.define_parameters(parameters)

    def define_mappings(self, mappings: dict[str, tuple[str, "Value"]] = None):
        
        """
        Adjusts the InstructionTemplate so that it uses a certain mappings
        
        :param mappings: The mappings of Values to bit positions
        :type mappings: dict[str, tuple[str, "Value"]]
        """

        self.fields: dict[str, Value] = {} #where the final fields will be stored
        self.used_up_bits = [False for _ in range(self.bits)] #to check if bits are already used up

        for field_name in mappings:

            assert isinstance(field_name, str), f"Expected name of fields to be strings, not {type(field_name)}"

            key = mappings[field_name][0]
            value = mappings[field_name][1]

            assert is_number_colon_number(key) or key.isnumeric(), "Expected mapping keys to be of the format \"number:number\" or \"number\""

            if is_number_colon_number(key): #format n:n
                parts = key.split(":")

                #convert the numbers to integer
                parts[0] = int(parts[0])
                parts[1] = int(parts[1])

                bits = abs(parts[0] - parts[1]) + 1 #calculate number of bits

                assert value.bits == bits, f"Expected {bits} bit[s], not {value.bits} bit[s]!"

                for bit_position in gradient_range(parts[0], parts[1]):
                    assert not self.used_up_bits[bit_position], f"Mapping with key \"{key}\", corresponding to {value}, is overlapping in bit {bit_position} of format!" #no overlap!
                    
                    self.used_up_bits[bit_position] = False
                    
                #if reached here -> there is no bit overlap -> add as field
                self.fields[field_name] = value

            else: #isnumeric()
                assert value.bits == 1, f"Expected 1 bit Value, not {value.bits} bits!"

                assert not self.used_up_bits[int(key)], f"Mapping with key \"{key}\", corresponding to {value}, is overlapping in bit {int(key)} of format!" #no overlap!

                self.used_up_bits[int(key)] = True #the bit is now used up
                self.fields[field_name] = value #now it is an official field

        if any(self.used_up_bits):
            _warn(f"InstructionTemplate used up only {sum(self.used_up_bits)} bits out of {self.bits} bits: there is unused bits!!")

    def define_parameters(self, parameters: dict):
        
        """
        Defines parameters of the InstructionTemplate
        
        :param parameters: The dict that contains the parameters -> includes values and mapping
        :type parameters: dict
        """

        assert "values" in parameters, f"Parameters dictionary has no \"values\" key!"
        assert "mapping" in parameters, f"Parameters dictionary has no \"mapping\" key!"

        assert isinstance(self.parameters["values"], list), f"The values of parameters are not in a list, it is a {type(self.parameters["values"])}!"
        assert isinstance(self.parameters["mapping"], list), f"The mapping of parameters are not in a list, it is a {type(self.parameters["mapping"])}!"

        assert len(self.parameters["values"]) == len(self.parameters["mapping"]), f"Missmatch between number of parameters and mapping specified, respectively: {len(self.parameters["values"])} vs {len(self.parameters["mapping"])}"

        for value in self.parameters["values"]:
            assert isinstance(value, str), f"Expected value \"{value}\" to be a string, not a {type(value)}"

        for mapping in self.parameters["mapping"]:
            assert isinstance(mapping, str) or isinstance(mapping, list), f"Expected mapping \"{mapping}\" to be a string or list, not a {type(mapping)}"

            if isinstance(mapping, list):
                for sub_mapping in mapping:
                    assert isinstance(sub_mapping, str), f"Expected sub_mapping \"{sub_mapping}\" to be a string, not a {type(sub_mapping)}"

        self.parameters = parameters

    def apply(self, translation_context: TranslationContext, parameters: list[str]):

        """
        Applies parameters to the instruction, following a certain translation context.
        
        :param translation_context: The translation context object that sets all parameter translations to fields
        :type translation_context: TranslationContext
        :param parameters: A list with all parameters
        :type parameters: list[str]
        """

        assert len(parameters) == len(self.parameters["values"]), f"Missmatch between number of parameters! Got {len(parameters)}, expected {len(self.parameters["values"])}"

        #iterate over each parameter
        for i in range(len(parameters)):

            translated_value = translation_context.translate(parameter_type=self.parameters["values"][i], value=parameters[i]) #translate each parameter

            if isinstance(self.parameters["mapping"][i], list):

                bits_done = 0 #counts how many bits are already set.
                
                for sub_mapping in reversed(self.parameters["mapping"][i]): #iterates over the mappings, reversed to do first the LSBs.

                    sub_mapping_bits = self.fields[sub_mapping].bits

                    mask = 2 ** sub_mapping_bits - 1 #creates a mask

                    final_value = (translated_value >> bits_done) & mask #the final value after applying the mask and correcting

                    self.fields[sub_mapping].set_full_value(final_value) #set the value

                    #increase bits_done to perform the next iterations correctly
                    bits_done += sub_mapping_bits

            else: #must 100% be str because of the check in define_parameters
                self.fields[self.parameters["mapping"][i]].set_full_value(translated_value)

    #SET FUNCTIONS
    def set_partial_field(self, name: str, set_dict: dict):

        """
        Sets the some partial value to some field. 
        
        :param name: The name of the field
        :type name: str
        :param set_dict: The dict that determines the set
        :type set_dict: dict
        """

        assert name in self.fields, f"\"{name}\" is not a field out of {list(self.fields.keys())}!"

        self.fields[name].set_partial_value(set_dict)

    def set_full_field(self, name: str, value: int): 
        
        """
        Sets the full value of a full field
        
        :param name: The name of the field
        :type name: str
        :param value: The value itself
        :type value: int
        """

        assert name in self.fields, f"\"{name}\" is not a field out of {list(self.fields.keys())}!"

        self.fields[name].set_full_value(value)

    def check_completeness(self):

        """
        Checks if all the fields of the InstructionTemplate have been filled.
        """

        for field_name in self.fields:
            if not self.fields[field_name].check_value():
                return False
            
        return True

class Value:

    """
    Represents a binary value
    """

    def __init__(self, bits: int = 1):

        self.bits = bits
        self.value = "?" * bits

    def create_from_definition(self, definition: str):

        """
        Configurates the value so that it initilizes following a definition
        
        :param definition: The string that defines the value
        :type definition: str
        """

        assert isinstance(definition, str), f"\"{definition}\" is not an str!" #just in case

        if ":" in definition:
            parts = definition.split(":")
            assert len(parts) == 2
            assert parts[0].isnumeric() and parts[1].isnumeric()

            parts[0] = int(parts[0])
            parts[1] = int(parts[1])

            self.bits = abs(parts[0] - parts[1]) + 1 #set the number of bits

            self.value = "?" * self.bits

        else: #is only one number -> one bit
            assert definition.isnumeric()

            self.bits = 1
            self.value = "?"

    def set_partial_value(self, set_dict: dict):

        """
        Sets the value partially according to a set dict.
        
        :param set_dict: The dict that determines the set
        :type set_dict: dict
        """

        assert "set" in set_dict, "Set dict does not include key \"set\""
        assert isinstance(set_dict["set"], int), f"\"set\" is not a numerical value! Dict: {set_dict}"

        assert "bits" in set_dict, "Set dict does not include key \"bits\""
        assert isinstance(set_dict["bits"], str), f"\"bits\" is not a string specifying the affected bits! Dict: {set_dict}"

        if ":" in set_dict["bits"]:
            parts = set_dict["bits"].split(":")
            assert len(parts) == 2
            assert parts[0].isnumeric() and parts[1].isnumeric()

            parts[0] = int(parts[0])
            parts[1] = int(parts[1])

            bits = abs(parts[0] - parts[1]) + 1 #count number of bits expected

            assert set_dict["set"] >= 0 and set_dict["set"] < 2**bits #assert that the value is compliant

            set_value = bin(set_dict["set"]).removeprefix("0b").rjust(bits, "0")

            self.value = list(self.value) #convert to list temporally (string is immutable)

            for idx, i in enumerate(gradient_range(parts[0], parts[1])):
                self.value[-i - 1] = set_value[idx]

            self.value = "".join(self.value) #return to string

        else:
            assert set_dict["bits"].isnumeric()
            assert set_dict["set"] == 0 or set_dict["set"] == 1

            self.value = list(self.value) #convert to list temporally (string is immutable)

            self.value[-int(set_dict["bits"]) - 1] = str(bin(set_dict["set"]).removeprefix("0b"))

            self.value = "".join(self.value) #return to string

    def set_full_value(self, value: int):

        """
        Sets the whole value
        
        :param value: The value to be set
        :type value: int
        """

        assert value >= 0 and value < 2**self.bits

        self.value = bin(value).removeprefix("0b").rjust(self.bits, "0")

    def check_value(self) -> bool:

        """
        Checks if the value is 100% set.
        
        :return: True or false
        :rtype: bool
        """

        if "?" in self.value:
            return False
        else:
            return True

def gradient_range(a: int, b: int):

    """
    Creates an iterator that returns all values from a to b, creating a gradient. For example, if a = 3 and b = 5, it returns 3, 4 and 5. If a = 2 and b = -1, it returns 2, 1, 0, -1
    
    :param a: Initial value
    :type a: int
    :param b: Final value
    :type b: int
    """

    return range(a, b + (1 if a < b else -1), 1 if a < b else -1)

def is_number_colon_number(s: str) -> bool:

    """
    Checks if a string follows the format "number:number"
    
    :param s: The string subject to the check
    :type s: str
    :return: True or false
    :rtype: bool
    """

    parts = s.split(":")

    if len(parts) != 2: #easy check
        return False
    
    return parts[0].isdigit() and parts[1].isdigit()

def iterate_nested_dictionary(d: dict, parent: str = None):

    for key in d:

        if isinstance(d[key], dict):

            if parent:
                yield from iterate_nested_dictionary(d[key], parent=parent + [key])
            else:
                yield from iterate_nested_dictionary(d[key], parent=[key])

        else:
            if parent:
                yield parent + [key, d[key]]
            else:
                yield [key, d[key]]

