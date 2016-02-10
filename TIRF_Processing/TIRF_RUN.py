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

tirf.traverse_directory(directory)

data = tirf.build_data(directory, row_names)

total_cells, compiled = tirf.compile_data(data)

ave_data = tirf.get_averages(total_cells, compiled)
