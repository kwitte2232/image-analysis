

#IMPORTS
import sys
import os
import csv
import re
import numpy
import matplotlib.pyplot as plt
from scipy import stats
from itertools import izip

#FUNCTIONS

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
    with open(curr_file, 'rU') as csvfile:

        f = csv.reader(csvfile, dialect=csv.excel_tab, delimiter=",")
        flip_f = izip(*f)
        #print flip_f
        new_name = "Trans_" + str(filename)
        csv.writer(open(new_path + new_name, "wb")).writerows(flip_f)

    return None

def read_csv(directory, filename):
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

    with open(curr_file, 'rU') as csvfile:

        reader = csv.reader(csvfile, dialect=csv.excel_tab, delimiter=",")

        data = {}
        for row in reader:
            print row
            measurement = row[0]
            row_data = row[1:]
            data_convert = []
            for string_number in row_data:
                number = float(string_number)
                data_convert.append(number)
            data[measurement] = numpy.array(data_convert)

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

    return None

def build_data(expt_dir):

    for dir_name, field_dir, dir_files in os.walk(expt_dir):
        if re.search('(.)*Trans', dir_name):
            for transposed_file in dir_files:
                data = read_csv(dir_name, transposed_file)

    return data


def convert_pix(data):

    converted_data = {}
    converted_data["Cell"] = data["Cell"]
    for data_type in data:
        if (data_type == "Prev X" or data_type == "Bud X" or
            data_type == "Targets X"):
            x_pix = data[data_type]
            x_degrees = x_pix * (360/100)
            converted_data[data_type] = x_degrees
        elif data_type == "Bud Y":
            y_pix = data[data_type]
            y_time = numpy.floor(y_pix/6)
            converted_data[data_type] = y_time

    return converted_data

def normalize_degrees(converted_data, normalizer):

    normalized_data = {}
    normalized_data["Cell"] = converted_data["Cell"]
    normalized_data["Bud Y"] = converted_data["Bud Y"]
    data_for_normal = converted_data[normalizer]
    for data_type in converted_data:
        if (data_type == "Prev X" or data_type == "Bud X" or
            data_type == "Targets X"):
            curr_data = converted_data[data_type]
            normalized = data_for_normal - curr_data
            for i, n in enumerate(normalized):
                if n < 0:
                    value = n + 360
                    normalized[i] = value

            normalized_data[data_type] = normalized

    return normalized_data

def bin_data(normalized_data):

    binned = {}
    for data_type in normalized_data:
        if (data_type == "Prev X" or data_type == "Bud X" or
            data_type == "Targets X"):
            curr_data = normalized_data[data_type]
            num_data_pts = numpy.asarray(numpy.shape(curr_data))
            print("num data pts", num_data_pts)
            hist, bin_edges = numpy.histogram(curr_data, 18)
            #total = hist.sum()
            #print hist, total
            hist_list = list(hist)
            print("hist list", hist_list)
            convert_to_float = [float(x) for x in hist_list]

            #percent_hist = (np_array/num_data_pts)*100
            percent_hist = [x/25*100 for x in convert_to_float]
            print("percent", percent_hist)
            binned[data_type] = percent_hist

    return binned


def plot_buds(binned, directory):

    N = 18 #number of data points
    bottom = 8
    max_height = 4

    theta = numpy.linspace(0.0, 2*numpy.pi, N, endpoint=False)
    #radii = max_height*(numpy.random.rand(N)) #actual data
    #radii = [binned["Bud X"], binned["Targets X"]]
    radii = binned["Bud X"]
    print radii
    width = (2*numpy.pi)/N

    #colors = ["b", "y"]
    ax = plt.subplot(111, polar=True)
    bars = ax.bar(theta, radii, width=width, color="b", bottom=bottom)
    ax.set_rmax(50)
    #for r, c in zip(radii, colors):
        #bars = ax.bar(theta, r, width=width, color=c, bottom=bottom)

    #for r, bar in zip(radii, bars):
        #bar.set_facecolor(plt.cm.jet(r/10))
        #bar.set_alpha(0.8)

    #save_to = directory + '/' + "Illumination_Bud_Distribution.pdf"
    #print(save_to)
    #plt.savefig(save_to)

    plt.show()
