import numpy as np
import pandas as pd
import math

directory = 'Your Directory'

for f in os.listdir(directory):
    if f == "Polarization_Efficiency.csv":
        current_file = os.path.join(directory, f)
        data = pd.read_csv(current_file)
        pol_eff = np.asarray(data['Degrees'])
        ten_hist, ten_bins = np.histogram(pol_eff, np.linspace(0, 180, num=19))
        rounded_ten_bins = [round(b,1) for b in ten_bins]
        ten_binned_angles = {'10 Bins':rounded_ten_bins[1:], '10 Number':ten_hist}
        labels = ['10 Bins', '10 Number']
        dist_by_ten = pd.DataFrame(ten_binned_angles)
        
        
        five_hist, five_bins = np.histogram(pol_eff, np.linspace(0, 180, num=37))
        rounded_five_bins = [math.ceil(b) for b in five_bins]
        five_binned_angles = {'5 Bins':rounded_five_bins[1:], '5 Number':five_hist}
        labels = ['5 Bins', '5 Number']
        dist_by_five = pd.DataFrame(five_binned_angles)
        
        combined_df = [dist_by_ten, dist_by_five]
        distribution = pd.concat(combined_df)
        save_path = os.path.join(directory, "Polarization_Efficiency_Angle_Distribution.csv")
        distribution.to_csv(save_path)

print "Done"