import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;



public class ScheduleGUI{ 
	Schedule sch;
	SchedulePanel schP;
	RequirementListPanel reqs;
	static ImageIcon icon;
	String instructYear = "Please pick a year you would like to add a ";
	String headInstructYear = "Pick a year";
	JFrame frame;
	ListOfMajors l;
	BellTower b;
	




	public ScheduleGUI(Schedule sch) {
		this.sch = sch;
		sch.setScheduleGUI(this);
	
		icon = FileHandler.getDialogBoxImage(); //Sets the image that appears in the dialog boxes. 
		l = FileHandler.getMajorsList();

		frame = new JFrame();
		
		//Adds the menu bar
		MainMenuBar menu = new MainMenuBar(this);
		menu.setFont(FurmanOfficial.normalFont); 
		frame.setJMenuBar(menu);
		
		
		JPanel layeredPanel = new JPanel();
		layeredPanel.setLayout(new BorderLayout());


		//Adds Additions Panel and Belltower
		AdditionsPanel highImpactPanel = new AdditionsPanel(this);
		JPanel left = new JPanel();
		left.setBackground(FurmanOfficial.bouzarthGrey);
		
		
		BellTower belltowerLabel = new BellTower(sch);
		this.b=belltowerLabel;
		left.add(belltowerLabel);
		left.add(highImpactPanel);
		
		
		layeredPanel.add(left, BorderLayout.WEST);

		//Creates, adds schedulePanel the panel, that holds all the semesters
		schP = new SchedulePanel(sch, this);
		layeredPanel.add(schP, BorderLayout.NORTH);
		//Creates/adds the RequirementList Panel which holds the major/Ger requirements 
		reqs = new RequirementListPanel(sch, this);
		layeredPanel.add(reqs, BorderLayout.CENTER);

		//Sets all of this inside a scroll panel so the user can see the whole program regardless of screen size. 
		JScrollPane scroll = new JScrollPane();
		scroll.getViewport().add(layeredPanel);
		
		
		frame.add(scroll);
		this.update();
		frame.pack();
		frame.setVisible(true);

	}


	
	//////////////////////////////////////////////////////////////////////////////////
	/////////Addition Panel (And MenuBar) Listed in the order they appear in the GUI
	/////////////////////////////////////////////////////////////////////////////////	
	/**
	 * Show the user a Webpage in another Window. 
	 * @param websiteType
	 */
	public void GUIOutsideLink(String websiteType) {
		try{
			if(websiteType.equals(MenuOptions.exploreInternship)){
				Desktop.getDesktop().browse(new URL("http://www.furman.edu/sites/internship/FindingInternships/Pages/default.aspx").toURI());
			}
			if(websiteType.equals(MenuOptions.exploreStudyAway)){
				Desktop.getDesktop().browse(new URL("https://studyaway.furman.edu/index.cfm?FuseAction=Programs.SimpleSearch").toURI());
			}
			if(websiteType.equals(MenuOptions.addResearch)){
				Desktop.getDesktop().browse(new URL("http://www.furman.edu/sites/ur/Pages/default.aspx").toURI());
			}
			if(websiteType.equals(MenuOptions.exploreMayX)){
				Desktop.getDesktop().browse(new URL("http://www.furman.edu/academics/may-experience/Pages/default.aspx").toURI());
			}
		} catch (Exception e1) {
			e1.printStackTrace();	
		}
	}

	
	/**
	 * This asks the user what year they would 
	 * like to add a MayX, and then provides them
	 * with a list of courses available at that time. 
	 * It then adds the user's choice to that semester.
	 * 
	 *  Does not allow the user to choose a year where a MayX already exists.
	 */
	public void addMayX(){
		Integer year = this.createYearDialogBox(getAvaliableYears(SemesterDate.MAYX), MenuOptions.addMayX);
		if(year == null){
			return;
		}
		Semester addedSemester = sch.addNewSemesterInsideSch(year, SemesterDate.MAYX); //Adds semester in schedule
		addCourseTo(addedSemester);//Ask user for which course they would like to add to that semester, if any available
	}



	/**
	 * First asks the user to pick the appropriate summer session.
	 * Then asks them what year they would like to add. 
	 * It then displays list of classes that the user can choose
	 * to add. 
	 */
	public void addSummerSession(){
		//Choose Summer Session One or Two
		int season = GUIChooseSummerSession();
		if(season == SemesterDate.OTHER){
			return;//If not valid. 
		}
		//What year you'd like this summer class
		Integer year = this.createYearDialogBox(getAvaliableYears(season), "Add Summer");
		if(year == null){
			return;//If not valid
		}
		Semester addedSemester = sch.addNewSemesterInsideSch(year, season);
		addCourseTo(addedSemester);//Choose a course. 
	}

	/**
	 * A method for letting the user choose among a list of majors.
	 * The string determines whether it chooses between actual majors,
	 * minors, or tracks.
	 * @param s
	 */
	public void GUIMajorPopUP(String s){
		ListOfMajors majors = l; //Links to test Major
		ArrayList<Major> collectionOfMajors =  majors.getGUIMajors();
		ArrayList <Major> displayThings = sch.filterAlreadyChosenMajors(collectionOfMajors);
		createMajorDiaologBox(s, displayThings);
	}
	
	
	////////////////////////////////////////////////////////////////
	///////// Main Menu Bar (Not Additions Panel)////////////////// 
	//////////////////////////////////////////////////////////////
	
	
	//The Furman Advantage Tab is all covered above. 
	
	/**
	 * Create a new schedule to work from. It either pulls from the last imported schedule
	 * or creates a blank one using the prior schedule's CourseList, and the start date saved in settings
	 */
	public void GUINewSchedule(String typeOfSchedule) {
		Schedule current;
		//Gives the user the option to save his/her schedule before opening new schedule. 
		int n = Driver.saveScheduleReminder();
		if(n == 0){
			GUISaveSchedule();
		}
		
		//This creates a schedule that matches the current schedules Course List and starting Semester Date
		if(typeOfSchedule.equals(MenuOptions.newBlankSchedule)){
			if(FileHandler.propertyGet(FileHandler.startSemester) == null && Driver.tryPickStartDate() == null){
				return;
			}
			else{
				current = new Schedule();
			}
		}
		else{
			current = new Schedule(FileHandler.getSavedStudentData());//Opens a user imported schedule. 
		}
		setSchedule(current);
		this.update();
	}
	
	//Driver open the schedule. 

	/**
	 * Relays the saveSchedule message to File Handler
	 * who deals with it because it involves writing the 
	 * seralized version of the schedule to a file. 
	 */
	public void GUISaveSchedule(){
		FileHandler.saveSchedule(this.sch);
	}

/**
 * This handlers all of the printing.
 * It asks the user for the format,
 *  formats the string accordingly, 
 * gives them a print preview, and finally
 * sends the document to the printer. 
 */
	public void GUIPrintSchedule(){
		//Sets up the dialog box that asks the user what format they would like to print their schedule as. 
		JPanel options = new JPanel();
		options.setLayout(new BorderLayout());
		//JCheckBox Construction. 
		JCheckBox ReqLayout = new JCheckBox("Requirement Layout");
		ReqLayout.setToolTipText("This is provides a checklist of all the requirements for your major, and GER. This will list out what has/hasn't been satisfied");
		JCheckBox ScheduleLayout = new JCheckBox("Schedule Layout  	       ");//Gives space between Req JCheckBox. 
		ScheduleLayout.setToolTipText("Diplays a the schedule created semester-by-semester. Includes scheduling errors, and a final checklist.");

		JLabel instruct = new JLabel("Which format(s) would you like to use?");//Gives space between JCheckBoxes. 
		options.add(instruct, BorderLayout.NORTH);
		options.add(ReqLayout, BorderLayout.EAST);
		options.add(ScheduleLayout, BorderLayout.WEST);


		//Start creating the print material
		String finalPrint = new String("<center> <h1> Academic Pathways Planner </h1> </center> "); 
		//Adds Student's Name to Printed Schedule. 
		if(sch.studentName != null){
			finalPrint += sch.studentName;
		}
		//Creates Date at the top of the page. 
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy");
		LocalDate localDate = LocalDate.now();
		finalPrint += "<br>" + dtf.format(localDate) + "<br>";

		int userSelection = (int) JOptionPane.showOptionDialog(null, options, "Format", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if(userSelection == 2 || userSelection == -1){//Cancel button, or "X"
			return;
		}


		//If they didn't select anything, the default is schedule layout.
		//Thus if ReqLayout is not selected ScheduleLayout must be. 
		if(!ReqLayout.isSelected()){
			ScheduleLayout.setSelected(true);
		}

		//Creates the schedule/reqs printout. 
		if(ScheduleLayout.isSelected()){
			finalPrint = finalPrint + sch.getPrintScheduleString() + "<br>";

		}
		if(ReqLayout.isSelected()){
			finalPrint = finalPrint + sch.getPrintRequirementString() + "<br>";
		}


		//Creates the TextPane that will display the printPreview, and print. 
		JTextPane schedulePrint = new JTextPane();
		schedulePrint.setFont(FurmanOfficial.normalFont);
		schedulePrint.setPreferredSize(new Dimension((int) new Paper().getWidth(), (int) new Paper().getHeight())); // Sets to standard piece of paper.
		schedulePrint.setContentType("text/html");
		schedulePrint.setText("<html><p>" + finalPrint + "</p>"+ Driver.getDisclaimer() +"</html>"); //Tacks disclaimer on end.
		schedulePrint.setCaretPosition(0);
		schedulePrint.setEditable(false);

		//Places it in scroll Pane for User view. 
		JScrollPane scrollPane = new JScrollPane(schedulePrint);
		scrollPane.setPreferredSize(new Dimension(schedulePrint.getPreferredSize().width,schedulePrint.getPreferredSize().height));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		String[] choices= {"Print", "Cancel"};
		//Allows user to cancel Print	
		int userChoice = (int) JOptionPane.showOptionDialog(null, scrollPane, "Print Preview", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
		if(userChoice == 0){
			try {
				schedulePrint.print();
			} catch (PrinterException e) {
				JOptionPane.showMessageDialog(null, "There was an error with printing.");
				e.printStackTrace();
			}
		}

	}

	//File Handler deletes a Schedule. 
	
	
	/**
	 * This method is called by the the RequirementListPanel (Check all Errors button), the option is in the MainMenuBar, 
	 * and once a schedule is deemed complete it is also displayed in a dialog box to the user via the BellTower. It provides a 
	 * series of two dialog boxes, first it gives a list of all errors currently in the schedule. (Excluding the optimism error)
	 * and then if the user has loaded a major that contains notes, a second dialog box will display those.  
	 * 
	 */
	public void GUICheckAllErrors() {
	String errorString = getErrorString();
	String result = "<html><body>" + errorString;
	if(errorString.equals("")){//The user has no errors. 
		result = "Your Schedule had no errors! You're a pretty savvy scheduler";
	}
	result= result.replaceAll("\n", "<br>");
	JOptionPane.showMessageDialog(null,  result, "All Errors", JOptionPane.INFORMATION_MESSAGE,  icon );
	
	String majorNotes = "";
	boolean hasNotes = false;
	for(Major m : sch.getMajors()){
		if(m.notes != null){
			hasNotes = true;
			majorNotes = majorNotes + this.htmlMajorNotes(m) + "<br><br>"; //Appends each note with a two line break btw
		}
	}
	majorNotes = "<html>" + majorNotes + "</html>";
	if(hasNotes){ //Only displays message if Major has notes. 
		JOptionPane.showMessageDialog(null, majorNotes, "Notes for all majors", JOptionPane.INFORMATION_MESSAGE);
	}


}



	public void importPriorCourses(boolean isStudent){
		String importText;
		if(isStudent){
			importText=  "To import your prior courses and placements, "
					+ "\n  1) Go to MyFurman. "
					+ "\n  2) Navigate to your unofficial transcript. "
					+ "\n  3) You should see your name and ID in the top left corner,"
					+ "\n     and your cumulative GPA listed at the bottom."
					+ "\n  4) Highlight all the data from your ID to your GPA and"
					+ "\n     drag it into this text box. "
					+ "\n  5) Click 'Validate' to make sure the process worked!";
		}
		else{
			importText = "To import a student's prior courses and placements, "
					+ "\n  1) Go to MyFurman. "
					+ "\n  2) Navigate to the student's course credit summary. "
					+ "\n  3) You should see the student's name and ID in the top left corner,"
					+ "\n     and their cumulative GPA listed at the bottom."
					+ "\n  4) Highlight all the data from their ID to their GPA and"
					+ "\n     drag it into this text box."
					+ "\n  5) Click 'Validate' to make sure the process worked!";
		}
	
		JTextArea importArea = new JTextArea(importText);
	
	
	
		//Validate button
		JButton validate = new JButton("Validate");
		validate.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				String givenText = importArea.getText();
				if(successText.equals(givenText)){
					return;
				}
				try{
					trySetStudentDataFromWebsite(givenText);
					importArea.setText(successText);
	
				}catch(Exception except){
					importArea.setText("Please try your import again");
					showImportException(except);
	
				}
			}
		}); //end action listener
	
	
		/* showInstructions button
		JButton showInstructions = new JButton("Show Instructions");
		showInstructions.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
	
			}
		});*/
	
	
	
		//Make the popup
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(new JScrollPane(importArea), BorderLayout.NORTH);
		p.add(validate, BorderLayout.EAST);
		JOptionPane.showOptionDialog(null,p, "Import your schedule",
				JOptionPane.CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,null, null, null);
	}



	/**
	 * Create the dialog box and get the user's choice of major.
	 * @param s, The instructions that are added to the dialog box. 
	 */
	public void createMajorDiaologBox(String s, ArrayList<Major> displayThings){
		Major[] dialogList = new Major[displayThings.size()];
		//Puts in a form the JOptionPane will take. 
		for(int i=0; i<displayThings.size(); i++){
			dialogList[i]=displayThings.get(i);
		}
		Major m = (Major)JOptionPane.showInputDialog(frame, "Please " + s,  s, JOptionPane.PLAIN_MESSAGE, icon, dialogList, "cat" );
		if((m != null) && (m instanceof Major)){
			this.GUIAddMajor(m);
		}
	}


	
	/**
	 * Called when user chooses a major from MajorPopUpChooser. 
	 * @param m
	 */
		public void GUIAddMajor(Major m) {
			//The degreeType is already set
			if(m.degreeTypes.size()==1){
					m.setChosenDegree(m.degreeTypes.get(0));
				}
		
			// If there are multiple degree types the user gets to choose. 
			else if(m.isNormalMajor()){//This filters out minor/tracks. 
				if(!GUIChooseMajorDegreeType(m)){
					//This stops the major from being added if the user does not specify an appropriate type
					return;
				}
			}
			else{
				m.setChosenDegree(Major.None);
			}
			//If Major has notes these are displayed
			if(m.notes != null){
				String title = "Notes for " + m.name + ":";
				String toUser = "<html>" + htmlMajorNotes(m) + "<html>";
				JOptionPane.showMessageDialog(null, toUser , title, JOptionPane.INFORMATION_MESSAGE);
	
			}
			this.sch.addMajor(m);
			this.update();
		}


	/**
	 * If a major has an ambiguous degree type (BA, BS, BM, ...)
	 * this lets the user choose a type.
	 * @param m
	 */
	
	public boolean GUIChooseMajorDegreeType(Major m){
		if(m.degreeTypes.size()>1 || m.degreeTypes.size()==0){
			ArrayList<String> toAdd= new ArrayList<String>();
			String instructions = null;
			String header = null;
	
	
			for(int i = 0; i<m.degreeTypes.size(); i++){
				toAdd.add(CourseList.getDegreeTypeString(m.degreeTypes.get(i)));
	
			}
	
	
	
			if(m.degreeTypes.size()==0 && m.isNormalMajor()){
				toAdd.add(CourseList.getDegreeTypeString(Major.BS));
				toAdd.add(CourseList.getDegreeTypeString(Major.BA));
				toAdd.add(CourseList.getDegreeTypeString(Major.BM));
				instructions = "Your major was not given a degree type. Please look up your major and choose the appropriate option.";
				header = "WARNING";
			}
	
			String[] choices = new String[toAdd.size()];
			for(int p = 0; p<toAdd.size(); p ++){
				choices[p]=toAdd.get(p);
	
	
			}
			if(m.degreeTypes.size()>1){
				instructions = "What type of degree would you like for " + m.name ;
				header = "Degree Type";
			}
	
			String GERNeeded = (String)JOptionPane.showInputDialog(null, instructions,  header, JOptionPane.PLAIN_MESSAGE, icon, choices, "cat" );
	
			int MajorType = Major.BA;
			if(GERNeeded == null){
				return false;
			}
	
			MajorType=CourseList.getDegreeTypeNumber(GERNeeded);
			//this.sch.removeMajor(sch.masterList.getGERMajor(0));
			//this.sch.addAtMajor(sch.masterList.getGERMajor(MajorType), 0);
			m.setChosenDegree(MajorType);
	
		}
		return true;
	}


	/**
	 * When the user clicks the  + button to add a new semester to the schedule
	 */
	public void GUISemesterPanelAdded(){
		sch.addNewSemester();
		this.update();
	}


	/**
	 * When a requirement panel is dropped into a semester panel
	 * @param r
	 * @param semesterP
	 */
	public void GUIRequirementPanelDropped(RequirementPanel r, SemesterPanel semesterP) {
		sch.addScheduleElement(r.req, semesterP.sem);
		this.update();
	}


	/**
	 * When a schedule element panel is dropped into a semester panel
	 * @param p
	 * @param semesterPanel
	 */
	public void GUIScheduleElementPanelDropped(ScheduleElementPanel p, SemesterPanel newSemesterPanel) {
		Semester old = p.container.sem; //Semester it was dragged from
		if(p.getElement() instanceof ScheduleCourse){
			Requirement r=	new Requirement(new Prefix[]{p.getElement().getPrefix()}, 1);
			sch.replace(p.getElement(), r, old, newSemesterPanel.sem);//replaces what was taken from old, to the new semester
		}
		else{
			sch.replace(p.getElement(), p.getElement(), old, newSemesterPanel.sem);
		}
		this.update();
	}


	/**
	 * When a schedule element is removed from a semester panel 
	 * (not replaced or moved)
	 * @param e
	 * @param semesterPanel
	 */
	public void GUIRemoveElement(ScheduleElementPanel e, SemesterPanel semesterPanel) {
		sch.removeElement(e.getElement(), semesterPanel.sem);
		this.update();
	
	}


	/**
	 * When an element is changed from one value to another, for example, 
	 * a requirement becoming a course.
	 * @param container
	 * @param toChange
	 * @param newValue
	 */
	public void GUIElementChanged(SemesterPanel container, ScheduleElementPanel toChange, ScheduleElement newValue){
		Semester s = container.sem;
		ScheduleElement old = toChange.getElement();
		sch.replace(old, newValue, s, s);
		update();
	}


	/**Whenever the scheduleGUI recieves a new schedule
	 * this method updates the classes that use this new
	 * schedule directly (Such as bellTower). And also lets the 
	 * current schedule set it's scheduleGUI as this one. (For 
	 * when another window is opened) 
	 * 
	 * @param current
	 */
	public void setSchedule(Schedule current) {
		sch=current;
		this.sch.setScheduleGUI(this);
		this.b.setSchedule(current);
	}


	/**
	 * Takes the major notes and formats them with bullet points. 
	 * @param m (Major)
	 * @return Formatted String of Major Notes. 
	 * 
	 * 
	

	 */

	public String htmlMajorNotes(Major m){
		//There is an alternate way to view these notes again. 
		String message = "Notes for " + m.name + ": (can be displayed by clicking " + MenuOptions.checkAllErrors + ")";

		String bulletedList = "<ul width =" + FurmanOfficial.defaultPixelWidth + ">";
		String [] toAddBullets = m.notes.split("\n");
		for(String s: toAddBullets){
			bulletedList += "<li>" + s + "</li>";
		}
		bulletedList += "</ul>";

		String toUser = message+ "<br><br>" + bulletedList;
		return toUser;
	}
	/**
	 * Ask the user to pick out some of the enemies that will be allowed to
	 * be satisfied by that course.
	 * @param enemies
	 * @param c
	 * @return
	 */
	public HashSet<Requirement> GUIResolveConflictingRequirements(ArrayList<Requirement> enemies, ArrayList<Major> majors, Course c){

		ArrayList<JCheckBox> userOptions =new ArrayList<JCheckBox>(); //Goes through after user selects, and get them from this. 
		HashSet<Requirement> result = new HashSet<Requirement>();

		JPanel backgroundPanel = new JPanel();
		backgroundPanel.setLayout(new BorderLayout());

		String advisorAdvice;//This is given when a group of three or more requirements conflict. Because as of now the program
		//does not specify which specific requirements are enemies (A might be enemies of B, and C. However B and C might work well together.
		// Thus a student could chose to take B and C at the same time. To know this however, the student would have to have intimate knowledge of the 
		// major, or talk to an advisor. With only two choices it is clear that you must choose one or the other.)
		if(enemies.size()>2){
			advisorAdvice = " \n If you are unsure of how these requirements conflict, talk to your advisor.";
		}
		else{
			advisorAdvice = "";
		}
		
		JLabel instruct = new JLabel(
				"<html><p style = 'width: 300px;'> The course "+ c.getPrefix() + " might satisfy the following requirements,"
						+  " but it is not allowed to satisfy all of them at once."
						+  advisorAdvice
						+ "<br> <br> Which requirements do you want it to satisfy?</p> </html>");

		backgroundPanel.add(instruct, BorderLayout.NORTH);
	
		JPanel checkBoxLayeredPanel = new JPanel();
		checkBoxLayeredPanel.setLayout(new GridLayout(enemies.size(), 1, 1, 1));//Each checkBox will stack on top of each other. 
		
		for(int i = 0; i<enemies.size(); i++){
			JCheckBox combattingReqs = new JCheckBox(enemies.get(i).shortString(10) + " (" +  majors.get(i).name + ")" );
			checkBoxLayeredPanel.add(combattingReqs);
			userOptions.add(combattingReqs);
		}


		backgroundPanel.add(checkBoxLayeredPanel);
		JOptionPane.showMessageDialog(null,  backgroundPanel, "Combatting Requirements", JOptionPane.INFORMATION_MESSAGE,  icon );

		//Goes through and adds all the Requirements the user specified the course would count towards to an ArrayList, then returns that list. 
		for(int i=0; i<userOptions.size(); i++){
			if(userOptions.get(i).isSelected()){
				result.add(enemies.get(i));
			}

		}

		return result;
	}




	/**
	 * If the user clicks an x button on a semester.
	 * @param semesterPanel
	 */
	public void GUIRemoveSemester(SemesterPanel semesterPanel) {
		sch.removeSemester(semesterPanel.sem);
		this.update();

	}

	public void GUIRemoveMajor(MajorPanel p) {
		sch.removeMajor(p.major);
		this.update();

	}
	/**
	 * Asks the user for which Summer Session they would like to add
	 * @return Number associated with the user specified semesterDate's season
	 * and returns SemesterDate.OTHER if c is null. 
	 */
	public int GUIChooseSummerSession() {
		String[] summerChoice = {SemesterDate.getSeason(SemesterDate.SUMMERONE), SemesterDate.getSeason(SemesterDate.SUMMERTWO)};
		String c = (String)JOptionPane.showInputDialog(null, "Choose Summer Session" , "Summer Session" , JOptionPane.PLAIN_MESSAGE, icon, summerChoice, "Dr. Fray");
		if(c == null){
			return SemesterDate.OTHER;
		}
		if(c.equals(summerChoice[0])){
			return SemesterDate.SUMMERONE;
		}
		if(c.equals(summerChoice[1])){
			return SemesterDate.SUMMERTWO;
		}
		return SemesterDate.OTHER;

	}
	/**
	 * This gives returns the list of years that
	 * a user can choose to add a MayX or Summer session.
	 * This looks at the years included in the schedule
	 * and filters out all years that already have that 
	 * type of addition in them. For example if the user 
	 * has already added a MayX in 2005, when they press
	 * "add MayX" again 2005 will not be an option. 
	 * @param season
	 * @return Viable years one can add that type of Semester. 
	 */
	public Integer[] getAvaliableYears(int season){
		Integer[] yearsDialog;
		//Gets available years
		ArrayList<Integer> availableYears = new ArrayList<Integer>();
		ArrayList<Semester> allSemesters = sch.getAllSemestersSorted();
		int last = (allSemesters.size()-1);
		int end = allSemesters.get(last).semesterDate.year;
		//Two first is Prior, second is odd one out
		for(int i=  allSemesters.get(2).semesterDate.year; i<= end; i++){
			if ((!sch.SemesterAlreadyExists(new SemesterDate(i, season)))){
				availableYears.add(i);
			}
		}
		yearsDialog = new Integer[availableYears.size()];
		for(int i=0; i<availableYears.size(); i++){
			yearsDialog[i]= availableYears.get(i);
		}
		return yearsDialog;




	}

	public void GUIExamineRequirement(Requirement r){
		//Make a new JFrame with info about this requirement.
		//Include the full definition as written in our code.
		String showText = r.examineRequirementString();
		JTextArea toShow = new JTextArea(showText);
		toShow.setEditable(false);
		JScrollPane pane = new JScrollPane(toShow);
		pane.setPreferredSize(new Dimension(toShow.getPreferredSize().width + 20,300));
		JOptionPane.showMessageDialog(null, pane, "Details of requirement " + r.shortString(RequirementPanel.shortStringLength), 
				JOptionPane.OK_OPTION, icon);
	}


	/**
	 * When adding a summer session, after choosing session 1 or 2, 
	 * this method lets the use choose which year they want to add it in
	 * 
	 * @param s
	 */
	public Integer createYearDialogBox(Integer[] yearsDialog, String s){
		Integer y = (Integer)JOptionPane.showInputDialog(null, instructYear + s,  headInstructYear, JOptionPane.PLAIN_MESSAGE, icon, yearsDialog, "cat" );
		return y;
	}

	/**
	 * Ask the user which course in this semester they want to add.
	 * Used by MayX or Summer semesters, or the addCourse button of
	 * a semester.
	 * 
	 * Does not perform the actual addition of the course to the semester
	 * @param s
	 */
	public ScheduleCourse addCourseDialogBox(Semester s){	
		//All courses in this semester that are not already scheduled.
		ArrayList<Course> addCourses = CourseList.getCoursesSatisfying(
				CourseList.inSemester(s)
				.and(CourseList.inSchedule(this.sch).negate()) 
				);
		//Convert to a list of schedule courses for displaying
		ArrayList<ScheduleCourse> toFinal =ScheduleCourse.toScheduleCourses(addCourses, this.sch);
		ScheduleCourse[] finalCourseList = new ScheduleCourse[toFinal.size()];
		for(int i = 0; i<toFinal.size(); i++){
			finalCourseList[i] = toFinal.get(i);
		}
		//choose the course
		ScheduleCourse c = GUIChooseCourse(finalCourseList);
		return c;
	}
	
	/**
	 * Add a new course to s.
	 * 
	 * Uses addCourseDialogBox to pick the course.
	 * @param s
	 */
	public void addCourseTo(Semester s){
		if(s.semesterDate.sNumber == SemesterDate.MAYX){
			if(!s.elements.isEmpty()){
				//TODO
				// when adding a course to a mayX which already has a course
				// replace, keep the old course, or keep both?
			}
		}
		ScheduleCourse c = addCourseDialogBox(s);
		if(c != null){
			sch.addScheduleElement(c, s);
		}
		this.update();
	}



	public ScheduleCourse GUIChooseCourse(ScheduleCourse[] finalListOfCourses) {
		if(finalListOfCourses.length <= 0){
			JOptionPane.showMessageDialog(null, 
					"There were no courses to choose from. \n"
							+"If you have a course in mind, you can add a note to the semester,\n"
							+"or you can drag a requirement up to act as a place holder.", "No courses",JOptionPane.INFORMATION_MESSAGE,  icon  );
			return null;
		}
		try{
			ScheduleCourse c = CourseChooser.chooseCourse(finalListOfCourses, sch.getAllRequirements());
			return c;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}
		/*

		String reqsString = "";
		String[] displayed = new String[finalListOfCourses.length];
		ArrayList<Requirement> allReqs = sch.getAllRequirements();
		for(int i = 0; i < finalListOfCourses.length ; i ++){
			ScheduleCourse c = finalListOfCourses[i];
			int reqsFulfilled = 0;
			reqsString = "";
			for(Requirement r : allReqs){
				if(r.isSatisfiedBy(c.getPrefix())){
					String s = r.shortString();
					if(s.length()>7){
						reqsString = reqsString + s.substring(0, 7) + "... ";
					}
					else{
						int length = s.length();
						reqsString = reqsString + s + " ";
						while(length < 10){
							reqsString += " ";
							length ++;
						}
					}
					reqsFulfilled ++;
				}
			}
			String displayedString = "";
			String clockTime = "";
			if(c.c.meetingTime != null && c.c.meetingTime[0]!= null){
				clockTime = c.c.meetingTime[0].clockTime();
			}
			displayedString = String.format(
					"%-12s %-50s %3d   %s", 
					clockTime,
					c.getPrefix().toString() + " " +c.c.professor,
					reqsFulfilled,
					reqsString);
			displayed[i] = displayedString;
		}

		String chosenString = (String)JOptionPane.showInputDialog(null, message , message , JOptionPane.PLAIN_MESSAGE, icon, displayed, "Dr. Fray");
		int chosenIndex = 0;
		for(; chosenIndex < displayed.length ; chosenIndex ++){
			if(displayed[chosenIndex] == chosenString){
				break;
			}
		}
		if(chosenIndex == displayed.length){
			return null;
		}
		return finalListOfCourses[chosenIndex];
		 */
	}





	public void GUISupriseWindow(Semester s) {
		if(s.semesterDate.sNumber == (SemesterDate.MAYX)){
			//Tells Suprise Me to skip asking for the GER Req, chooses from all the classes. 
			new SupriseMe(sch, s, this, "Skip");
		}
		else{
			new SupriseMe(sch, s, this, null);
		}
	}

	public void GUIChallengeExcepted(Semester s, Course c){
		if(s.semesterDate.sNumber==SemesterDate.MAYX){
			//s.elements.clear();
			sch.addScheduleElement(new ScheduleCourse(c, this.sch), s);


		}
		else{

			sch.addScheduleElement(new ScheduleCourse(c, this.sch), s);

		}	

		this.update();
	}



	public void GUImakeSemesterStudyAway(Semester sem) {
		sem.setStudyAway(true);
		this.update();
		JOptionPane.showMessageDialog(null, "This semester is marked as Study Away, please drag in any requirements you will fulfill while abroad.", "Study abroad",JOptionPane.INFORMATION_MESSAGE,  icon  );

	}

	public void GUIremoveSemesterStudyAway(Semester sem) {

		sem.setStudyAway(false);
		this.update();
	}

	public void GUIaddNotes(Semester sem) {
		sem.setNotes("");
		this.update();

	}


	public void GUITextBeingWritten(DocumentEvent e, Semester s) throws BadLocationException {
		int length = e.getDocument().getLength();
		String noteWritten = e.getDocument().getText(0, length);
		s.setNotes(noteWritten);


	}

	public void GUIremoveNotes(Semester sem) {
		sem.setNotes(null);
		this.update();

	}







	/**
	 * A highly used method.
	 * Whenever there is a scheudling error, like classes overlapping,
	 * this method is called. The user is asked whether we should ignore
	 * the error. If the user overrides the error,
	 *  this method returns true, otherwise false.
	 * @param s
	 * @return
	 */
	public boolean userOverrideError(ScheduleError s){
		String header=headerFor(s);
		int maxElementStringLength = 100;//Use as many characters as necessary
		// for each of the schedule elements in the string.

		//instruct will be displayed as the main warning message
		String instruct= errorString(s, maxElementStringLength, true); 

		//Special cases for overload and duplicate - they get different strings here than when they're 
		// found in the checkAllErrors button
		if(s.error.equals(ScheduleError.overloadError)){
			instruct="Adding " + s.offendingCourse.shortString(maxElementStringLength) + " exceeds this semester's overload limit of " + s.overloadLimit + " credit hours ";
		}
		if(s.error.equals(ScheduleError.duplicateError)){
			instruct = s.elementList[0].getDisplayString() + " duplicates " +s.elementList[1].getDisplayString();
		}
		Object[] options = {"Ignore", "Cancel"};
		int n = JOptionPane.showOptionDialog(null, parseIntoReadable(instruct, FurmanOfficial.defaultCharacterLength) , header, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, icon, options, options[0]);
		return (n==JOptionPane.OK_OPTION);

	}

	/**
	 * Given a schedule error, return the string 
	 * @param e
	 * @return
	 */
	private String errorString(ScheduleError e, int preferredMaxLength, boolean surroundHTML){
		String result = "";
		if(e.error.equals(ScheduleError.overlapError)){
			result = (e.elementList[0].shortString(preferredMaxLength) + "\n    overlaps " + e.elementList[1].shortString(preferredMaxLength));
			ArrayList<String> issueStrings = new ArrayList<String>();
			if(e.meetingOverlap){
				issueStrings.add("meeting times");
			}
			if(e.examOverlap){
				issueStrings.add( "exams");
			}
			if(e.labOverlap){
				issueStrings.add("labs");
			}
			result += "\nOverlaps in :" + issueStrings.toString();

		}
		else if(e.error.equals(ScheduleError.overloadError)){
			result = e.offendingSemester.semesterDate.getUserString() + "  exceeds its overload limit of " 
					+ e.offendingSemester.getOverloadLimit()  + " credit hours";
		}
		else if(e.error.equals(ScheduleError.preReqError)){
			result =  e.offendingCourse.shortString(preferredMaxLength) + " needs prerequsite(s): " + e.req.toString();
		}
		else if(e.error.equals(ScheduleError.duplicateError)){
			result = e.offendingCourse.shortString(preferredMaxLength) + " is a duplicate course";

		}
		else if(e.error.equals(ScheduleError.optimisticSchedulerError)){
			Requirement r = e.getOptimisticRequirement();
			ArrayList<Requirement> pair = r.atLeastRequirementPairs();
			Requirement subset = pair.get(1);

			result = "The requirement " 
					+ r.shortString(70)
					+ " must include at least " 
					+ r.indent(subset.getDisplayString(),  "   ")
					+ "Until you change this requirment into a course, "
					+ "\nwe will assume that it represents one of these.";

		}

		result = parseIntoReadable(result, FurmanOfficial.defaultCharacterLength);


		return result;
	}

	private String headerFor(ScheduleError e){
		switch(e.error){
		case ScheduleError.overlapError:
			return "Overlap Error";
		case ScheduleError.duplicateError:
			return "Duplicate Error";
		case ScheduleError.overloadError:
			return "Overload Error";
		case ScheduleError.optimisticSchedulerError:
			return "Optimistic Planning";
		case ScheduleError.preReqError:
			return "Prereq Error";
		}
		return null;
	}


	public String getErrorString(){
		int elementPreferredMaxLength = 30;
		ArrayList<ScheduleError> allErrors =sch.checkAllErrors();
		String result = new String();
		if(!allErrors.isEmpty()){
			for(ScheduleError s : allErrors){
				result += errorString(s, elementPreferredMaxLength, false) + "\n";
			}
		}
		return result;
	}



	public void showExamineRequirementHelp(){
		JOptionPane.showMessageDialog(null, 
				"To see details about a requirement,"
						+ "\n  1) Right-click the requirement you want to examine"
						+ "\n  2) select \"" + MenuOptions.examineRequirementRightClick + "\" ");
	}
	public void showFindACourseHelp(){
		JOptionPane.showMessageDialog(null, 
				"To find a particular course, you can do one of the following:"
						+ "\n  1) If you know which semester(s) that course is offered, "
						+ "\n        click \"Options,\""
						+ "\n        and choose \"" + MenuOptions.addInstruct + "\""
						+ "\n  2) If you know of a requirement the course satisfies, you can"
						+ "\n        drag that requirement into a semester and click "
						+ "\n        \"" + MenuOptions.addCourseWithRequirement + "\""
						+ "\n  3) If the course is not in our database yet, you can"
						+ "\n        add a note to the semester where it is offered"
						+ "\n        by clicking \"Options\" and then \"" +MenuOptions.addNotes+"\"");
	}


	/**
	 * 
	 * @param s
	 * @param pixels
	 * @return
	 */
	public static String parseIntoReadable(String s, int characterLength){
		ArrayList<String> lines = new ArrayList<String>();
		for(String line: s.split("\n")){
			lines.add(line);
		}
		for(int i = 0; i<lines.size(); i++){
			String line = lines.get(i);
			if(line.length()>characterLength){
				int splitIndex = line.substring(0, characterLength).lastIndexOf(" ");
				lines.add(i+1, line.substring(splitIndex));
				lines.set(i, line.substring(0, splitIndex));
			}
		}
		StringBuilder result = new StringBuilder();
		for(String element: lines){
			result.append(element + "\n");
		}
		return result.toString();


	}






	public void dragStarted(ScheduleElement e){
		this.schP.dragStarted(e);

	}

	public void dragEnded(){
		this.schP.dragEnded();
	}

	public void updateAll(){
		if(sch.studentName != null){
			frame.setTitle("Academic Pathways Planner - " + sch.studentName);
		}
		else{
			frame.setTitle("Academic Pathways Planner");
		}
		schP.update(sch);
		reqs.update(sch);
		b.update();
	}

	public void repaintAll(){
		schP.revalidate();
		schP.repaint();

		reqs.revalidate();
		reqs.repaint();

		b.revalidate();
		b.repaint();

	}

	public void update() {
		updateAll();
		repaintAll(); 


	}





	public void addWindowListener(WindowListener w){
		frame.addWindowListener(w);
	}

	public void askMasterPassword(){
		String input = JOptionPane.showInputDialog("Knock knock \n \n...\n \n ... \n \n Password?");
		if(input == null){
			return;
		}
		if(input.toLowerCase().equals("a happy cat peers at you from its cloud")){
			//Doesn't repaint Driver, and can't repaint mainMenuBar, so if
			// there's a master-specific function in the menu bar then 
			// you have to make a new schedule to see it.
			FurmanOfficial.masterIsAround = true;
			JFrame debugScreen = new JFrame();
			JTextArea out = new JTextArea();
			System.setOut(new PrintStream(new OutputStream(){
				public void write(int b) throws IOException{
					out.append(String.valueOf( ( char )b ) );
				}

			}));

			debugScreen.setTitle("Debug area");
			debugScreen.add(out);
			debugScreen.pack();
			debugScreen.setVisible(true);
			debugScreen.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		}
	}






	final String successText = "You're all set! The import went smoothly.";
	
	public void showImportException(Exception e){
		String errorText = "";
		if(e.getMessage() == null){
			errorText = "Something very unexpected happened with your import, we're not sure what went wrong. \n";
			for(StackTraceElement element: e.getStackTrace()){
				errorText += element + "\n";
			}
		}
		else{
			errorText = "Something went wrong with the import: "
					+ "\n" + e.getMessage();
			if(FurmanOfficial.masterIsAround){
				errorText += "\n\n";
				for(StackTraceElement element: e.getStackTrace()){
					errorText += element + "\n";
				}
			}
		}
		JScrollPane scroll = new JScrollPane(new JTextArea(errorText));
		scroll.setPreferredSize(new Dimension(300,300));
		JOptionPane.showMessageDialog(null,scroll , "Import error ", JOptionPane.ERROR_MESSAGE);
	}
	/**
	 * Try to:
	 * 		Find the student data hidden in text.
	 * 		Test the data on an actual schedule
	 * 		use the data on this schedule
	 * 		write the data to the save file
	 * 
	 * If anything goes wrong, call showImportException.
	 * 
	 * text should be the text of the import textArea after the user has dragged in
	 * some data.
	 * 
	 * @param text
	 * @return
	 */

	public void trySetStudentDataFromWebsite(String text){


		PriorData d = new PriorData();
		try{
			d.readFromWebsiteDraggedData(text);
			new Schedule(d); //this actually tests the data out in practice.
			this.sch.readPrior(d);
			FileHandler.writeStudentData(d);
			this.update();
		}
		catch(Exception e){
			showImportException(e);
			this.update();
		}
	}



	/**
	 * Try to use the csv data:
	 * 		on a test schedule
	 * 		to load courses into the current schedule
	 * 		saved the data to a file
	 * 
	 * if anything goes wrong, call showImportException.
	 * 
	 */
	public void tryImportPriorCoursesViaFile(){
		PriorData d = new PriorData();
		try{
			String[][] data = FileHandler.importCSVStudentData();
			if(data == null){
				//the user canceled.
				return;
			}
			d.readFromCSV(data);
			new Schedule(d); //this actually tests the data out in practice.
			this.sch.readPrior(d);
			FileHandler.writeStudentData(d);
			this.update();
		}
		catch(Exception e){
			showImportException(e);
			this.update();
		}
	}
}










