selectWindow("Duplicate_Centers");
run("In");
run("In");
run("In");
makeOval(0,0,10,10);

waitForUser("Place target at target site");

selectWindow("Duplicate_mCherry");
run("In");
run("In");
run("In");
run("Restore Selection");
frames=nSlices();

for(i=1; i<=frames; i++){
	selectWindow("Duplicate_mCherry");
	setSlice(i);
	// run("Restore Selection");
	// 	run("Enlarge...", 5);
	List.setMeasurements;
	maxInt=List.getValue("Max");
	setResult("Target Max", i-1, maxInt);
	updateResults();
	waitForUser("adjust target if needed");
	}