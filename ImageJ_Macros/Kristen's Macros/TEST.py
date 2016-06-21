//FUNCTIONS

//Closes all images
function closeAllImages()	{
 	while (nImages>0) {
		selectImage(nImages);
		close();
	}
}

waitForUser("Choose the current directory");
//Choose the upper most directory of the experiment
expt_dir = getDirectory("Choose a Directory");

all_expts = getFileList(expt_dir)

for (i = 0; i < all_expts.length; i++) {
  current_expt = all_expts[i]
  images = getFileList(current_expt + '/')
  //images is an array of folders
  for (j = 0; j < images.length; j++) {
    if (startsWith("488_"))
      //open ImageSequence
      gfp_dir = images[j]

    if (startsWith("561_"))
      //open ImageSequence
      
    if (startsWith("Current"))



  }




}




proc=dir+"LowProcessImages"+File.separator;
File.makeDirectory(proc);
