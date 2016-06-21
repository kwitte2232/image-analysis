

#IMPORTS
import sys
import csv
from itertools import izip


# In this script the goal is to combine multiple csv files
# generated from intensity measurements of pre, dual, and
# post images from TIRF illumination.
#
# Data structure = nested dictionaries and lists
#   keys = experiment number
#   values = dictionary
#       keys = cell number
#       values = dictionary
#           keys = string, "pre", "dual", "post"
#           values = dictionary
#               keys =
#
#
#
#
#

def open_csvs(filename):

    with open(filename) as csvfile:

        f = csv.reader(csvfile)
        flip_f = izip(*f)
        new_name = "Trans_" + str(filename)
        csv.writer("Trans_" + filename, "wb").writerows(flip_f)
