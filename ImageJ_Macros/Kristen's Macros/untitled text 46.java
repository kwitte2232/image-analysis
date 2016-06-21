makeLine(50,0,50,725);
waitForUser("Adjust line as needed");
profile=getProfile();
row=0;
for (k=0; k<profile.length-1; k+=6) {
	intensity1=profile[k];
	intensity2=profile[k+1];
	//IJ.log("Intensity2="+intensity2);
	intensity3=profile[k+2];
	intensity4=profile[k+3];
	intensity5=profile[k+4];
	intensity6=profile[k+5];
	average=(intensity1+intensity2+intensity3+intensity4+intensity5+intensity6)/6;
	IJ.log("Average Intensity="+average);
	//setResult("Pixel_YCoord", k, k);
	setResult("Intensity", row, average);
	row++;
}