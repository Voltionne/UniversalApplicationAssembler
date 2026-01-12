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

def iterate_nested_dictionary(d: dict):

    for value in d.values():

        if isinstance(value, dict):

            yield from iterate_nested_dictionary(value)

        else:
            yield value

