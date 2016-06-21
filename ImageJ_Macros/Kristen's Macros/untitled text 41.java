selectWindow("Kymo_HM_Centers.tif");
frames=nSlices;
run("Duplicate...", "title=Centers_Duplicate duplicate range=1-["+frames+"]");

selectWindow("dsRED_Stack.tif");
run("Duplicate...", "title=dsRED_Duplicate duplicate range=1-["+frames+"]");

n=0;
selectWindow("Centers_Duplicate");
run("In");
run("In");
run("In");
selectWindow("dsRED_Duplicate");
run("In");
run("In");
run("In");

x=0;
y=0;

centers=getBoolean("Measure Targets?"); 
	if (centers) {
		do {
			n++;
			target=n-1;
			selectWindow("Centers_Duplicate");
			makeRectangle(x,y,20,6);
			waitForUser("Set Slice and Place rectangle in line with target n-1 at t1/2 of target n");
			slice=getSliceNumber();
			selectWindow("dsRED_Duplicate");
			setSlice(slice);
			run("Restore Selection");
			List.setMeasurements;
			n1_Min=List.getValue("Min");
			setResult("Min_Target_n-1", target, n1_Min);
			n1_Max=List.getValue("Max");
			setResult("Max_Target_n-1", target, n1_Max);
			n1_IntDen=List.getValue("IntDen");
			setResult("IntDen_Target_n-1", target, n1_IntDen);
			n1_Mean=List.getValue("Mean");
			setResult("Mean_Target_n-1", target, n1_Mean);
			
			waitForUser("Place rectangle in line with target n at t1/2 of target n");
			getSelectionBounds(x,y,width,height);
			time=y/6;
			selectWindow("dsRED_Duplicate");
			run("Restore Selection");
			List.setMeasurements;
			n_Min=List.getValue("Min");
			setResult("Min_Target_n", target, n_Min);
			n_Max=List.getValue("Max");
			setResult("Max_Target_n", target, n_Max);
			n_IntDen=List.getValue("IntDen");
			setResult("IntDen_Target_n", target, n_IntDen);
			n_Mean=List.getValue("Mean");
			setResult("Mean_Target_n", target, n_Mean);
			setResult("Time", target, time);
			
			anotherCenter=getBoolean("Is there another set of targets?");
			} while(anotherCenter);
		}	
	

selectWindow("Centers_Duplicate");
makeRectangle(0,0,20,6);
waitForUser("Place rectangle for background measurement");
List.setMeasurements;
bg_Mean=List.getValue("Mean");
setResult("Background_Mean", 0, bg_Mean);
bg_IntDen=List.getValue("IntDen");
setResult("Background_IntDen", 0, bg_IntDen);
			
			
	