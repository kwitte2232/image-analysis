

def extract_aves(df, type_data, time, be):

    one = type_data+"_ROI_0"
    two = type_data+"_ROI_1"
    three = type_data+"_ROI_2"
    data = df[[one, two, three]]
    data['ave'] = data.mean(axis=1)
    data['sem'] = data.std(axis=1)/math.sqrt(3)

    ave_col = pd.Series.to_frame(data['ave']) #
    initial = ave_col.iloc[0]
    ave_norm_to_initial = ave_col.divide(initial) #
    ave_norm_np = np.asarray(ave_norm_to_initial.as_matrix)

    sem_col = pd.Series.to_frame(data['sem']) #

    ave_np = np.asarray(data['ave'])
    sem_np = np.asarray(data['sem'])


    #slice_ave_col, slice_ave_norm, slice_sem_col
    slice_ave_col = slice_timepoints(ave_col, time, be)
    slice_ave_norm = slice_timepoints(ave_norm_to_initial, time, be)
    slice_sem_col = slice_timepoints(sem_col, time, be)


    return ave_col, sem_col, ave_norm_to_initial,
        ave_norm_np, ave_np, sem_np

def compile_lists(extract_aves, df, type_data, ave, sem, ave_raw,
    sem_raw, ave_norm, ave_norm_np):

    ave_col, sem_col, ave_norm_to_initial,
        ave_norm_np, ave_np, sem_np = extract_aves(df, type_data)

    ave.append(ave_np)
    sem.append(sem_np)

    ave_norm.append(ave_norm_to_initial)
    ave_norm_np.append(ave_norm)

    ave_raw.append(ave_col)
    sem_raw.append(sem_col)

def finalize_ave_and_sem(ave_raw, ave_norm):

    ave_raw_df = pd.concat(ave_raw, axis=1)
    ave_norm_df = pd.concat(ave_norm, axis=1)

    df_dimensions = ave_raw_df.shape
    num_cells = df_dimensions[1]

    ave_norm_df['Final Ave'] = ave_norm_df.mean(axis=1)
    ave_norm_df['SEM'] = (ave_norm_df.std(axis=1))/math.sqrt(num_cells)

    final_norm_ave = np.asarray(ave_norm_df['Final Ave'])
    final_norm_sem = np.asarray(ave_norm_df['SEM'])

    return final_norm_ave, final_norm_sem
