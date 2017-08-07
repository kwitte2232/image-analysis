

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
        #print(flip_f)
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

        reader = csv.reader(csvfile)

        data = {}
        for row in reader:
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
            if re.search('intensities[.]csv$', files):
                flip_csvs(dir_name, files)
                print("flipped!")

    return None


def build_data(expt_dir):

    all_cells = {}
    be = {}
    for dir_name, field_dir, dir_files in os.walk(expt_dir):
        if re.search('(.)*Trans', dir_name):
            split_dir_name = dir_name.split('/')
            #print split_dir_name
            cell = split_dir_name[7] #'Cell_X'

            for transposed_file in dir_files:
                all_cells[cell] = read_csv(dir_name, transposed_file)

            mother_cell_dir = '/' + '/'.join(split_dir_name[1:8]) + "/MotherCell/"
            be_text = mother_cell_dir + "BudEmergence.txt"

            curr_be = extract_BE_time(be_text)
            be[cell] = curr_be

    return all_cells, be

def combine_multiple_data_sets(data_1, data_2, buds_1, buds_2):

    all_cells = data_1
    all_buds = buds_1
    for cell in data_2:
        new_name = cell + "_2"
        all_cells[new_name] = data_2[cell]
        all_buds[new_name] = buds_2[cell]

    return all_cells, all_buds


def get_averages(all_cells):

    all_aves = {}
    for cell in all_cells:
        curr_cell_data = all_cells[cell]
        means = {}
        maxes = {}

        for data in curr_cell_data:
            if data != " " and data.startswith("Mean"):
                means[data] = curr_cell_data[data]
            elif data != " " and data.startswith("Max"):
                maxes[data] = curr_cell_data[data]

        mean_aves = get_data_aves(means) #list of numpy arrays
        max_aves = get_data_aves(maxes) #list of numpy arrays

        all_aves[cell] = {"Means":mean_aves, "Maxes":max_aves}

    return all_aves


def get_data_aves(data):

    data_list = []

    for v in data.values():
        data_list.append(v)

    data_tuple = tuple(data_list)
    all_data = numpy.vstack(data_tuple)
    data_mean = numpy.mean(all_data, axis = 0)
    data_sem = stats.sem(all_data, axis = 0)
    final = [data_mean, data_sem]

    return final

def extract_BE_time(be_text):

    with open(be_text, 'r') as be:

        reader = be.readlines()

        all_lines = []
        for line in reader:
            split = line.split("\t")
            all_lines.append(split)

        be = int(all_lines[1][1])

    return be

def norm_to_be(be, all_aves):

    norm_aves = {}
    for cell in all_aves:
        curr_data = all_aves[cell] #dict with Means and Maxes as keys
        bud_emerge = be[cell]
        #print cell
        norm_aves[cell] = {}
        for data_type in curr_data:
            #print data_type
            if data_type == "Means":
                means_data = curr_data[data_type] # list of 2 numpy arrays
                means = means_data[0]
                sem_mean = means_data[1]
                num_timepoints = numpy.asarray(numpy.shape(means))
                time = range(1, num_timepoints+1)
                mean_norm, dex = slice_timepoints(means, time, bud_emerge)
                mean_sem_norm = slice_sem(sem_mean, dex, bud_emerge)
                norm_aves[cell]["Means"] = [mean_norm, mean_sem_norm]
            elif data_type == "Maxes":
                maxes_data = curr_data[data_type] # list of 2 numpy arrays
                maxes = maxes_data[0]
                sem_max = maxes_data[1]
                num_timepoints = numpy.asarray(numpy.shape(maxes))
                time = range(1, num_timepoints+1)
                max_norm, dex = slice_timepoints(maxes, time, bud_emerge)
                max_sem_norm = slice_sem(sem_max, dex, bud_emerge)
                norm_aves[cell]["Maxes"] = [max_norm, max_sem_norm]

    return norm_aves

def slice_timepoints(list_to_slice, timepoints, be):

    intensity_be_index = 0
    start = be - 41
    if start < 0:
        start = start - start
    end = be + 20
    sliced = list_to_slice[start:end]
    time_sliced = timepoints[start:end]
    #print(be)
    dex_be = time_sliced.index(be)
    if numpy.asarray(numpy.shape(sliced)) < 61:
        intensity_at_be = sliced[dex_be]
        #print("int at be", intensity_at_be)
        intensity_be_index = numpy.nonzero(sliced==intensity_at_be)[0][0]
        #print("int be index", intensity_be_index)
        if intensity_be_index < 40:
            num_zeroes_to_pad = 40 - intensity_be_index
            #print ("num zeroes", num_zeroes_to_pad)
            sliced = numpy.pad(sliced, (num_zeroes_to_pad, 0), mode='constant',
                constant_values=numpy.nan)
        elif intensity_be_index == 40:
            num_zeroes_to_pad = int(61 - numpy.asarray(numpy.shape(sliced)))
            #print ("num zeroes", num_zeroes_to_pad)
            sliced = numpy.pad(sliced, (0, num_zeroes_to_pad), mode='constant',
                constant_values=numpy.nan)
    return sliced, intensity_be_index

def slice_sem(sem_to_slice, intensity_be_index, be):

    start = be - 41
    if start < 0:
        start = start - start
    end = be + 20
    sliced = sem_to_slice[start:end]
    #time_sliced = timepoints[start:end]
    #print(be)
    if numpy.asarray(numpy.shape(sliced)) < 61:
        if intensity_be_index < 40:
            num_zeroes_to_pad = 40 - intensity_be_index
            #print ("num zeroes", num_zeroes_to_pad)
            sliced = numpy.pad(sliced, (num_zeroes_to_pad, 0), mode='constant',
                constant_values=numpy.nan)
    return sliced


def plot(all_aves, directory):

    for cell in all_aves:
        curr_folder = directory + "/" + cell
        data = all_aves[cell]
        for data_type in data:
            current_data = data[data_type]
            values = current_data[0]
            num_timepoints = numpy.asarray(numpy.shape(values))
            time = range(1, num_timepoints+1)
            sem = current_data[1]
            if data_type == "Means":
                fig = plt.figure()
                plt.errorbar(time, values, yerr = sem, color="b")
                title = "Means"
                plt.title(title)
                plt.xlabel("Time (minutes)")
                plt.ylabel("Intensity (A.U.)")
                axes = plt.gca()
                axes.set_ylim(1000, 1500)
                save_to = curr_folder + '/' + "Means_Relative_BE.pdf"
                print(save_to)
                plt.savefig(save_to)

                #plt.show()
            elif data_type == "Maxes":
                fig = plt.figure()
                plt.errorbar(time, values, yerr = sem, color="r")
                title = "Maxes"
                plt.title(title)
                plt.xlabel("Time (minutes)")
                plt.ylabel("Intensity (A.U.)")
                axes = plt.gca()
                axes.set_ylim(1000, 1500)
                save_to = curr_folder + '/' + "Maxes_Relative_BE.pdf"
                print(save_to)
                plt.savefig(save_to)
                #plt.show()

def average_across_time(norm_data):

    norm_tuple = tuple(norm_data)
    #print(dual_tuple)
    all_norm_data = numpy.vstack(norm_tuple)
    norm_mean = numpy.nanmean(all_norm_data, axis = 0) #finds the mean of each column
    norm_sem = stats.sem(all_norm_data, axis = 0, nan_policy="omit") #finds the stdev of each column
    final_norm_data = [norm_mean, norm_sem]

    return final_norm_data

def output_to_csv(final_data, directory, data_type):

    destination = directory + '/' + data_type + "_data.csv"
    tuple_data = tuple(final_data)
    stacked = numpy.vstack(tuple_data)
    numpy.savetxt(destination, stacked, fmt='%.2f', delimiter=',', newline='\n')
    print "done"

    return None


def plot_all(data_to_plot, time, directory):

    all_means = {}
    all_maxes = {}
    means = []
    sem_means = []
    maxes = []
    sem_maxes = []
    for cell in data_to_plot:
        curr_folder = directory + "/" + cell
        data = data_to_plot[cell]
        for data_type in data:
            current_data = data[data_type]
            values = current_data[0]
            sem = current_data[1]
            if data_type == "Means":
                means.append(values)
                sem_means.append(sem)

            elif data_type == "Maxes":
                maxes.append(values)
                sem_maxes.append(sem)

    plot_lines(means, "Mean", time, directory)
    #plot_lines(maxes, "Max", time, directory)

    compiled_means = average_across_time(means)
    #compiled_maxes = average_across_time(maxes)

    plot_combined(compiled_means, "Mean", time, directory)
    #plot_combined(compiled_maxes, "Max", time, directory)

    return compiled_means #, compiled_maxes

def plot_lines(data, data_type, time, directory):

    fig = plt.figure()

    for line in data:
        #print len(time)
        #print numpy.asarray(numpy.shape(line))
        line = plt.plot(time, line)

    plt.xlabel("Time Relative to Bud Emergence (minutes)")
    plt.ylabel("Intensity (A.U.)")
    axes = plt.gca()
    axes.set_ylim(1000, 1500)
    #plt.show()

    save_to = directory + '/' + data_type + "_Intensity_Traces.pdf"
    print(save_to)
    plt.savefig(save_to)

def plot_combined(compiled_data, data_type, time, directory):

    fig = plt.figure()
    y = compiled_data[0]
    y_err = compiled_data[1]

    plt.errorbar(time, y, yerr=y_err)
    plt.xlabel("Time Relative to Bud Emergence (minutes)")
    plt.ylabel("Intensity (A.U.)")
    axes = plt.gca()
    axes.set_ylim(1000, 1500)

    save_to = directory + '/' + data_type + "_Intensity_Combined.pdf"
    print(save_to)
    plt.savefig(save_to)
    #plt.show()
