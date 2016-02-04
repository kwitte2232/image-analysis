#
#
# Script for finding data in metadata text files
#
#
#
#
#

import re

def open_metadata(metadata, info, num_items):
    '''
    Looks for specific data within a metadata text file

    Inputs:
        metadata: string, name of file
        info: string, information that you are searching for
            ** Will alter this to account for mutliple things (list of strings)
        num_items: integer, number of items to include
            ** Will alter this to be more generalized

    Returns:
        imp_data: important data as a list of strings.
    '''

    with open(metadata, 'r') as meta:

        reader = meta.readlines()

        data_found = False
        imp_data = []

        regex = '.*' + info
        print(regex)

        for line in reader:
            if data_found:
                if len(imp_data) <= num_items:
                #while len(imp_data) <= num_items:
                    imp_data.append(line)
            elif re.search(regex, line):
                #print("worked!")
                data_found = True
                imp_data.append(line)

        return imp_data
