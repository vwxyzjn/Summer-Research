import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class SemesterDate implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	public static final int SPRING = 1;
	public static final int FALL = 5;
	public static final int SUMMERONE = 3;
	public static final int SUMMERTWO = 4;
	public static final int MAYX = 2;
	public static final int OTHER = 6;

	int year;
	int sNumber; //one of FALL, SPRING, MAYX, SUMMER, OTHER

	public SemesterDate(int year, int semesterNumber){
		this.year = year;
		this.sNumber = semesterNumber;
	}
	
	public SemesterDate(String season, int year){
		this.year = year;
		this.sNumber = toSNumber(season);
	}

	public int getYear(){
		return year;
	}

	public String saveString(){
		return this.year + "-" + this.sNumber;
	}
	public static SemesterDate readFrom(String saveString){
		String[] parsed = saveString.split("-");
		return new SemesterDate(Integer.parseInt(parsed[0]), Integer.parseInt(parsed[1]));
	}
	/**
	 * 
	 * Reads from strings of the form 2017D1
	 * these strings come from an import of prior courses from myFurman
	 */
	public static int[] fromDNumToSeason = {FALL, SPRING, MAYX, SUMMERONE, SUMMERTWO};
	public static SemesterDate readFromFurman(String s){
		s=s.trim();
		if(s == null || s.length() < 4){
			return null;
		}
		int year = Integer.parseInt(s.substring(0, 4));
		int dNumber = Integer.parseInt(s.substring(5));
		//d1 = spring of the previous year
		//d2 = fall
		//d3 = mayX
		//d4 = 
		if(dNumber == 1){
			year = year - 1;
		}
		int season = fromDNumToSeason[dNumber-  1];
		return new SemesterDate(year, season);
		
		
		
	}
	/**
	 * Read from strings of the form Fall 2017 - Day
	 * these strings come from courselist columns
	 * @param semesterString
	 * @return
	 */
	public static SemesterDate fromFurman(String semesterString){
		//Examples:
		// May Experience 2017 - Day
		// Fall 2017 - Day
		Matcher m = Pattern.compile("\\d+").matcher(semesterString);
		if(!m.find()){
			throw new RuntimeException("Can't make a semester from the string" + semesterString);
		}
		String yearString = m.group();
		String semesterName = semesterString.substring(0, m.start());
		
		return new SemesterDate(Integer.parseInt(yearString), toSNumber(semesterName));
	}

	public int getStartMonth(){
		switch(sNumber){
		case FALL:
			return 8;
		case SPRING:
			return 1;
		case SUMMERONE:
			return 6;
		case SUMMERTWO:
			return 7;
		case MAYX:
			return 5;
		default:
			return 0;
		}
	}

	/**
	 * Return the next school semester, either in fall or spring.
	 *  
	 * @return
	 */
	public SemesterDate next(){
		if(this.sNumber == SemesterDate.FALL){
			return new SemesterDate(this.year + 1, SemesterDate.SPRING);
		}
		//Automatically assumes that next is spring or fall, not summer, if you're already
		// a spring/fall semester. TODO not good style.
		if(this.sNumber == SemesterDate.SPRING){
			return new SemesterDate(this.year, SemesterDate.FALL);
		}
		else{
			return new SemesterDate(this.year,(this.sNumber + 1) );
		}
	}
	
	public SemesterDate previous(){
		if(this.sNumber == SPRING){
			return new SemesterDate(this.year - 1, SemesterDate.SUMMERTWO);
		}
		return new SemesterDate(this.year, this.sNumber - 1);
	}
	
	public String getUserString(){
		String result = getSeason(sNumber);
		if(result == null){
			result="Error";
		}
		result +=  " " + year;
		return result;
	}

	
	public static String getSeason(int p){
		String[] season = {null, "Spring", "MayX", "Summer Session One", "Summer Session Two", "Fall", "Other"};
		return season[p];
	}
	public static int toSNumber(String season){
		switch(season.toUpperCase().replaceAll(" ", "")){
		case "FALL":
			return FALL;
		case "SPRING":
			return SPRING;
		case "MAYX:":
		case "MAY-X":
		case "MAYEXPERIENCE":
			return MAYX;
		case "SUMMER":
			return SUMMERONE;
		default:
			return OTHER;
		}
	}

	//@Override
	public int compareTo(SemesterDate o) {
		if(o == null){
			return -1;
		}
		if(this.year < o.year){
			return - 1;
		}
		if(this.year > o.year){
			return  1;
		}

		if(this.sNumber < o.sNumber){
			return -1;
		}
		if(this.sNumber == o.sNumber && this.year == o.year){
			return 0;
		}
		else{
			return 1;
		}

	}
	

public String toString(){
	String result = "";
	result= result +this.getSeason(sNumber)+ " " + this.year;
	return result;
}
	
 @Override 
 public boolean equals(Object other){
	 if(!(other instanceof SemesterDate)){
		 return false;
	 }
	 SemesterDate o = (SemesterDate) other;
	 if(o.compareTo(this)==0){
		 return true;
	 }
	 else{
		 return false;
	 }
	 
	 
	 
 }
}
