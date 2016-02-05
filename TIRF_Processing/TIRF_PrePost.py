

#IMPORTS
import sys
import os
import csv
import re
import numpy
from itertools import izip
#import math
import matplotlib.pyplot as plt

from PIL import Image
from PIL.ExifTags import TAGS

def get_exif(fn):
    ret = {}
    i = Image.open(fn)
    print(i)
    #im = i.load()
    info = i._getexif()
    for tag, value in info.items():
        decoded = TAGS.get(tag, tag)
        ret[decoded] = value
    return ret



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
####               keys = Measurement
####               values = list of intensity measuremnts (strings)
#
#
#
#

def flip_csvs(directory, filename):
    '''
    Opens csv files and then transposes the rows and columns

    Inputs: string, filename

    Returns: saves a new .csv as "Trans" + filename
        Returns None if the opening or writing failed.
    '''

    #curr_dir = os.getcwd()
    #print(curr_dir)
    new_path = directory + "/Trans/"
    if not os.path.exists(new_path):
        os.makedirs(new_path)

    curr_file = directory + '/' + filename
    with open(curr_file) as csvfile:

        f = csv.reader(csvfile)
        flip_f = izip(*f)
        new_name = "Trans_" + str(filename)
        csv.writer(open(new_path + new_name, "wb")).writerows(flip_f)

    return None


def read_csv(directory, filename, row_names):
    '''
    Reads csv files of intensity measurements and stores
    the data in a dictionary.

    Inputs:
        filename, string
        row_names, list of strings with the rows to deal with

    Returns:
        dictionary
            keys = Data Measurement, string
            values = list of intensity values, strings not ints
    '''

    curr_file = directory + '/' + filename

    with open(curr_file) as csvfile:

        reader = csv.reader(csvfile)

        data = {}
        for row in reader:
            measurement = row[0]
            if measurement != ' ':
                if (measurement == "Bkgrnd Area" or
                    measurement == "Bkgrnd Mean" or
                    measurement == "Bkgrnd IntDen"):
                    data[measurement] = [row[1]]
                else:
                    data[measurement] = row[1:]

        return data

    return None

def traverse_directory(expt_dir):
    '''
    Traverses the exeriment directory and flips all the csv files and in
    doing so makes a new folder that the flipped csvs are saved into.

    Input:
        expt_dir, uppermost directory (string)

    Returns:
        None
    '''

    for dir_name, field_dir, dir_files in os.walk(expt_dir):
        for files in dir_files:
            if re.search('[.]csv$', files):
                flip_csvs(dir_name, files)
                print("flipped!")

def build_data(expt_dir, row_names):
    '''
    Returns a nested dictionary of dictionaries to account for all
    cells in every experiment in the experiment directory

    Input:
        expt_dir, uppermost directory (string)
        row_names, list of strings with the row names of the intensity csvs

    Returns:
        nested dictionary
    '''

    ## row_names = ['Area', 'Mean', 'IntDen', 'Bkgrnd Area', 'Bkgrnd Mean', 'Bkgrnd IntDen']

    all_data = {}
    for dir_name, field_dir, dir_files in os.walk(expt_dir):
        if re.search('(.)*Trans', dir_name):
            split_dir_name = dir_name.split('/')
            print(split_dir_name)
            field_name = split_dir_name[5]
            cell = split_dir_name[7]
            if field_name not in all_data:
                all_data[field_name] = {}
            field_dict = all_data[field_name]
            if cell not in field_dict:
                field_dict[cell] = {}
            cell_dict = field_dict[cell]
            #print("dir_files ", dir_files)
            for transposed_file in dir_files:
                if re.search('(.)*Pre(.)*', transposed_file):
                    curr_data = read_csv(dir_name, transposed_file, row_names)
                    cell_dict['pre'] = curr_data
                elif re.search('(.)*Dual(.)*', transposed_file):
                    curr_data = read_csv(dir_name, transposed_file, row_names)
                    cell_dict['dual'] = curr_data
                elif re.search('(.)*Post(.)*', transposed_file):
                    curr_data = read_csv(dir_name, transposed_file, row_names)
                    cell_dict['post'] = curr_data

    return all_data

def compile_data(all_data):

    compiled_data = {}
    total_cells = 0
    for field in all_data:
        curr_field = all_data[field] #dictionary
        pre_data = [0]*3
        post_data = [0]*2
        dual_data = [0]*2
        for cell in curr_field:
            total_cells += 1
            curr_cell = curr_field[cell] #dictionary
            cell_tag = field + "_" + cell
            compiled_data[cell_tag] = {}
            for measurement in curr_cell:
                curr_data = curr_cell[measurement]
                #dictionary with lists of intensities as values
                for key, value in curr_data.items():
                    integer_value = [float(v) for v in value]
                    data = numpy.asarray(integer_value)
                    curr_data[key] = data #integer_value is a list
                if measurement == "pre":
                    for key, value in curr_data.items():
                        if key == "Pre Mean":
                            num_values = len(value) #value is a numpy array
                            pre_average = numpy.asarray(numpy.mean(value))
                            pre_stdev = numpy.asarray(numpy.std(value))
                            pre_data[0] = pre_average
                            pre_data[1] = pre_stdev
                            compiled_data[cell_tag]["pre"] = pre_data
                        elif key == "Bkgrnd Mean":
                            pre_data[2] = value[:]
                                #This should make a copy...
                        compiled_data[cell_tag]["pre"] = pre_data

                elif measurement == "dual":
                    for key, value in curr_data.items():
                        if key == "RFP_Dual Mean":
                            dual_data[0] = value[:]
                        elif key == "Bkgrnd Mean":
                            dual_data[1] = value[:]
                        compiled_data[cell_tag]["dual"] = dual_data

                elif measurement == "post":
                    for key, value in curr_data.items():
                        if key == "Post Mean":
                            post_data[0] = value[:]
                        elif key == "Bkgrnd Mean":
                            post_data[1] = value[:]
                        compiled_data[cell_tag]["post"] = post_data

    return total_cells, compiled_data

def subtract_bkgrnd(compiled_data):

    all_cells = {}
    for cell in compiled_data:
        cell_data = compiled_data[cell]
        if cell not in all_cells:
            all_cells[cell] = {}
        cell_ID = all_cells[cell] # empty dict
        for data_type in cell_data:
            if data_type == "post" or data_type == "dual":
                data = cell_data[data_type] #list of numpy arrays
                raw_intensities = data[0] # numpy array
                bkgrnd = data[1][0] # only the background value
                bk_sub_intensities = raw_intensities - bkgrnd
                cell_ID[data_type] = bk_sub_intensities
            elif data_type == "pre":
                data = cell_data[data_type] #list of numpy arrays
                raw_intensities = data[0] # numpy array
                bkgrnd = data[2][0] # only the background value
                bk_sub_intensities = raw_intensities - bkgrnd
                cell_ID[data_type] = bk_sub_intensities

    return all_cells


def get_averages(total_cells, compiled_data):

    pre = numpy.array(total_cells)
    post = []
    dual = []

    ave_data = {}

    bk_sub_data = subtract_bkgrnd(compiled_data)

    for cell in bk_sub_data:
        curr_cell_data = bk_sub_data[cell] #dictionary of pre, post, dual
        for data_type in curr_cell_data:
            if data_type == "pre":
                pre_data = curr_cell_data[data_type]
                #numpy.append(pre, pre_data[0])

            elif data_type == "post":
                post_data = curr_cell_data[data_type]
                post.append(post_data)

            elif data_type == "dual":
                dual_data = curr_cell_data[data_type]
                dual.append(dual_data)

    pre_mean = numpy.mean(pre)
    pre_stdev = numpy.std(pre)
    final_pre_data = [pre_mean, pre_stdev]
    ave_data["pre"] = final_pre_data

    post_tuple = tuple(post)
    print("post_tuple ", post_tuple)
    all_post = numpy.vstack(post_tuple)
    print("all_post: ", all_post)
    post_mean = numpy.mean(all_post, axis = 0) #finds the mean of each column
    post_stdev = numpy.std(all_post, axis = 0, dtype = None) #finds the stdev of each column
    final_post_data = [post_mean, post_stdev]
    ave_data["post"] = final_post_data

    dual_tuple = tuple(dual)
    all_dual = numpy.vstack(dual_tuple)
    dual_mean = numpy.mean(all_dual, axis = 0) #finds the mean of each column
    dual_stdev = numpy.std(all_dual, axis = 0) #finds the stdev of each column
    final_dual_data = [dual_mean, dual_stdev]
    ave_data["dual"] = final_dual_data

    return all_post, ave_data


def plot_post_ints(compiled_data, total_cells):

    all_cells = []
    cell_names = numpy.array(total_cells)
    for cell in compiled_data:
        print("cell ", cell)
        cell_data = compiled_data[cell]
        cell_names = numpy.append(cell_names, cell)
        print("cell_names ", cell_names)
        # create a list with the cell names. Indices between all_cells and
            # cell_names correlate
        for data_type in cell_data:
            if data_type == "post":
                post_data = cell_data[data_type] #list of numpy arrays
                raw_intensities = post_data[0] # numpy array
                bkgrnd = post_data[1][0] # only the background value
                bk_sub_intensities = raw_intensities - bkgrnd
                all_cells.append(bk_sub_intensities)
                total_time = len(raw_intensities)

    time = []
    for i in range(total_time):
        time_pt = i + 1
        time.append(time_pt)

    print(cell_names)
    print("time ", time)
    #print("all_cells ", all_cells)
    for i, curr_cell in enumerate(all_cells):
        print("curr_cell", len(curr_cell))
        curr_cell_name = cell_names[i]
        line = plt.plot(time, curr_cell, label = curr_cell_name)

    plt.show()


def plot_averages(ave_data, total_cells):

    post_data = ave_data["post"] #list of 2 numpy arrays, one for aves other std
    dual_aves = ave_data["dual"] #list of 2 numpy arrays, one for aves other std
    pre_data = ave_data["pre"] #list of 2 elements, 0th = ave, 1st = std

    fig = plt.figure()

    #post data
    post_aves = post_data[0]
    post_stdev = post_data[1]

    total_time = len(post_aves)
    time = []
    for i in range(total_time):
        time_pt = i + 1
        time.append(time_pt)

    plt.errorbar(time, post_aves, xerr = post_stdev, yerr = post_stdev)

    plt.show()
