package research;

import java.awt.*;
import javax.swing.*;

public class Research_main extends JFrame {
	private static int FrameWidth = 1000;
	private static int FrameHeight = 500;

	private static final int textPanelWidth = 300;
	private static final int textPanelHeight = 500;

	public Research_main() {
		this.init();
		this.processingPanelInit();

		// 　内部のコンポーネントからサイズを決める
		this.getContentPane().setPreferredSize(new Dimension(FrameWidth, FrameHeight));
		pack();
	}
	private void init() {
		this.setTitle("リサーチ");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	private void processingPanelInit() {
		Research_panel RPanel = new Research_panel();
		RPanel.init();
		
		Container contentPane = this.getContentPane();
		contentPane.add(RPanel);
	}
	
	public static void main(String[] args) {
		try {
			final Research_main frame = new Research_main();

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			
		}

		catch (Exception e) {

		}
	}

}
