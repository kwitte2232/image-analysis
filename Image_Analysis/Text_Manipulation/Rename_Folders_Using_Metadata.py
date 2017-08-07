#
#
# Test script to extract and rename folders
#
#
#

import Extract_Text_Data as extract

directory = "/Users/Kristen/Desktop/TIRF_Gradient_Intensity_Recruitment/kristen_379_2645_Gic2_01262016/Experiments/"
gen = "GFP_metadata.txt"
info = "Channel - 488"
specific_info = "Camera Exposure"
num_items = 7

extract.rename_folders(directory, gen, info, 7, specific_info)
