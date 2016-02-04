#
#
# Script for finding data in metadata text files
#
#
#
#
#



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

        for line in reader:
            if data_found:
                while len(imp_data) <= num_items
                    imp_data.append(line)
            elif line == info:
                data_found = True
                imp_data.append(line)
