package research;
import java.awt.*;
import javax.swing.*;

public class Research_main extends JFrame {
	private static int FrameWidth;
	private static int FrameHeight;
	
	private static final int textPanelWidth = 300;
	private static final int textPanelHeight = 500;
	
	public Research_main() {
		this.setTitle("ピアノロール");
		this.setLayout(new GridLayout(1, 2));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Research_panel rPanel = new Research_panel();
		rPanel.init();
		rPanel.setBounds(0, 0, 1000, 500);
		
		// Create container
		Container contentPane = getContentPane();
		contentPane.setLayout(null);
		contentPane.add(rPanel);
		
		JTextArea JTArea= new JTextArea();
		JTArea.setEditable(false); // Set ReadOnly
		//contentPane.add(JTArea);
		
		/*JScrollPane sp = new JScrollPane(rPanel);
		contentPane.add(sp);*/
		
		System.out.println(JTArea.getWidth());
		//　内部のコンポーネントからサイズを決める
		getContentPane().setPreferredSize(new Dimension(1000, 500));
		pack();
	}

	public static void main(String[] args) {
		final  Research_main frame = new Research_main();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

}
