import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;



public class Major {
	String name;
	int groupNumber;
	ArrayList<Requirement> reqList;
	ArrayList<Integer> degreeTypes = new ArrayList<Integer>();
	
	public int chosenDegree;
	


	public static final String NORMAL_MAJOR = "Major";
	public static final String MINOR = "Minor";
	public static final String TRACK = "Track";
	
	String majorType;

	public static final int MajorDDNRange = 100;

	public Major(String name){
		this.name = name;
		this.majorType = NORMAL_MAJOR;
		this.reqList = new ArrayList<Requirement>();


	
	}

	/**
	 * Sets the type of this major.
	 * Should be one of NORMAL_MAJOR, MINOR, or TRACK. 
	 * @param majorType
	 */
	public void setType(String majorType){
		this.majorType = majorType;
	}

	public boolean isType(String majorType){
		return this.majorType.equals(majorType);
	}

	public void addDegreeType(String degreeType){
		if(degreeType.equals("BM")){
			this.degreeTypes.add(CourseList.BM);
		}
		if(degreeType.equals("BS")){
			this.degreeTypes.add(CourseList.BS);
		}
		if(degreeType.equals("BA")){
			this.degreeTypes.add(CourseList.BA);
		}

	}

	
	
	public int getChosenDegree() {
		return chosenDegree;
	}

	public void setChosenDegree(int chosenDegree) {
		this.chosenDegree = chosenDegree;
	}

	public void addRequirement(Requirement r){
		int dipNumber = r.doubleDipNumber;
		addRequirement(r, dipNumber%MajorDDNRange);
	}
	/**
	 * Add this requirement with the specified doubleDipNumber
	 * 
	 * This method will automatically attempt to prevent clashing between
	 * doubleDipNumbers of requirements in different majors.
	 * To help with this, it is best if doubleDipNumber is less than 100.
	 * However, doubleDipNumber of 0 is the default (and will be treated as if there is
	 * no double dip number.)
	 * @param r
	 * @param doubleDipNumber
	 */
	public void addRequirement(Requirement r, int doubleDipNumber){
		r.setDoubleDipNumber(groupNumber * MajorDDNRange + doubleDipNumber);
		this.reqList.add(r);
	}

	public static Major testMajor(){
		Major result = new Major("Math-BS");
		result.addRequirement(new Requirement(new Prefix[]{new Prefix("MTH", 250)}, 1));
		result.addRequirement(new Requirement(new Prefix[]{new Prefix("MTH", 260)}, 1));
		result.addRequirement(new Requirement(new Prefix[]{
				new Prefix("MTH", 350), new Prefix("MTH",450)}, 1));
		result.addRequirement(new Requirement(new Prefix[]{
				new Prefix("MTH", 360), new Prefix("MTH",460)}, 1));
		Requirement electives = new Requirement(new Prefix[]{
				new Prefix("MTH",151), 
				new Prefix("MTH",160),
				new Prefix("MTH",250),
				new Prefix("MTH",320),
				new Prefix("MTH",335),
				new Prefix("MTH",337),
				new Prefix("MTH",340),
				new Prefix("MTH",341),
				new Prefix("MTH",350),
				new Prefix("MTH",360),
				new Prefix("MTH",420),
				new Prefix("MTH",435),
				new Prefix("MTH",450),
				new Prefix("MTH",451),
				new Prefix("MTH",450),
				new Prefix("MTH",460),
				new Prefix("MTH",461),
				new Prefix("MTH",504)
		}, 		
				7);
		electives.setName("MTH Electives");
		result.addRequirement(electives, 2);
		return result;
	}

	
	

	static final String typeString = "Type: ";
	static final String degreeTypeString = "Possible Degree(s):";
	public String saveString(){
		StringBuilder result = new StringBuilder();
		result.append(name + "\n");
		result.append(typeString + this.majorType + "\n");
		for (Requirement r : this.reqList){
			result.append("REQ:" + r.saveAsJSON() + "\n");
		}
		return result.toString();
	}


	public static Major readFrom(String saveString){
		String[] lines = saveString.split("[\n]+");
		Major result = new Major(lines[0]);
		int startIndex = 1;
		if(lines[startIndex].contains(typeString)){
			result.setType(lines[startIndex].substring(typeString.length()));
			startIndex ++;	
		}
		if(lines[startIndex].contains(degreeTypeString)){
			String cat = lines[startIndex].substring(degreeTypeString.length());
			String noSpace = cat.replaceAll("\\s+","");
			String[] degreeType = noSpace.split(",");
			for(String s: degreeType){
				result.addDegreeType(s);
			}
			startIndex++;
		}


		for(int i = startIndex; i < lines.length ; i ++){
			if(lines[i].length() < 3){
				continue;
			}
			Requirement newRequirement = Requirement.readFromJSON(lines[i].substring(4));
			newRequirement.numFinished = 0;
			result.addRequirement(newRequirement);
		}
		return result;
	}


	public static Major readFrom(File f){

		Scanner scan;
		try {
			scan = new Scanner(f);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		scan.useDelimiter("\\Z");
		String contents = scan.next();
		scan.close();
		return readFrom(contents);
	}

	@Override
	public String toString(){
		return this.name;
	}

	@Override 
	public boolean equals(Object o){
		if(!(o instanceof Major)){
			return false;
		}
		Major other = (Major) o;
		if(this.name.equals(other.name)){
			return true;
		}
		else{
			return false;
		}

	}

	public static void main(String[] args){
		Major t = Major.testMajor();
		System.out.println(t.saveString());
		Major x = Major.readFrom(t.saveString());
		System.out.println(x.saveString());

		ListOfMajors m = ListOfMajors.testList();
		System.out.println(m.getCompleteMajorsList().get(0).saveString());
		System.out.println(m.getCompleteMajorsList().get(0).degreeTypes.toString());

	}




}
