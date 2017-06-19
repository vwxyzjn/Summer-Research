import java.awt.Dimension;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class bellTower extends JPanel{
	Schedule sch;
	JPanel overlap = new JPanel();
	ImageIcon icon = new ImageIcon("src/bellTower.jpg");
	ImageIcon firework = new ImageIcon("src/fireworks.jpg");
	JLabel belltowerLabel;
	JPanel fire = new JPanel();
	Image fireWork = firework.getImage();
	ImageIcon fires = new ImageIcon(fireWork);
	JLabel almost = new JLabel(fires);


	public bellTower(Schedule sch) {
		super();
		fire.add(almost);
		this.sch= sch;
		int givenHeight = 300;
		int givenWidth =100;
		//Belltower icon and scaling
		Image image = icon.getImage();
		Image newImage = image.getScaledInstance(givenWidth, givenHeight , java.awt.Image.SCALE_SMOOTH);
		icon = new ImageIcon(newImage);

		belltowerLabel = new JLabel(icon);
		belltowerLabel.add(overlap);

		this.add(belltowerLabel);
		this.setPreferredSize(new Dimension(100, 300));
		this.setBackground(FurmanOfficial.bouzarthGrey);
	}


	public void update(){


		int percentDone= sch.getPercentDone(icon.getIconHeight());
		System.out.println(percentDone);
		overlap.setSize(icon.getIconWidth(), percentDone);
		overlap.setLocation(0, icon.getIconHeight()-percentDone);
		overlap.setBackground(FurmanOfficial.bouzarthDarkWithAlpha(230));
		if(percentDone==icon.getIconHeight()-1){
			if(sch.checkAllErrors().isEmpty()){
				JOptionPane.showMessageDialog(sch.d.popUP, fire , "Congrats on having your life put together", JOptionPane.INFORMATION_MESSAGE);
			}
			else{
				sch.d.GUICheckAllErrors();
			}


		}
	}
}



