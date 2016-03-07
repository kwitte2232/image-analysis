import sys
import os
import shutil
import re


def delete_files(directory, file_type):

    regex = "[.]" + file_type
    for dir_name, folders, dir_files in os.walk(directory):
        for files in dir_files:
            if re.search(regex, files):
                print("directory: ", dir_name)
                print("Folder: ", folders)
                print("files: ", files)
                os.remove(dir_name + "/" + files)

    return None

def delete_folders(directory, gen_folder_name):

    for dir_name, folders, dir_files in os.walk(directory, topdown = False):
        for folder in folders:
            if re.search(gen_folder_name, folder):
                folder_to_delete = dir_name + '/' + folder + '/'
                print(folder_to_delete)
                shutil.rmtree(folder_to_delete)

    return None
