#
#
# Script for finding data in metadata text files
#
#
#
#
#

import re
import os

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
                if char.isdigit():
                    desired_info = desired_info + char

    desired_info = desired_info + '_msec'
    return desired_info




def rename_folders(directory, generic_file_name, info, num_items, specific_info):
    '''
    '''

    for dir_name, expt_dir, dir_files in os.walk(directory):
        for files in dir_files:
            if files == generic_file_name:
                print("dir_name ", dir_name)
                print("expt_dir ", expt_dir)
                print("files ", files)
                curr_file = dir_name + "/" + files
                imp_data = open_metadata(curr_file, info, num_items)
                desired_info = extract_data(imp_data, specific_info)
                new_name = dir_name + "_" + desired_info
                os.rename(dir_name, new_name)
