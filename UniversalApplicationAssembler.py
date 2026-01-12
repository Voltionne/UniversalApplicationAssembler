# Universal Application Assembler (UAA) v0.1

import os as _os
import yaml as _yaml

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

                pass