

lines=10;
x=96;

for(i=1; i<=lines; i++){
	selectWindow("Duplicate_mCh");
	makeLine(x,0,x,82);
	run("Add Selection...");
	run("Overlay Options...", "stroke=white width=0 apply");
	x=x+96;
	}

lines=10;
x=96;

for(i=1; i<=lines; i++){
	selectWindow("Duplicate_Tar");
	makeLine(x,0,x,82);
	run("Add Selection...");
	run("Overlay Options...", "stroke=white width=0 apply");
	x=x+96;
	}


lines=10;
x=96;

for(i=1; i<=lines; i++){
	selectWindow("Duplicate");
	makeLine(x,0,x,82);
	run("Add Selection...");
	run("Overlay Options...", "stroke=white width=0 apply");
	x=x+96;
	}

lines=2
y=82

for(i=1; i<=lines; i++){
	selectWindow("Duplicate");
	makeLine(0,y,672,y);
	run("Add Selection...");
	run("Overlay Options...", "stroke=white width=0 apply");
	y=y+82;
	}






makeLine(x,0,x,246);
run("Add Selection...");
run("Overlay Options...", "stroke=white width=0 apply");

makeLine(x,0,x,246);
run("Add Selection...");
run("Overlay Options...", "stroke=white width=0 apply");