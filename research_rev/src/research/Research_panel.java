package research;

/*仕様変更・注意点
 0.1953125 = getWidth()/measureLength * 4 画面に描画するための比率,変数を使うとうまくいかない
 bassの音域はE1(28),c1(24)
 */

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import processing.core.*;

import jp.crestmuse.cmx.amusaj.sp.MidiEventWithTicktime;
import jp.crestmuse.cmx.amusaj.sp.MidiInputModule;
import jp.crestmuse.cmx.amusaj.sp.MidiOutputModule;
import jp.crestmuse.cmx.processing.CMXController;

public final class Research_panel extends PApplet {
	private final CMXController cmx = CMXController.getInstance();
	private final Struct st = new Struct();
	private final Sequence_manager sm = new Sequence_manager(480, 100, 50, st, cmx);
	private final OutputBassNote obn = new OutputBassNote(sm, st, cmx);
	int numberOfkenban = 73; // 鍵盤の数
	int resolutionOfMeasure = 8; // 小節の分解能
	float magnification; // ノートを描画する際の倍率
	int changedRiff = -1;
	int nowMeasure = -1;
	private boolean deleteFlag = false;
	private boolean stopFlag = true;
	String changeMode = null;
	float WDivRB;
	float HDivNk;
	
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

		cmx.connect(mi, 0, pm, 0);
		cmx.connect(pm, 0, obn, 0);
		cmx.connect(obn, 0, mo, 0);
		cmx.startSP();

		sm.dmetronome();
		sm.play();
		mi.setTickTimer(sm);

		size(1000, 500);
		frameRate(60);

	}

	public void draw() {

		try {
			// width,heightを毎回更新
			WDivRB = (float) this.getWidth() / (float) (resolutionOfMeasure * 4 + 1);
			HDivNk = (float) this.getHeight() / (float) numberOfkenban;
			magnification = (float) this.getWidth() / (float) (st.getMeasureLength() * 4 + st.getMeasureLength() / 8); // 画面の大きさが変更されても平気なように毎回倍率を計算
			
			background(255);
			
			drawKeyboard();
			drawMeasureLine();
			
			drawSingleOrChord(OutputBassNote.singleOrChord);
			drawChordName(OutputBassNote.chordArrayForDraw);

			drawNoteList(OutputBassNote.firstNoteList, 105, 175, 242);
			drawNoteList(OutputBassNote.revisedFirstNoteList, 101, 247, 72);
			drawNoteList(OutputBassNote.bassNoteList, 255, 175, 242);
			//drawNoteList(OutputBassNote.bassRiffLists.get(0), 100 ,100 ,100);
			drawingNowPosition();

			nowMeasure = (int) cmx.getTickPosition() / 1920; // 現在の小節
			
			obn.newRiffPlay(nowMeasure, "0ff");
			//obn.riffPlay(nowMeasure);
			
			
			
		}
		catch (Exception e) {
			System.out.println("Draw Error");
			e.printStackTrace();
		}
	}

	
	

	public void keyPressed() {

		/*
		 * if (key == ' ') { System.out.println("deleated_NoteList"); st.getFirstNoteList().clear(); } if (key == 'v') { System.out.println("deleated_BassNoteList"); sm.deleateBass(); }
		 */

		if (key == ' ' && stopFlag == false) {
			cmx.stopMusic();
			stopFlag = true;
			System.out.println(cmx.getTickPosition());
			
			// obn.writeSMF();
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
				// sm.deleteBass();
			}
			if (keyCode == LEFT) {
				System.out.println("revised_FistNoteList");
				st.getRevisedFirstNoteList().clear();
				st.getRevisedFirstNoteList().addAll(obn.reviseFirstNoteList(OutputBassNote.firstNoteList));
			}
		}
	}

	void drawKeyboard() {
		int countOfc = 7;
		int countOfn = 97;

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
				textSize(10);
				textAlign(RIGHT);
				text("C" + countOfc + " " + countOfn, WDivRB, i * HDivNk);
				countOfc--;
			}
			else if (i % 12 == 11 || i % 12 == 9 || i % 12 == 6 || i % 12 == 4 || i % 12 == 2) {
				stroke(200);
				fill(200);
				rect(WDivRB, i * HDivNk, getWidth(), HDivNk);

				fill(150);
				textSize(10);
				textAlign(RIGHT);
				text(countOfn, WDivRB, i * HDivNk);
			}
			else {
				stroke(200);
				line(WDivRB, i * HDivNk, getWidth(), i * HDivNk);

				fill(150);
				textSize(10);
				textAlign(RIGHT);
				text(countOfn, WDivRB, i * HDivNk);
			}
			countOfn--;
		}
	}

	void drawMeasureLine() {
		// 小節線の描画
		stroke(0);
		line(WDivRB, 0, WDivRB, getHeight());
		for (int i = 0; i <= resolutionOfMeasure * 4 + 1; i++) {

			if (i % resolutionOfMeasure == 0) {
				stroke(105, 175, 242);
				strokeWeight(2);
				line(i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, 0, i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, getHeight()); // i * WDivRBではダメ i * (getWidth() / (resolution)
																																						// // * 4)でもダメ
				strokeWeight(0);
			}
			else if (i % (resolutionOfMeasure / 2) == 0) {
				stroke(200, 175, 242);
				strokeWeight(2);
				line(i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, 0, i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, getHeight()); // i * WDivRBではダメ i * (getWidth() / (resolution)
																																						// // * 4)でもダメ
				strokeWeight(0);
			}
			else {
				stroke(150);
				line(i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, 0, i * getWidth() / (resolutionOfMeasure * 4 + 1) + WDivRB, getHeight());
			}

		}
	}

	void drawNoteList(LinkedList<MidiEventWithTicktime> NoteList, int fill_1,
			int fill_2, int fill_3) {
		try {
			// ベースノートの描画 47音
			// 配列化
			HashMap<Byte, Float> tmpNoteOnForPrint = new HashMap<Byte, Float>(); // プリントの際のノートオンのハッシュマップ

			// Print the firstNoteList
			for (int i = 0; i < NoteList.size(); i++) {
				MidiEventWithTicktime nowMidievt = NoteList.get(i);
				byte[] data = nowMidievt.getMessageInByteArray();
				float note = (float) this.getHeight() - ((data[1] - 23) * HDivNk);

				if (data[2] > 0) {
					tmpNoteOnForPrint.put(data[1], (float) nowMidievt.music_position);
				}
				else {
					float length = -1;
					if (tmpNoteOnForPrint.get(data[1]) == null) {
						length = 120.0F * magnification;
					}
					else {
						length = ((float) nowMidievt.music_position - tmpNoteOnForPrint.get(data[1])) * magnification;
					}
					float position = (float) tmpNoteOnForPrint.get(data[1]) * magnification + WDivRB;
					fill(fill_1, fill_2, fill_3);
					stroke(200);
					rect(position, note, length, HDivNk);
					tmpNoteOnForPrint.remove(data[1]);
				}
			}
		}
		catch (Exception e) {

			System.out.println("drawig_note_error {");
			e.printStackTrace();
			System.out.println("}");
		}
	}

	void drawingNowPosition() {
		// 現在の位置
		stroke(211, 75, 75);
		float position = (float) (((cmx.getTickPosition() - st.getMeasureLength()) % (st.getMeasureLength() * 4)) * magnification) + WDivRB; // "-st.getMeasureLength()"はカウント分マイナス
		line(position, 0, position, getHeight());
	}
	
	void drawSingleOrChord(String[] singleOrChordArray) {
		textSize(30);
		textAlign(LEFT);
		fill(10, 10, 200);
		for (int i = 0; i < singleOrChordArray.length; i++) {
			text(singleOrChordArray[i], i * (WDivRB * 8) + WDivRB, 20); // 1小節ごとに表示
		}
	}

	void drawChordName(String[] chordName) {
		textSize(40);
		textAlign(LEFT);
		fill(200, 10, 10);
		for (int i = 0; i < chordName.length; i++) {
			text(chordName[i], i * (WDivRB * 4) + WDivRB, 60); // 1小節ごとに２つ表示
		}
	}
	
	
	
	void changeTheRiff(String mode) {
		try {
			if (mode == "delete") {
				System.out.println("Delete_the_riff");
				OutputBassNote.firstNoteList.clear();
				//sm.deleteBass();
				OutputBassNote.revisedFirstNoteList.clear();
				changeMode = null;
				changedRiff = nowMeasure;

				LinkedList<MidiEventWithTicktime> tmp = sm.pickoutMidiMessegeOfSmf(new File("/home/masaki/Dropbox/midis/demoGuitar.mid"), 2, nowMeasure - 1, nowMeasure + 3);
				OutputBassNote.firstNoteList.addAll(tmp);
			}

			else if (mode == "create" && OutputBassNote.firstNoteList != null && OutputBassNote.firstNoteList.size() > 0) {
				System.out.println("Create_the_bass");
				OutputBassNote.revisedFirstNoteList.addAll(obn.reviseFirstNoteList(OutputBassNote.firstNoteList));
				
				//sm.createBassTrack();
				changeMode = null;
				changedRiff = -1;
			}

			else {
				// Do nothing
			}
		}
		catch (Exception e) {
			System.out.println("Change The Riff Errror");
			e.printStackTrace();
		}
	}
}
