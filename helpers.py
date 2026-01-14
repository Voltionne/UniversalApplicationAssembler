from warnings import warn as _warn

class InstructionTemplate:

    """
    Represents a full ISA instruction
    """

    def __init__(self, bits: int , mappings: dict[str, tuple[str, "Value"]] = None):

        """
        Creates a new InstructionTemplate
        
        :param bits: Description
        :type bits: int
        :param mappings: The mapping of Values to bit positions (optional -> can be later specified calling define_mappings)
        :type mappings: dict[str, tuple[str, "Value"]]
        """

        #dict -> FIELD_NAME: (BIT_RANGE, VALUE)

        self.bits = bits

        if mappings:
            self.define_mappings(mappings)

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

        self.parameters = parameters

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

