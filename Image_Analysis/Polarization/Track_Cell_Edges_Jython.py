
#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''



import os
import re
import math
import java.util

from ij import IJ
from ij import ImagePlus
from ij import ImageStack
from ij.plugin import filter
from ij import WindowManager
from ij.gui import GenericDialog
from ij.gui import OvalRoi
from ij.gui import PolygonRoi
from ij.gui import Roi
from ij.gui import WaitForUserDialog
from ij.process import FloatPolygon
from ij.process import StackProcessor
from snakeprogram import IntensityEnergy
from snakeprogram import TwoDContourDeformation

IJ.log('updated')
#CONSTANTS
initial_parameters = java.util.ArrayList()
initial_parameters.add(1.0)
initial_parameters.add(2000.0)
initial_parameters.add(40.0)
initial_parameters.add(1.0)
initial_parameters.add(10.0)
initial_parameters.add(1000.0)
initial_parameters.add(1.0)

tracking_parameters = java.util.ArrayList()
tracking_parameters.add(1.0)
tracking_parameters.add(2000.0)
tracking_parameters.add(40.0)
tracking_parameters.add(1.0)
tracking_parameters.add(10.0)
tracking_parameters.add(5.0)
tracking_parameters.add(3.0)

def folder_to_process(current_dir):
    '''
    Finds the cell outlines for each frame for every cell in a directory
    Inputs:
        current_dir, string, name of the directory to process
    Returns:
        None
    '''

    for cell in os.listdir(current_dir):
        current_cell_path = os.path.join(current_dir, cell)
        for image in os.listdir(current_cell_path):
            current_image = os.path.join(current_cell_path, image)
            if re.search('Targets_Edges', current_image):
                target_edges = open_images(current_image, 'Target_Edges')
                target_edges.show()
            if re.search('Nom_crop', current_image):
                nom_edges = open_images(current_image, 'Nom_Edges')
                nom_edges.show()
        mask = make_mask(nom_edges)
        x, y = find_center(target_edges)
        IJ.log(str(x))
        IJ.log(str(y))
                #Keep these coordinates either in a file or in an image
        edge_x, edge_y = find_edge(mask, x, y)
        IJ.log(str(edge_x))
        IJ.log(str(edge_y))
        x_coord, y_coord, pairs, jx, jy = make_circle(x, y, edge_x, edge_y)
        init_circle = PolygonRoi(x_coord, y_coord, len(x_coord), Roi.POLYGON)
        nom_edges.setRoi(init_circle)
        IJ.log(str(jx))
        set_parameters_deform(nom_edges, jx, 1, initial_parameters, tracking_parameters)

        return
            None




def open_images(current_image, name):
    '''
    Opens the current image
    Inputs:
        current_image, string, directory location of the image to open
        name, string, once opened, the image will be duplicated. Name for the
            duplicate
    Returns:
        dup_imp, ImagePlus object, a duplicate of the original image
    '''

    IJ.log(current_image)
    imp = IJ.openImage(current_image)
    imp.show()
    duplicate = imp.getProcessor().duplicate()
    dup_imp = imp.createImagePlus()
    dup_imp.setProcessor(name, duplicate)

    return dup_imp


def find_center(edges):
    '''
    Finds the x,y coordinates of the cell center using a user-supplied ROI
    Inputs:
        edges, ImagePlus object, edge-detected Nomarski image
    Returns:
        center_x, integer, x-coordinate of the cell center
        center_y, integer, y-coordinate of the cell center
    '''
    roi = OvalRoi(0,0,5,5)
    edges.setRoi(roi)
    wait = WaitForUserDialog('Move circle to center of cell. When finished press OK')
    wait.show()
    centered_roi = edges.getRoi()
    center_x = roi.getBounds().x + 2.5
    center_y = roi.getBounds().y + 2.5

    return center_x, center_y

def make_mask(edges):
    '''
    Makes a mask of the edge-detected Nomarski Image (already made)
    Inputs:
        edges, ImagePlus object, edge-detected Nomarski image
    Returns:
        mask, ImagePlus object, binary image mask
    '''

    IJ.setAutoThreshold(edges, "Default dark");
    roi = filter.ThresholdToSelection.run(edges)
    edges.setRoi(roi)
    mask = ImagePlus('Mask', edges.getMask())
    mask = mask.duplicate()
    mask.getProcessor().invert()
    mask.show()

    return mask

def find_edge(mask, x, y):
    '''
    Finds the edges of the cell by searching the binary mask image for a
        pixel that has value 0
    Inputs:
        mask, ImagePlus object, masked image of the cell outlines
        x, integer, x-coordinate of the cell center
        y, integer, y-coordinate of the cell center
    Returns:
        edge_x, integer, x-coordinate of the cell initial point on the cell outline
        edge_y, integer, y-coordinate of the cell initial point on the cell outline
    '''

    edge_x = 0
    edge_y = 0
    max_iterations = 20

    for step in xrange(max_iterations):
        step_x = x + step
        step_y = y + step
        value = mask.getProcessor().getPixel(int(step_x), int(step_y))
        if value == 0:
            edge_x = step_x
            edge_y = step_y
            break
        else:
            x = step_x
            y = step_y

    return edge_x, edge_y

def make_circle(center_x, center_y, edge_x, edge_y):
    '''
    Defines the coordinates of the initial ROI to load into TwoDContourDeformation
    Inputs:
        center_x, integer, x-coordinate of the cell center
        center_y, integer, y-coordinate of the cell center
        edge_x, integer, x-coordinate of the cell initial point on the cell outline
        edge_y, integer, y-coordinate of the cell initial point on the cell outline

        Returns:
            x_coord, list, x-coordinates for the entire circle
            y_coord, list, y-coordinates for the entire circle
            java_x, java.util.ArrayList, x-coordinates for the entire circle
            java_y, java.util.ArrayList, y-coordinates for the entire circle
    '''

    dist = math.sqrt(((center_x - edge_x)*(center_x - edge_x)) + ((center_y - edge_y)*(center_y - edge_y)));
    current_angle = 0
    sector = 3.141592653589793/100

    x_coord = []
    y_coord = []
    pairs = []

    for i in xrange(201):
        x_pt = center_x + dist*math.cos(current_angle)
    	y_pt = center_y + dist*math.sin(current_angle)
        x_coord.append(x_pt)
        y_coord.append(y_pt)
        pairs.append([x_pt, y_pt])
        current_angle = current_angle + sector

    java_coords = java.util.ArrayList()
    java_x = java.util.ArrayList()
    java_y = java.util.ArrayList()
    for x, y, xy_pair in zip(x_coord, y_coord, pairs):
        current_pair = java.util.ArrayList()
        java_x.add(float(x))
        java_y.add(float(y))
        for coord in xy_pair:
            current_pair.add(float(coord))
        java_coords.add(current_pair)

    return x_coord, y_coord, java_coords, java_x, java_y

def set_parameters_deform(edges, outline, num_slice, initial_parameters, tracking_parameters):
    '''
    Sets parameters for JFilament and deforms snake around cell edges
    Inputs:
        edges: ImagePlus, binary image stack
        outline: java.util.ArrayList, x coordinates for the initial snake
        num_slice: int, initial frame of the image stack
        initial_parameters: java.util.ArrayList, list of starting parameters
        tracking_parameters: java.util.ArrayList, list of parameters for tracking through all frames of the image stack

    Returns:
        outline: java.util.ArrayList, list of java.util.ArrayList. Length is equal to number of frames
    '''

    deform = TwoDContourDeformation(outline, IntensityEnergy(edges.getProcessor(), 1.0))
    if num_slice == 1:
        set_parameters(deform, initial_parameters)
        for i in xrange(initial_parameters[5]):
            try:
                deform.addSnakePoints(initial_parameters[6])
                deform.deformSnake()
            except ValueError:
                IJ.log('snake failure')

    set_parameters(deform, tracking_parameters)
    for i in xrange(initial_parameters[5]):
        try:
            deform.addSnakePoints(initial_parameters.get(6))
            deform.deformSnake()
        except ValueError:
            IJ.log('snake failure')

    return outline

def set_parameters(deform, initial_parameters):
    '''
    Sets the parameters for the deformation of the outline
    Inputs:
        deform, TwoDContourDeformation object
        initial_parameters, java.util.ArrayList, list of the initial deform parameters
    Returns:
        None
    '''
    deform.setAlpha(initial_parameters[0])
    deform.setBeta(initial_parameters[1])
    deform.setGamma(initial_parameters[2])
    deform.setWeight(initial_parameters[3])
    deform.setStretch(initial_parameters[4])

    return None

current_dir = '/Users/Kristen/Desktop/PULSED_POLARIZATION/07272016_8440_PolPulses/WYK8440_10secPulses_Expt1/processedImages'

folder_to_process(current_dir)
