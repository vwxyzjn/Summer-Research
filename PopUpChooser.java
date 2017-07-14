

import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;




/**
 * This class lets the user choose a Major from a list of majors.
 * 
 *
 */
public class PopUpChooser implements java.io.Serializable {
	JList <Major> addList;
	ScheduleGUI d;
	Schedule s;
	JFrame frame = new JFrame();
	ArrayList <Major> displayThings;
	String polite = "Please ";
	ImageIcon icon = new ImageIcon(MenuOptions.resourcesFolder + "dioglogIcon.gif");



	public PopUpChooser(String type, ScheduleGUI d, Schedule s){
		//Creates the PopUp Window
		frame = new JFrame(type);
		this.d=d;


		//Retrieves correct list to display on dialog box, and calls diaolog box method
		if(type.equals(MenuOptions.addMajor)){
			ListOfMajors majors = d.l; //Links to test Major
			ArrayList<Major> collectionOfMajors =  majors.getGUIMajors();
			displayThings = d.sch.filterAlreadyChosenMajors(collectionOfMajors);
			createDiaologBox(type);

		}

		if(type.equals(MenuOptions.addMinor)){
			ListOfMajors majors = d.l;//Links to test List
			ArrayList<Major> collectionOfMinors =  majors.getGUIMinor();
			displayThings = d.sch.filterAlreadyChosenMajors(collectionOfMinors);
			createDiaologBox(type);
		}

		if(type.equals(MenuOptions.addTrack)){
			ListOfMajors majors = d.l;
			ArrayList<Major> collectionOfTrack =  majors.getGUITrack();
			displayThings = d.sch.filterAlreadyChosenMajors(collectionOfTrack);
			createDiaologBox(type);

		}
	}



	/**
	 * Create the dialog box and get the user's choice of major.
	 * @param s
	 */
	public void createDiaologBox(String s){
		Major[] dialogList = new Major[displayThings.size()];

		for(int i=0; i<displayThings.size(); i++){
			dialogList[i]=displayThings.get(i);
		}
		Major m = (Major)JOptionPane.showInputDialog(frame, polite + s,  s, JOptionPane.PLAIN_MESSAGE, icon, dialogList, "cat" );
		if((m != null) && (m instanceof Major)){
			d.GUIAddMajor(m);
		}
	}
}




