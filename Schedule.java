
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringJoiner;


public class Schedule implements java.io.Serializable {
	/**
	 * 
	 */

	private static final long serialVersionUID = 1L;
	private ArrayList<Major> majorsList;
	private ArrayList<Semester> semesters;
	public HashSet<Prereq> prereqs;
	private Major GER;






	//transient is for Serializable purposes.
	public transient ScheduleGUI d;
	CourseList masterList;
	private int CLP;
	private Prefix languagePrefix;
	private int totalCoursesNeeded;
	private Semester priorSemester;
	
	


	public boolean skipOverrides = false;

	public static SemesterDate defaultFirstSemester; //TODO this should be removed after demos.
	public SemesterDate currentSemester;
	SemesterDate firstSemester;

	public static final boolean prereqsCanBeSatisfiedInSameSemester = false;






	//boolean reqsValid; // The set of requirements entailed by all majors is up to date


	public static Schedule testSchedule(){
		//	CourseList l = CourseList.testList();
		Schedule result = new Schedule();
		result.readBlankPrior();
		return result;
	}


	/**
	 * Make a new schedule of semesters where firstSemester is the first shown semester 
	 * and currentSemester is the first semester that might be scheduled
	 * (assume that earlier semesters have passed and already have their courses fixed.)
	 * @param masterList
	 * @param firstYear
	 * @param currentSemester
	 */
	public Schedule(){
		//Majors and requirements
		this.majorsList= new ArrayList<Major>();
		this.prereqs = new HashSet<Prereq>();
		//this.masterList = masterList;

		String past = FileHandler.getSavedStudentData();
		if(past != null){
			readPrior(past);
		}
		else{
			readBlankPrior(); //loads prior courses, recalc firstSemester and sets as currentSemester as well,
			// and create default semesters.
		}
		this.recalcGERMajor();
	}

	public Schedule(String priorCourses){
		this.majorsList= new ArrayList<Major>();
		this.prereqs = new HashSet<Prereq>();
		readPrior(priorCourses);
		recalcGERMajor();
	}


	public void setDriver(ScheduleGUI d){
		this.d = d;
	}


	/**
	 * May wipe all scheduleElements from the schedule!!!!
	 * @param firstSemester
	 */
	public void setFirstSemester(SemesterDate firstSemester){
		if(firstSemester.compareTo(this.firstSemester) == 0){
			return;
		}
		//Semesters
		this.semesters = new ArrayList<Semester>();
		//Set prior semester 
		priorSemester = new Semester (firstSemester.previous(), this);
		priorSemester.isAP = true;
		SemesterDate s = firstSemester;
		for(int i = 0; i < 8 ; i ++){
			this.semesters.add(new Semester(s, this));
			s = s.next();
		}
		this.firstSemester = firstSemester;
	}

	public void readBlankPrior(){
		if(defaultFirstSemester == null){
			defaultFirstSemester = Driver.tryPickStartDate();
		}
		setFirstSemester(defaultFirstSemester);
		setCurrentSemester(defaultFirstSemester);
	}

	public void readTestPrior(){
		readBlankPrior();

		this.setLanguagePrefix(new Prefix("SPN", "115"));

		//Class One 
		Course a = new Course(new Prefix("THA", 101), new SemesterDate(2016, SemesterDate.FALL), null, null, 4, "03");
		ScheduleCourse aa = new ScheduleCourse(a, this);
		this.addScheduleElement(aa,this.semesters.get(0));


		//Class Two
		Course b = new Course(new Prefix("MTH", 120), new SemesterDate(2016, SemesterDate.FALL), null, null, 4, "01");

		ScheduleCourse bb = new ScheduleCourse(b,this);
		this.addScheduleElement(bb , this.semesters.get(0));



		//Class Three
		Course d = new Course(new Prefix("PSY", 111), new SemesterDate(2016, SemesterDate.FALL), null, null,  4, "03");
		ScheduleCourse dd = new ScheduleCourse(d, this);
		this.addScheduleElement(dd, this.semesters.get(0));
		//result.semesters.add(b);


		this.setCLP(10);
	}

	/**
	 * Read in the info from Furman's text string, copied from myFurman.
	 * Sets language prefix, first semester and creates prior semester,
	 * 
	 * 
	 * @param s
	 */
	public void readPrior(String s){
		skipOverrides = true;
		int index = s.indexOf("global awareness") + 17; // this is the last column from MyFurman.
		int endIndex = s.indexOf("Total Earned");

		String[] lines = s.substring(index, endIndex).split("\n");
		//every entry in lines should be in the form
		/*
		Course/Section and Title
		midterm
		Grade
		Credits
		CEUs
		Repeat
		term
		FY seminar
		core
		global awareness
		 */

		int row = 0;
		int numCols = 10;
		//find the earliest and latest dates.
		SemesterDate earliestDate = new SemesterDate(100000,1);
		SemesterDate latestDate = new SemesterDate(0, 1);
		for(; (row+1) * numCols < lines.length ; row ++){
			int startIndex = row * numCols;
			String termString = lines[startIndex + 6];
			SemesterDate takenDate = SemesterDate.readFromFurman(termString);
			if(takenDate != null){
				if(takenDate.compareTo(earliestDate) < 0){
					earliestDate = takenDate;
				}
				else if(takenDate.compareTo(latestDate) > 0){
					latestDate = takenDate;
				}
			}
		}
		currentSemester = latestDate;
		setFirstSemester(earliestDate);

		
		


		//Add each of the prior courses to the schedule
		row = 0;
		ArrayList<Course> priorCourses = new ArrayList<Course>();
		for(; (row+1) * numCols < lines.length ; row ++){

			//Collect relevant string data
			int startIndex = row * numCols;
			String courseString = lines[startIndex].trim();
			String creditsString = lines[startIndex + 3].trim();
			String termString = lines[startIndex + 6].trim();

			//Turn the strings into objects

			//Prefix, title, and section number
			String title = null;
			String section = null;
			int firstSpace = courseString.indexOf(" ");
			String prefixString = courseString.substring(0, firstSpace);
			Prefix p = Prefix.readFrom(prefixString);
			String numString = p.getNumber();
			if(numString.contains("PL")){
				String number = numString.substring(numString.indexOf(".") + 1);
				if(numString.compareTo("PL.110") > 0){
					Prefix prefixLP = new Prefix(p.getSubject(), number);//remove the  "PL."
					this.setLanguagePrefix(prefixLP); 
				}
			}
			int secondSpace = courseString.indexOf(" ", firstSpace + 1);
			if(secondSpace == firstSpace + 2){
				title = courseString.substring(secondSpace);
				section = courseString.substring(firstSpace + 1, secondSpace);
			}
			else{
				title = courseString.substring(firstSpace);
			}

			//credits
			System.out.println(title);
			int credits= CourseList.getCoursesCreditHours(p);
			//if(!" ".equals(creditsString)&&! "".equals(creditsString)&& !(creditsString==null)){
			//	System.out.println((int)(Double.parseDouble(creditsString)));
			//	credits = (int)(Double.parseDouble(creditsString));
			//}
			//Semester / term
			SemesterDate takenDate = SemesterDate.readFromFurman(termString);

			Course c = null;
			if(takenDate != null){
				c = new Course(p, takenDate, null, null, credits, section);
			}
			else{
				c = new Course(p, priorSemester.semesterDate, null, null, credits, section);
			}
			c.setName(title);
			priorCourses.add(c);
		}

		//Add the courses

		for(Course c : priorCourses){
			// Skip all the placement courses
			//if(c.getPrefix().getNumber().contains("PL")){
			//	continue;
			//}
			ScheduleCourse cc = new ScheduleCourse(c, this);
			this.addScheduleElement(cc, c.semester);
		}
		skipOverrides = false;
	}









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
	public Semester addNewSemester(){
		SemesterDate last = semesters.get(semesters.size() - 1).getDate();
		Semester next = new Semester(last.next(), this);
		this.semesters.add(next);
		return next;
	}

	public Semester addNewSemesterInsideSch(int year, int season) {
		SemesterDate inside = new SemesterDate(year, season);
		Semester toAdd = new Semester(inside, this);
		this.semesters.add(toAdd);
		Collections.sort(semesters);
		return toAdd;
	}

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


	/**
	 * In semester S, remove ScheduleElement e.
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

	public boolean addScheduleElement(ScheduleElement e, SemesterDate d){
		for(Semester s : semesters){
			if(s.semesterDate.compareTo(d) == 0){
				addScheduleElement(e, s);
				return true;
			}
		}
		if(priorSemester.semesterDate.compareTo(d) == 0){
			addScheduleElement(e, priorSemester);
		}
		return false;
	}
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
	 * @return
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


	public void addMajor(Major newMajor){
		majorsList.add(newMajor);
		if(!newMajor.name.equals("GER")){
			recalcGERMajor();
		}
		updateReqs();
		updateTotalCoursesNeeded();
	}


	public void removeMajor(Major major) {
		majorsList.remove(major);
		if(!major.name.equals("GER")){
			recalcGERMajor();
		}
		updatePrereqs();//courses might be removed?
		updateReqs();
		updateTotalCoursesNeeded();
	}

	public int determineGER(){
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
	//	General error checks for GUI events
	///////////////////////////////
	///////////////////////////////
	public boolean userOverride(ScheduleError s){
		if(skipOverrides){
			return true;
		}
		if(this.d != null){
			return(d.userOverrideError(s));
		}
		else{
			return true;
		}
	}



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

	public boolean checkErrorsWhenRemoving(ScheduleElement e, Semester s){
		return(this.checkPrerequsitesRemoving(e, s));
	}





















	///////////////////////////////
	///////////////////////////////
	//	Nice getters
	///////////////////////////////
	///////////////////////////////
	/**
	 * This does not include prior Semester, because that is not apart of this.semesters
	 * @return The first real (not prior Semester)
	 */
	public Semester getStartSemester(){
		//TODO is the sort necessary?
		Collections.sort(this.semesters);
		return this.semesters.get(0);
	}



	public Semester getPriorSemester() {
		return priorSemester;
	}


	
	
	public SemesterDate getCurrentSemester() {
		return currentSemester;
	}


	public void setCurrentSemester(SemesterDate currentSemester) {
		this.currentSemester = currentSemester;
	}
	


	/**
	 * Find the list of all ScheduleElements in any semester of this Schedule.
	 * Will be sorted based on the time the element was scheduled.
	 * @return
	 */
	public ArrayList<ScheduleElement> getAllElementsSorted(){
		ArrayList<ScheduleElement> result = new ArrayList<ScheduleElement>();
		for(Semester s : this.getAllSemestersSorted()){
			result.addAll(s.getElements());
		}
		return result;
	}


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



	public void setLanguagePrefix(Prefix languagePrefix) {
		//The language classes prereq diagram looks like this:
		// arrows a --> b can be read as "a needs b"
		//  201 --> 120 --> 110
		// 					 
		//           115
		String[] Language = {"110", "120", "201"};
		this.languagePrefix = languagePrefix;
		recalcGERMajor();
		//Figure out the index of the given prefixe's number,
		// so if you were given 120 savedLocation = 1.
		int savedLocation = -1;
		for(int i=0; i<Language.length; i++){
			if(languagePrefix.getNumber().equals(Language[i])){
				savedLocation=i;
			}
		}
		//add all the things before it to your prior courses, so if you got
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
	}





	private void recalcGERMajor(){
		int type = this.determineGER();
		if(type == -1){
			type = CourseList.BA;
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






















	///////////////////////////////
	///////////////////////////////
	//	Prerequisite error checking
	///////////////////////////////
	///////////////////////////////

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
			if(this.prereqsCanBeSatisfiedInSameSemester){
				taken.addAll(inSemester);
			}
			for (ScheduleElement e : s.getElements()){
				inSemester.add(e);
				Requirement needed = CourseList.getPrereqsShallow(e.getPrefix());
				if(needed != null){
					boolean complete = needed.isComplete(taken, true);
					if(!complete){
						ScheduleError prereq = new ScheduleError(ScheduleError.preReqError);
						prereq.setOffendingCourse(e);
						prereq.setNeededCourses(needed);
						prereq.setOffendingSemester(s);
						result.add(prereq);
					}
				}
			}
			if(!this.prereqsCanBeSatisfiedInSameSemester){
				taken.addAll(inSemester);
			}
		}
		return result;
	}

	public boolean checkPrerequsitesAdding(ScheduleElement e, SemesterDate sD){
		Requirement needed = prereqsNeededFor(e.getPrefix(), sD);
		if(needed == null){
			return false; //no errors found
		}
		if(!needed.storedIsComplete()){

			ScheduleError preReq = new ScheduleError(ScheduleError.preReqError);
			preReq.setOffendingCourse(e);
			preReq.setNeededCourses(needed);
			//	preReq.setInstructions(e.getDisplayString() + " needs prerequisite(s)" + needed.toString());

			return(!this.userOverride(preReq));
			//throw new PrerequsiteException(needed, e);
		}
		return false;
	}

	/**
	 * Find the prereqs needed for this prefix if taken at the given
	 * time.
	 * @param p
	 * @param sD
	 * @return
	 */
	public Requirement prereqsNeededFor(Prefix p, SemesterDate sD){
		if(p == null){
			return null;
		}
		else{
			ArrayList<ScheduleElement> taken = elementsTakenBefore(sD);
			if(this.prereqsCanBeSatisfiedInSameSemester){
				taken.addAll(elementsTakenIn(sD));
			}
			Requirement needed = CourseList.getPrereqsShallow(p);
			if(needed != null){
				needed.updateAllStoredValues(taken);
			}
			return needed;
		}
	}

	public boolean checkPrerequsitesRemoving(ScheduleElement e, Semester s){
		//TODO fix this logic to use the sorted nature of getAllSemestersSorted
		// right now it's checking every semester when that's not necessary.
		Prefix currentP = e.getPrefix();
		for(Semester other : this.getAllSemestersSorted()){
			if(other.getDate().compareTo(s.getDate()) >= 0 ){
				for(ScheduleElement oElement : s.getElements()){
					Requirement needed = prereqsNeededFor(oElement.getPrefix(),other.semesterDate);
					if(needed == null){
						return false;
					}
					if(needed.isSatisfiedBy(currentP)){
						ScheduleError preReq = new ScheduleError(ScheduleError.preReqError);
						preReq.setOffendingCourse(e);
						preReq.setNeededCourses(needed);
						//		preReq.setInstructions(e.getDisplayString() + " needs prerequisit(s) " + needed.toString());
						return(!this.userOverride(preReq));
					}
				}
			}
		}
		return false;
	}

	/**
	 * If an error is found, return true.
	 * @param oldSem
	 * @param newSem
	 * @param oldE
	 * @param newE
	 * @return
	 */
	public boolean checkPrerequsitesReplacing(Semester oldSem, Semester newSem, 
			ScheduleElement oldE, ScheduleElement newE){
		//If the elements are equal, we're really moving the time we take it.
		// Check to see if anything happening before the new placement but
		// after the old placement now has an unfilled prereq.
		if(newE.equals(oldE)){
			//If we're moving the course backward in time 
			// we might no longer satisfy its prereqs.
			if(oldSem.semesterDate.compareTo(newSem.semesterDate) >= 1){
				Requirement stillNeeded = prereqsNeededFor(oldE.getPrefix(), newSem.semesterDate);

				if(stillNeeded != null && !stillNeeded.storedIsComplete()){
					ScheduleError preReq = new ScheduleError(ScheduleError.preReqError);
					preReq.setOffendingCourse(newE);
					preReq.setNeededCourses(stillNeeded);
					//	preReq.setInstructions(newE.toString() + " has prerequisite " + missing.toString());
					return((!this.userOverride(preReq)));
				}
				else{
					return false;
				}
			}
			//If we're moving the course forward in time. 
			//The only courses that need to be checked are those in between the new position and
			//the old position of the moving course, this is because any other course 
			//has already had an error thrown, and therefore has been checked by the user. 

			//prefixes between oldSem and newSem.
			// In the following code, new refers to the new location for the element, and 
			// to the semester who's date comes later. (2020 is new, 2018 is old).
			ArrayList<ScheduleElement> beforeNew = this.elementsTakenBefore(newSem.semesterDate);
			ArrayList<ScheduleElement> afterOld = this.elementsTakenAfter(oldSem.semesterDate);
			ArrayList<ScheduleElement> old = this.elementsTakenIn(oldSem);
			if(this.prereqsCanBeSatisfiedInSameSemester){
				afterOld.addAll(old);
			}
			afterOld.retainAll(beforeNew);
			ArrayList<ScheduleElement> intersection = afterOld;

			//for each element we're jumping over, check if
			// we satisfied one of that element's prereqs.
			HashSet<Prefix> elementsThatUsedTheMovingElement = new HashSet<Prefix>();
			for(ScheduleElement p : intersection){
				Requirement r = CourseList.getPrereqsShallow(p.getPrefix());
				if(r != null && r.isSatisfiedBy(newE)){
					elementsThatUsedTheMovingElement.add(p.getPrefix());
				}
			}
			if(!elementsThatUsedTheMovingElement.isEmpty()){
				ScheduleError preReq = new ScheduleError(ScheduleError.preReqError);
				preReq.setNeededCourses(new Requirement(elementsThatUsedTheMovingElement
						.toArray(new Prefix[elementsThatUsedTheMovingElement.size()]),1));
				preReq.setOffendingCourse(newE);
				return(!this.userOverride(preReq));
			}
			return false;

		}
		//If the element's aren't the same,
		// we're really just doing an
		// add and a remove.
		else{
			return (checkPrerequsitesRemoving(oldE, oldSem) ||checkPrerequsitesAdding(newE, newSem.semesterDate));
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



	/**
	 * Check all semesters for duplicates.
	 * If one is found, throw an exception.
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

	public boolean checkDuplicates(ScheduleElement e, boolean alreadyAdded, boolean isAll){
		int exactDuplicateCount = 1;
		if(alreadyAdded){
			exactDuplicateCount = 0;
		}

		for(ScheduleElement e1 : getAllElementsSorted()){
			if(e1 == e){
				exactDuplicateCount++;
				if(exactDuplicateCount > 1){
					ScheduleElement[] result = {e, e1};
					if(e1.isDuplicate(e) || e.isDuplicate(e1)){
						ScheduleElement[] results = {e, e1};
						ScheduleError duplicate = new ScheduleError(ScheduleError.duplicateError);
						//	duplicate.setInstructions(results[0].getDisplayString() + " duplicates " + results[1]	);
						duplicate.setElementList(results);
						if(isAll == false){
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
				duplicate.setElementList(result);
				//	duplicate.setInstructions(result[0].getDisplayString() + " duplicates " + result[1]	);
				if(isAll == false){
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
		if(r.storedIsComplete()){
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
		return (! d.userOverrideError(optimismError));

	}




	///////////////////////////////
	///////////////////////////////
	//	Course overlap error checking
	///////////////////////////////
	///////////////////////////////

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
		for(Requirement r : reqList){
			updateRequirement(r, reqList, allTakenElements);
		}

		updatePrereqs();
	}

	/**
	 * check if anything needs to be updated, and update if it does.
	 */
	public void checkUpdateReqs(){
		/*if(!reqsValid){*/
		updateReqs();
		//}
	}

	/**
	 * Count the number of currently placed schedule elements
	 * satisfying this requirement.
	 * @param r
	 */
	public void updateRequirement(Requirement r, ArrayList<Requirement> reqList, ArrayList<ScheduleElement> allTakenElements){
		//For each requirement, find all the schedule elements that satisfy it
		// (this prevents enemy requirements from both seeing the same course)s

		//Courses that don't have enemies, and exclude courses that do have enemies
		ArrayList<ScheduleElement> satisficers = new ArrayList<ScheduleElement>();

		for(ScheduleElement e : allTakenElements){
			if(e.getRequirementsFulfilled(reqList).contains(r)){
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
	 * Find all the requirements that this ScheduleElement satisfies.
	 * @param e
	 * @return
	 */
	public ArrayList<Requirement> getRequirementsSatisfied(ScheduleElement e){
		return e.getRequirementsFulfilled(getAllRequirements());
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


	/**
	 * doesn't update the requirements (this would cause an infinite loop, 
	 * updating a requirement means knowing all requirements, which can't happen unless
	 * you get all majors.)
	 * @return
	 */
	public ArrayList<Major> getMajors(){
		ArrayList<Major> result = new ArrayList<Major>();
		if(GER != null){
			result.add(GER);
		}
		if(prereqs.size() > 0){
			Major prereqsM = new Major("Prereqs");
			prereqsM.chosenDegree = -1;
			HashSet<Requirement> uniquePrereqs = new HashSet<Requirement>();
			for(Prereq p : prereqs){
				if(!p.getRequirement().storedIsComplete()){
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
			Requirement r = CourseList.getPrereqsShallow(e.getPrefix());
			if(r != null){
				Prereq newPrereq = new Prereq(r, e.getPrefix());
				prereqs.add(newPrereq);
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







	public ArrayList<Major> filterAlreadyChosenMajors(ArrayList<Major> collectionOfMajors ) {
		collectionOfMajors.removeAll(this.getMajors());
		return collectionOfMajors;

	}

	public ArrayList<ScheduleCourse> filterAlreadyChosenCourses(ArrayList<ScheduleCourse> collectionOfCourses){
		collectionOfCourses.removeAll(this.getAllElementsSorted());
		return collectionOfCourses;
	}


	public boolean SemesterAlreadyExists(SemesterDate semesterDate) {
		for(Semester s: this.semesters){
			if(s.semesterDate.equals(semesterDate)){
				return true;
			}
		}
		return false;
	}


	/**
	 * Find the set of requirements that this course should satisfy, given the
	 * list of requirement enemies that are trying to be fulfilled by this course.
	 * @param enemies
	 * @param c
	 * @return
	 */
	public HashSet<Requirement> resolveConflictingRequirements(HashSet<Requirement> enemies, Course c){
		HashSet<Requirement> result = new HashSet<Requirement>();
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
		if(this.d != null){
			//Ask the user to pick for us.
			return d.GUIResolveConflictingRequirements(reqs, majors, c);
		}
		else{
			//If we try to resolve conflicts on our own, this is where we would do it.
			return new HashSet<Requirement>();
		}
	}


	public  ArrayList<ScheduleError> checkAllErrors(){
		ArrayList<ScheduleError> allErrors = new ArrayList<ScheduleError>();
		//Don't check for errors in PriorSemester
		for(Semester s: this.semesters){
			if(s.checkAllOverlap()!=null){
				allErrors.addAll(s.checkAllOverlap());
			}
			if(s.checkOverload(true, null)==true){
				ScheduleError overload = new ScheduleError(ScheduleError.overloadError);
				overload.setOffendingSemester(s);
				allErrors.add(overload);	
			}
		}
		allErrors.addAll(this.checkAllPrerequsites());
		allErrors.addAll(this.checkAllDuplicates());
		return allErrors;
	}



	public ArrayList<ScheduleCourse> getCoursesInSemester(Semester s) {
		ArrayList<Course> toFinal = CourseList.getCoursesIn(s);
		ArrayList<ScheduleCourse> scheduleCoursesInSemester = new ArrayList<ScheduleCourse>();
		for(Course c: toFinal){
			ScheduleCourse sc = new ScheduleCourse(c, this);
			scheduleCoursesInSemester.add(sc);
		}
		return scheduleCoursesInSemester;
	}




	public String printScheduleString(){
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

		result.append("<center> <h1> Schedule </h1> </center> ");
		//Adds all the scheduleElements from each major
		for(Semester s: this.getAllSemestersSorted()){
			result.append("\n");
			if(s.isAP){
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
				}
				result.append(prefix + se.shortString(10000) + "\n");
			}
			if(s.elements.isEmpty()){
				result.append("Nothing scheduled for this semester \n");
			}
			if(s.hasNotes){
				result.append("<b> Notes: </b>" +  s.notes + "\n");
			}

		}
		result.append("\n");
		//If any Errors Prints them 
		if(!d.GUICheckAllErrors(false).equals("")){
			result.append("Scheduling Errors:"); // + d.GUICheckAllErrors(false));
		}
		//Things left CLPS, Estimated Courses Left, CrditHours
		result.append("\n <h2>The Final Countdown: </h2>");
		result.append("<b> CLPs Left: </b> " + Math.max(0, 32 - this.getCLP()) + "\n");
		result.append("<b> Estimated Courses Left: </b> " + Math.max(0, this.estimatedCoursesLeft()) + "\n");
		result.append("<b> Credit Hours Left:</b> " +  Math.max(0, (128 - this.getCreditHoursComplete())) + "\n");





		return result.toString().replaceAll("\n", "<br>");



	}



	public String printRequirementString(){
		SemesterDate defaultPrior = new SemesterDate(1995, SemesterDate.OTHER);
		ArrayList<ScheduleElement> allOrderedElements = new ArrayList<ScheduleElement>();
		ArrayList<SemesterDate> coorespondingDates = new ArrayList<SemesterDate>();
		for(Semester s: this.getAllSemestersSorted()){
			for(ScheduleElement se: s.elements){
				allOrderedElements.add(se);
				if(s.isAP){
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
			elementsSatisfy.put(e, new HashSet<Requirement>(e.getRequirementsFulfilled(this.getAllRequirements())));
		}
		for(Major m: this.getMajors()){
			result.append("\n");
			result.append("<b>" + m.name + "</b>");
			ArrayList<Requirement> sortedReq = new ArrayList<Requirement>(m.reqList);
			Collections.sort(sortedReq);
			for(Requirement r: sortedReq){
				String rDisplay = r.shortString(10000) + "-";
				if(rDisplay.length()<=30){
					String spaces = new String (new char[30-rDisplay.length()]).replace("\0", " ");
					rDisplay = rDisplay + spaces;

				}
				result.append("\n" + rDisplay);

				boolean isComplete = r.storedIsComplete();
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
					if(se instanceof Requirement){
						part.append(priorIndent + "Scheduled  " + se.shortString(10000));

					}
					else{
						part.append(priorIndent + se.shortString(100000));

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


		return result.toString().replaceAll("\n", "<br>");

	}


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


	public int getCLP() {
		return CLP;
	}
	public void setCLP(int cLP) {
		CLP = cLP;
	}
	public Prefix getLanguagePrefix() {
		return languagePrefix;
	}





	public int getCreditHoursComplete(){
		int result = 0;
		for (Semester s : this.getAllSemestersSorted()){
			result = result + s.getCreditHours();
		}
		return result;
	}

	public SemesterDate getStartDate(){
		return this.getStartSemester().semesterDate;
	}
	public ArrayList<Semester> getSemesters(){
		return this.semesters;
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
			System.out.println(major.name +"  " + major.chosenDegree);
			Major refreshed = m.getMajor(major.name);
			refreshed.setChosenDegree(major.chosenDegree);
			newMajorsList.add(refreshed);


		}

		setMajorsList(newMajorsList);

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

	public void setMajorsList(ArrayList<Major> majors){
		this.majorsList= majors;
		this.recalcGERMajor();
		this.updatePrereqs();
		this.updateReqs();
		this.updateTotalCoursesNeeded();

	}



	//Collect all the elements before that semester date.


	public Major getGER() {
		return GER;
	}


	public void setGER(Major gER) {
		GER = gER;
	}

}

