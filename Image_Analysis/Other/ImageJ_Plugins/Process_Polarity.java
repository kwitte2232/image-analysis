import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.FileInfo;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder.Method;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import snakeprogram.IntensityEnergy;
import snakeprogram.TwoDContourDeformation;

public class Process_Polarity
  implements PlugIn
{
  final double version = 0.4D;
  final boolean verbose = true;
  final boolean debugging = false;
  final String debugSourceImage = "/Users/devin/Documents/Lab/Data/Microscopy/Yeast/Polarity/TestImages/test4/NomStack02.stk";

  ImagePlus sourceImage = null;
  ImagePlus reporter = null;
  ImagePlus points = null;
  ImagePlus edges = null;
  ImagePlus offsets = null;
  ImagePlus masks = null;
  ImagePlus energy = null;

  String baseName = null;
  String expN = null;
  String dir = null;
  String procDir = null;
  String snakeDir = null;
  String dataDir = null;
  String kymoDir = null;
  String logFile = null;
  String dcPrefix = null;
  static final int BRIGHTFIELD = 0;
  static final int M_CHERRY = 1;
  static final int REGIONS = 2;
  final String[] rawImagePrefixes = { "NomStack", "mChStack", "Exp" };
  boolean[] foundRawImageFiles = { false, false, false };
  static final int CENTERS = 0;
  static final int BUDS = 1;
  final String[] oldStyleSuffixes = { "_centers.tif", "_buds.tif" };
  boolean[] foundOldStyleFiles = { false, false };
  static final int REPORTER = 0;
  static final int POINTS = 1;
  static final int EDGES = 2;
  static final int MASKS = 3;
  static final int ENERGY = 4;
  ImagePlus[] allImages = { this.reporter, this.points, this.edges, this.masks, this.energy };
  final String[] newStylePrefixes = { "Reporter", "Points", "Edges", "Masks", "Energy" };
  boolean[] foundNewStyleFiles = { false, false, false, false, false };
  boolean foundKymographs = false;

  int nTargetsDetected = 0;
  int nCentersDetected = 0;
  int nBudsDetected = 0;
  boolean targetsOK = false;
  boolean centersOK = false;
  boolean budsOK = false;
  boolean useFoundCenters = false;
  boolean useFoundBuds = false;
  static final int ASSEMBLE_AND_FILTER = 0;
  static final int TRACK_CENTERS = 1;
  static final int MARK_BUDS = 2;
  static final int MAKE_KYMOGRAPHS = 3;
  static final int EXTRACT_DATA = 4;
  final String[] steps = { "Assemble & filter", "Track centers", "Mark buds", "Make kymographs", "Extract data" };
  int nextStep = 0;
  ArrayList<Point> initTargets = new ArrayList();

  int width = 0;
  int height = 0;
  int slices = 0;
  int[] xOffs = null;
  int[] yOffs = null;

  ImagePlus circleMasks = null;
  ImagePlus ringMasks = null;
  ImagePlus wedgeMasks = null;
  ImagePlus reporterMasks = null;

  final int margin = 20;
  final int maxRadius = 20;
  final int mDiam = 41;
  final int nCircles = 21;
  final int nRings = 20;
  final int nWedges = 16;
  final int tNum = 5;
  final int bNum = 5;
  final int nTargetEnergies = 3;
  final int snakeThickness = 3;
  final double wAngle = 0.3926990816987241D;
  static final int TAR_X = 0;
  static final int TAR_Y = 1;
  static final int TAR_E = 2;
  int nCells = 0;
  boolean arraysInitialized = false;
  int[] cellNumbers = null;
  Point[] initCenters = null;
  boolean[] cellDiscarded = null;
  Point[][] centers = (Point[][])null;
  int[] cRadii = null;
  PolygonRoi[][] snakePolygons = (PolygonRoi[][])null;
  int[][] cAreas = (int[][])null;
  double[][] repint = (double[][])null;
  int[][][][] targets = (int[][][][])null;
  double[][][] tAngles = (double[][][])null;
  double[] zeroAngles = null;
  Point[][] buds = (Point[][])null;
  double[][] bAngles = (double[][])null;
  int[][] bSlices = (int[][])null;
  int[] nBuds = null;
  static final int ALPHA = 0;
  static final int BETA = 1;
  static final int GAMMA = 2;
  static final int WEIGHT = 3;
  static final int STRETCH = 4;
  static final int ITERATIONS = 5;
  static final int SPACING = 6;
  final double[] initParams = { 1.0D, 2000.0D, 40.0D, 1.0D, 10.0D, 1000.0D, 1.0D };
  final double[] trackParams = { 1.0D, 2000.0D, 40.0D, 1.0D, 10.0D, 5.0D, 3.0D };
  final int circlePoints = 200;

  ImagePlus[] kymographs = null;

  double startTime = 0.0D;
  double thisStopTime = 0.0D;
  double lastStopTime = 0.0D;
  double timeThisTook = 0.0D;
  double totalTime = 0.0D;
  StringBuffer logBuffer = new StringBuffer();
  DateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy 'at' HH:mm:ss Z");
  Date now = null;
  boolean madeDataFile = false;
  boolean writeHeaders = false;
  boolean errorThrown = false;

  public void run(String paramString)
  {
    this.sourceImage = WindowManager.getCurrentImage();

    this.baseName = this.sourceImage.getTitle();
    this.dir = this.sourceImage.getOriginalFileInfo().directory;
    this.sourceImage.close();

    this.startTime = System.currentTimeMillis();
    this.lastStopTime = this.startTime;
    this.now = new Date();

    setupFilesAndLogging(); //Class Line 261
    logTime("Method setupFilesAndLogging");

    seeWhatsBeenDone(); //Class Line 339
    logTime("Method seeWhatsBeenDone");

    figureOutWhatToDo(); //Class Line 423
    logTime("Method figureOutWhatToDo");

    if (this.nextStep == 0) {
      assembleAndFilter(); //Class Line 493

      arrayOfInt = new int[] { 0, 1, 2 };
      saveImageFiles(arrayOfInt); //Class Line 1265

      logTime("Method assembleAndFilter");
      this.nextStep = 1;
    }
    else {
      getInitTargetsFromImage(); //Class Line 602
      logTime("Method getInitTargetsFromImage");
    }

    setNCells(this.initTargets.size()); //Class Line 1193
    initializeDataArrays(); //Class Line 1202
    makeCircleMasks(); //Class Line 626
    logTime("Methods setNCells, initializeDataArrays and makeCircleMasks");

    if (this.useFoundBuds) {
      getAllBudsFromImage(); //Class Line 1340
    }

    if (this.nextStep == 1) {
      findInitCenters(); //Class Line 643
      logTime("Method findInitCenters");
      findSnakes(); //Class Line 749

      arrayOfInt = new int[] { 0, 1, 2 };
      saveImageFiles(arrayOfInt); //Class Line 1265

      logTime("Method findSnakes");
      this.nextStep = 2;
    }
    else {
      loadSnakesFromFiles(); //Class Line 807
      checkROICentersAgainstImage(); //Class Line 1144
      logTime("Method checkROICentersAgainstImage");
    }

    getAllTargetsFromImage(); //Class Line 1276

    if (this.nextStep == 2) {
      markBuds(); //Class Line 841

      arrayOfInt = new int[] { 0, 1, 2 };
      saveImageFiles(arrayOfInt); //Class Line 1265

      this.nextStep = 3;
    }

    getAllBudsFromImage(); //Class Line 1340

    if (this.nextStep == 3) {
      makeKymographs(); //Class Line 962

      this.nextStep = 4;
    }

    int[] arrayOfInt = { 3, 4 };
    saveImageFiles(arrayOfInt); //Class Line 1265

    GenericDialog localGenericDialog = new GenericDialog("Process Polarity");
    localGenericDialog.addStringField("Enter a prefix for data columns.", "", 10);
    localGenericDialog.hideCancelButton();
    localGenericDialog.showDialog();
    this.dcPrefix = localGenericDialog.getNextString();
    this.dcPrefix = (this.dcPrefix.endsWith("_") ? this.dcPrefix : this.dcPrefix.concat("_"));

    writeCellData(); //Class Line 1639
    writePopulationData(); //Class Line 1695
    finishUp(); //Class Line 1016
  }

  private void setupFilesAndLogging()
  {
    boolean bool1 = false;
    boolean bool2 = false;
    boolean bool3 = false;
    boolean bool4 = false;
    if ((this.baseName.startsWith(this.rawImagePrefixes[0])) || (this.baseName.startsWith(this.rawImagePrefixes[1]))) {
      this.expN = this.baseName.substring(8, 10);
      this.procDir = (this.dir + "processedImages/");
      if (!fileExists(this.procDir)) {
        bool1 = new File(this.procDir).mkdirs();
        if (!bool1) {
          quitWithError("Could not create a directory for processed images.");
        }
      }
    }
    else if (this.dir.endsWith("processedImages/")) {
      this.procDir = this.dir;
      this.dir = this.dir.replace("processedImages/", "");
      if (this.baseName.startsWith("Composite")) {
        this.expN = this.baseName.substring(9, 11);
      }
      else {
        for (int i = 0; i < this.newStylePrefixes.length; i++) {
          if (this.baseName.startsWith(this.newStylePrefixes[i]))
            this.expN = this.baseName.substring(this.newStylePrefixes[i].length(), this.newStylePrefixes[i].length() + 2);
        }
      }
    }
    else
    {
      quitWithError("The selected file or directory structure is not valid.");
    }

    this.snakeDir = (this.procDir + "snakes/");
    if (!fileExists(this.snakeDir)) {
      bool2 = new File(this.snakeDir).mkdirs();
      if (!bool2) {
        quitWithError("Could not create a directory for snakes.");
      }
    }

    this.dataDir = (this.procDir + "data/");
    if (!fileExists(this.dataDir)) {
      bool3 = new File(this.dataDir).mkdirs();
      if (!bool3) {
        quitWithError("Could not create a directory for data.");
      }
    }

    this.kymoDir = (this.procDir + "kymographs/");
    if (!fileExists(this.kymoDir)) {
      bool4 = new File(this.kymoDir).mkdirs();
      if (!bool4) {
        quitWithError("Could not create a directory for kymographs.");
      }

    }

    this.logFile = (this.procDir + "Log" + this.expN + ".txt");
    this.logBuffer.append("Process Polarity ImageJ plugin version 0.4 by Devin Strickland.");

    if (fileExists(this.logFile)) {
      this.logBuffer.append("\n");
      this.logBuffer.append("\nNew data processing initiated on " + this.dateFormat.format(this.now));
    }
    else {
      this.logBuffer.append("\n");
      this.logBuffer.append("\nLog file created and new data processing initiated on " + this.dateFormat.format(this.now));
    }
    if (bool1) {
      this.logBuffer.append("\nMade new processedImages directory: " + this.procDir);
    }

    if (RoiManager.getInstance() != null)
      RoiManager.getInstance().close();
  }

  private void seeWhatsBeenDone()
  {
    this.logBuffer.append("\n");

    for (int i = 0; i < this.rawImagePrefixes.length - 1; i++) {
      if (fileExists(this.dir + this.rawImagePrefixes[i] + this.expN + ".stk")) {
        this.foundRawImageFiles[i] = true;
        this.logBuffer.append("\nFound file: " + this.rawImagePrefixes[i] + this.expN + ".stk in experiment directory.");
      }

    }

    for (i = 0; i < this.oldStyleSuffixes.length; i++) {
      if (fileExists(this.procDir + "Composite" + this.expN + this.oldStyleSuffixes[i])) {
        this.foundOldStyleFiles[i] = true;
        this.logBuffer.append("\nFound file: Composite" + this.expN + this.oldStyleSuffixes[i] + " in processedImages directory.");
      }

    }

    for (i = 0; i < this.newStylePrefixes.length; i++) {
      if (fileExists(this.procDir + this.newStylePrefixes[i] + this.expN + ".tif")) {
        this.foundNewStyleFiles[i] = true;
        this.logBuffer.append("\nFound file: " + this.newStylePrefixes[i] + this.expN + ".tif in processedImages directory.");
      }

    }

    if (fileExists(this.procDir + "Registration" + this.expN + ".txt")) {
      this.logBuffer.append("\nFound file: Registration" + this.expN + ".txt in processedImages directory.");
    }

    if (fileExists(this.procDir + "Angles" + this.expN + ".txt")) {
      this.logBuffer.append("\nFound file: Angles" + this.expN + ".txt in processedImages directory.");
    }

    for (i = 1; i < 10; i++) {
      if (fileExists(this.kymoDir + "Cell0" + i + "_kymo.tif")) {
        this.foundKymographs = true;
      }
    }
    if (this.foundKymographs) {
      this.logBuffer.append("\nFound kymograph files.");
    }

    if ((this.foundNewStyleFiles[0] != 0) && (this.foundNewStyleFiles[1] != 0) && (this.foundNewStyleFiles[2] != 0)) {
      this.reporter = IJ.openImage(this.procDir + this.newStylePrefixes[0] + this.expN + ".tif");
      this.reporter.setTitle("Reporter");
      this.points = IJ.openImage(this.procDir + this.newStylePrefixes[1] + this.expN + ".tif");
      this.points.setTitle("Points");
      this.edges = IJ.openImage(this.procDir + this.newStylePrefixes[2] + this.expN + ".tif");
      this.edges.setTitle("Edges");
      detectAndValidatePoints();
      if (this.centersOK) {
        this.nextStep = 2;
      }
      if (this.budsOK) {
        this.nextStep = 3;
      }
      if (this.foundKymographs) {
        this.nextStep = 4;
      }

    }
    else if (this.foundOldStyleFiles[1] != 0) {
      splitChannels(IJ.openImage(this.procDir + "Composite" + this.expN + this.oldStyleSuffixes[1]));
      detectAndValidatePoints();
      this.nextStep = 1;
    }
    else if (this.foundOldStyleFiles[0] != 0) {
      splitChannels(IJ.openImage(this.procDir + "Composite" + this.expN + this.oldStyleSuffixes[0]));
      detectAndValidatePoints();
      this.nextStep = 1;
    }
    else if ((this.foundRawImageFiles[0] != 0) && (this.foundRawImageFiles[1] != 0)) {
      this.nextStep = 0;
    }
    else
    {
      quitWithError("Failed to find required files.");
    }
    this.logBuffer.append("\n");
  }

  private void figureOutWhatToDo()
  {
    if (this.nextStep > 0) {
      GenericDialog localGenericDialog = new GenericDialog("Process Polarity");
      StringBuffer localStringBuffer1 = new StringBuffer();
      StringBuffer localStringBuffer2 = new StringBuffer();
      localStringBuffer1.append("Image files found:");

      for (int i = 0; i < this.rawImagePrefixes.length; i++) {
        if (this.foundRawImageFiles[i] != 0) {
          localStringBuffer1.append("\n     " + this.rawImagePrefixes[i] + this.expN + ".stk");
        }
      }

      for (i = 0; i < this.oldStyleSuffixes.length; i++) {
        if (this.foundOldStyleFiles[i] != 0) {
          localStringBuffer1.append("\n     Composite" + this.expN + this.oldStyleSuffixes[i]);
        }
      }

      for (i = 0; i < this.newStylePrefixes.length; i++) {
        if (this.foundNewStyleFiles[i] != 0) {
          localStringBuffer1.append("\n     " + this.newStylePrefixes[i] + this.expN + ".tif");
        }
      }

      localStringBuffer2.append("Point data:");
      localStringBuffer2.append("\n     Detected " + this.nTargetsDetected + " targets.");
      localStringBuffer2.append("\n     Detected " + this.nCentersDetected + " cell centers.");
      localStringBuffer2.append("\n     Detected " + this.nBudsDetected + " buds.");

      localGenericDialog.addMessage(localStringBuffer1.toString());
      localGenericDialog.addMessage(localStringBuffer2.toString());

      localGenericDialog.setInsets(20, 20, 5);
      String[] arrayOfString;
      switch (this.nextStep) {
      case 1:
        arrayOfString = new String[] { this.steps[0], this.steps[1] };
        localGenericDialog.addChoice("Step to begin at: ", arrayOfString, this.steps[this.nextStep]);
        break;
      case 2:
        arrayOfString = new String[] { this.steps[0], this.steps[1], this.steps[2] };
        localGenericDialog.addChoice("Step to begin at: ", arrayOfString, this.steps[this.nextStep]);
        break;
      case 3:
        arrayOfString = new String[] { this.steps[0], this.steps[1], this.steps[2], this.steps[3] };
        localGenericDialog.addChoice("Step to begin at: ", arrayOfString, this.steps[this.nextStep]);
        break;
      case 4:
        arrayOfString = new String[] { this.steps[0], this.steps[1], this.steps[2], this.steps[3], this.steps[4] };
        localGenericDialog.addChoice("Step to begin at: ", arrayOfString, this.steps[this.nextStep]);
        break;
      }

      localGenericDialog.setInsets(20, 20, 0);
      localGenericDialog.addCheckbox("Use found centers as starting point.", true);
      localGenericDialog.addCheckbox("Use found bud locations.", false);

      localGenericDialog.showDialog();
      if (localGenericDialog.wasCanceled()) {
        throw new RuntimeException("Macro canceled");
      }

      this.nextStep = localGenericDialog.getNextChoiceIndex();
      this.useFoundCenters = localGenericDialog.getNextBoolean();
      this.useFoundBuds = localGenericDialog.getNextBoolean();
    }
  }

  private void assembleAndFilter()
  {
    this.edges = IJ.openImage(this.dir + "NomStack" + this.expN + ".stk");
    this.edges.setTitle("Edges");
    this.allImages[2] = this.edges;
    updateStackDimensions();
    IJ.run(this.edges, "Find Edges", "stack");

    int[] arrayOfInt = new int[256];
    int i = 0;
    int j = 255;
    for (int k = 0; k < this.slices; k++) {
      ImageProcessor localImageProcessor1 = this.edges.getStack().getProcessor(k + 1);
      localImageProcessor1.setAutoThreshold(AutoThresholder.Method.Default, true);
      i = (int)localImageProcessor1.getMinThreshold();
      for (int m = 0; m < 256; m++) {
        if (m <= i)
          arrayOfInt[m] = 0;
        else if (m >= j)
          arrayOfInt[m] = 255;
        else
          arrayOfInt[m] = ((int)((m - i) / (j - i) * 255.0D));
      }
      localImageProcessor1.applyTable(arrayOfInt);
      localImageProcessor1.smooth();
    }

    this.reporter = IJ.openImage(this.dir + "mChStack" + this.expN + ".stk");
    this.reporter.setTitle("Reporter");
    this.allImages[0] = this.reporter;
    IJ.run(this.reporter, "Subtract Background...", "rolling=256 stack");

    updateStackDimensions();
    this.points = IJ.createImage("temp", "8-bit White", this.width, this.height, this.slices);

    this.xOffs = new int[this.slices];
    this.yOffs = new int[this.slices];

    Point localPoint = new Point();

    ZProjector localZProjector = new ZProjector(this.reporter);
    localZProjector.setStartSlice(1); localZProjector.setStopSlice(this.slices); localZProjector.setMethod(1);
    localZProjector.doProjection();
    ImagePlus localImagePlus1 = localZProjector.getProjection();
    this.reporter.getProcessor().setMinAndMax(0.0D, getUpper(localImagePlus1.getProcessor().getHistogram()));
    localImagePlus1.close();
    IJ.run(this.reporter, "8-bit", "");

    RGBStackMerge localRGBStackMerge = new RGBStackMerge();
    ImageStack localImageStack = localRGBStackMerge.mergeStacks(this.width, this.height, this.slices, this.reporter.getStack(), this.points.getStack(), this.edges.getStack(), false);
    this.reporter.close();
    this.points.close();
    this.edges.close();
    ImagePlus localImagePlus2 = new ImagePlus("Composite", localImageStack);
    StackProcessor localStackProcessor = new StackProcessor(localImageStack, null);
    this.width -= 40;
    this.height -= 40;
    localImageStack = localStackProcessor.crop(20, 20, this.width, this.height);
    localImagePlus2.setStack(localImageStack);
    IJ.runPlugIn(localImagePlus2, "StackReg_", "transformation=[Translation]");
    splitChannels(localImagePlus2);
    this.edges.setStack(boxcar(this.edges, 3));
    ImageProcessor localImageProcessor2;
    int i3;
    for (int n = 0; n < this.slices; n++) { localImageProcessor2 = this.points.getStack().getProcessor(n + 1);
      localImageProcessor2.invert();
      int i1 = 0; int i2 = 0; i3 = 0; int i4 = 0;

      int i9 = 0;
      int i5;
      int i6;
      int i7;
      int i8;
      do { i5 = localImageProcessor2.getPixel(i9, this.height / 2);
        i1 += i5 / 255;
        i6 = localImageProcessor2.getPixel(this.width / 2, i9);
        i2 += i6 / 255;
        i7 = localImageProcessor2.getPixel(this.width - 1 - i9, this.height / 2);
        i3 += i7 / 255;
        i8 = localImageProcessor2.getPixel(this.width / 2, this.height - 1 - i9);
        i4 += i8 / 255;
        i9++; }
      while (i5 + i6 + i7 + i8 > 0);

      this.xOffs[n] = (i1 - i3 - 20);
      this.yOffs[n] = (i2 - i4 - 20);
    }
    this.points = IJ.createImage("Targets", "8-bit Black", this.width, this.height, this.slices);
    this.points.setTitle("Points");
    this.allImages[1] = this.points;

    for (n = 0; n < this.slices; n++) {
      localImageProcessor2 = this.points.getStack().getProcessor(n + 1);
      IJ.run("Metamorph ROI", "open=" + this.dir + "Regions" + this.expN + "/Exp" + this.expN + "tar" + (n + 1) + ".rgn");
      RoiManager localRoiManager = RoiManager.getInstance2();
      Roi[] arrayOfRoi = localRoiManager.getRoisAsArray();
      for (i3 = 0; i3 < arrayOfRoi.length; i3++) {
        Rectangle localRectangle = arrayOfRoi[i3].getBounds();
        localPoint.x = (localRectangle.x + this.xOffs[n] + localRectangle.width / 2);
        localPoint.y = (localRectangle.y + this.yOffs[n] + localRectangle.height / 2);
        localImageProcessor2.putPixel(localPoint.x, localPoint.y, 254);
        if (n == 0) {
          this.initTargets.add(new Point(localPoint));
        }
      }
      localRoiManager.close();
    }
  }

  private void getInitTargetsFromImage()
  {
    if (this.targetsOK) {
      if (this.initTargets.isEmpty())
      {
        ImageProcessor localImageProcessor = this.points.getStack().getProcessor(1);
        int i = localImageProcessor.getWidth();
        int j = localImageProcessor.getHeight();
        for (int k = 0; k < i; k++) {
          for (int m = 0; m < j; m++) {
            if (localImageProcessor.get(k, m) == 254)
              this.initTargets.add(new Point(k, m));
          }
        }
      }
      else
      {
        quitWithError("Tried to fill initTargets ArrayList twice.");
      }
    }
    else
      quitWithError("Cannot fill initTargets ArrayList because no targets were detected.");
  }

  private void makeCircleMasks()
  {
    this.circleMasks = IJ.createImage("Circle Masks", "8-bit black", 41, 41, 21);
    this.ringMasks = IJ.createImage("Ring Masks", "8-bit black", 41, 41, 20);
    ImageProcessor localImageProcessor = this.circleMasks.getImageStack().getProcessor(1).duplicate();

    for (int i = 0; i < 21; i++) {
      localImageProcessor.setRoi(new OvalRoi(20 - i, 20 - i, 2 * i + 1, 2 * i + 1));
      this.circleMasks.getStack().getProcessor(i + 1).copyBits(localImageProcessor.getMask(), 20 - i, 20 - i, 0);
    }

    for (i = 0; i < 20; i++) {
      this.ringMasks.getStack().getProcessor(i + 1).copyBits(this.circleMasks.getStack().getProcessor(i + 2), 0, 0, 0);
      this.ringMasks.getStack().getProcessor(i + 1).copyBits(this.circleMasks.getStack().getProcessor(i + 1), 0, 0, 11);
    }
  }

  private void findInitCenters()
  {
    if (!this.arraysInitialized) {
      quitWithError("Data arrays have not been initialized.");
    }
    updateStackDimensions();

    Point localPoint = new Point();
    Rectangle localRectangle = new Rectangle();
    double d = 0.0D;
    int i = 0;

    ImageProcessor localImageProcessor = this.edges.getStack().getProcessor(1);
    localImageProcessor.setAutoThreshold(AutoThresholder.Method.IsoData, false);
    d = localImageProcessor.getMaxThreshold();
    localImageProcessor.resetThreshold();

    if (this.useFoundCenters) {
      getInitCentersFromImage();
      wipeCenters();
    }
    else {
      this.edges.show();
    }

    for (int j = 0; j < this.nCells; j++) {
      if (this.useFoundCenters) {
        localPoint.setLocation(this.initCenters[j]);
      }
      else {
        localRectangle.setBounds(((Point)this.initTargets.get(j)).x - 25, ((Point)this.initTargets.get(j)).y - 25, 50, 50);
        this.edges.setRoi(localRectangle);
        IJ.run(this.edges, "To Selection", "");

        OvalRoi localOvalRoi = new OvalRoi(((Point)this.initTargets.get(j)).x - 2, ((Point)this.initTargets.get(j)).y - 2, 5, 5);
        this.edges.setRoi(localOvalRoi, true);

        WaitForUserDialog localWaitForUserDialog = new WaitForUserDialog("Process Polarity", "Move the ROI to the center of the cell associated with this target.");
        localWaitForUserDialog.show();

        localPoint.setLocation(localOvalRoi.getBounds().x + localOvalRoi.getBounds().width / 2, localOvalRoi.getBounds().y + localOvalRoi.getBounds().height / 2);
      }

      i = growFromInitialCenter(localImageProcessor, localPoint, d);

      this.cRadii[j] = growAlongGradient(j, i, localImageProcessor, localPoint, d);
    }
    this.edges.deleteRoi();
    this.edges.hide();
  }

  private void getInitCentersFromImage()
  {
    if (this.nCells != 0) {
      ImageProcessor localImageProcessor = this.points.getStack().getProcessor(1);
      int i = 0;

      for (int n = 0; n < this.height; n++) {
        for (int i1 = 0; i1 < this.width; i1++) {
          int k = localImageProcessor.get(i1, n);
          if ((k > 0) && (k <= this.nCells)) {
            int j = localImageProcessor.get(i1 - 1, n);
            int m = k - 1;
            if (j == 255) {
              this.initCenters[m] = new Point(i1 - 1, n);
              this.cellNumbers[m] = k;
              this.cellDiscarded[m] = false;
              i++;
            }
            else if (j == 253) {
              this.initCenters[m] = new Point(i1 - 1, n);
              this.cellNumbers[m] = k;
              this.cellDiscarded[m] = true;
              i++;
            }
            else {
              quitWithError("Detected index pixel but did not find corresponding center pixel at (x-1, y).");
            }
          }
        }
      }
      if (i != this.nCells)
        quitWithError("Failed to find the correct number of centers.");
    }
    else
    {
      quitWithError("Cannot fill initCenters ArrayList because nCells = 0.");
    }
  }

  private void wipeCenters()
  {
    for (int k = 0; k < this.slices; k++) {
      ImageProcessor localImageProcessor = this.points.getStack().getProcessor(k + 1);
      int i = localImageProcessor.getPixelCount();

      for (int m = 0; m < i; m++) {
        int j = localImageProcessor.get(m);
        if ((j == 255) || (j == 253)) {
          localImageProcessor.set(m, 0);
          localImageProcessor.set(m + 1, 0);
        }
      }
    }
  }

  private void findSnakes()
  {
    if (!this.arraysInitialized) {
      quitWithError("Data arrays have not been initialized.");
    }
    updateStackDimensions();

    for (int i = 0; i < this.nCells; i++) {
      int j = this.cellNumbers[i];
      Object localObject = makeArrayListFromCircle(this.initCenters[i], this.cRadii[i]);
      RoiManager localRoiManager = new RoiManager(true);
      this.errorThrown = false;
      for (int k = 0; k < this.slices; k++) {
        int m = k + 1;
        ImageProcessor localImageProcessor = this.edges.getStack().getProcessor(m);
        // getProcessor is method in snakeprogram.IntensityEnergy
        try {
          ArrayList localArrayList = setParamsAndDeform(localImageProcessor, (ArrayList)localObject, k);
          if (this.errorThrown) {
            this.logBuffer.append("\n\n>>> Processing of cell " + j + " aborted due to JFilament error. <<<");
            this.cellDiscarded[i] = true;
            k = this.slices;
          }
          else {
            FloatPolygon localFloatPolygon = new FloatPolygon();
            for (double[] arrayOfDouble : localArrayList) {
              localFloatPolygon.addPoint(arrayOfDouble[0], arrayOfDouble[1]);
            }
            this.snakePolygons[i][k] = new PolygonRoi(localFloatPolygon, 2);
            this.snakePolygons[i][k].setName("cell_" + j + "_slice_" + m);
            this.snakePolygons[i][k].setPosition(m);

            this.cAreas[i][k] = getRoiArea(this.snakePolygons[i][k]);
            this.centers[i][k] = getRoiCentroid(this.snakePolygons[i][k]);
            this.repint[i][k] = getReporterIntensity(this.snakePolygons[i][k], m);
            this.points.getStack().getProcessor(m).putPixel(this.centers[i][k].x, this.centers[i][k].y, 255);
            this.points.getStack().getProcessor(m).putPixel(this.centers[i][k].x + 1, this.centers[i][k].y, j);

            localRoiManager.addRoi(this.snakePolygons[i][k]);
            localObject = localArrayList;
          }
        }
        catch (IllegalAccessException localIllegalAccessException) {
          IJ.error("Illegal access exception when attempting to deform snake.");
        }
      }
      if (!this.errorThrown) {
        if (j < 10) {
          localRoiManager.runCommand("Save", this.snakeDir + "Cell0" + j + "_snakes.zip");
        }
        else {
          localRoiManager.runCommand("Save", this.snakeDir + "Cell" + j + "_snakes.zip");
        }
        this.cellDiscarded[i] = false;
      }
      localRoiManager.close();
    }
  }

  private void loadSnakesFromFiles()
  {
    File localFile = new File(this.snakeDir);
    if (localFile.exists()) {
      String[] arrayOfString = localFile.list();
      for (int i = 0; i < arrayOfString.length; i++) {
        if (this.cellNumbers[i] == 0) {
          if (arrayOfString[i].startsWith("Cell")) {
            RoiManager localRoiManager = new RoiManager(true);
            localRoiManager.runCommand("Open", this.snakeDir + arrayOfString[i]);
            int j = Integer.parseInt(arrayOfString[i].replace("Cell", "").replace("_snakes.zip", ""));
            this.cellNumbers[i] = j;
            Roi[] arrayOfRoi = localRoiManager.getRoisAsArray();
            if (arrayOfRoi.length == this.slices) {
              for (int k = 0; k < this.slices; k++) {
                this.snakePolygons[i][k] = new PolygonRoi(arrayOfRoi[k].getFloatPolygon(), 2);
                this.cAreas[i][k] = getRoiArea(this.snakePolygons[i][k]);
                this.centers[i][k] = getRoiCentroid(this.snakePolygons[i][k]);
                this.repint[i][k] = getReporterIntensity(this.snakePolygons[i][k], k + 1);
              }
            }
            localRoiManager.close();
          }
        }
        else
          quitWithError("Tried to fill cellNumbers array twice.");
      }
    }
    else
    {
      quitWithError("Failed to find directory containing snakes.");
    }
  }

  private void markBuds()
  {
    if (!this.arraysInitialized) {
      quitWithError("Data arrays have not been initialized (markBuds).");
    }
    updateStackDimensions();

    if (this.nBudsDetected > 0) {
      wipeBuds();
    }
    this.reporter.deleteRoi();
    this.points.deleteRoi();
    this.edges.deleteRoi();
    ImagePlus localImagePlus1 = new Duplicator().run(this.reporter);
    ImagePlus localImagePlus2 = new Duplicator().run(this.points);
    ImagePlus localImagePlus3 = new Duplicator().run(this.edges);
    localImagePlus2.getProcessor().setMinAndMax(254.0D, 255.0D);
    IJ.run(localImagePlus2, "Apply LUT", "stack");

    ImagePlus localImagePlus4 = IJ.createImage("CentersOnly", "RGB black", this.width, this.height, this.slices);
    RGBStackMerge localRGBStackMerge = new RGBStackMerge();
    localImagePlus4.setStack(localRGBStackMerge.mergeStacks(this.width, this.height, this.slices, localImagePlus1.getStack(), localImagePlus2.getStack(), localImagePlus3.getStack(), false));
    localImagePlus4.show();
    localImagePlus4.getWindow().setLocation(350, 150);

    Point localPoint = new Point();
    Rectangle localRectangle1 = new Rectangle();
    Rectangle localRectangle2 = new Rectangle();
    String[] arrayOfString = { "Does this cell bud during the experiment?", "Is there a 2nd bud?", "Is there a 3rd bud?", "Is there a 4th bud?", "Is there a 5th bud?" };

    boolean bool1 = true;
    for (int i = 0; i < this.nCells; i++)
      if ((this.cellDiscarded[i] == 0) && (this.snakePolygons[i][0] != null)) {
        localImagePlus4.setSlice(1);
        localRectangle1.setBounds(this.centers[i][0].x - 25, this.centers[i][0].y - 25, 50, 50);
        localImagePlus4.setRoi(localRectangle1);
        IJ.run(localImagePlus4, "To Selection", "");
        OvalRoi localOvalRoi = new OvalRoi(this.centers[i][0].x - 2, this.centers[i][0].y - 2, 5, 5);
        localImagePlus4.setRoi(localOvalRoi, true);
        IJ.setTool("Oval");
        boolean bool2 = true;
        boolean bool3 = false;

        NonBlockingGenericDialog localNonBlockingGenericDialog1 = new NonBlockingGenericDialog("Process Polarity");
        localNonBlockingGenericDialog1.addMessage("Is the center of this cell tracked correctly?");
        localNonBlockingGenericDialog1.enableYesNoCancel();
        localNonBlockingGenericDialog1.hideCancelButton();
        localNonBlockingGenericDialog1.centerDialog(false);
        localNonBlockingGenericDialog1.setLocation(350, 0);
        localNonBlockingGenericDialog1.showDialog();
        bool2 = localNonBlockingGenericDialog1.wasOKed();

        if (!bool2) {
          NonBlockingGenericDialog localNonBlockingGenericDialog2 = new NonBlockingGenericDialog("Process Polarity");
          localNonBlockingGenericDialog2.addMessage("Do you want to exclude this cell from further analysis?");
          localNonBlockingGenericDialog2.enableYesNoCancel();
          localNonBlockingGenericDialog2.hideCancelButton();
          localNonBlockingGenericDialog2.centerDialog(false);
          localNonBlockingGenericDialog2.setLocation(350, 0);
          localNonBlockingGenericDialog2.showDialog();
          bool3 = localNonBlockingGenericDialog2.wasOKed();
        }
        int j;
        ImageProcessor localImageProcessor;
        if ((!bool2) && (bool3)) {
          for (j = 0; j < this.slices; j++) {
            int k = j + 1;
            localImageProcessor = this.points.getStack().getProcessor(k);
            if (localImageProcessor.get(this.centers[i][j].x, this.centers[i][j].y) == 255) {
              localImageProcessor.set(this.centers[i][j].x, this.centers[i][j].y, 253);
            }
            else {
              quitWithError("Failed to detect center pixel where one was expected (markBuds).");
            }
          }
          this.cellDiscarded[i] = true;
        }
        else
        {
          j = 0;
          do {
            NonBlockingGenericDialog localNonBlockingGenericDialog3 = new NonBlockingGenericDialog("Process Polarity");
            localNonBlockingGenericDialog3.addMessage(arrayOfString[j]);
            localNonBlockingGenericDialog3.enableYesNoCancel();
            localNonBlockingGenericDialog3.hideCancelButton();
            localNonBlockingGenericDialog3.centerDialog(false);
            localNonBlockingGenericDialog3.setLocation(350, 0);
            localNonBlockingGenericDialog3.showDialog();
            bool1 = localNonBlockingGenericDialog3.wasOKed();
            if (bool1) {
              WaitForUserDialog localWaitForUserDialog = new WaitForUserDialog("Process Polarity", "Move the ROI so that it is centered on the incipient\nbud in the slice where it first emerges.");
              localWaitForUserDialog.setLocation(350, 0);
              localWaitForUserDialog.show();
              WindowManager.setCurrentWindow(localImagePlus4.getWindow());
              int m = localImagePlus4.getSlice();
              localRectangle2.setBounds(localOvalRoi.getBounds());
              localPoint.setLocation(localRectangle2.x + localRectangle2.width / 2, localRectangle2.y + localRectangle2.height / 2);
              localImageProcessor = this.points.getStack().getProcessor(m);
              localImageProcessor.set(localPoint.x, localPoint.y, this.cellNumbers[i] + 100);
            }
            j++;
          }
          while ((bool1) && (j < 5));
        }
      }
  }

  private void wipeBuds()
  {
    for (int k = 0; k < this.slices; k++) {
      ImageProcessor localImageProcessor = this.points.getStack().getProcessor(k + 1);
      int i = localImageProcessor.getPixelCount();

      for (int m = 0; m < i; m++) {
        int j = localImageProcessor.get(m);
        if ((j > 100) && (j < 200))
          localImageProcessor.set(m, 0);
      }
    }
  }

  private void makeKymographs()
  {
    if (!this.arraysInitialized) {
      quitWithError("Data arrays have not been initialized.");
    }
    updateStackDimensions();

    this.masks = IJ.createImage("All Masks", "8-bit black", this.width, this.height, this.slices);

    int i1 = (int)(255.0D / Math.pow(2.0D, 2.0D));

    for (int i2 = 0; i2 < this.nCells; i2++) {
      int i3 = this.cellNumbers[i2];
      if ((this.cellDiscarded[i2] == 0) && (this.snakePolygons[i2][0] != null)) {
        this.kymographs[i2] = IJ.createImage("kymo" + i3, "RGB black", 64, this.slices, 1);
        ColorProcessor localColorProcessor = (ColorProcessor)this.kymographs[i2].getProcessor();
        byte[] arrayOfByte1 = new byte[64 * this.slices];
        byte[] arrayOfByte2 = new byte[64 * this.slices];
        byte[] arrayOfByte3 = new byte[64 * this.slices];
        makeWedgeMasks(this.zeroAngles[i2]);
        int j;
        for (int i4 = 0; i4 < this.slices; i4++) {
          int[] arrayOfInt = makeMasksAndMeasureReporter(i2, i4);
          int k = 64 * i4;
          for (int i5 = 0; i5 < 16; i5++) {
            int i = i5 < 8 ? 32 : -32;
            for (int i6 = 0; i6 < 4; i6++) {
              j = 4 * i5 + i6 + i + k;
              arrayOfByte1[j] = ((byte)arrayOfInt[i5]);
            }
          }
          for (i5 = 0; i5 < 5; i5++) {
            if (this.targets[i2][i4][i5][2] > 0) {
              int m = getKymoOffset(this.tAngles[i2][i4][i5] - this.zeroAngles[i2]);
              j = m + k;
              if (j < arrayOfByte2.length) {
                arrayOfByte2[j] = ((byte)(this.targets[i2][i4][i5][2] * i1));
              }
            }
          }
        }
        for (i4 = 0; i4 < this.nBuds[i2]; i4++) {
          int n = getKymoOffset(this.bAngles[i2][i4] - this.zeroAngles[i2]);
          j = n + 64 * (this.bSlices[i2][i4] - 1);
          arrayOfByte3[j] = -1;
        }
        localColorProcessor.setRGB(arrayOfByte1, arrayOfByte2, arrayOfByte3);
        String str = i3 < 10 ? "Cell0" : "Cell";
        IJ.saveAs(this.kymographs[i2], "Tiff", this.kymoDir + str + i3 + "_kymo.tif");
        this.logBuffer.append("\nSaved file: " + str + i3 + "_kymo.tif");
      }
    }
  }

  private void finishUp()
  {
    this.thisStopTime = System.currentTimeMillis();
    this.totalTime = ((this.thisStopTime - this.startTime) / 1000.0D);
    this.logBuffer.append("\nPlugin finished in " + this.totalTime + " seconds.");

    this.logBuffer.append("\n\n");
    this.logBuffer.append("####################################################################################################");
    this.logBuffer.append("\n");

    String str = this.logBuffer.toString();

    IJ.log(str);

    IJ.append(str, this.logFile);
  }

  private void logTime(String paramString)
  {
    this.thisStopTime = System.currentTimeMillis();
    this.timeThisTook = ((this.thisStopTime - this.lastStopTime) / 1000.0D);
    this.logBuffer.append("\n" + paramString + " finished in " + this.timeThisTook + " seconds.");
    this.lastStopTime = this.thisStopTime;
  }

  private void updateStackDimensions()
  {
    this.width = this.edges.getWidth();
    this.height = this.edges.getHeight();
    this.slices = this.edges.getNSlices();

    if ((this.reporter != null) && (
      (this.reporter.getWidth() != this.width) || (this.reporter.getHeight() != this.height) || (this.reporter.getNSlices() != this.slices))) {
      quitWithError("Image dimensions do not match (reporter).");
    }

    if ((this.points != null) && (
      (this.points.getWidth() != this.width) || (this.points.getHeight() != this.height) || (this.points.getNSlices() != this.slices)))
      quitWithError("Image dimensions do not match (points).");
  }

  private void updateImageFileArray()
  {
    this.allImages[0] = this.reporter;
    this.allImages[1] = this.points;
    this.allImages[2] = this.edges;
    this.allImages[3] = this.masks;
    this.allImages[4] = this.energy;
  }

  private void detectAndValidatePoints()
  {
    this.nTargetsDetected = countInitTargetsFromImage();
    this.logBuffer.append("\n");
    if (this.nTargetsDetected > 0) {
      this.targetsOK = true;
      this.logBuffer.append("\nDetected " + this.nTargetsDetected + " targets.");
      this.nextStep = 1;
      this.nCentersDetected = countInitCentersFromImage();
      if ((this.nCentersDetected > 0) && (this.nCentersDetected <= this.nTargetsDetected)) {
        this.centersOK = true;
        this.logBuffer.append("\nDetected " + this.nCentersDetected + " centers.");
        this.nextStep = 2;
        this.nBudsDetected = countBudsFromImage();
        if (this.nBudsDetected > 0) {
          this.budsOK = true;
          this.logBuffer.append("\nDetected " + this.nBudsDetected + " buds.");
          this.nextStep = 3;
        }
      }
      else {
        quitWithError("The points image doesn't contain any center points or there are more centers than targets.");
      }
    }
    else {
      quitWithError("The points image doesn't contain any target points.");
    }
    updateStackDimensions();
    updateImageFileArray();
  }

  private int countInitTargetsFromImage()
  {
    ImageProcessor localImageProcessor = this.points.getStack().getProcessor(1);
    int i = localImageProcessor.getPixelCount();
    int j = 0;
    for (int k = 0; k < i; k++) {
      if (localImageProcessor.get(k) == 254) {
        j++;
      }
    }
    return j;
  }

  private int countInitCentersFromImage()
  {
    int i = 0;
    ImageProcessor localImageProcessor = this.points.getStack().getProcessor(1);
    int j = localImageProcessor.getPixelCount();

    for (int m = 0; m < j; m++) {
      int k = localImageProcessor.get(m);
      if ((k == 255) || (k == 253)) {
        i++;
      }
    }

    return i;
  }

  private int countBudsFromImage()
  {
    int i = 0;
    int j = this.points.getNSlices();
    int k = this.points.getWidth() * this.points.getHeight();

    for (int n = 0; n < j; n++) {
      ImageProcessor localImageProcessor = this.points.getStack().getProcessor(n + 1);
      for (int i1 = 0; i1 < k; i1++) {
        int m = localImageProcessor.get(i1);
        if ((m > 100) && (m < 200)) {
          i++;
        }
      }
    }
    return i;
  }

  private void checkROICentersAgainstImage()
  {
    for (int i1 = 0; i1 < this.slices; i1++) {
      int i = i1 + 1;
      ImageProcessor localImageProcessor = this.points.getStack().getProcessor(i);

      for (int i2 = 0; i2 < this.height; i2++)
        for (int i3 = 0; i3 < this.width; i3++) {
          int k = localImageProcessor.get(i3, i2);
          int m = 0;
          int n = 0;
          if ((k > 0) && (k <= 100)) {
            int j = localImageProcessor.get(i3 - 1, i2);
            if (j == 255) {
              for (int i4 = 0; i4 < this.nCells; i4++) {
                if (k == this.cellNumbers[i4]) {
                  n = 1;
                  m = i4;
                }
              }
              if (n != 0) {
                if (this.centers[m][i1] != null) {
                  if ((this.centers[m][i1].x == i3 - 1) && (this.centers[m][i1].y == i2)) {
                    if (i1 == 0) {
                      this.cellDiscarded[m] = false;
                    }
                  }
                  else {
                    this.logBuffer.append("\nCenter for cell " + k + " has different locations in image and saved ROIs in slice " + i + " (checkROICentersAgainstImage).");
                  }
                }
              }
              else {
                this.logBuffer.append("\nDid not find cell number for cell " + k + " in cellNumbers array in slice " + i + " (checkROICentersAgainstImage).");
              }
            }
            else if (j == 253) {
              if (i1 == 0)
                this.cellDiscarded[m] = true;
            }
            else
            {
              this.logBuffer.append("\nDetected index pixel but did not find corresponding center pixel at (x-1, y) in slice " + i + " (checkROICentersAgainstImage).");
            }
          }
        }
    }
  }

  private void setNCells(int paramInt)
  {
    if (this.nCells == 0) {
      this.nCells = paramInt;
    }
    else
      quitWithError("Tried to set the variable nCells twice.");
  }

  private void initializeDataArrays()
  {
    if (!this.arraysInitialized) {
      this.centers = new Point[this.nCells][this.slices];
      this.cRadii = new int[this.nCells];
      this.snakePolygons = new PolygonRoi[this.nCells][this.slices];
      this.cAreas = new int[this.nCells][this.slices];
      this.repint = new double[this.nCells][this.slices];
      this.targets = new int[this.nCells][this.slices][5][3];
      this.tAngles = new double[this.nCells][this.slices][5];
      this.zeroAngles = new double[this.nCells];
      this.buds = new Point[this.nCells][5];
      this.bAngles = new double[this.nCells][5];
      this.bSlices = new int[this.nCells][5];
      this.kymographs = new ImagePlus[this.nCells];
      this.initCenters = new Point[this.nCells];
      this.cellNumbers = new int[this.nCells];
      this.cellDiscarded = new boolean[this.nCells];
      this.nBuds = new int[this.nCells];

      this.arraysInitialized = true;
      this.logBuffer.append("\nInitialized nCells arrays with " + this.nCells + " cells and " + this.slices + " slices.");
    }
    else {
      quitWithError("Tried to initialize nCells arrays twice.");
    }
  }

  private void splitChannels(ImagePlus paramImagePlus)
  {
    if (!paramImagePlus.isComposite()) {
      int i = paramImagePlus.getWidth();
      int j = paramImagePlus.getHeight();
      int k = paramImagePlus.getNSlices();
      ImageStack localImageStack1 = paramImagePlus.getStack();
      ImageStack localImageStack2 = new ImageStack(i, j);
      ImageStack localImageStack3 = new ImageStack(i, j);
      ImageStack localImageStack4 = new ImageStack(i, j);
      for (int m = 0; m < k; m++) {
        ColorProcessor localColorProcessor = (ColorProcessor)localImageStack1.getProcessor(1);
        localImageStack1.deleteSlice(1);
        byte[] arrayOfByte1 = new byte[i * j];
        byte[] arrayOfByte2 = new byte[i * j];
        byte[] arrayOfByte3 = new byte[i * j];
        localColorProcessor.getRGB(arrayOfByte1, arrayOfByte2, arrayOfByte3);
        localImageStack2.addSlice(null, arrayOfByte1);
        localImageStack3.addSlice(null, arrayOfByte2);
        localImageStack4.addSlice(null, arrayOfByte3);
      }
      this.reporter.setStack(localImageStack2);
      this.points.setStack(localImageStack3);
      this.edges.setStack(localImageStack4);
    }
    else {
      ImagePlus[] arrayOfImagePlus = ChannelSplitter.split(paramImagePlus);
      this.reporter = arrayOfImagePlus[0];
      this.points = arrayOfImagePlus[1];
      this.edges = arrayOfImagePlus[2];
    }
    paramImagePlus.changes = false;
    paramImagePlus.close();
  }

  private void saveImageFiles(int[] paramArrayOfInt)
  {
    updateImageFileArray();
    this.logBuffer.append("\n");
    for (int i = 0; i < paramArrayOfInt.length; i++)
      if (this.allImages[paramArrayOfInt[i]] != null) {
        IJ.saveAs(this.allImages[paramArrayOfInt[i]], "Tiff", this.procDir + this.newStylePrefixes[paramArrayOfInt[i]] + this.expN + ".tif");
        this.logBuffer.append("\nSaved file: " + this.newStylePrefixes[paramArrayOfInt[i]] + this.expN + ".tif");
      }
  }

  private void getAllTargetsFromImage()
  {
    ImageProcessor[] arrayOfImageProcessor = new ImageProcessor[3];

    this.energy = IJ.createImage("All Scales", "8-bit black", this.width, this.height, this.slices);
    int i4;
    int j;
    int k;
    for (int i3 = 0; i3 < this.slices; i3++) {
      ImageProcessor localImageProcessor1 = this.points.getStack().getProcessor(i3 + 1);
      ImageProcessor localImageProcessor2 = this.energy.getStack().getProcessor(i3 + 1);
      for (i4 = 0; i4 < this.nCells; i4++) {
        if ((this.cellDiscarded[i4] == 0) && (this.snakePolygons[i4][i3] != null)) {
          Point localPoint = this.centers[i4][i3];
          Object localObject = new ByteProcessor(this.width, this.height);
          ((ImageProcessor)localObject).setColor(1);
          ((ImageProcessor)localObject).fill(getIntFromFloat(this.snakePolygons[i4][i3]));
          Rectangle localRectangle = new Rectangle(localPoint.x - 20, localPoint.y - 20, 41, 41);
          ((ImageProcessor)localObject).setRoi(localRectangle);
          ImageProcessor localImageProcessor3 = ((ImageProcessor)localObject).crop();
          localObject = localImageProcessor3.duplicate();
          for (int i = 0; i < 3; i++) {
            ((ImageProcessor)localObject).erode();
            localImageProcessor3.dilate();
            arrayOfImageProcessor[i] = new ByteProcessor(41, 41);
            arrayOfImageProcessor[i].copyBits((ImageProcessor)localObject, 0, 0, 0);
            arrayOfImageProcessor[i].copyBits(localImageProcessor3, 0, 0, 4);
          }
          ByteProcessor localByteProcessor = new ByteProcessor(41, 41);
          for (i = 0; i < 3; i++) {
            arrayOfImageProcessor[(2 - i)].multiply(Math.pow(2.0D, i));
            localByteProcessor.copyBits(arrayOfImageProcessor[(2 - i)], 0, 0, 13);
          }
          j = 0;
          for (int i5 = -20; i5 <= 20; i5++) {
            for (int i6 = -20; i6 <= 20; i6++) {
              int m = localPoint.x + i5; int n = localPoint.y + i6;
              int i1 = 20 + i5; int i2 = 20 + i6;
              k = localByteProcessor.getPixel(i1, i2);
              if (k > 0) {
                localImageProcessor2.putPixel(m, n, Math.max(k, localImageProcessor2.getPixel(m, n)));
              }
              if ((localImageProcessor1.getPixel(m, n) == 254) && (j < 5)) {
                this.targets[i4][i3][j][0] = m;
                this.targets[i4][i3][j][1] = n;
                this.targets[i4][i3][j][2] = k;
                this.tAngles[i4][i3][j] = Math.atan2(i6, i5);
                j++;
              }
            }
          }
        }
      }
    }
    for (i3 = 0; i3 < this.nCells; i3++) {
      k = this.targets[i3][0][0][2];
      i4 = 0;
      for (j = 1; j < 5; j++) {
        i4 = k < this.targets[i3][0][j][2] ? j : i4;
      }
      this.zeroAngles[i3] = this.tAngles[i3][0][i4];
    }
  }

  private void getAllBudsFromImage()
  {
    for (int n = 0; n < this.nCells; n++) {
      this.nBuds[n] = 0;
    }
    for (n = 0; n < this.slices; n++) {
      int i = n + 1;
      ImageProcessor localImageProcessor = this.points.getStack().getProcessor(i);

      for (int i1 = 0; i1 < this.height; i1++)
        for (int i2 = 0; i2 < this.width; i2++) {
          int j = localImageProcessor.get(i2, i1);
          if ((j > 100) && (j <= 200)) {
            int k = j - 100;
            for (int i3 = 0; i3 < this.nCells; i3++)
              if (k == this.cellNumbers[i3])
                if (this.nBuds[i3] < 5) {
                  int m = this.nBuds[i3];
                  this.buds[i3][m] = new Point(i2, i1);
                  this.bAngles[i3][m] = Math.atan2(i1 - this.centers[i3][n].y, i2 - this.centers[i3][n].x);
                  this.bSlices[i3][m] = i;
                  this.nBuds[i3] += 1;
                }
                else {
                  quitWithError("Tried to assign too many buds for cell " + this.cellNumbers[i3] + " (getAllBudsFromImage).");
                }
          }
        }
    }
  }

  private int growFromInitialCenter(ImageProcessor paramImageProcessor, Point paramPoint, double paramDouble)
  {
    int i = 1;
    boolean bool = false;
    do
    {
      bool = checkRing(i, paramImageProcessor, paramPoint, paramDouble);

      if (bool) {
        i++;
        bool = checkRing(i, paramImageProcessor, paramPoint, paramDouble);
        if (bool) {
          i--;
        }
      }
      i++;
    }
    while ((!bool) && (i < 20));
    i--;
    return i;
  }

  private int growAlongGradient(int paramInt1, int paramInt2, ImageProcessor paramImageProcessor, Point paramPoint, double paramDouble)
  {
    Point localPoint1 = new Point();
    Point localPoint2 = new Point();
    boolean bool = false;
    int i = 0;
    double d = 1.0D;
    int j = 0;
    do
    {
      paramInt2++;
      localPoint1.setLocation(0, 0);
      int k = 0;
      do
      {
        k++;
        for (int m = -k; m <= k; m++) {
          for (int n = -k; n <= k; n++) {
            if ((Math.abs(m) == k) || (Math.abs(n) == k)) {
              localPoint2.setLocation(paramPoint.x + m, paramPoint.y + n);
              bool = checkRing(paramInt2, paramImageProcessor, localPoint2, paramDouble);
              if (!bool) {
                localPoint1.translate(m, n);
                i = 1;
              }
            }
          }
        }
      }
      while ((i == 0) && (k <= 3));

      if ((i != 0) && (
        (localPoint1.x != 0) || (localPoint1.y != 0))) {
        if (Math.abs(localPoint1.y) > Math.abs(localPoint1.x)) {
          j = Math.abs(localPoint1.y);
        }
        else {
          j = Math.abs(localPoint1.x);
        }
        d = k / j;
        paramPoint.translate((int)Math.round(d * localPoint1.x), (int)Math.round(d * localPoint1.y));
      }
    }

    while ((i != 0) && (paramInt2 < 20));
    paramInt2--;
    this.initCenters[paramInt1] = new Point(paramPoint);
    this.cellNumbers[paramInt1] = (paramInt1 + 1);
    return paramInt2;
  }

  private boolean checkRing(int paramInt, ImageProcessor paramImageProcessor, Point paramPoint, double paramDouble)
  {
    int i = paramInt + 1;
    boolean bool = false;
    ImageProcessor localImageProcessor = this.ringMasks.getStack().getProcessor(paramInt);

    for (int j = -i; j <= i; j++) {
      for (int k = -i; k <= i; k++)
      {
        if ((localImageProcessor.getPixel(20 + j, 20 + k) == 255) && (paramImageProcessor.getPixel(paramPoint.x + j, paramPoint.y + k) > paramDouble)) {
          bool = true;
        }

      }

    }

    return bool;
  }

  private ArrayList<double[]> makeArrayListFromCircle(Point paramPoint, int paramInt)
  {
    ArrayList localArrayList = new ArrayList();
    double d = 0.03141592653589793D;

    for (int i = 0; i < 200; i++) {
      double[] arrayOfDouble = { paramPoint.x + paramInt * Math.cos(i * d), paramPoint.y + paramInt * Math.sin(i * d) };
      localArrayList.add(i, arrayOfDouble);
    }
    return localArrayList;
  }

  private ArrayList<double[]> setParamsAndDeform(ImageProcessor paramImageProcessor, ArrayList<double[]> paramArrayList, int paramInt)
    throws IllegalAccessException
  {
    //Class exists in findSnakes Class
    TwoDContourDeformation localTwoDContourDeformation = new TwoDContourDeformation(paramArrayList, new IntensityEnergy(paramImageProcessor, 1.0D));
    this.errorThrown = false;
    if (paramInt == 0) {
      setParams(localTwoDContourDeformation, this.initParams);
      for (i = 0; i < this.initParams[5]; i++) {
        try {
          localTwoDContourDeformation.addSnakePoints(this.initParams[6]);
            //In TwoDContourDeformation class from JFilament
          localTwoDContourDeformation.deformSnake();
            //In TwoDContourDeformation class from JFilament
        }
        catch (Throwable localThrowable1) {
          this.errorThrown = true;
          i = (int)this.initParams[5];
        }
      }
    }

    setParams(localTwoDContourDeformation, this.trackParams);
    for (int i = 0; i < this.trackParams[5]; i++) {
      try {
        localTwoDContourDeformation.addSnakePoints(this.trackParams[6]);
        localTwoDContourDeformation.deformSnake();
      }
      catch (Throwable localThrowable2) {
        this.errorThrown = true;
        i = (int)this.trackParams[5];
      }
    }
    return paramArrayList;
  }

  private void setParams(TwoDContourDeformation paramTwoDContourDeformation, double[] paramArrayOfDouble)
  {
    paramTwoDContourDeformation.setAlpha(paramArrayOfDouble[0]);
    paramTwoDContourDeformation.setBeta(paramArrayOfDouble[1]);
    paramTwoDContourDeformation.setGamma(paramArrayOfDouble[2]);
    paramTwoDContourDeformation.setWeight(paramArrayOfDouble[3]);
    paramTwoDContourDeformation.setStretch(paramArrayOfDouble[4]);
  }

  private int getKymoOffset(double paramDouble)
  {
    if (paramDouble > 3.141592653589793D) {
      paramDouble -= 6.283185307179586D;
    }
    else if (paramDouble < -3.141592653589793D) {
      paramDouble += 6.283185307179586D;
    }
    return (int)(32.5D + paramDouble * 2.0D * 16.0D / 3.141592653589793D);
  }

  private void makeWedgeMasks(double paramDouble)
  {
    this.wedgeMasks = IJ.createImage("Wedge Masks", "8-bit black", 41, 41, 16);

    ImageProcessor localImageProcessor = this.wedgeMasks.getImageStack().getProcessor(1).duplicate();
    double d = 20.5D;
    for (int i = 0; i < 16; i++)
    {
      float[] arrayOfFloat1 = new float[3];
      float[] arrayOfFloat2 = new float[3];
      arrayOfFloat1[0] = ((float)d);
      arrayOfFloat2[0] = ((float)d);
      arrayOfFloat1[1] = ((float)(d + 1000.0D * Math.cos(paramDouble + i * 0.3926990816987241D)));
      arrayOfFloat1[2] = ((float)(d + 1000.0D * Math.cos(paramDouble + (i + 1) * 0.3926990816987241D)));
      arrayOfFloat2[1] = ((float)(d + 1000.0D * Math.sin(paramDouble + i * 0.3926990816987241D)));
      arrayOfFloat2[2] = ((float)(d + 1000.0D * Math.sin(paramDouble + (i + 1) * 0.3926990816987241D)));
      FloatPolygon localFloatPolygon = new FloatPolygon(arrayOfFloat1, arrayOfFloat2, 3);
      localImageProcessor.setColor(255);
      localImageProcessor.fill(new PolygonRoi(localFloatPolygon, 6));
      this.wedgeMasks.getStack().getProcessor(i + 1).copyBits(localImageProcessor, 0, 0, 0);
      localImageProcessor.setColor(0);
      localImageProcessor.fill();
    }
  }

  private int[] makeMasksAndMeasureReporter(int paramInt1, int paramInt2)
  {
    int[] arrayOfInt = new int[16];
    Point localPoint = this.centers[paramInt1][paramInt2];

    this.reporterMasks = IJ.createImage("Search Masks", "8-bit black", 41, 41, 16);
    Object localObject = new ByteProcessor(this.width, this.height);
    ((ImageProcessor)localObject).setColor(255);
    ((ImageProcessor)localObject).fill(getIntFromFloat(this.snakePolygons[paramInt1][paramInt2]));
    Rectangle localRectangle = new Rectangle(localPoint.x - 20, localPoint.y - 20, 41, 41);
    ((ImageProcessor)localObject).setRoi(localRectangle);
    ImageProcessor localImageProcessor1 = ((ImageProcessor)localObject).crop();

    localObject = localImageProcessor1.duplicate();
    for (int i = 0; i < 3; i++) {
      localImageProcessor1.dilate();
    }
    ((ImageProcessor)localObject).copyBits(localImageProcessor1, 0, 0, 11);

    ImageProcessor localImageProcessor2 = this.masks.getStack().getProcessor(paramInt2 + 1);
    ImageProcessor localImageProcessor3 = this.reporter.getStack().getProcessor(paramInt2 + 1);

    for (int k = 0; k < 16; k++) {
      ImageProcessor localImageProcessor4 = this.reporterMasks.getStack().getProcessor(k + 1);
      ImageProcessor localImageProcessor5 = this.wedgeMasks.getStack().getProcessor(k + 1);
      localImageProcessor4.copyBits((ImageProcessor)localObject, 0, 0, 0);
      localImageProcessor4.copyBits(localImageProcessor5, 0, 0, 9);
      double d = 0.0D;
      int m = 0;
      for (int n = -20; n <= 20; n++) {
        for (int i1 = -20; i1 <= 20; i1++) {
          if (localImageProcessor4.get(20 + n, 20 + i1) > 0) {
            int j = localImageProcessor2.get(localPoint.x + n, localPoint.y + i1);
            localImageProcessor2.set(localPoint.x + n, localPoint.y + i1, j + 1);

            d += localImageProcessor3.get(localPoint.x + n, localPoint.y + i1);
            m++;
          }
        }
      }
      arrayOfInt[k] = ((int)Math.round(d / m));
    }
    return arrayOfInt;
  }

  private double getReporterIntensity(Roi paramRoi, int paramInt)
  {
    double d = 0.0D;
    ImageProcessor localImageProcessor1 = paramRoi.getMask();
    ImageProcessor localImageProcessor2 = this.reporter.getStack().getProcessor(paramInt);
    int i = localImageProcessor1.getWidth();
    int j = localImageProcessor1.getHeight();
    int k = 0;
    for (int m = 0; m < i; m++) {
      for (int n = 0; n < j; n++) {
        if (localImageProcessor1.get(m, n) == 255) {
          d += localImageProcessor2.get(m, n);
          k++;
        }
      }
    }
    return d /= k;
  }

  private void showAllImagesAndAbort()
  {
    for (int i = 0; i < this.allImages.length; i++) {
      if (this.allImages[i] != null) {
        this.allImages[i].show();
      }
    }
    this.logBuffer.append("\nPlugin aborted by showAllImagesAndAbort method.");
    this.logBuffer.append("\n");
    finishUp();
    throw new RuntimeException("Macro canceled");
  }

  private void quitWithError(String paramString)
  {
    this.logBuffer.append("\nError generated with message: " + paramString);
    this.logBuffer.append("\n");
    IJ.error("Process Polarity", paramString);
    showAllImagesAndAbort();
  }

  private void writeCellData()
  {
    for (int i = 0; i < this.nCells; i++)
      if ((this.cellDiscarded[i] == 0) && (this.snakePolygons[i][0] != null)) {
        int j = i + 1;
        try
        {
          String str;
          if (j < 10) {
            str = this.dataDir + "Cell0" + j + "_data.txt";
          }
          else {
            str = this.dataDir + "Cell" + j + "_data.txt";
          }
          new File(str);
          FileWriter localFileWriter = new FileWriter(str, false);
          StringBuffer localStringBuffer = new StringBuffer();

          localStringBuffer.append("slice");
          localStringBuffer.append("\tc" + j + "_cen_X");
          localStringBuffer.append("\tc" + j + "_cen_Y");
          localStringBuffer.append("\tc" + j + "_area");
          for (int k = 0; k < 5; k++) {
            localStringBuffer.append("\tc" + j + "_tar" + (k + 1) + "_X");
            localStringBuffer.append("\tc" + j + "_tar" + (k + 1) + "_Y");
            localStringBuffer.append("\tc" + j + "_tar" + (k + 1) + "_E");
            localStringBuffer.append("\tc" + j + "_tar" + (k + 1) + "_O");
          }
          localStringBuffer.append("\tc" + j + "_repint");
          localStringBuffer.append("\n");

          for (k = 0; k < this.slices; k++) {
            localStringBuffer.append(Integer.toString(k + 1));
            localStringBuffer.append("\t" + Integer.toString(this.centers[i][k].x));
            localStringBuffer.append("\t" + Integer.toString(this.centers[i][k].y));
            localStringBuffer.append("\t" + Integer.toString(this.cAreas[i][k]));
            for (int m = 0; m < 5; m++) {
              localStringBuffer.append("\t" + Integer.toString(this.targets[i][k][m][0]));
              localStringBuffer.append("\t" + Integer.toString(this.targets[i][k][m][1]));
              localStringBuffer.append("\t" + Integer.toString(this.targets[i][k][m][2]));
              localStringBuffer.append("\t" + Double.toString(this.tAngles[i][k][m]));
            }
            localStringBuffer.append("\t" + Double.toString(this.repint[i][k]));
            localStringBuffer.append("\n");
          }

          localFileWriter.write(localStringBuffer.toString());
          localFileWriter.close();
        }
        catch (IOException localIOException)
        {
          IJ.error("IOException: " + localIOException.getMessage());
        }
      }
  }

  private void writePopulationData()
  {
    try
    {
      String str1 = this.procDir + "Exp" + this.expN + "_data.txt";
      new File(str1);
      FileWriter localFileWriter = new FileWriter(str1, false);
      StringBuffer localStringBuffer = new StringBuffer();

      localStringBuffer.append(this.dcPrefix + "Event");
      localStringBuffer.append("\t" + this.dcPrefix + "bAng");
      localStringBuffer.append("\t" + this.dcPrefix + "tAng");
      localStringBuffer.append("\t" + this.dcPrefix + "slice");
      localStringBuffer.append("\t" + this.dcPrefix + "theta");
      localStringBuffer.append("\t" + this.dcPrefix + "thetaAbs");
      localStringBuffer.append("\t" + this.dcPrefix + "rndmX");
      localStringBuffer.append("\t" + this.dcPrefix + "DelT");
      localStringBuffer.append("\t" + this.dcPrefix + "area");
      localStringBuffer.append("\n");

      for (int i = 0; i < this.nCells; i++) {
        if ((this.cellDiscarded[i] == 0) && (this.snakePolygons[i][0] != null)) {
          for (int j = 0; j < this.nBuds[i]; j++) {
            if (this.buds[i][j] != null) {
              double d1 = this.bAngles[i][j];
              double d2 = this.tAngles[i][(this.bSlices[i][j] - 1)][0];

              double d3 = d2 - d1;
              if (d3 > 3.141592653589793D) {
                d3 = -(6.283185307179586D - d3);
              }
              if (d3 < -3.141592653589793D) {
                d3 = 6.283185307179586D + d3;
              }

              double d4 = Math.abs(d2 - d1);
              if (d4 > 3.141592653589793D) {
                d4 = 6.283185307179586D - d4;
              }

              d1 *= 57.295779513082323D;
              d2 *= 57.295779513082323D;
              d3 *= 57.295779513082323D;
              d4 *= 57.295779513082323D;
              int k = this.cellNumbers[i];
              String str2 = k < 10 ? "0" : "";
              localStringBuffer.append("c" + str2 + this.cellNumbers[i] + "_b" + (j + 1));
              localStringBuffer.append("\t" + Double.toString(d1));
              localStringBuffer.append("\t" + Double.toString(d2));
              localStringBuffer.append("\t" + Integer.toString(this.bSlices[i][j]));
              if (this.bSlices[i][j] < 16) {
                localStringBuffer.append("\tNaN");
                localStringBuffer.append("\tNaN");
              }
              else {
                localStringBuffer.append("\t" + Double.toString(d3));
                localStringBuffer.append("\t" + Double.toString(d4));
              }
              localStringBuffer.append("\t" + Double.toString(0.3D - 0.6D * Math.random()));
              if (j == 0) {
                localStringBuffer.append("\tNaN");
              }
              else {
                localStringBuffer.append("\t" + Integer.toString(this.bSlices[i][j] - this.bSlices[i][(j - 1)]));
              }
              localStringBuffer.append("\t" + Integer.toString(this.cAreas[i][(this.bSlices[i][j] - 1)]));
              localStringBuffer.append("\n");
            }
          }
        }
      }

      localFileWriter.write(localStringBuffer.toString());
      localFileWriter.close();
    }
    catch (IOException localIOException) {
      IJ.error("IOException: " + localIOException.getMessage());
    }
  }

  public ImageStack boxcar(ImagePlus paramImagePlus, int paramInt)
  {
    int i = paramImagePlus.getWidth();
    int j = paramImagePlus.getHeight();
    int k = paramImagePlus.getNSlices();

    ZProjector localZProjector = new ZProjector(paramImagePlus);
    localZProjector.setMethod(0);
    ImageStack localImageStack = new ImageStack(i, j);

    for (int m = 1; m <= k; m++) {
      if (m <= paramInt) {
        localZProjector.setStartSlice(1); localZProjector.setStopSlice(m + paramInt);
        localZProjector.doProjection();
        localImageStack.addSlice(null, localZProjector.getProjection().getProcessor());
      }
      else if (m <= k - paramInt) {
        localZProjector.setStartSlice(m - paramInt); localZProjector.setStopSlice(m + paramInt);
        localZProjector.doProjection();
        localImageStack.addSlice(null, localZProjector.getProjection().getProcessor());
      }
      else {
        localZProjector.setStartSlice(m - paramInt); localZProjector.setStopSlice(m);
        localZProjector.doProjection();
        localImageStack.addSlice(null, localZProjector.getProjection().getProcessor());
      }
    }
    return localImageStack;
  }

  public boolean fileExists(String paramString)
  {
    File localFile = new File(paramString);
    return localFile.exists();
  }

  public int getUpper(int[] paramArrayOfInt)
  {
    int i = paramArrayOfInt[0];
    for (int j = 1; j < paramArrayOfInt.length; j++) {
      i += paramArrayOfInt[j];
    }
    j = 0;
    int k = -1;
    double d = i - i / 100;
    do
      j += paramArrayOfInt[(++k)];
    while (j <= d);
    return k;
  }

  public double getArrayMean(int[] paramArrayOfInt)
  {
    int i = paramArrayOfInt.length;
    int j = 0;
    for (int k = 0; k < i; k++) {
      j += paramArrayOfInt[k];
    }
    return j / i;
  }

  public int getRoiArea(Roi paramRoi)
  {
    int i = 0;
    ImageProcessor localImageProcessor = paramRoi.getMask();
    int j = localImageProcessor.getPixelCount();
    for (int k = 0; k < j; k++) {
      if (localImageProcessor.get(k) == 255) {
        i++;
      }
    }
    return i;
  }

  public Point getRoiCentroid(Roi paramRoi)
  {
    double d1 = 0.0D; double d2 = 0.0D;
    ImageProcessor localImageProcessor = paramRoi.getMask();
    int i = localImageProcessor.getWidth();
    int j = localImageProcessor.getHeight();
    int k = 0;
    for (int m = 0; m < i; m++) {
      for (int n = 0; n < j; n++) {
        if (localImageProcessor.get(m, n) == 255) {
          d1 += m;
          d2 += n;
          k++;
        }
      }
    }
    d1 /= k;
    d2 /= k;
    d1 += paramRoi.getBounds().x + 0.5D;
    d2 += paramRoi.getBounds().y + 0.5D;
    return new Point((int)(0.5D + d1), (int)(0.5D + d2));
  }

  public PolygonRoi getIntFromFloat(PolygonRoi paramPolygonRoi)
  {
    float[] arrayOfFloat1 = paramPolygonRoi.getFloatPolygon().xpoints;
    float[] arrayOfFloat2 = paramPolygonRoi.getFloatPolygon().ypoints;
    Polygon localPolygon = new Polygon();
    for (int i = 0; i < arrayOfFloat1.length; i++) {
      localPolygon.addPoint(Math.round(arrayOfFloat1[i]), Math.round(arrayOfFloat2[i]));
    }
    return new PolygonRoi(localPolygon, 2);
  }
}
