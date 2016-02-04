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

        for line in reader:
            if data_found:
                if len(imp_data) <= num_items:
                    imp_data.append(line)
            elif re.search(regex, line):
                data_found = True
                imp_data.append(line)

        return imp_data

def extract_data(imp_data, specific_info):
    '''
    Extracts the desired data from a list

    Inputs:
        imp_data: list of strings containing the image info that you wanted
        specific_info: string, what data you want out of imp_data
            ** Will convert to list of strings
    Returns:
        string, string with the desired info that you wanted
    '''

    regex = ".*" + specific_info

    for data in imp_data:
        if re.search(regex, data):
            data_split = data.split(" - ")
            important_info = data_split[1]
            desired_info = ""
            for char in important_info:
                if char.isdigit() or char == '.':
                    desired_info = desired_info + char

    desired_info = desired_info + '_(s)'
    return desired_info




def rename_folders(directory):
    '''
    '''
