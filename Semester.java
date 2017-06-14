
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;


public class Semester implements Comparable<Semester>{
	public SemesterDate semesterDate;
	public ArrayList<ScheduleElement> elements;
	public Schedule schedule;
	private int OverloadLimit;


	public Semester(SemesterDate sD, Schedule s){
		elements = new ArrayList<ScheduleElement>();
		this.semesterDate = sD;
		this.schedule = s;
		if(this.semesterDate.sNumber == SemesterDate.FALL || this.semesterDate.sNumber == SemesterDate.SPRING){
			OverloadLimit = 20;
		}
		if(this.semesterDate.sNumber == SemesterDate.SUMMERONE|| this.semesterDate.sNumber == SemesterDate.SUMMERTWO){
			OverloadLimit = 8;
		}
		if(this.semesterDate.sNumber == SemesterDate.MAYX ){
			OverloadLimit = 2;
		}
	}

	public ArrayList<ScheduleElement> getElements(){
		return elements;
	}

	public SemesterDate getDate(){
		return semesterDate;
	}

	/**
	 * Check for any overlap in this semester with the new element.
	 * 
	 * true if overlap is found.
	 * 
	 * @param addition
	 */
	
	public boolean checkOverlap(ScheduleElement addition){
		//only courses can have overlap.
		
		if(addition == null || ( ! (addition instanceof Course))){
			return false;
		}
		ArrayList<Course> courses = allCourses();
		for(int i= 0; i<courses.size(); i++){
			if(courses.get(i).overlaps(addition)){
				ScheduleError overlaped = makeOverlapError(courses.get(i), (Course) addition);
				if(!this.schedule.userOverride(overlaped)){
					return true; //we found an overlap, and the user didn't override
				}
			}
		}
		return false;
	}
	
	/**
	 * Check all courses in this semester for overlap issues.
	 * 
	 * Return a list of all errors found, size 0 if none found.
	 * @return
	 */
	public ArrayList<ScheduleError> checkAllOverlap(){
		ArrayList<ScheduleError> overlaps = new ArrayList<ScheduleError>();
		ArrayList<Course> courses = allCourses();
		for (int i = 0; i < courses.size() ; i ++){
			for(int j = i+1; j < courses.size() ; j ++){
				if(courses.get(i).overlaps(courses.get(j))){
						ScheduleError overlapped = makeOverlapError(courses.get(i), courses.get(j));
						overlaps.add(overlapped);
				}
			}
		}
		return overlaps;
	}
	
	private ArrayList<Course> allCourses(){
		ArrayList<Course> courses = new ArrayList<Course>();
		for (ScheduleElement e : this.elements){
			if(e instanceof Course){
				courses.add((Course)e);
			}
		}
		return courses;
	}

	/**
	 * Given courses where c1.overlaps(c2) is true.
	 * @param c1
	 * @param c2
	 * @return
	 */
	private ScheduleError makeOverlapError(Course c1, Course c2){
		ScheduleError result = new ScheduleError(ScheduleError.overlapError);
		Course[] overlap = new Course[]{c1, c2};
		result.setElementList(overlap);
		
		//Figure out whether it's lab, meeting, or exam that overlaps.
		if(c1.examTime().overlaps(c2.examTime())){
			result.examOverlap = true;
		}
		if(c1.labTime().overlaps(c2.labTime())){
			result.labOverlap = true;
		}
		if(c1.meetingTimes().overlaps(c2.meetingTimes())){
			result.meetingOverlap = true;
		}
		return result;
	}




	public boolean checkOverload(ScheduleElement addition){

		int totalHours = 0;

		if(addition instanceof Course){
			Course toAdd = (Course) addition;
			totalHours= totalHours + toAdd.getCreditHours();
		}
		if(addition instanceof Requirement){
			Requirement toAdd = (Requirement) addition;
			totalHours = totalHours + toAdd.getCreditHours();
		}
		for(ScheduleElement e : this.elements){
			if(e instanceof Course){
				totalHours = totalHours + ((Course) e).getCreditHours();
			}
			if(e instanceof Requirement){
				totalHours = totalHours + ((Requirement) e).getCreditHours();
			}
		}
		if(totalHours > OverloadLimit){
			ScheduleError overload = new ScheduleError(ScheduleError.overloadError);
			overload.setOverloadLimit(this.OverloadLimit);
			overload.setOffendingCourse(addition);
		//	overload.setInstructions("Adding " + addition.getDisplayString() + " exceeds this semester's overload limit of " + this.OverloadLimit );
			return (!this.schedule.userOverride(overload));
		}
		else{
			return false;
		}
	}




	public int compareTo(Semester other){
		return this.semesterDate.compareTo(other.semesterDate);
	}


	/**
	 * Add a schedule element to this semester.
	 * This method should only be called by Schedule.
	 * @param e
	 * @return
	 */
	public boolean add(ScheduleElement e){
		if(!this.checkOverload(e)){
			this.elements.add(e);
			return true;
		}
		else{
			return false;
		}
	}






	public boolean remove(ScheduleElement e){
		//this.schedule.checkErrorsWhenRemoving(e, this);
		this.elements.remove(e);
		return true;
	}




	public boolean replace(ScheduleElement oldElement, ScheduleElement newElement){
		if(!this.checkOverload(newElement)){
			int i = this.elements.indexOf(oldElement);
			this.elements.set(i, newElement);
			return true;
		}
		else{
			return false;
		}

		//this.schedule.checkErrorsWhenAdding(newElement, this);
		//this.schedule.checkErrorsWhenRemoving(oldElement, this);

	}


	public ArrayList<Course> getCoursesSatisfying(Requirement r){
		ArrayList<Course> semesterCourses = this.schedule.masterList.getCoursesIn(this);
		return this.schedule.masterList.onlyThoseSatisfying(semesterCourses, r);
	}

	@Override
	public boolean equals(Object other){
		if(!(other instanceof Semester)){
			return false;
		}
		Semester o = (Semester) other;
		if(this.compareTo(o)==0){
			return true;
		}
		else{
			return false;
		}
	}


}
