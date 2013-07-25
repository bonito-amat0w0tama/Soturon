package research;
/*仕様変更・注意点
  0.1953125 = getWidth()/measureLength * 4 画面に描画するための比率,変数を使うとうまくいかない
  bassの音域はE1
 */

import java.io.File;
import java.math.BigDecimal;
import java.util.LinkedList;

import processing.core.*;

import jp.crestmuse.cmx.amusaj.sp.MidiEventWithTicktime;
import jp.crestmuse.cmx.amusaj.sp.MidiInputModule;
import jp.crestmuse.cmx.amusaj.sp.MidiOutputModule;
import jp.crestmuse.cmx.processing.CMXController;

public class Research_panel extends PApplet {
	CMXController cmx = CMXController.getInstance();
	Struct st = new Struct();
	Sequence_manager sm = new Sequence_manager(480, 100, 50, st, cmx);
	OutputBassNote obn = new OutputBassNote(sm, st, cmx);
	int numberOfkenban = 73; // 鍵盤の数
	int resolutionOfMeasure = 8; // 小節の分解能
	float magnification; // ノートを描画する際の倍率
	int nowMeasure = -1;
	int changedRiff = -1;
	boolean deleteFlag = false;
	boolean stopFlag = true;
	String changeMode = null;
	int[] changingTiming = { 1, 9, 17, 25, 33, 41, 50 };

	public void setup() {
		// モジュールの準備
		cmx.showMidiInChooser(this);
		MidiInputModule mi = cmx.createMidiIn();
		// MidiInputModule vk = cmx.createVirtualKeyboard(this);
		cmx.showMidiOutChooser(this);
		MidiOutputModule mo = cmx.createMidiOut();
		PrintModule pm = new PrintModule(sm, cmx);

		cmx.addSPModule(mi);
		cmx.addSPModule(mo);
		cmx.addSPModule(pm);
		cmx.addSPModule(obn);

		cmx.connect(mi, 0, obn, 0);
		cmx.connect(obn, 0, pm, 0);
		cmx.connect(pm, 0, mo, 0);
		cmx.startSP();

		sm.dmetronome();
		sm.play();
		mi.setTickTimer(sm);

		size(1000, 500);
		frameRate(60);

	}

	public void draw() {

		background(255);
		magnification = (float) getWidth() / (float) (st.getMeasureLength() * 4 + st.getMeasureLength() / 8);
		drawingPianoRoll();
		drawingNote();
		drawingNowPosition();

		nowMeasure = (int) cmx.getTickPosition() / 1920;

		for (int i = 0; i < changingTiming.length; i++) {
			if (nowMeasure == changingTiming[i] && deleteFlag == false) {
				changeMode = "delete";
				deleteFlag = true;
			}
		}

		if (nowMeasure == changedRiff + 4) {
			changeMode = "create";
			deleteFlag = false;
		}
		changeTheRiff(changeMode);

	}

	public void mousePressed() {
		System.out.println("createBassLine");
		obn.checkRoute();
		obn.createBassLine();

		System.out.println(st.getRiffList1().size());
		System.out.println(st.getRiffList2().size());
		System.out.println(st.getRiffList3().size());
		System.out.println(st.getRiffList4().size());

		/*
		 * for (int i = 0; i < st.getRiffList1().size(); i++) { byte[] data = st.getRiffList1().get(i).getMessageInByteArray(); System.out.println("1 " + data[0] + " " + data[1] + " " + data[2] + " "
		 * + st.getRiffList1().get(i).music_position); } for (int i = 0; i < st.getRiffList2().size(); i++) { byte[] data = st.getRiffList1().get(i).getMessageInByteArray();
		 * 
		 * System.out.println("2 " + data[0] + " " + data[1] + " " + data[2] + " " + st.getRiffList2().get(i).music_position); } for (int i = 0; i < st.getRiffList3().size(); i++) { byte[] data =
		 * st.getRiffList1().get(i).getMessageInByteArray(); System.out.println("3 " + data[0] + " " + data[1] + " " + data[2] + " " + st.getRiffList3().get(i).music_position); } for (int i = 0; i <
		 * st.getRiffList4().size(); i++) { byte[] data = st.getRiffList1().get(i).getMessageInByteArray(); System.out.println("4 " + data[0] + " " + data[1] + " " + data[2] + " " +
		 * st.getRiffList4().get(i).music_position); }
		 */

		int count = 1;
		System.out.println("note");
		for (int i = 0; i < st.getFirstNoteList().size(); i++) {
			byte[] data = st.getFirstNoteList().get(i).getMessageInByteArray();
			System.out.println(data[0] + " " + data[1] + " " + data[2] + " " + st.getFirstNoteList().get(i).music_position);

		}
		System.out.println("bass");
		for (int i = 0; i < st.getBassNoteList().size(); i++) {
			byte[] data = st.getBassNoteList().get(i).getMessageInByteArray();
			System.out.println(data[0] + " " + data[1] + " " + data[2] + " " + st.getBassNoteList().get(i).music_position);

		}
		sm.newCreateBassTrack();

	}

	public void keyPressed() {

		/*
		 * if (key == ' ') { System.out.println("deleated_NoteList"); st.getFirstNoteList().clear(); } if (key == 'v') { System.out.println("deleated_BassNoteList"); sm.deleateBass(); }
		 */

		if (key == ' ' && stopFlag == false) {
			cmx.stopMusic();
			stopFlag = true;
			System.out.println(cmx.getTickPosition());
		}
		else if (key == ' ' && stopFlag == true) {
			cmx.playMusic();
			stopFlag = false;
			System.out.println(cmx.getTickPosition());
		}
		if (key == CODED) {
			if (keyCode == UP) {
				System.out.println("deleated_FirstNoteList");
				st.getFirstNoteList().clear();
				st.getRevisedFirstNoteList().clear();
			}
			if (keyCode == DOWN) {
				System.out.println("deleated_BassNoteList");
				sm.deleteBass();
			}
			if (keyCode == LEFT) {
				System.out.println("revised_FistNoteList");
				st.getRevisedFirstNoteList().clear();
				st.getRevisedFirstNoteList().addAll(obn.reviseFirstNoteList());
			}
		}
	}

	void drawingPianoRoll() {
		float HDivNk = (float) getHeight() / (float) numberOfkenban;
		float WDivRB = (float) getWidth() / (float) (resolutionOfMeasure * 4 + 1);
		float w = ((float) numberOfkenban / 12.0F) * 7.0F; // 白鍵の時の縦
		int countOfc = 7;
		int countOfn = 109;

		// 鍵盤の描画
		// 白鍵
		for (int i = 0; i <= numberOfkenban; i++) {
			stroke(0);
			fill(255);
			line(0, i * HDivNk, WDivRB, i * HDivNk);
		}
		// 黒鍵
		for (int i = 0; i <= numberOfkenban; i++) {
			if (i % 12 == 11 || i % 12 == 9 || i % 12 == 6 || i % 12 == 4 || i % 12 == 2) {
				stroke(0);
				fill(0);
				rect(0, i * HDivNk, WDivRB / 2, HDivNk);
			}
		}

		for (int i = 0; i <= numberOfkenban; i++) {
			// 1オクターブごとのライン
			if (i % 12 == 1) {
				stroke(105, 175, 242);
				strokeWeight(2);
				line(0, i * HDivNk, getWidth(), i * HDivNk);
				strokeWeight(0);
				fill(255);

				fill(150);
				textAlign(RIGHT);
				text("C" + countOfc + " " + countOfn, WDivRB, i * HDivNk);
				countOfc--;
			}
			else if (i % 12 == 11 || i % 12 == 9 || i % 12 == 6 || i % 12 == 4 || i % 12 == 2) {
				stroke(200);
				fill(200);
				rect(WDivRB, i * HDivNk, getWidth(), HDivNk);

				fill(150);
				textAlign(RIGHT);
				text(countOfn, WDivRB, i * HDivNk);
			}
			else {
				stroke(200);
				line(WDivRB, i * HDivNk, getWidth(), i * HDivNk);

				fill(150);
				textAlign(RIGHT);
				text(countOfn, WDivRB, i * HDivNk);
			}
			countOfn--;
		}

		// 小節線の描画
		stroke(0);
		line(WDivRB, 0, WDivRB, getHeight());
		for (int i = 0; i <= resolutionOfMeasure * 4 + 1; i++) {

			if (i % resolutionOfMeasure == 0) {
				stroke(105, 175, 242);
				strokeWeight(2);
				line(i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, 0, i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, getHeight()); // i * WDivRBではダメ i * (getWidth() / (resolution
																																						// * 4)でもダメ
				strokeWeight(0);
			}
			else {
				stroke(150);
				line(i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, 0, i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, getHeight());
			}

		}
	}

	void drawingNote() {

		try {
			// ベースノートの描画 47音
			// 配列化
			// MidiEventWithTicktime[] nl =
			// (MidiEventWithTicktime[])st.getNoteList().toArray(new
			// MidiEventWithTicktime[0]);
			float HDivNk = (float) getHeight() / (float) numberOfkenban;
			float WDivRB = (float) getWidth() / (float) (resolutionOfMeasure * 4 + 1);
			float length = -1;
			float rlength = -1;
			float blength = -1;

			for (int i = 0; i < st.getFirstNoteList().size(); i++) {
				byte[] data = st.getFirstNoteList().get(i).getMessageInByteArray();
				float note = (float) getHeight() - ((data[1] - 23) * HDivNk);
				if (data[2] > 0) {
					st.getNotePrint().put(data[1], (float) st.getFirstNoteList().get(i).music_position);
				}
				else {
					if (st.getNotePrint().get(data[1]) == null) {
						length = 120.0F;
					}
					else {
						length = (float) st.getFirstNoteList().get(i).music_position - st.getNotePrint().get(data[1]);
					}
					fill(105, 175, 242);
					stroke(200);
					rect(((st.getNotePrint().get(data[1]) * magnification) + WDivRB), note, (length * magnification), HDivNk);
					st.getNotePrint().remove(data[1]);

				}
			}

			for (int i = 0; i < st.getRevisedFirstNoteList().size(); i++) {
				byte[] data = st.getRevisedFirstNoteList().get(i).getMessageInByteArray();
				float rnote = (float) getHeight() - ((data[1] - 23) * HDivNk);
				if (data[2] > 0) {
					st.getRevisedNotePrint().put(data[1], (float) st.getRevisedFirstNoteList().get(i).music_position);
				}
				else {
					rlength = (float) st.getRevisedFirstNoteList().get(i).music_position - st.getRevisedNotePrint().get(data[1]);
					fill(101, 247, 72);
					stroke(200);
					rect(((st.getRevisedNotePrint().get(data[1]) * magnification) + WDivRB), rnote, (rlength * magnification), HDivNk);
					st.getRevisedNotePrint().remove(data[1]);

				}
			}

			for (int i = 0; i < st.getBassNoteList().size(); i++) {
				byte[] data = st.getBassNoteList().get(i).getMessageInByteArray();
				float bnote = (float) getHeight() - ((data[1] - 23) * HDivNk);

				if (data[2] > 0) {
					st.getBassNotePrint().put(data[1], (float) st.getBassNoteList().get(i).music_position);
				}
				else {
					blength = (float) st.getBassNoteList().get(i).music_position - st.getBassNotePrint().get(data[1]);
					fill(255, 175, 242);
					stroke(200);
					rect(((st.getBassNotePrint().get(data[1]) * magnification) + WDivRB), bnote, (blength * magnification), HDivNk);
					st.getBassNotePrint().remove(data[1]);
				}
			}
		}
		catch (Exception e) {
			
			System.out.println("drawig_note_error");
			e.printStackTrace();
		}
	}

	void drawingNowPosition() {
		// 現在の位置
		float WDivRB = (float) getWidth() / (float) (resolutionOfMeasure * 4 + 1);
		stroke(211, 75, 75);
		float position = (float) (((cmx.getTickPosition() - st.getMeasureLength()) % (st.getMeasureLength() * 4)) * magnification) + WDivRB; // "-st.getMeasureLength()"はカウント分マイナス
		line(position, 0, position, getHeight());
	}

	void changeTheRiff(String mode) {
		try {
		if (mode == "delete") {
			System.out.println("Delete_the_riff");
			st.getFirstNoteList().clear();
			sm.deleteBass();
			st.getRevisedFirstNoteList().clear();
			changeMode = null;
			changedRiff = nowMeasure;

			LinkedList<MidiEventWithTicktime> tmp = sm.pickoutMidiMessegeOfSmf(new File("../midis/demoGuitar.mid"), 2, nowMeasure - 1);
			st.getFirstNoteList().addAll(tmp);
		}

		else if (mode == "create") {
			System.out.println("Create_the_bass");
			st.getRevisedFirstNoteList().addAll(obn.reviseFirstNoteList());
			obn.checkRoute();
			obn.createBassLine();
			sm.newCreateBassTrack();
			changeMode = null;
			changedRiff = -1;
		}

		else {
			// Do nothing
		}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
