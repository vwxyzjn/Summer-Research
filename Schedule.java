
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringJoiner;

/**
 * Blurb Written:7/27/2017
 * Last updated: 8/1/2017
 * 
 * The schedule class is the main object in the DATA side.
 * It is the half of the program that is serialized and saved. 
 * Each instantiation of this class is responsible for a single schedule.
 * It handles communication with the GUI side of classes.
 * 
 * The main GUI class (scheduleGUI) can make edits to the schedule.
 * Schedule can communicate to the GUI 
 * via the userOverride method. NOTHING IN THE DATA SIDE SHOULD DIRECTLY
 * CAUSE CHANGES TO THE SCREEN.
 * 
 * It also represents the current schedule (made of ScheduleElements, which can be
 * requirements or courses) and current majors that the user has loaded.
 * 
 * It also handles the bulk of error checking, looking for course overlap and such. 
 * If it finds an error that the user should be alerted to, it creates a new 
 * ScheduleError and then calls the method userOverride. 
 * 
 * Schedule handles updating any DATA classes that need updating, for example, 
 * updating all requirements if the user adds a new course. Any DATA class getter, 
 * like getMajors() or getAllElementsSorted(), should return fully updated objects.
 * 
 *
 */
public class Schedule implements java.io.Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private ArrayList<Major> majorsList;//the list of majors currently 
	//loaded by the schedule, excluding the GER. These are the majors that a student
	//has chosen to take, not a list of all possible majors. 
	private ArrayList<Semester> semesters;//The list of semesters. 
	// MayX and summer sessions must be explicitly added.
	
	public HashSet<Prereq> prereqs; //the list of all prereqs for any schedule element
	
	private Major GER; //the GER major is not stored in MajorsList.


	//transient is for Serializable purposes, Java does not like serializing swing things
	public transient ScheduleGUI schGUI;
	
	private Prefix languagePrefix; //Furman students get a language placement, which
	// determines their FL requirement in the GER major. This field stores 
	// a student's language placement.
	
	private int totalCoursesNeeded; //Used to calculate how done a req is. 
	
	
	boolean studentDecidesDegreeType = true; //There was some discussion if a student was taking
	//both a major with BA and BS then could they choose or would one of these be automatically given 
	//to them. This assumes that the student has the power to choose. 
	
	
	SemesterDate firstSemester; //the first semester at furman
	public SemesterDate currentSemester; //The earliest semester that can still be scheduled.
	// used to tell which semesters are already taken
	
	private Semester priorSemester;//the semester before you arrived at furman.
	// holds AP courses and placements

	public String studentName;// placed on print, and at the top of the program, only given if 
	//a schedule has been imported. 
	
	public static final boolean prereqsCanBeSatisfiedInSameSemester = false; //This is
	//so if Furman allows for a preReq to be taken at the same time as the upper level class
	//all one has to is switch this boolean. 

	/**
	 * This can be used for testing purposes, but is currently not in use. 
	 * @return Schedule. 
	 */
	public static Schedule testSchedule(){
		Driver.tryPickStartDate();
		Schedule result = new Schedule();
		result.readBlankPrior();
		return result;
	}


	/**
	 * Make a new schedule of semesters where firstSemester is the first shown semester 
	 * and currentSemester is the first semester that might be scheduled
	 * (assume that earlier semesters have passed and already have their courses fixed.)
	 * 
	 */
	public Schedule(){
		//Majors and requirements
		this.majorsList= new ArrayList<Major>();
		this.prereqs = new HashSet<Prereq>();
		//this.masterList = masterList;
		readBlankPrior(); //loads prior courses, recalc firstSemester and sets as currentSemester as well,
		// and create default semesters.
		this.recalcGERMajor();
	}

	
	/**
	 * This is called when a student has loaded their schedule into the 
	 * program.
	 * @param priorCourses Refers to the student's schedule. 
	 */
	
	public Schedule(PriorData priorCourses){
		this.majorsList= new ArrayList<Major>();
		this.prereqs = new HashSet<Prereq>();
		readPrior(priorCourses);
		recalcGERMajor();
		updatePrereqs();
		updateReqs();
	}


	public void setScheduleGUI(ScheduleGUI schGUI){
		this.schGUI = schGUI;
	}

	/**
	 * WARNING
	 * May wipe all scheduleElements from the schedule!!!!
	 * Set this semester as the first semester, and if the old first semester
	 * isn't equal to the new one, make new default semesters for the whole schedule.
	 * @param firstSemester
	 */
	public void setFirstSemester(SemesterDate firstSemester){
		if(firstSemester.compareTo(this.firstSemester) == 0){
			return;
		}
		//Semesters
		this.semesters = new ArrayList<Semester>();
		//Set prior semester 
		priorSemester = new Semester (firstSemester.previousSemester(), this);
		priorSemester.isPriorSemester = true;
		SemesterDate s = firstSemester;
		for(int i = 0; i < 8 ; i ++){
			this.semesters.add(new Semester(s, this));
			s = s.nextSemester();
		}
		this.firstSemester = firstSemester;
	}

	/**
	 * Perform the default operations if no priorData is given.
	 */
	public void readBlankPrior(){
		SemesterDate firstSemester = null;
		if(FileHandler.propertyGet(FileHandler.startSemester) != null){
			firstSemester = SemesterDate.readFrom(FileHandler.propertyGet(FileHandler.startSemester));
		}
		if( firstSemester == null){
			throw new RuntimeException("Tried to make a blank schedule before a default first semester was chosen");
		}
		setFirstSemester(firstSemester);
		setCurrentSemester(firstSemester);
		
	}


	/**
	 * Given this prior data, set 
	 * the current semester, first semester, add
	 * the correct semesters after that, load in all courses,
	 * and set the language prefix for the GER
	 * 
	 * @param pd
	 */
	public void readPrior(PriorData pd){
		this.studentName = pd.getStudentName();
		currentSemester = pd.getLatestDate();
		setFirstSemester(pd.getEarliestDate());
		this.setLanguagePrefix(pd.getLanguagePrefix());
		
		//Add the courses
		for(Course c : pd.getAllCourses()){
			ScheduleCourse cc = new ScheduleCourse(c, this);
			if(!this.directAddScheduleElement(cc, c.semesterDate)){
				throw new RuntimeException("Could neither find nor make the semester for the course \n" + cc.getDisplayString() +"," + c.semesterDate);
			}
		}
		MainMenuBar.addImportScheduleOption();
		
	}

	/**
	 * Add the schedule element with no error checks and no updates
	 *
	 * used when loading prior courses to avoid popups to the user.
	 * @param e
	 * @param d
	 * @return
	 */
	private boolean directAddScheduleElement(ScheduleElement e, SemesterDate d){
		ArrayList<Semester> allSemesters = this.getAllSemestersSorted();
		
		for(int i = 0; i < allSemesters.size(); i ++){
			
			Semester s  = allSemesters.get(i);
			if(s.semesterDate.compareTo(d) == 0){
				return s.directAdd(e);
			}
			if(s.semesterDate.compareTo(d) > 0){
				//we've passed where the semester should have been, so we need to make a 
				// new semester.
				
				//Replaced this with addNewSemesterInsideSch method
				// (if the method name changes, it's the second method in the 
				// gui methods for adding semesters).
				//int indexInSemesters = i-1;
				if(i == -1){
					return false;
				}
				Semester newSemester = this.addNewSemesterInsideSch(d.year, d.sNumber);
				//Semester newSemester = new Semester(d, this);
				//semesters.add(indexInSemesters, newSemester);
				return newSemester.directAdd(e);
			}
		}
		return false;
	}









	@SuppressWarnings("unused")
	private boolean ___StartMethodsUsedByGUI_________;

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	//////////	Methods called by the GUI  //////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/*		|		|		|		|
	 *		|		|		|		|
	 *		V		V		V		V
	 */


	///////////////////////////////
	///////////////////////////////
	//	Adding and removing semesters
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________addRemoveSemesters_________;
	
	/**
	 * This adds a Semester with the next SemesterDate. This adds it to the end
	 * of the schedule. 
	 * @return
	 */
	public Semester addNewSemester(){
		SemesterDate last = semesters.get(semesters.size() - 1).getDate();
		Semester next = new Semester(last.nextSemester(), this);
		this.semesters.add(next);
		return next;
	}

	/**
	 * This makes a semester and adds it within the schedule. 
	 * @param year
	 * @param season
	 * @return
	 */
	public Semester addNewSemesterInsideSch(int year, int season) {
		SemesterDate inside = new SemesterDate(year, season);
		Semester toAdd = new Semester(inside, this);
		this.semesters.add(toAdd);
		Collections.sort(semesters); //Puts in the correct spot. 
		return toAdd;
	}

	/**
	 * Removes the given semester. Currently only used for
	 * normal semesters at the end, and MayX, Summer semesters. 
	 * @param sem
	 */
	public void removeSemester(Semester sem) {
		this.semesters.remove(sem);
		Collections.sort(semesters);
		updatePrereqs();
		updateReqs();
	}



	///////////////////////////////
	///////////////////////////////
	//	Adding and removing Elements
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________addRemoveElements_________;


	/**
	 * In semester S, remove ScheduleElement e. Returns if the remove was
	 * successful, will return false if there was an error, or if the remove/
	 * updates were not successful. 
	 * @param e
	 * @param s
	 */
	public boolean removeElement (ScheduleElement e, Semester s){
		if(this.checkErrorsWhenRemoving(e, s)){
			return false;
		}
		if(s.remove(e)){
			updatePrereqs();
			updateReqs();
			return true;
		}
		System.out.println("Remove didn't work");
		return false;
	}

	/**
	 * In Semester sem, add scheduleElement element, if remove 
	 * was successful return true, false otherwise. 
	 * @param element
	 * @param sem
	 * @return
	 */
	public boolean addScheduleElement(ScheduleElement element, Semester sem) {
		if(this.checkErrorsWhenAdding(element, sem)){
			System.out.println("add didn't work");
			return false;
		}
		if(sem.add(element)){
			//updateRequirementsSatisfied(element);
			updatePrereqs();
			updateReqs();
			return true;
		}
		System.out.println("add didn't work");


		return false;
	}



	/**
	 * Replace the old element with new element.
	 * Assumes that oldSemester contains oldElement and
	 * that newSemester does NOT contain newElement.
	 * @param oldElement
	 * @param newElement
	 * @param oldSemester
	 * @param newSemester
	 * @return true if successful, false otherwise. 
	 */
	public boolean replace(ScheduleElement oldElement, ScheduleElement newElement, Semester oldSemester, Semester newSemester){
		if(this.checkErrorsWhenReplacing(oldSemester, newSemester, oldElement, newElement)){

			return false;
		}

		//Perform the addition and removal
		if(!oldSemester.remove(oldElement)){
			System.out.println("Replace didn't work");
			return false;
		}
		if(!newSemester.add(newElement)){
			System.out.println("Replace didn't work");
			return false;
		}
		//update things.
		if(oldElement != newElement){
			updatePrereqs();
			updateReqs();
		}
		return true;
	}

	///////////////////////////////
	///////////////////////////////
	//	Adding and removing Majors
	///////////////////////////////
	///////////////////////////////
	
	@SuppressWarnings("unused")
	private boolean _________addRemoveMajors_________;

	
	public void warnUserAboutDegreeChange(int orginalGERType){
		if(orginalGERType != GER.chosenDegree){
			schGUI.alertUserToThisChange();
		}
	}
	
	
	/**
	 * Adds Major to a student's majorsList,
	 * and updates teh GER "major"
	 * @param newMajor
	 */
	public void addMajor(Major newMajor){
		int orginalGERType = GER.chosenDegree;
		majorsList.add(newMajor);
		if(!newMajor.name.equals("GER")){
			recalcGERMajor();
		}
		warnUserAboutDegreeChange(orginalGERType);
		updateReqs();
		updateTotalCoursesNeeded();
	}

	/**
	 * Removes a Major from a student's list of majors.
	 * Recalcs GER accordingly. 
	 * @param major
	 */
	public void removeMajor(Major major) {
		int orginalGERType = GER.chosenDegree;
		majorsList.remove(major);
		if(!major.name.equals("GER")){//User should not be able to remove GER major.
			//This is a precaution. 
			recalcGERMajor();
		}
		warnUserAboutDegreeChange(orginalGERType);
		updatePrereqs();//courses might be removed in the future. This functionality is not in place. 
		updateReqs();
		updateTotalCoursesNeeded();
	}

	/**
	 * Currently the GER's are ordered by difficulty, this returns
	 * the most strenuous GER the student has based on the majors they
	 * have in their majorList. 
	 * @return
	 */
	public int determineGER(){
		
		if(studentDecidesDegreeType){
			HashSet<Integer> possibleMajorTypes = new HashSet<Integer>();
			for(Major m: this.majorsList){
				possibleMajorTypes.add(m.getChosenDegree());	
			}
			if(possibleMajorTypes.isEmpty()){
				return -1;
			}
			if(possibleMajorTypes.size() == 1){
				return (int) possibleMajorTypes.toArray()[0];	
			}
			else{
				return schGUI.askUserGERType(possibleMajorTypes.toArray());
			}
		}
		else{
			int highestDegree = -1;
			for(Major m: this.majorsList){
				if(!m.name.equals("GER")){
					if(m.getChosenDegree() > highestDegree){
						highestDegree=m.getChosenDegree();
					}

				}
			}
			return highestDegree;
		}
	}

	///////////////////////////////
	///////////////////////////////
	//	Completion Status
	///////////////////////////////
	///////////////////////////////
	
	@SuppressWarnings("unused")
	private boolean _________completionStatus_________;

	/**
	 * Return a number between 0 and 1 representing the completion level.
	 * @param iconHeight
	 * @return
	 */
	public double getPercentDone() {
		int leftToDo = this.estimatedCoursesLeft();
		int done = totalCoursesNeeded-leftToDo;
		double result = (done*1.0)/totalCoursesNeeded;
		return result;
	}

	/**
	 * True if student has complete all requirements, and they
	 * have chosen at atleast one major. This is used by BellTower. 
	 * @return
	 */
	public boolean isComplete(){
		if(getPercentDone() > 1.0 - 0.000000000001){
			if(majorsList.size() >= 1){
				return true;
			}
		}
		return false;
	}





	/*				^		^		^
	 * 				|		|		|
	 * 				|		|		|
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	//////////	Methods called by the GUI  //////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	 */






















	///////////////////////////////
	///////////////////////////////
	//	Nice getters
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean ___GettersAndSetters_________;
	

	

	///////////////////////////////
	///////////////////////////////
	//	Semesters
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________semesterGetSet_________;
	

	/**
	 * Return the list of semesters sorted in chronological order.
	 * PriorSemester is earlier than all other semesters.
	 */
	public ArrayList<Semester> getAllSemestersSorted(){
		ArrayList<Semester> allSemesters = new ArrayList<Semester>();
		allSemesters.add(priorSemester);
		Collections.sort(this.semesters);
		allSemesters.addAll(this.semesters);
		return allSemesters;
	}
	
	/**
	 * POSSIBLE EXTRANEOUS METHODS
	 * Returns the semesterDate of the student's first semster at Furman. 
	 * @return
	 */
//	public SemesterDate getStartDate(){ 
//		return this.getStartSemester().semesterDate;
//	}
//	public ArrayList<Semester> getSemesters(){
//		return this.semesters;
//	}

	
	/**
	 * POSSIBLE EXTRANEOUS METHODS
	 * This does not include prior Semester, because that is not apart of this.semesters
	 * @return The first real (not prior Semester)
	 */
//	public Semester getStartSemester(){
	//	Collections.sort(this.semesters); //Kept in as precaution. 
	//	return this.semesters.get(0);
	//}
	
//	public Semester getPriorSemester() {
	//	return priorSemester;
//	}

//	public SemesterDate getCurrentSemester() {
//		return currentSemester;
//	}


	public void setCurrentSemester(SemesterDate currentSemester) {
		this.currentSemester = currentSemester;
	}

	///////////////////////////////
	///////////////////////////////
	//	Elements
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________elementGetSet_________;


	/**
	 * Find the list of all ScheduleElements in any semester of this Schedule.
	 * Will be sorted based on the time the element was scheduled.
	 * @return ArrayList of scheduleElements. 
	 */
	public ArrayList<ScheduleElement> getAllElementsSorted(){
		ArrayList<ScheduleElement> result = new ArrayList<ScheduleElement>();
		for(Semester s : this.getAllSemestersSorted()){
			result.addAll(s.getElements());
		}
		return result;
	}
	

	
	///////////////////////////////
	///////////////////////////////
	//	Requirements
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean ________requirementGetSet_________;

	/**
	 * Find the list of all requirements in any major of this schedule.
	 * May include duplicate requirements if two majors share a requirement.
	 * Includes prereq requirements too.
	 * 
	 * Doesn't update requirements
	 * 		it can't, because it would cause an infinite loop.
	 * @return
	 */
	public ArrayList<Requirement> getAllRequirements(){
		ArrayList<Requirement> result = getAllRequirementsMinusPrereqs();
		for(Prereq p : prereqs){
			result.add(p.getRequirement());
		}
		return result;
	}
	
	public ArrayList<Requirement> getAllRequirementsMinusPrereqs(){
		ArrayList<Requirement> result = new ArrayList<Requirement>();
		for(Major m : this.majorsList){
			result.addAll(m.reqList);
		}
		result.addAll(GER.reqList);
		return result;
	}


	///////////////////////////////
	///////////////////////////////
	//	Majors
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________majorGetSet_________;

	/**
	 * doesn't update the requirements (this would cause an infinite loop, 
	 * updating a requirement means knowing all requirements, which can't happen unless
	 * you get all majors.)
	 * @return
	 */
	public ArrayList<Major> getMajors(){
		ArrayList<Major> result = new ArrayList<Major>();
		if(GER != null){ //This should never be null, a student should always have GER. 
			result.add(GER);
		}
		if(prereqs.size() > 0){ //Creates a preReq major, that holds all the classes a
			//student needs to schedule. 
			Major prereqsM = new Major("Prereqs");
			prereqsM.chosenDegree = Major.None;
			HashSet<Requirement> uniquePrereqs = new HashSet<Requirement>(); //no duplicates because
			//if two courses has a preReq of a course, you'd only want that course to show up once. 
			for(Prereq p : prereqs){
				if(!p.getRequirement().getStoredIsComplete()){
					uniquePrereqs.add(p.getRequirement());
				}
			}
			for(Requirement r : uniquePrereqs){
				prereqsM.addRequirement(r);
			}
			Collections.sort(prereqsM.reqList);
			if(uniquePrereqs.size() > 0){
				result.add(prereqsM);
			}
		}
		result.addAll(this.majorsList);
		return result;
	}

	/**
	 * Gets GER 'Major'
	 * @return
	 */
	public Major getGER() {
		return GER;
	}
	
//	public void setGER(Major gER) {
//		GER = gER;
//	}
	
	///////////////////////////////
	///////////////////////////////
	//  Other get set
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________otherGetSet_________;
	
	public int getCreditHoursComplete(){
		int result = 0;
		for (Semester s : this.getAllSemestersSorted()){
			result = result + s.getCreditHours();
		}
		return result;
	}


	//public Prefix getLanguagePrefix() {
	//	return languagePrefix;
	//}

	public void setLanguagePrefix(Prefix languagePrefix) {
		//The language classes prereq diagram looks like this:
		// arrows a --> b can be read as "a needs b"
		//  201 --> 120 --> 110
		// 					 
		//           115
		
		
		//String[] Language = {"110", "120", "201"};
		
		Integer.parseInt(languagePrefix.getNumber()); 
		//if the prefix can't be made into a terminal req of the form
		// FRN > 201, then we need to know sooner rather than later.
		
		this.languagePrefix = languagePrefix;
		recalcGERMajor();
		
		/**
		 * This was used to add the prereqs for language placement instead we added
		 * that placement class to the prereqs of the highest language class. 
		 * 
		 * //Figure out the index of the given prefixe's number,
			// so if you were given 120 savedLocation = 1.
		int savedLocation = -1;
		for(int i=0; i<Language.length; i++){
			if(languagePrefix.getNumber().equals(Language[i])){
				savedLocation=i;
			}
		}
		 * 
		 * //add all the things before it to your prior courses, so if you got
		// placed in 120, we'll add 110 to your prior courses,
		// and if you got placed in 201, we'll add 120 and 110 to your prior courses.
		if(savedLocation != -1){
			for(int p=0; p<savedLocation; p++){
				Course c= new Course(new Prefix(languagePrefix.getSubject(), "PL."+Language[p]), priorSemester.semesterDate, null, null, 
						0, null);
				ScheduleCourse cc = new ScheduleCourse(c, this);
				//cc.setTaken(true);
				addScheduleElement(cc,priorSemester);
			}
		}
		//Assume that if the prefix isn't in Language, then it's higher than 201.
		else if((!languagePrefix.getNumber().equals("115"))){
			for(int p=0; p<Language.length; p++){
				Course c= new Course(new Prefix(languagePrefix.getSubject(), "PL."+ Language[p]), this.getAllSemestersSorted().get(0).semesterDate, null, null, 
						0, null);
				ScheduleCourse cc = new ScheduleCourse(c, this);
				//	cc.setTaken(true);
				priorSemester.add(cc);
			}
		}
		 * 
		 
		 * 
		 */
		
	}
	
	
	
	


/**
 * This returns the string that is of the Schedule format variety. 
 * @return
 */
	public String getPrintScheduleString(){
		StringBuilder result = new StringBuilder();
		
		//Add Majors
		if(!this.majorsList.isEmpty()){
			result.append("<table style = 'width: 100%'> <tr>");
			result.append( "<th> Major:</th> " +
					" <th>Notes:</th> " +
					"</tr>");
			for(Major m: this.majorsList){
				result.append("<tr Align='left'> <td>" + m.name + "</td>  ");
				if(m.notes != null){
					result.append("<td>" + m.notes + "</td>");
				}
				result.append("</tr>");

			}
			result.append("</table>");
		}

		result.append("<center> <h2> Schedule </h2> </center> ");
		//Adds all the scheduleElements from each major
		for(Semester s: this.getAllSemestersSorted()){
			result.append("\n");
			if(s.isPriorSemester){
				result.append("<b>Prior Courses: </b>" + "\n");
			}
			else{
				result.append("<b>" + s.semesterDate.toString() + ": </b> \n");
			}
			if(s.studyAway){
				result.append("<b> STUDY AWAY SEMESTER </b>\n");
			}
			for(ScheduleElement se : s.elements){
				String prefix = "  ";
				if(se instanceof Requirement){
					prefix = "  Scheduled one course of: ";
					result.append(prefix + se.shortString(1000) + "\n");
				}
				else{
					result.append(se.getDisplayString() + "\n");
				}
			}
			if(s.elements.isEmpty()){
				result.append("Nothing scheduled for this semester \n");
			}
			if(s.hasNotes()){
				result.append("<b> Notes: </b>" +  s.notes + "\n");
			}

		}
		result.append("\n");
		//If any Errors Prints them 
		if(!schGUI.getErrorStrings().equals("")){
			result.append("<b> Scheduling Errors: </b> <br>" + schGUI.getErrorStrings());
		}
		//Things left CLPS, Estimated Courses Left, CrditHours
		result.append("\n <h3>The Final Countdown: </h3>");
		result.append("<b> Estimated Courses Left: </b> " + Math.max(0, this.estimatedCoursesLeft()) + "\n");
		result.append("<b> Credit Hours Left:</b> " +  Math.max(0, (128 - this.getCreditHoursComplete())) + "\n");
		String toResult = result.toString().replaceAll("&", "&amp;");
		return toResult.replaceAll("\n", "<br>");
	}


/**
 *  
 * @return String that is set up in the Requirement Layout. 
 */
	public String getPrintRequirementString(){
		SemesterDate defaultPrior = new SemesterDate(1995, SemesterDate.OTHER);
		ArrayList<ScheduleElement> allOrderedElements = new ArrayList<ScheduleElement>();
		ArrayList<SemesterDate> coorespondingDates = new ArrayList<SemesterDate>();
		for(Semester s: this.getAllSemestersSorted()){
			for(ScheduleElement se: s.elements){
				allOrderedElements.add(se);
				if(s.isPriorSemester){
					coorespondingDates.add(defaultPrior);

				}
				else {
					coorespondingDates.add(s.semesterDate);
				}
			}
		}
		StringBuilder result = new StringBuilder();
		result.append("<h2><center> Degree Checklist \n </center></h2>");
		result.append("<b> General Education Requirements </b>");

		Hashtable<ScheduleElement, HashSet<Requirement>> elementsSatisfy = new Hashtable<ScheduleElement, HashSet<Requirement>>();
		for(ScheduleElement e : this.getAllElementsSorted()){
			elementsSatisfy.put(e, new HashSet<Requirement>(e.filterEnemyRequirements(this.getAllRequirements())));
		}
		for(Major m: this.getMajors()){
			result.append("\n");
			result.append("<b>" + m.name + "</b>");
			ArrayList<Requirement> sortedReq = new ArrayList<Requirement>(m.reqList);
			Collections.sort(sortedReq);
			for(Requirement r: sortedReq){
				String rDisplay = r.shortString(10000) + " -";
				if(rDisplay.length()<=30){
					String spaces = new String (new char[30-rDisplay.length()]).replace("\0", " ");
					rDisplay = rDisplay + spaces;

				}
				result.append("\n <b>" +  rDisplay + "</b>" );

				boolean isComplete = r.getStoredIsComplete();
				if(!isComplete){
					int  coursesNeeded =  r.minMoreNeeded(getAllElementsSorted(), false);
					if(coursesNeeded == 1){
						result.append("<b><font color = '#F75D59'>" + coursesNeeded + " Course Needed </b></font>	\n");
					}
					if(coursesNeeded >1){
						result.append("<b><font color = '#F75D59'>" + coursesNeeded + " Courses Needed </b></font> \n");
					}
				}
				int counter = 0;

				ArrayList<Integer> satisfiedSEPointers = new ArrayList<Integer>();
				for(int i=0; i<this.getAllElementsSorted().size(); i++){
					ScheduleElement se = allOrderedElements.get(i);


					if(elementsSatisfy.get(se).contains(r)){
						satisfiedSEPointers.add(i);
					}
				}

				ArrayList<Integer> finalList = trimSEList(satisfiedSEPointers, allOrderedElements, r);
				for(int p=0; p<finalList.size(); p++){
					ScheduleElement se = allOrderedElements.get(finalList.get(p));
					if(counter ==0 && !isComplete){
						result.append("Partially Satisfied by: \n");
					}
					else if(counter == 0 && isComplete){
						result.append("Satisfied by: \n");
					}


					StringJoiner joiner = new StringJoiner("\n");
					StringBuilder part = new StringBuilder();

					//Different strings for requirements
					String priorIndent = "   ";
					part.append(priorIndent);
					if(se instanceof Requirement){
						part.append("Scheduled  " + se.shortString(10000).trim());

					}
					else{
						part.append(se.getDisplayString().trim());
					}

					//When was this thing taken?
					if(coorespondingDates.get(finalList.get(p)).equals(defaultPrior)){
						part.append(", " + "Taken before Furman \n");
					}
					else{
						part.append(", " + coorespondingDates.get(finalList.get(p)).toString() + "\n");
					}
					joiner.add(part.toString());
					counter++;
					result.append(joiner.toString());
				}
			}
		}
		String toResult = result.toString().replaceAll("&", "&amp;");
		return toResult.replaceAll("\n", "<br>");
	}
	
	/**
	 * Given a list of ScheduleElements (denoted by satisfiedSEPointers) 
	 * that complete Requirement r, trim that list down until it
	 * uses a minimal number of elements required to satisfy r. 
	 *   Not guaranteed to be the minimum, just that removing anything
	 *   else will break it.
	 * @param satisfiedSEPointers
	 * @param allOrderedElements
	 * @param r
	 * @return
	 */
	private ArrayList<Integer> trimSEList(ArrayList<Integer> satisfiedSEPointers, ArrayList<ScheduleElement> allOrderedElements, Requirement r) {
		ArrayList<ScheduleElement> toCompleteR = new ArrayList<ScheduleElement>();
		for(int i: satisfiedSEPointers){
			toCompleteR.add(allOrderedElements.get(i));
		}
		for(int i = 0; i<satisfiedSEPointers.size(); i++){
			ScheduleElement toRemove = allOrderedElements.get(satisfiedSEPointers.get(i));
			toCompleteR.remove(i);
			if(!r.isComplete(toCompleteR, false)){
				toCompleteR.add(i, toRemove);
			}
			else{
				satisfiedSEPointers.remove(i);
				i--;
			}
		}
		return satisfiedSEPointers;
	}


	///////////////////////////////
	///////////////////////////////
	//	General error checks for GUI events
	///////////////////////////////
	///////////////////////////////
	
	@SuppressWarnings("unused")
	private boolean ___GeneralErrorChecksForGUIEvents_________;
	
	public boolean userOverride(ScheduleError s){
		if(this.schGUI != null){
			return(schGUI.userOverrideError(s)); //SchGUI asks user to override. 
		}
		else{
			return true;
		}
	}
	
	/**
	 * Check the entire schedule for errors, and make a list of the errors.
	 * @return
	 */
	public  ArrayList<ScheduleError> checkAllErrors(){
		ArrayList<ScheduleError> allErrors = new ArrayList<ScheduleError>();
		//Don't check for errors in PriorSemester
		for(Semester s: this.semesters){
			ArrayList<ScheduleError> overlap = s.checkAllOverlap();
			if(overlap!=null){
				allErrors.addAll(s.checkAllOverlap());
			}
			ScheduleError overload = s.checkOverload(null);
			if(overload != null){
				allErrors.add(overload);	
			}
		}
		allErrors.addAll(this.checkAllPrerequsites());
		allErrors.addAll(this.checkAllDuplicates());
		return allErrors;
	}



/**
 * Return true if error is found. 
 * @param oldS
 * @param newS
 * @param oldElement
 * @param newElement
 * @return
 */
	public boolean checkErrorsWhenReplacing(Semester oldS, Semester newS, ScheduleElement oldElement, ScheduleElement newElement){
		if(this.checkPrerequsitesReplacing(oldS, newS, oldElement, newElement)){
			return true;
		}
		if(newElement == oldElement){
			if (checkDuplicates(newElement, true, false)){
				return true;
			}
		}
		else{
			if (checkDuplicates(newElement, false, false)){
				return true;
			}
		}
		if(newS.checkOverlap(newElement)){
			return true;
		}

		return false;
	}


	/**
	 * Return true if an error is found.
	 * @param e
	 * @param s
	 * @return
	 */
	public boolean checkErrorsWhenAdding(ScheduleElement e, Semester s){
		if(checkPrerequsitesAdding(e, s.semesterDate)){
			return true;
		}
		if(s.checkOverlap(e)){
			return true;
		}
		if(checkDuplicates(e,false, false)){
			return true;
		}
		if(e instanceof Requirement){
			if(checkOptimismError((Requirement)e)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Return true if error is found. 
	 * @param e
	 * @param s
	 * @return
	 */
	public boolean checkErrorsWhenRemoving(ScheduleElement e, Semester s){
		return(this.checkPrerequsitesRemoving(e, s.getDate()));
	}



	///////////////////////////////
	///////////////////////////////
	//	Prerequisite error checking
	///////////////////////////////
	///////////////////////////////
	
	@SuppressWarnings("unused")
	private boolean _________prereqErrorChecking_________;

	/**
	 * Check every element to see if it has all the prereqs it needs.
	 * Returns a list of the following form
	 * [ [e1, needed1] , [e2, needed2], [e3, ...]
	 * or null if no prerequsite issues were found.
	 */
	public ArrayList<ScheduleError> checkAllPrerequsites(){
		ArrayList<ScheduleError> result = new ArrayList<ScheduleError>();
		ArrayList<ScheduleElement> taken = new ArrayList<ScheduleElement>();
		//Go through semesters one at a time so that you efficiently
		// build the list of prior-taken elements.
		for(Semester s : getAllSemestersSorted()){
			ArrayList<ScheduleElement> inSemester = new ArrayList<ScheduleElement>();
			for(ScheduleElement e : s.getElements()){
				inSemester.add(e);
			}
			if(Schedule.prereqsCanBeSatisfiedInSameSemester){
				taken.addAll(inSemester);
			}
			for (ScheduleElement e : inSemester){
				Prereq needed = CourseList.getPrereq(e.getPrefix());
				if(needed != null){
					needed.updateOn(taken);
					boolean complete = needed.getRequirement().getStoredIsComplete();
					if(!complete){
						ScheduleError prereq = new ScheduleError(ScheduleError.preReqError);
						prereq.setOffendingCourse(e);
						prereq.setNeededCourses(needed.getRequirement());
						prereq.setOffendingSemester(s);
						result.add(prereq);
					}
				}
			}
			if(!Schedule.prereqsCanBeSatisfiedInSameSemester){
				taken.addAll(inSemester);
			}
		}
		return result;
	}


	/**
	 * Find the Prereq needed for this prefix if taken at the given
	 * time. Return the Prereq after it has been updated based on the
	 * correct set of schedule elements.
	 * 
	 * May return null.
	 * @param p
	 * @param sD
	 * @return
	 */
	public Prereq prereqNeededFor(Prefix p, SemesterDate sD){
		if(p == null){
			return null;
		}
		else{
			ArrayList<ScheduleElement> taken = elementsTakenBefore(sD);
			if(Schedule.prereqsCanBeSatisfiedInSameSemester){
				taken.addAll(elementsTakenIn(sD));
			}
			Prereq needed = CourseList.getPrereq(p);
			if(needed != null){
				needed.updateOn(taken);
			}
			return needed;
		}
	}
	

	/**
	 * Check if any prereq errors happen as a result of adding (not replacing or removing)
	 * element e to the schedule at date SD.
	 * @param e
	 * @param sD
	 * @return
	 */
	public boolean checkPrerequsitesAdding(ScheduleElement e, SemesterDate sD){
		//The only possible errors are if e itself has a prereq - 
		// we can't mess up any other courses by just adding.
		Prereq needed = prereqNeededFor(e.getPrefix(), sD);
		//needed will have correct storedMinMoreNeeded values at this point.
		if(needed == null){
			return false; //no errors found
		}
		if(!needed.getRequirement().getStoredIsComplete()){
			ScheduleError preReq = new ScheduleError(ScheduleError.preReqError);
			preReq.setOffendingCourse(e);
			preReq.setNeededCourses(needed.getRequirement());
			//	preReq.setInstructions(e.getDisplayString() + " needs prerequisite(s)" + needed.toString());
			return(!this.userOverride(preReq));
			//throw new PrerequsiteException(needed, e);
		}
		return false;
	}

	/**
	 * Check if any prereq errors happen as a result of removing (not adding or replacing) 
	 * element e to the schedule at date SD.
	 * @param e
	 * @param s
	 * @return
	 */
	public boolean checkPrerequsitesRemoving(ScheduleElement e, SemesterDate SD){
		//A remove might mess up a course after e, but can't hurt courses
		// before e.
		Prefix currentP = e.getPrefix();
		if(currentP == null){
			return false; //null prefix can't satisfy any prereqs.
		}
		ArrayList<ScheduleElement> taken = new ArrayList<ScheduleElement>();
		for(Semester other : this.getAllSemestersSorted()){
			//if this semester is after s
			if( (prereqsCanBeSatisfiedInSameSemester && other.getDate().compareTo(SD) == 0 ) //If they are in the same semester, and allowed to be so
					||  other.getDate().compareTo(SD) > 0 ){
				//add all the elements in this semester into taken, leaving out e.
				for(ScheduleElement laterElement : other.elements){
					if(other.getDate().compareTo(SD) != 0 || (!laterElement.equals(e))){
						taken.add(laterElement);
					}
				}
				//compare the prereqs needed if you include e to the
				// prereqs needed if you leave e out. If they're different,
				// throw a prereq error.
				ArrayList<ScheduleElement> causes = new ArrayList<ScheduleElement>();
				for(ScheduleElement laterElement  : other.elements){
					Prereq needed = CourseList.getPrereq(laterElement.getPrefix());
					if(needed == null){
						continue;
					}
					needed.updateOn(taken);
					int neededWithoutE = needed.getRequirement().getStoredNumberLeft();
					taken.add(e);
					needed.updateOn(taken);
					int neededWithE = needed.getRequirement().getStoredCoursesLeft();
					taken.remove(taken.size() - 1);
					
					if(neededWithoutE != neededWithE){
						causes.add(laterElement);
					}
				}
				for( int i = 0; i < causes.size() ; i ++){
					ScheduleError preReq = new ScheduleError(ScheduleError.preReqError);
					preReq.setOffendingCourse(causes.get(i));
					preReq.setNeededCourses(new TerminalRequirement(e.getPrefix()));
					//		preReq.setInstructions(e.getDisplayString() + " needs prerequisit(s) " + needed.toString());
					if(!this.userOverride(preReq)){
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * If an error is found, return true.
	 * @param fromSem
	 * @param toSem
	 * @param oldE
	 * @param newE
	 * @return
	 */
	public boolean checkPrerequsitesReplacing(Semester fromSem, Semester toSem, 
			ScheduleElement oldE, ScheduleElement newE){
		Prefix newP = newE.getPrefix();
		Prefix oldP = oldE.getPrefix();
		
		
		//If the prefixes aren't the same,
		// then as far as prereqs care,
		// we're just doing an add and a remove.
		if(newP == null || oldP == null || !(newP.equals(oldP))){
			return (checkPrerequsitesRemoving(oldE, fromSem.getDate()) ||checkPrerequsitesAdding(newE, toSem.semesterDate));
		}
		//If the prefixes are equal, we're really moving the time we take this prefix.
		else{
			if(fromSem.semesterDate.compareTo(toSem.semesterDate) == 0){
				return false;// no errors.
			}
			//If we're moving the course backward in time 
			// we might no longer satisfy its prereqs.
			if(fromSem.semesterDate.compareTo(toSem.semesterDate) > 0){
				Prereq stillNeeded = prereqNeededFor(newP, toSem.semesterDate);
				Prereq neededBefore = prereqNeededFor(oldP, fromSem.semesterDate);
				if(stillNeeded == null){
					 return false;
				}
				int stillNeededNum = stillNeeded.getRequirement().getStoredNumberLeft();
				int neededBeforeNum = neededBefore.getRequirement().getStoredNumberLeft();

				if(stillNeededNum != neededBeforeNum){
					ScheduleError preReq = new ScheduleError(ScheduleError.preReqError);
					preReq.setOffendingCourse(newE);
					preReq.setNeededCourses(stillNeeded.getRequirement());
					//	preReq.setInstructions(newE.toString() + " has prerequisite " + missing.toString());
					return((!this.userOverride(preReq)));
				}
				else{
					return false;
				}
			}
			//If we're moving the course forward in time. 
			//the only courses that need to be checked are those in between the new position and
			//the old position of the moving course, these are the only
			//courses that might loose a prereq by this operation. 

			//prefixes between oldSem and newSem.
			// In the following code, new refers to the new location for the element, and 
			// to the semester who's date comes later. (2020 is new, 2018 is old).
			ArrayList<ScheduleElement> beforeTo = this.elementsTakenBefore(toSem.semesterDate);
			ArrayList<ScheduleElement> afterFrom = this.elementsTakenAfter(fromSem.semesterDate);
			ArrayList<ScheduleElement> inFrom = this.elementsTakenIn(fromSem);
			if(Schedule.prereqsCanBeSatisfiedInSameSemester){
				afterFrom.addAll(inFrom);
			}
			afterFrom.retainAll(beforeTo);
			ArrayList<ScheduleElement> intersection = afterFrom;

			//for each element we're jumping over, check if
			// we satisfied one of that element's prereqs.
			HashSet<ScheduleElement> elementsThatUsedTheMovingElement = new HashSet<ScheduleElement>();
			for(ScheduleElement e : intersection){
				Prereq p = CourseList.getPrereq(e.getPrefix());
				if(p != null && p.getRequirement().isSatisfiedBy(newE)){
					elementsThatUsedTheMovingElement.add(e);
				}
			}
			for(ScheduleElement e : elementsThatUsedTheMovingElement){
				ScheduleError preReq = new ScheduleError(ScheduleError.preReqError);
				preReq.setOffendingCourse(e);
				preReq.setNeededCourses(new TerminalRequirement(newP));
				if(!this.userOverride(preReq)){
					return true;
				}
			}
			return false;
		}
	}


	/**
	 * All prefixes taken before or including this semester.
	 * @param sd
	 * @return
	 */
	public ArrayList<ScheduleElement> elementsTakenBefore(SemesterDate sd){
		ArrayList<ScheduleElement> taken = new ArrayList<ScheduleElement>();
		for(Semester s : this.getAllSemestersSorted()){
			// Allow courses taken before or in the same semester.
			if(s.semesterDate.compareTo(sd) < 0){
				taken.addAll(elementsTakenIn(s));
			}
		}
		return taken;
	}
	/**
	 * All prefixes scheduled strictly after this semester.
	 * @param sd
	 * @return
	 */
	public ArrayList<ScheduleElement> elementsTakenAfter(SemesterDate sd){
		ArrayList<ScheduleElement> taken = new ArrayList<ScheduleElement>();
		for(Semester s : this.getAllSemestersSorted()){
			// Allow courses taken before or in the same semester.
			if(s.semesterDate.compareTo(sd) > 0){
				taken.addAll(elementsTakenIn(s));
			}
		}
		return taken;
	}
	public ArrayList<ScheduleElement> elementsTakenIn(SemesterDate sD){
		ArrayList<ScheduleElement> taken = new ArrayList<ScheduleElement>();
		for(Semester s : this.getAllSemestersSorted()){
			// Allow courses taken before or in the same semester.
			if(s.semesterDate.compareTo(sD) == 0){
				taken.addAll(elementsTakenIn(s));
			}
		}
		return taken;
	}
	public ArrayList<ScheduleElement> elementsTakenIn(Semester s){
		ArrayList<ScheduleElement> result = new ArrayList<ScheduleElement>();
		result.addAll(s.getElements());
		return result;
	}





	///////////////////////////////
	///////////////////////////////
	//	Duplicate error checking
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________duplicateErrorChecking_________;


	/**
	 * Check all semesters for duplicates.
	 * If one is found, return true.
	 */
	public boolean  checkDuplicates(){
		ArrayList<ScheduleError> result = checkAllDuplicates();
		for(ScheduleError s: result){
			if(checkDuplicates(s.getOffendingCourse(), true, false) == false){
				return true;
			}
		}
		return false;
	}

	public ArrayList<ScheduleError> checkAllDuplicates(){
		ArrayList<ScheduleError> result = new ArrayList<ScheduleError>();
		for(ScheduleElement element : getAllElementsSorted()){
			if(checkDuplicates(element, true, true)){
				ScheduleError duplicate = new ScheduleError(ScheduleError.duplicateError);
				duplicate.setOffendingCourse(element);
				result.add(duplicate);


			}
		}
		return(result);
	}

	public boolean checkDuplicates(ScheduleElement e, boolean alreadyAdded, boolean hideUserOverride){
		int exactDuplicateCount = 1;
		if(alreadyAdded){
			exactDuplicateCount = 0;
		}

		for(ScheduleElement e1 : getAllElementsSorted()){
			if(e1 == e){
				exactDuplicateCount++;
				if(exactDuplicateCount > 1){
					if(e1.isDuplicate(e) || e.isDuplicate(e1)){
						ScheduleElement[] results = {e, e1};
						ScheduleError duplicate = new ScheduleError(ScheduleError.duplicateError);
						duplicate.setOffendingCourse(results[0]);
						//	duplicate.setInstructions(results[0].getDisplayString() + " duplicates " + results[1]	);
						duplicate.setElementList(results);
						if(hideUserOverride == false){
							return(!this.userOverride(duplicate));
						}
						else{
							return(true);
						}
					}
				}
				continue;
			}
			if(e1.isDuplicate(e) || e.isDuplicate(e1)){
				ScheduleElement[] result = {e, e1};
				ScheduleError duplicate = new ScheduleError(ScheduleError.duplicateError);
				duplicate.setOffendingCourse(result[0]);
				duplicate.setElementList(result);
				//	duplicate.setInstructions(result[0].getDisplayString() + " duplicates " + result[1]	);
				if(hideUserOverride == false){
					return (!this.userOverride(duplicate));
				}
				else{
					return true;
				}
			}
		}
		return false;
	}

	
	

	///////////////////////////////
	///////////////////////////////
	//	Optimism error
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________optimismErrorChecking_________;



	/**
	 * Check if adding r to the schedule would implicitly assume
	 * some kind of optimal future behavior on the user's part.
	 * 
	 * In this particular method, it checks if r is an 'at least' requirement,
	 * like "5 of A-Z with at least 3 of A-K." If, when we drag r into the schedule,
	 * the only course that could be taken to further the completion of r is in the
	 * subset (A-K is a subset of A-Z), then we're subtly requiring the user to
	 * take an A-K class when they're seeing a different requirement in the schedule.
	 * 
	 * This method assumes that r
	 * is not yet in getAllElementsSorted().
	 * @param r
	 * @return
	 */
	public boolean checkOptimismError(Requirement r){
		if(r.getStoredIsComplete()){
			return false;
		}
		//If adding this requirement to the schedule would entail a leap of faith
		ArrayList<Requirement> pairs = r.atLeastRequirementPairs();
		if(pairs.isEmpty()){
			return false;
		}
		Requirement superset = pairs.get(0);
		Requirement subset = pairs.get(1);
		ArrayList<ScheduleElement> allCurrentElements = this.getAllElementsSorted();

		//First, if subset is already complete then we're not
		// assuming any optimal behavior - we're just scheduling a
		// superset-only course.
		if(subset.isComplete(allCurrentElements, false)){
			//We've already completed subset, so r isn't making the schedule
			// assume any optimal behavior. 
			return false;
		}


		//At this point, we know that r has at least one subsetOnly course to schedule.
		//The next test checks if adding r would force us take a subset-only course,
		// or if we might get away by just taking a superset-only course.
		//replace instances of r with instances of superset so that the call to 
		//minMoreNeeded will work.
		for(int i = 0; i < allCurrentElements.size() ; i ++){
			if(r.equals(allCurrentElements.get(i))){
				allCurrentElements.set(i,  superset);
			}
		}
		if(superset.minMoreNeeded(allCurrentElements, false) > subset.getOriginalNumberNeeded()){
			//We still need more classes to fill out superset-only, so we can 
			// pretend that this requirement stands for a member of superset
			// rather than standing for a member of subset-only.
			return false;
		}

		//We are now left with only one possibility:
		// The only class that this requirement could count for is
		// one of the classes in subset - if we say nothing, then
		// this requirement will act as a planned member of subset.
		// So we should alert the user to this and ask them what they think.
		ScheduleError optimismError = new ScheduleError(ScheduleError.optimisticSchedulerError);
		optimismError.setOptimisticRequirement(r);
		return (! schGUI.userOverrideError(optimismError));

	}




	///////////////////////////////
	///////////////////////////////
	//	Course overlap error checking
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________courseOverlapErrorChecking_________;

	/**
	 * Check all semesters to see if any elements in them
	 * have overlapping times (are taken at the same time)
	 * This method will not cause any errors to be displayed to the user.
	 */
	public ArrayList<ScheduleError> checkOverlap(){
		ArrayList<ScheduleError> result = new ArrayList<ScheduleError>();
		//This check intentionally does not check for overlap in PriorSemester
		for(Semester s : semesters){
			result.addAll(s.checkAllOverlap());
		}
		return result;
	}


	
	
	
	///////////////////////////////
	///////////////////////////////
	//	Updating methods
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean ___Updates_________;

	private void recalcGERMajor(){	
		int type = this.determineGER();
		if(type == -1){ //None given assume BA
			type = Major.BA;
		}
		this.GER =CourseList.getGERMajor(languagePrefix, type);
		updateTotalCoursesNeeded();
	}

	private void updateTotalCoursesNeeded(){
		totalCoursesNeeded = 0;
		
		for(Requirement r : getAllRequirements()){
			
			totalCoursesNeeded += r.getOriginalCoursesNeeded();
		}
	}

	/**
	 * used when loading a schedule from a file, in case the details of the
	 * majors changed since the schedule was saved. Probably will be unused
	 * in the final version, but it does offer a neat strategy for seniors
	 * to hold onto majors that were valid when they were freshmen, but are
	 * no longer technically offered.
	 */
	public void reloadMajors() {
		ListOfMajors m = FileHandler.getMajorsList();
		ArrayList<Major> newMajorsList = new ArrayList<Major>();
		for(Major major: this.majorsList){
			Major refreshed = m.getMajor(major.name);
			refreshed.setChosenDegree(major.chosenDegree);
			newMajorsList.add(refreshed);
		}
		setMajorsList(newMajorsList);
	}

	private void setMajorsList(ArrayList<Major> majors){
		this.majorsList= majors;
		this.recalcGERMajor();
		this.updatePrereqs();
		this.updateReqs();
		this.updateTotalCoursesNeeded();
	}

	///////////////////////////////
	///////////////////////////////
	//	Keeping requirements up to date
	///////////////////////////////
	///////////////////////////////
	// Two main booleans are used here:
	// reqsValid and reqsFulfilledValid.
	// 
	// reqsValid tells if all the 
	// requirements in all majors are up-to-date
	// and know how many scheduled elements are currently
	// satisfying them
	//
	// reqsFulfilledValid tells if each scheduleElement knows
	// which requirements it satisfies.
	@SuppressWarnings("unused")
	private boolean _________updateRequirements_________;


	/**
	 * Forces a full update of all of the requirements,
	 * ensuring that all majors know which relevant 
	 * courses have been taken.
	 * 
	 * Also updates the prereqs 
	 *  
	 */
	private void updateReqs(){
		//This list cannot be a set because we need duplicate requirements
		// to potentially be satisfied twice.
		ArrayList<ScheduleElement> allTakenElements = getAllElementsSorted();
		//Same issue here.
		ArrayList<Requirement> reqList = this.getAllRequirementsMinusPrereqs();
		ArrayList<ArrayList<Requirement>> reqsFulfilled = new ArrayList<ArrayList<Requirement>> ();
		for(ScheduleElement e: allTakenElements){
			if(e instanceof ScheduleCourse){
				if (((ScheduleCourse) e).c.name.contains("Roman")){
					ArrayList<Requirement> filter = e.filterEnemyRequirements(reqList);
					System.out.println("updateReqs " + filter);
					System.out.println("updateReqs" + filter.size() );
				}
			}
			reqsFulfilled.add(e.filterEnemyRequirements(reqList));
		}
		for(Requirement r : reqList){
			updateRequirement(r, allTakenElements, reqsFulfilled);
		}

		updatePrereqs();
	}

	/**
	 * check if anything needs to be updated, and update if it does.
	 * 
	 * This is the only public method to ask schedule to revalidate requirements.
	 */
	public void checkUpdateReqs(){
		/*if(!reqsValid){*/
		updateReqs();
		//}
	}

	/**
	 * Update this requirement's stored more-needed values based on 
	 * the correct list of elements, taking into account requirement enemies.
	 * (See Requirement graph for explanation of requirement enemies)
	 * @param r
	 */
	public void updateRequirement(Requirement r, 
			 ArrayList<ScheduleElement> allTakenElements, ArrayList<ArrayList<Requirement>> reqsFulfilled){
		//These last two are passed in for speed issues. 
		//For each requirement, find all the schedule elements that satisfy it
		// (this prevents enemy requirements from both seeing the same course)s
		
		//Courses that don't have enemies, and exclude courses that do have enemies
		ArrayList<ScheduleElement> satisficers = new ArrayList<ScheduleElement>();

		for(int i=0; i<allTakenElements.size();i++){
			ScheduleElement e = allTakenElements.get(i);
			if(reqsFulfilled.get(i).contains(r)){
				satisficers.add(e);
			}
		}
		r.updateAllStoredValues(satisficers);
	}


	/**
	 * Get an estimate of the number of scheduleElements still needed for this
	 * schedule to be complete. This is probably an overestimate (it can 
	 * be done in fewer steps) because some elements may be able to satisfy
	 * multiple requirements at once.
	 * @return
	 */
	public int estimatedCoursesLeft(){
		int counter = 0;
		for(Requirement r: this.getAllRequirements()){
			counter += r.getStoredCoursesLeft();
		}
		return counter;
	}

	/**
	 * /**
	 * Tells this scheduleElement the list of requirements that it satisfies.
	 * 		this list is a subset of the requirements list implied by the set
	 * 		of majors. So, if an element satisfies the MTH-elective requirement and
	 * 		the MTH-GER requirement, and the schedule  (at this point) has the
	 * 		GER major but not the math major, then after calling this method
	 * 		on element e, e.getRequirementsSatisfied() will return {MTH_GER}.
	 * 
	 * @param e
	 */
	/*public void updateRequirementsSatisfied(ScheduleElement e){
		if(e instanceof Course){
			updateRequirementsSatisfied((Course) e);
		}
		if(e instanceof Requirement){
			updateRequirementsSatisfied((Requirement)e);
		}
	}
	private void updateRequirementsSatisfied(Requirement r){
		//TODO fill this out.
	}

	private void updateRequirementsSatisfied(Course c){
		ArrayList<Requirement> allSatisfied = new ArrayList<Requirement>();
		for(Requirement r : this.getAllRequirements()){
			if(r.isSatisfiedBy(c.getPrefix())){
				allSatisfied.add(r);
			}
		}
		// If this course satisfies two requirements that don't play well with
		// each other, then we don't want to add them to the list.
		// Rather, the user will be prompted to tell us which one they want
		// to be satisfied.
		ArrayList<Requirement> toRemove = new ArrayList<Requirement>();
		for(int i = 0; i < allSatisfied.size() ; i ++){
			for(int j = i+1 ; j < allSatisfied.size(); j ++){
				if(this.dontPlayNice(allSatisfied.get(i), allSatisfied.get(j))){
					toRemove.add(allSatisfied.get(i));
					toRemove.add(allSatisfied.get(j));
				}
			}
		}
	}
	 */

	public boolean dontPlayNice(Requirement r1, Requirement r2){
		return !RequirementGraph.doesPlayNice(r1, r2);
	}


	///////////////////////////////
	///////////////////////////////
	//// Updating Prereqs
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean _________updatePrereqs_________;
	
	public void updatePrereqs(){
		//Make sure prereqs accurately reflect the currently scheduled elements.
		prereqs = new HashSet<Prereq>();
		for(ScheduleElement e : getAllElementsSorted()){
			//Taken courses don't need prereqs loaded.
			if(e instanceof ScheduleCourse){
				if(((ScheduleCourse) e).getSemester().compareTo(currentSemester)<0){
					continue;
				}
			}
			Prereq p = CourseList.getPrereq(e.getPrefix());
			if(p != null){
				prereqs.add(p);
			}
		}

		//Actually update the prereqs.
		for(Prereq p : prereqs){
			updatePrereq(p);
		}
	}

	public void updatePrereq(Prereq p){
		ArrayList<ScheduleElement> elementsBefore = this.elementsBefore(p.getPrefix(), prereqsCanBeSatisfiedInSameSemester);
		p.updateOn(elementsBefore);
	}





	

	///////////////////////////////
	///////////////////////////////
	//// Filters
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean ___Filters_________;

	//TODO use streams
	public ArrayList<Major> filterAlreadyChosenMajors(ArrayList<Major> collectionOfMajors ) {
		collectionOfMajors.removeAll(this.getMajors());
		return collectionOfMajors;
	}

	
	///////////////////////////////
	///////////////////////////////
	//// Utilities
	///////////////////////////////
	///////////////////////////////
	@SuppressWarnings("unused")
	private boolean ___Utilities_________;


	public boolean SemesterAlreadyExists(SemesterDate semesterDate) {
		for(Semester s: this.getAllSemestersSorted()){
			if(s.semesterDate.equals(semesterDate)){
				return true;
			}
			if(s.semesterDate.compareTo(semesterDate) > 0){
				return false;
			}
		}
		return false;
	}


	/**
	 * Find the set of requirements that this course should satisfy, given the
	 * list of requirement enemies that are trying to be fulfilled by this course.
	 * 
	 * If schGUI is not null, ask the user to resolve. Otherwise, none of the requirements
	 * get to be used.
	 * @param enemies
	 * @param c
	 * @return
	 */
	public HashSet<Requirement> resolveConflictingRequirements(HashSet<Requirement> enemies, Course c){
		ArrayList<Requirement> reqs = new ArrayList<Requirement>();
		ArrayList<Major> majors = new ArrayList<Major>();
		//associate each requirement with its major.
		for(Requirement r : enemies){
			boolean found = false;
			for(Major m : this.getMajors()){
				if(found != true){
					if(m.reqList.contains(r)){
						found = true;
						reqs.add(r);
						majors.add(m);
					}
				}
			}
			if(found == false){
				throw new RuntimeException("I couldn't find the major for this requirement" + r);
			}
		}
		if(this.schGUI != null){
			//Ask the user to pick for us.
			return schGUI.resolveConflictingRequirements(reqs, majors, c);
		}
		else{
			//If we try to resolve conflicts on our own, this is where we would do it.
			return new HashSet<Requirement>();
		}
	}


	/**
	 * Returns all scheduledCourses in a semester. Might be able to turn into Stream. 
	 * @param s
	 * @return
	 */
	public ArrayList<ScheduleCourse> getCoursesInSemester(Semester s) {
		ArrayList<Course> toFinal = CourseList.getCoursesIn(s);
		ArrayList<ScheduleCourse> scheduleCoursesInSemester = new ArrayList<ScheduleCourse>();
		for(Course c: toFinal){
			ScheduleCourse sc = new ScheduleCourse(c, this);
			scheduleCoursesInSemester.add(sc);
		}
		return scheduleCoursesInSemester;
	}


	/**
	 * find the first (temporal first) scheduled instance of this prefix,
	 * and return the list of all elements before it.
	 * 
	 * If includeElementsInSameSemester, then the element with prefix p will
	 * also be included in the list.
	 * 
	 * returns empty array list if this prefix isn't scheduled.
	 * 
	 * @param e
	 * @param includeElementsInSameSemester
	 * @return
	 */
	public ArrayList<ScheduleElement> elementsBefore(Prefix p, boolean includeElementsInSameSemester){
		ArrayList<ScheduleElement> result = new ArrayList<ScheduleElement>();
		SemesterDate firstScheduledTime = earliestInstanceOf(p);
		if(firstScheduledTime == null){
			return result;
		}
		result.addAll(this.elementsTakenBefore(firstScheduledTime));
		if(includeElementsInSameSemester){
			result.addAll(this.elementsTakenIn(firstScheduledTime));
		}
		return result;
	}

	public SemesterDate earliestInstanceOf(Prefix p){
		if(p == null){
			return null;
		}
		for(Semester s : this.getAllSemestersSorted()){
			for(ScheduleElement e : s.getElements()){
				if(p.equals(e.getPrefix())){
					return s.getDate();
				}
			}
		}
		return null;
	}

}

