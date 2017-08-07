#IMPORTS
import csv
import os
import re
import sys

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from PIL import Image, ImageFilter, ImageSequence

from scipy import ndimage as ndi
from scipy import stats
from scipy.integrate import trapz
from scipy.signal import fftconvolve
from scipy.signal import gaussian

from skimage import color
from skimage import feature
from skimage import filters
from skimage import measure
from skimage import morphology



#FUNCTIONS

def find_nearest(array,value):
    '''
    Given mouseclick coords on a plt, finds the corresponding index
        in the array.
    stackoverflow.com/questions/25521120/
        store-mouse-click-event-coordinates-with-matplotlib

    Inputs:
        array, np array
        value, list, mouseclick coordinates

    Returns:
        index of array, NOT the element of the array at that index
    '''

    idx = (np.abs(array-value)).argmin()
    print array-value
    print idx
    print array[idx]/59
    return idx

# Simple mouse click function to store coordinates
def onclick(event):
    '''
    Records and returns the coordinates of a mouseclick
    stackoverflow.com/questions/25521120/
        store-mouse-click-event-coordinates-with-matplotlib

    Inputs:
        event, mouseclick event

    Return:
        coords, list, x,y coordinates of the mouseclick
    '''

    global ix, iy
    ix, iy = event.xdata, event.ydata

    print 'x = %d, y = %d'%(
         ix, iy)

    # assign global variable to access outside of function
    global coords
    #coords = []
    coords.append((ix, iy))
    #print coords

    # Disconnect after 2 clicks
    if len(coords) == 1:
        #implot.figure.canvas.mpl_disconnect(cid)
        plt.close(1)

    return coords


def find_centers_of_cells(d):
    '''
    Opens Nomarski images in a directory. User-suppplied mouseclick finds the
        center of the cell. Find the center of the mother-daughter pair?

    Inputs:
        d, string, current directory

    Returns:
        None
        *Saves images into the directory
    '''

    for d_name, folders, files in os.walk(d):
        if re.search('Cell', d_name):
            nom = os.path.join(d_name, files[3])
            im = plt.imread(nom)
            #fig = plt.figure(im)
            gray_im = color.rgb2gray(im)
            global coords
            coords = []
            global implot
            implot = my_imshow(gray_im)
            cid = implot.figure.canvas.mpl_connect('button_press_event', onclick)
            plt.show()
            print cid
            print coords
            print gray_im

            ch1 = np.where(gray_im == (find_nearest(gray_im, coords[0][0])))
            ch2 = np.where(gray_im == (find_nearest(gray_im, coords[0][1])))

            print ch1, ch2
    return None


def my_imshow(im, title=None, **kwargs):
    '''
    Show function that allows to see images how you want without having
    to always input that into plt function

    Inputs:
        im, np array, images
        title, string, what is the image
        kwargs, image visual specifications

    Returns:
        None
    '''

    if 'cmap' not in kwargs:
        kwargs['cmap'] = 'gray'
    fig = plt.figure()

    implot = plt.imshow(im, interpolation='none', **kwargs)
    #plt.show()
    if title:
        plt.title(title)
    #plt.axis('off')

    return implot





def analyze_all_cells(d, start_analysis, analysis_fxn):
    '''
    Analyzes all cells in a directory. Can call function for what
    type of analysis

    Inputs:
        d, string, current directory
        start_analysis, function, begin analysis
        analysis_fxn, function, what type of analysis to perform

    Returns:
        None
        *Saves CSVs with the data into each folder
    '''

    for d_name, folders, files in os.walk(d):
        if re.search('Cell', d_name):
            start_analysis(d_name, files, analysis_fxn)

    return None

def process_images(d, files, analysis_fxn):
    '''
    Opens images and starts the analysis_fxn

    Inputs:
        d, string, current cell folder
        files, list of strings, names of the files in the
            current cell folder
        analysis_fxn, function, type of analysis to perform

    Returns:
        None
    '''

    nom = os.path.join(d, files[3]) #Nom_crop
    mch = os.path.join(d, files[2])

    nom_im = Image.open(nom)
    gray_nom = color.rgb2gray(nom_im)

    mch_im = Image.open(mch)
    gray_mch = color.rgb2gray(gray_im)

    analysis_fxn(gray_nom, gray_mch)
    return None

def find_regions(gray_nom, gray_mch):
    '''
    Finds and separates mother cell from daughter cell in a nom Image

    Inputs:
        nomarski Image, PIL object
        mcherry image, PIL object

    Returns:
        stuff
    '''

    for nom_frame, mch_frame in zip(ImageSequence.Iterator(gray_nom),
        ImageSequence.Iterator(gray_mch)):

        nom_uni_blur = ndi.uniform_filter(nom_frame, size=5)
        thresh = 25
        mask = nom_uni_blur > thresh
            #save mask
        filled_mask = ndi.binary_fill_holes(mask)
            #save filled_mask
        dist = ndi.distance_transform_edt(filled_mask)
            #save dist
        local_peaks = feature.peak_local_max(dist, indices=False,
            footprint=np.ones((10,10)))
            #save local peaks
        markers = ndi.label(local_peaks)[0]
        dist = dist.astype(np.int64, copy=False)
        labelled_cells = morphology.watershed(~dist, markers,
            mask=filled_mask)
            #save labelled_cells

        analyze_regions(labelled_cells, mch_frame)


    return None

def analyze_regions(labelled_cells, mch_frame):

    regions = ndi.measure.regionprops(labelled_cells, mch_frame)

    for region in regions:
        y, x = region.centroid
        area = region.area
        mean = region.mean_intensity
        maxi = region.max_intensity







def import_csv(directory, filename):
    '''
    Imports csv of intensity measurements as pandas dataframe

    Inputs:
        directory, sting (cwd)
        filename, string

    Returns:
        pandas dataframe
    '''

    current_file = os.path.join(directory, filename)
    data = pd.read_csv()

    return data

def build_data(expt_dir):
    '''
    Builds a dictionary of pandas dataframes
    '''
