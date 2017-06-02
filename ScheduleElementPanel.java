


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;



public class ScheduleElementPanel extends JPanel {
	private int updateCount = 0;
	private ScheduleElement s;
	private SemesterPanel container;
	private Dimension buttonSize = new Dimension(20, 20);
	private String removeButtonText = "x";


	//	Driver coursesSatisfy = new Driver();
	JComboBox<ScheduleElement>  requirementDropDown;
	JButton addCourse = new JButton (MenuOptions.addCourseWithRequirement);




	public ScheduleElementPanel(ScheduleElement s, SemesterPanel container) {

		super();
		this.s=s;
		this.container = container;





		this.setTransferHandler(new SEPDragHandler());
		this.addMouseListener(ComponentDragHandler.getDragListener());





	}

	public ScheduleElement getElement(){
		return s;
	}

	/**
	 * This method should only be called if the user selected a course 
	 * from the dropdown.
	 */
	public void dropdownSelected(){
		ScheduleElement e = (ScheduleElement) this.requirementDropDown.getSelectedItem();
		container.d.GUIElementChanged(container, this, e);
	}



	public void updatePanel(){ //This can be taken out later
		JLabel elementLabel = new JLabel(s.getDisplayString());
		elementLabel.setFont(FurmanOfficial.getFont(12));
		this.add(elementLabel);
		if(s instanceof Requirement) {
			updateDropDown();
		}


		//Adds remove Button
		JPanel remove = new JPanel();
		JButton toRemove = new JButton(removeButtonText);
		toRemove.setForeground(FurmanOfficial.darkPurple);
		toRemove.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				removeSelf();
			}
		});
		toRemove.setPreferredSize(buttonSize);
		remove.add(toRemove);
		this.add(remove);
	}





	/**
	 * This should only be called if the schedule element is a requirement.
	 */
	public void updateDropDown(){

		if(container.getSemester().getCoursesSatisfying((Requirement)s).size()>0){
			addCourse.setActionCommand(MenuOptions.addCourseWithRequirement);
			addCourse.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					container.d.GUIAddCourseWithRequirement(s, container, e.getActionCommand());
				}
			});
			this.add(addCourse);
		}	
	}

	public void removeSelf(){
		container.removeElement(this);
	}

	public class SEPDragHandler extends ComponentDragHandler{

		@Override
		public void initiateDrag(JComponent toBeDragged) {

		}

		@Override
		public void afterDrop(Container source, JComponent dragged,
				boolean moveAction) {
			container.removeElement((ScheduleElementPanel) dragged);
			//container.d.reqs.update();
			//container.d.reqs.revalidate();
			//	container.d.reqs.repaint();

		}

	}



}










