import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

#FUNCTIONS

def get_data(curr_data, space):
    data = pd.read_csv(curr_data)
    angle_data = np.asarray(data['Radians'])

    shape = angle_data.shape
    print shape
    N = shape[0] #number of data points

    hist, bin_edges = np.histogram(angle_data, 180)

    to_list = list(prev_hist)
    to_float = [float(x) for x in prev_list]
    percent = [x/25*100 for x in prev_float]
    
    bottom = 8
    max_height = 4

    theta = np.linspace((0.0+space), np.pi, 180, endpoint=False)

    radii = percent
    width = (np.pi)/((180))
    
    return [theta, radii, width, bottom]


def plot_angle_distribution(angle_data, save_to=None)
    
    ax = plt.subplot(111, polar=True)
    bars = ax.bar(angle_data[0], angle_data[1], width=angle_data[2], color="#bebebe", bottom=angle_data[3])
    ax.set_yticklabels([])

    ax.set_rmax(60)

    if save_to:
        plt.savefig(save_to)

    plt.show()


#BODY
bud_angles = 'your directory that with a csv file that compares the angle of two points'
angle_data = get_data(bud_angles, 0)
plot_angle_distribution(angle_data, save_to='Your directory')

