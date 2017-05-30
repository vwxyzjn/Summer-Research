


import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;



public class ScheduleElementPanel extends JPanel {
	private int updateCount = 0;
	private ScheduleElement s;
	private SemesterPanel container;


	//	Driver coursesSatisfy = new Driver();
	JComboBox<ScheduleElement>  requirementDropDown;

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
		this.add(elementLabel);
		if(s instanceof Requirement) {
			updateDropDown();
		}
	}
	//If course is dropped then no dropDown Panel is needed




	/**
	 * This should only be called if the schedule element is a requirement.
	 */
	public void updateDropDown(){
		this. requirementDropDown = new JComboBox<ScheduleElement>();
		
		
		Requirement r = (Requirement)this.s;
		this.requirementDropDown.removeAllItems();
		
		//Find the list of courses that might satisfy this requirement
		ArrayList<Course> listOfCourses = container.getSemester().getCoursesSatisfying(r);
		for( Course c : listOfCourses){
			requirementDropDown.addItem(c);
		}
		requirementDropDown.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				dropdownSelected();
			}
		});
		this.add(requirementDropDown);
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










