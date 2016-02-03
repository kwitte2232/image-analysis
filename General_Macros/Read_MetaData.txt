// ImageJ Macro
// Kristen Witte
// February 3rd, 2016
//
// Simple macro to read the metadata from an image and store it as text file
// for parsing.

waitForUser("Choose a directory containing the image stacks to be processed.\n \nThe directory should contain only image stacks to be processed.");

dir = getDirectory("Choose a directory");

expt_dir = dir + "Experiments/";

list = getFileList(expt_dir);

for (i = 0; i < list.length; i++) {

  curr_field = list[i];
  data = getFileList(expt_dir + curr_field);

  dual = data[0];
  dual_data = getFileList(expt_dir + curr_field + dual);
  open(expt_dir + curr_field + dual + dual_data[2]);
    //Opens the first GFP image in the folder
  rename("GFP");
  run("Show Info...");
  selectWindow("Info for GFP");
  saveAs("text", expt_dir + curr_field + "GFP_metadata.txt");

  selectWindow("Info for GFP");
  run("Close");
  selectWindow("GFP");
  run("Close");
}
