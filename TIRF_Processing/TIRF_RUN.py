#
#
#
#
#
#
#s
#
#
#

import argparse
import sys
import TIRF_PrePost as tirf

parser = argparse.ArgumentParser(description = "Compile and plot TIRF data")
parser.add_argument("directory", help = "string for experiment directory to process",
    type = str)

parser.parse_args()

#directory = "/Users/Kristen/Desktop/TIRF_Gradient_Intensity_Recruitment/kristen_8341_349_01202016/Experiments/8341/"
directory = sys.argv[1]
row_names = ['Area', 'Mean', 'IntDen', 'Bkgrnd Area', 'Bkgrnd Mean', 'Bkgrnd IntDen']
title = 'Recruitment in Response to GFP Illumination'


### Ask whether the Trans directory exists or not. 
#tirf.traverse_directory(directory)

data = tirf.build_data(directory, row_names)

total_cells_dict, compiled = tirf.compile_data(data)

all_aves = tirf.get_averages(compiled)

tirf.make_all_plots(all_aves, total_cells_dict, tirf.plot_averages, directory, title)
