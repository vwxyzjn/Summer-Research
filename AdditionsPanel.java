import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;


public class AdditionsPanel extends JPanel implements ActionListener{
	/**
	 * This panel provides options for the high impact request the user might need. Those buttons that have explore link to 
	 * an outside web page, while the others contained within this program. 
	 * 
	 */

	public String AdditionsHeader = new String(MenuOptions.FurmanAdvantage);
	public String classAddition = new String(MenuOptions.MajorMinor);
	
	public int optionsNumber = 8; //Number of Buttons
	public int headerNumber = 2;  //Number of Headers (The Furman Advantage. Major/Minor)
	
	ScheduleGUI d;
	
	//Buttons As Seen in Panel
	private JButton ExploreStudyAwayButton;
	private JButton ExploreResearchButton;
	private JButton ExploreIntershipsButton;
	private JButton AddMayXButton;
	private JButton AddSummerClassButton;
	
	private JButton AddMinorButton;
	private JButton AddMajorButton;
	private JButton AddTrackButton;


	public AdditionsPanel( ScheduleGUI d){
		//Sets layout, and style of the Panel
		super();
		this.d=d;
		this.setLayout((new GridLayout(optionsNumber+headerNumber, 1, 3, 3)));
		this.setBackground(FurmanOfficial.bouzarthGrey);
		JLabel header = new JLabel(AdditionsHeader);
		header.setHorizontalAlignment(JLabel.CENTER);
		header.setFont(FurmanOfficial.smallHeaderFont);
		this.add(header);



		
		this.ExploreStudyAwayButton = this.addButton(MenuOptions.exploreStudyAway);
		ExploreStudyAwayButton.setActionCommand(MenuOptions.exploreStudyAway);

		this.ExploreResearchButton = this.addButton(MenuOptions.addResearch);
		ExploreResearchButton.setActionCommand(MenuOptions.addResearch);
		
		this.ExploreIntershipsButton =this.addButton(MenuOptions.exploreInternship);
		ExploreIntershipsButton.setActionCommand(MenuOptions.exploreInternship);
		
		this.AddMayXButton = this.addButton(MenuOptions.addMayX);
		AddMayXButton.setActionCommand(MenuOptions.addMayX);
		
		this.AddSummerClassButton = this.addButton(MenuOptions.addSummerClass);
		AddSummerClassButton.setActionCommand(MenuOptions.addSummerClass);


		//Major/Minor Heading 
		JLabel classAdditions = new JLabel(classAddition);
		classAdditions.setHorizontalAlignment(JLabel.CENTER);
		classAdditions.setFont(FurmanOfficial.smallHeaderFont);
		this.add(classAdditions);

	
		this.AddMinorButton = this.addButton(MenuOptions.addMinor);
		AddMinorButton.setActionCommand(MenuOptions.addMinor);
		
		this.AddMajorButton = this.addButton(MenuOptions.addMajor);
		AddMajorButton.setActionCommand(MenuOptions.addMajor);
		
		this.AddTrackButton =this.addButton(MenuOptions.addTrack);
		AddTrackButton.setActionCommand(MenuOptions.addTrack);

	}

	
	//This creates the buttons used in this panel
	public JButton addButton(String s){
		//Formats Button Panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.setOpaque(true);
		buttonPanel.setBackground(FurmanOfficial.bouzarthGrey);
	
		//Formats Button
		JButton button = new JButton(s);
		button.setFont(FurmanOfficial.normalFont);
		button.setForeground(Color.white);
		button.setHorizontalTextPosition(SwingConstants.LEFT);
		button.setBorderPainted(false);
		button.setBackground(FurmanOfficial.darkPurple);
		button.setPreferredSize(new Dimension(153, 20));
		button.setOpaque(false);
		
		
		button.addActionListener(this);
		buttonPanel.add(button);
		this.add(buttonPanel);
		return button;


	}


	@Override
	public void actionPerformed(ActionEvent e) {
		//For add Major, Minor, Track
		if(e.getActionCommand().equals(MenuOptions.addMajor) || e.getActionCommand().equals(MenuOptions.addMinor)|| e.getActionCommand().equals(MenuOptions.addTrack)){
			d.GUIPopUP(e.getActionCommand());

		}
		//Explore Buttons, goes to outside links
		if((e.getActionCommand().equals(MenuOptions.exploreInternship)) || (e.getActionCommand().equals(MenuOptions.addResearch))||(e.getActionCommand().equals(MenuOptions.exploreStudyAway))){

			d.GUIOutsideLink(e.getActionCommand());

		}
		if(e.getActionCommand().equals(MenuOptions.addSummerClass)){
			d.GUIChooseSummerSession();

		}
		if(e.getActionCommand().equals(MenuOptions.addMayX)){
			d.GUIYearsPopUP(e.getActionCommand());
		}


	}

}
