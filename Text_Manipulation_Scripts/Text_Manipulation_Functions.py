import sys
import os
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
