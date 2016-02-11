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

import TIRF_PrePost as tirf

directory = "/Users/Kristen/Desktop/TIRF_Gradient_Intensity_Recruitment/kristen_8341_349_01202016/Experiments/8341/"
row_names = ['Area', 'Mean', 'IntDen', 'Bkgrnd Area', 'Bkgrnd Mean', 'Bkgrnd IntDen']
title = 'Recruitment in Response to GFP Illumination'

tirf.traverse_directory(directory)

data = tirf.build_data(directory, row_names)

total_cells_dict, compiled = tirf.compile_data(data)

all_aves = tirf.get_averages(compiled)

make_all_plots(all_aves, total_cells_dict, tirf.plot_averages, directory, title)
