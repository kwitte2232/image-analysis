'''
Kristen Witte
Glotzer Lab

Compile data with respect to Bud Emergence from intensity measurements derived from "Measure_Intensities.txt".
 Best run in iPython Notebook.
'''

#IMPORTS
import os
import pandas as pd
import numpy as np

#FUNCTIONS

def finalize_ave_and_sem(ave_raw, ave_norm, curr_path, save_as):

    ave_raw_df = pd.concat([x['ave'] for x in ave_raw], axis=1)
    ave_norm_df = pd.concat([x['ave'] for x in ave_norm], axis=1)

    df_dimensions = ave_raw_df.shape
    num_cells = df_dimensions[1]

    ave_norm_df['Final Ave'] = ave_norm_df.mean(axis=1)
    ave_norm_df['SEM'] = (ave_norm_df.std(axis=1))/math.sqrt(num_cells)
    
    save_path = os.path.join(curr_path, save_as)
    ave_norm_df.to_csv(save_path)

    final_norm_ave = np.asarray(ave_norm_df['Final Ave'])
    final_norm_sem = np.asarray(ave_norm_df['SEM'])

    return final_norm_ave, final_norm_sem

def compile_lists(extract_aves, df, type_data, time, be, ave_raw,
    sem_raw, ave_normalized):

    ave_col, sem_col, ave_norm = extract_aves(df, type_data, time, be)
    
    ave_normalized.append(ave_norm)

    ave_raw.append(ave_col)
    sem_raw.append(sem_col)
    
    return [ave_raw, sem_raw, ave_normalized]

def extract_aves(df, type_data, time, be):

    one = type_data+"_ROI_0"
    two = type_data+"_ROI_1"
    three = type_data+"_ROI_2"
    data = df[[one, two, three]]
    data['ave'] = data.mean(axis=1)
    data['sem'] = data.std(axis=1)/math.sqrt(3)

    ave_col = pd.Series.to_frame(data['ave']) 
    initial = ave_col.iloc[0]
    ave_norm = ave_col.divide(initial) 
    sem_col = pd.Series.to_frame(data['sem']) 

    slice_ave_col = slice_timepoints(ave_col, time, be)
    slice_ave_norm = slice_timepoints(ave_norm, time, be)
    slice_sem_col = slice_timepoints(sem_col, time, be)
    
    return slice_ave_col, slice_sem_col, slice_ave_norm

def extract_bckgrd_aves(df, type_data, time, be):

    one = type_data+"_ROI_0"
    two = type_data+"_ROI_1"
    three = type_data+"_ROI_2"
    data = df[[one, two, three]]
    data['ave'] = data.mean(axis=1)
    data['sem'] = data.std(axis=1)/math.sqrt(3)

    ave_col = pd.Series.to_frame(data['ave']) 
    sem_col = pd.Series.to_frame(data['sem']) 

    slice_ave_col = slice_timepoints(ave_col, time, be)
    slice_sem_col = slice_timepoints(sem_col, time, be)
    
    return slice_ave_col, slice_sem_col

def compile_bckgrd_be(extract_bckgrd_aves, df, type_data, time, be, back_ave, back_sem):
    
    bckgrd_ave, bckgrd_sem = extract_bckgrd_aves(df, type_data, time, be)
    
    back_ave.append(bckgrd_ave)
    back_sem.append(bckgrd_sem)
    
    return back_ave, back_sem
    
def extract_BE_time(be_text):

    with open(be_text, 'r') as be:

        reader = be.readlines()

        all_lines = []
        for line in reader:
            split = line.split("\t")
            all_lines.append(split)

        be = int(all_lines[1][1])

    return be

def slice_timepoints(df, timepoints, be):

    labels = [x for x in range(62)]
    intensity_be_index = 0
    start = be - 41
    if start < 0:
        start = start - start
    end = be + 20
    if end > len(timepoints):
        end = len(timepoints)

    sliced = df[start:end]

    time_sliced = timepoints[start:end]

    dex_be = time_sliced.index(be)

    if sliced.shape[0] < 61:
        i = 0
        if dex_be < 40:
            num_zeroes_to_pad = 40 - dex_be
            zeroes = np.asarray([1.0]*num_zeroes_to_pad)
            column_name = list(df.columns.values)[0]
            #print column_name
            zeroes_df = pd.DataFrame(zeroes, columns=[column_name])
            df_to_concat = [zeroes_df, sliced]
            insert_zeroes = pd.concat(df_to_concat).reset_index().reindex(labels)

        elif dex_be == 40:
            num_zeroes_to_pad = int(61 - sliced.shape[0])
            zeroes = np.asarray([1.0]*num_zeroes_to_pad)
            column_name = list(df.columns.values)[0]
            #print column_name
            zeroes_df = pd.DataFrame(zeroes, columns=[column_name])
            df_to_concat = [sliced, zeroes_df]
            insert_zeroes = pd.concat(df_to_concat).reset_index().reindex(labels)        
    else:
        insert_zeroes = sliced.reset_index().reindex(labels)
        

    return insert_zeroes

def normalize_to_background(ave_raw, bckgrd_ave, curr_path, save_as):
    
    ave_raw_df = pd.concat(ave_raw, axis=1)
    bckgrd_df = pd.concat(bckgrd_ave, axis=1)
    
    df_dimensions = ave_raw_df.shape
    num_cells = df_dimensions[1]
    
    norm_to_bckgrd = ave_raw_df.div(bckgrd_df)
    
    del norm_to_bckgrd['index']
    
    norm_to_bckgrd['Final Ave'] = norm_to_bckgrd.mean(axis=1)
    norm_to_bckgrd['SEM'] = (norm_to_bckgrd.std(axis=1))/math.sqrt(num_cells)
    
    save_path = os.path.join(curr_path, save_as)
    norm_to_bckgrd.to_csv(save_path)

    final_norm_ave = np.asarray(norm_to_bckgrd['Final Ave'])
    final_norm_sem = np.asarray(norm_to_bckgrd['SEM'])

    return final_norm_ave, final_norm_sem


#MAIN PROCESSING

directory = 'Your Directory'

mean_ave_raw = []
mean_sem_raw = []
mean_ave_norm = []

back_ave = []
back_sem = []

for root, expt_dirs, files in os.walk(directory):
    for f in files:
        if f == 'intensities.csv' or f == "MotherCell_intensities.csv" or f == "Target_intensities.csv":
            mother_cell_path = os.path.join(root, 'MotherCell')
            if os.path.isdir(mother_cell_path):

                current_file = os.path.join(root, f)
                data = pd.read_csv(current_file)
        
                num_timepoints = data.shape[0]
                time = range(1, num_timepoints+1)

                bud_emerge_path = os.path.join(root, 'BudEmergence.txt')
                bud_emerge_path = os.path.join(mother_cell_path, 'BudEmergence.txt')
                be = extract_BE_time(bud_emerge_path)

                all_mean_lists = compile_lists(extract_aves, data, 'Mean', time, be, mean_ave_raw, mean_sem_raw, mean_ave_norm)
                all_max_lists = compile_lists(extract_aves, data, 'Max', time, be, max_ave_raw, max_sem_raw, max_ave_norm)
        elif f == 'Background_intensities.csv':
            mother_cell_path = os.path.join(root, 'MotherCell')
            if os.path.isdir(mother_cell_path):
                current_file = os.path.join(root, f)
                bckgrd = pd.read_csv(current_file)
            
                num_timepoints = bckgrd.shape[0]
                time = range(1, num_timepoints+1)

                bud_emerge_path = os.path.join(mother_cell_path, 'BudEmergence.txt')
                be = extract_BE_time(bud_emerge_path)

                back_ave, back_sem = compile_bckgrd_be(extract_bckgrd_aves, bckgrd, 'Mean', time, be, back_ave, back_sem)


final_mean_norm_ave, final_mean_norm_sem = finalize_ave_and_sem(mean_ave_raw, mean_ave_norm, directory, 'Mean_Polarization_BE_Intensities.csv')
final_max_norm_ave, final_max_norm_sem = finalize_ave_and_sem(max_ave_raw, max_ave_norm, directory, 'Max_Polarization_BE_Intensities.csv')
print 'Done'