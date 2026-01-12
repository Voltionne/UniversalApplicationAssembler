class Value:

    """
    Represents a binary value
    """

    def __init__(self, bits: int):

        self.bits = bits
        self.value = "?" * bits

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

    def check_value(self):

        """
        Checks if the value is 100% set. If not, it raises an exception
        """

        if "?" in self.value:
            raise ValueError(f"There are bits that are still not set in value. Current value is {self.value}")

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

