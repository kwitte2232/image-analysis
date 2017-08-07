
#FUNCTIONS

def setup(expt, control, expt_dir, control_dir)

    names_for_expt = expt
    names_for_control = control
                
    expt_pol, expt_ave, expt_sem = extract_all_data(expt_dir, names_for_expt)
    control_pol, control_ave, control_sem = extract_all_data(control_dir, names_for_control)

    return [expt_pol, expt_ave, expt_sem] , [control_pol, control_ave, control_sem]

def extract_all_data(directory, file_names):
    extracted_pol = {}
    extracted_sem = {}
    extracted_ave = {}
    for name in os.listdir(directory): 
        curr_dir = os.path.join(directory, name)
        for f in os.listdir(curr_dir):
            if f == "new_bud_vs_target.csv" or f == "Polarization_Efficiency.csv":
                current_file = os.path.join(curr_dir, f)
                data = pd.read_csv(current_file)
                rads = np.asarray(data['Radians'])
                pol_eff = []
                for r in rads:
                    pol = 1-2*(np.mean(r)/np.pi)
                    pol_eff.append(pol)
                std = np.std(np.asarray(pol_eff))
                num_cells = float(len(pol_eff))
                sem = std/(math.sqrt(num_cells))
            
                extracted_pol[name] = pol_eff
                extracted_sem[name] = sem
                extracted_ave[name] = np.mean(pol_eff)

    data_pol = []
    data_ave = []
    data_sem = []
    for curr_data in file_names:
        pol = extracted_pol[curr_data]
        data_pol.append(pol)
    
        ave = extracted_ave[curr_data]
        data_ave.append(ave)
    
        sem = extracted_sem[curr_data]
        data_sem.append(sem)
    return data_pol, data_ave, data_sem

def plot(expt_pol, expt_ave, expt_sem,  control_pol, control_ave, control_sem, save_to=None)

    expt_positions = [1,4,7]
    control_positions = [2,5,8]

    fig = plt.figure()
    ax = fig.gca()

    expt_bp = plt.boxplot(expt_pol, positions=expt_positions, showbox=False, showcaps=False, meanprops=meanlineprops, meanline=True, showmeans=True, medianprops=medianprops, showfliers=False, whiskerprops=whiskerprops)
    control_bp = plt.boxplot(control_pol, positions=control_positions, showbox=False, showcaps=False, meanprops=meanlineprops, meanline=True, showmeans=True, medianprops=medianprops, showfliers=False, whiskerprops=whiskerprops)
    for i, n in enumerate(expt_positions):
        y = expt_pol[i]
        x = np.random.normal(n, 0.04, size=len(y))
        plt.plot(x, y, color='r', marker='.', linestyle='')
    
    for i, n in enumerate(control_positions):
        y = control_pol[i]
        x = np.random.normal(n, 0.04, size=len(y))
        plt.plot(x, y, color='b', marker='.', linestyle='')
    
    plt.errorbar(expt_positions, expt_ave, yerr=expt_sem, fmt=None, ecolor='k')
    plt.errorbar(control_positions, control_ave, yerr=control_sem, fmt=None, ecolor='k')
    
    plt.xlim(0,9)
    plt.ylim(-1,1)
    ax.set_yticks(np.arange(-1.0,1,0.25))
    plt.rc('grid', linestyle=":", color='black')
    ax.yaxis.grid()

    plt.style.use("dark_background")

    if save_to:
        save_to = save_to
        plt.savefig(save_to)

    plt.show()


expt_dir = 'your_directory'
control_dir = 'your directory'

expt = 'names of experimental directories as a list'
control = 'names of control directories as a list'

expt_data, control_data = setup(expt, control, expt_dir, control_dir)
expt_pol, expt_ave, expt_sem = expt_data[0], expt_data[1], expt_data[2]
control_pol, control_ave, control_sem = control_data[0], control_data[1], control_data[2]

plot(xpt_pol, expt_ave, expt_sem, control_pol, control_ave, control_sem, save_to='Your directory')


