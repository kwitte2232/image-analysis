import pandas as pd
import numpy as np
import scipy.stats
import math

#FUNCTIONS

def extract_pol_eff_data(df):

    target_x = list(df['Targets X'])
    bud_x = list(df['Bud X'])
    prev_x = list(df['Prev X'])
    pol_eff = []
    difference = []
    rads = []
    for targ, bud in zip(target_x, bud_x):
        diff = abs(targ - bud)
        if diff > 50:
            diff = 100 - diff
        r = diff*(np.pi/50)
        pol = 1-2*(np.mean(r)/np.pi)
        pol_eff.append(pol)
        difference.append(diff)
        rads.append(r)

    return pol_eff

def find_ave_sem(pol):
    std = np.std(np.asarray(pol))
    num_cells = float(len(pol))
    sem = std/(math.sqrt(num_cells))
    ave = np.mean(pol)
    median = np.median(pol)
    print median, std
    return ave, sem

def from_rads_get_pol(df):
    rads = np.asarray(df['Radians'])
    pol_eff = []
    for r in rads:
        pol = 1-2*(np.mean(r)/np.pi)
        pol_eff.append(pol)
    return pol_eff

#BODY

cdc24_expt_file = "~/PositiveFeedback_Data/rsr1_data/8278_dip_polarization/Expt/expt_combined.csv"
cdc24_expt_data = pd.read_csv(cdc24_expt_file)

cdc24_control_file = "~/rsr1_data/8278_dip_polarization/Control/control_combined.csv"
cdc24_control_data = pd.read_csv(cdc24_control_file)

bem1_expt_file = "~/PositiveFeedback_Data/rsr1_data/07242016_357rsr1_PolAndControl/Polarization/expt_targets_vs_new_bud.csv"
bem1_control_file = "~/PositiveFeedback_Data/rsr1_data/07242016_357rsr1_PolAndControl/Control/control_targets_vs_new_bud.csv"

bem1_expt_data = pd.read_csv(bem1_expt_file)
bem1_control_data = pd.read_csv(bem1_control_file)

bem_expt = from_rads_get_pol(bem1_expt_data)
bem_ave, bem_sem = find_ave_sem(bem_expt)

bem_control = from_rads_get_pol(bem1_control_data)
bem_control_ave, bem_control_sem = find_ave_sem(bem_control)

cdc24_expt = extract_pol_eff_data(cdc24_expt_data)
cdc24_ave, cdc24_sem = find_ave_sem(cdc24_expt)

cdc24_control = extract_pol_eff_data(cdc24_control_data)
cdc24_control_ave, cdc24_control_sem = find_ave_sem(cdc24_control)

expt_positions = [1.5,3.5]
control_positions = [2,4]

expt = [cdc24_expt, bem_expt]
control = [cdc24_control, bem_control]

expt_sem = [cdc24_sem, bem_sem]
control_sem = [cdc24_control_sem, bem_control_sem]

fig = plt.figure()
ax = fig.gca()

expt_bp = plt.boxplot(expt, positions=expt_positions, showbox=False, showcaps=False, meanprops=meanlineprops, meanline=True, showmeans=True, medianprops=medianprops, showfliers=False, whiskerprops=whiskerprops)
control_bp = plt.boxplot(control, positions=control_positions, showbox=False, showcaps=False, meanprops=meanlineprops, meanline=True, showmeans=True, medianprops=medianprops, showfliers=False, whiskerprops=whiskerprops)

for i, n in enumerate(expt_positions):
    y = expt[i]
    x = np.random.normal(n, 0.04, len(y))
    plt.plot(x, y, color='r', marker='.', linestyle='')
    
for i, n in enumerate(control_positions):
    y = control[i]
    x = np.random.normal(n, 0.04, len(y))
    plt.plot(x, y, color='b', marker='.', linestyle='')
    
plt.errorbar(expt_positions, [cdc24_ave, bem_ave], yerr=expt_sem, fmt=None, ecolor='k')
plt.errorbar(control_positions, [cdc24_control_ave, bem_control_ave], yerr=control_sem, fmt=None, ecolor='k')
    
plt.xlim(1,4.5)
plt.ylim(-1,1)
ax.set_yticks(np.arange(-1.0,1,0.25))
plt.rc('grid', linestyle=":", color='black')
ax.yaxis.grid()

save_to = "~/PositiveFeedback_Data/rsr1_data/Rsr1_Polarization_Efficiency.pdf"
plt.savefig(save_to)

plt.show()