package research;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import jp.crestmuse.cmx.processing.*;
import jp.crestmuse.cmx.amusaj.sp.*;
import jp.crestmuse.cmx.filewrappers.SCC;
import jp.crestmuse.cmx.filewrappers.SCCDataSet;
import jp.crestmuse.cmx.filewrappers.SCCDataSet.Part;
import jp.crestmuse.cmx.filewrappers.SCCXMLWrapper;
import jp.crestmuse.cmx.sound.*;
import javax.sound.midi.*;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import processing.core.*;

/*
 * チャンネル０：入力のMIDIギター
 * チャンネル１：ベース
 * チャンネル２：記録されるギター
 */
public final class OutputBassNote extends SPModule {
	private static final HashMap<Byte, Long> gapList = new HashMap<Byte, Long>();// クオンタイズの際のノートのズレ
	private static Sequence_manager sm = null;
	private static Struct st = null;
	private static CMXController cmx;

	public static LinkedList<MidiEventWithTicktime> NoteList = new LinkedList<MidiEventWithTicktime>();// ノートリスト
	public static LinkedList<MidiEventWithTicktime> bassNoteList = new LinkedList<MidiEventWithTicktime>();// ベースノートリスト
	public static LinkedList<MidiEventWithTicktime> firstNoteList = new LinkedList<MidiEventWithTicktime>();// 最初の四小節のノートリスト
	public static LinkedList<MidiEventWithTicktime> revisedFirstNoteList = new LinkedList<MidiEventWithTicktime>(); // 修正したノートリスト
	public static LinkedList<MidiEventWithTicktime> mainBassRiff = new LinkedList<MidiEventWithTicktime>();

	public static String[] singleOrChord = { "", "", "", "" };
	public static String[] chordArrayForDraw = { "", "", "", "", "", "", "", "" };
	private static String stateOfBassPlay = "Input"; // ベースラインの入力か生成の状態 Input OR Create & play
	private static int inputMeasure = -1; // インプットが呼ばれた小節
	private static int createMeasure = -1; // ベースを作るべきタイミンぐ
	final HashMap<Byte, MidiEventWithTicktime> tmpNoteOnList = new HashMap<Byte, MidiEventWithTicktime>(); // ノートオンの一時保管場所

	boolean rec = false;
	boolean stateOfFoot = false;
	boolean stateOfMain = false;
	boolean stateOfFree = false;
	boolean inputAfterFoot = false;

	int smfNum = 0;

	// ベースのリフのリスト
	public final static LinkedList<LinkedList<MidiEventWithTicktime>> bassRiffLists = new LinkedList<LinkedList<MidiEventWithTicktime>>() {
		{
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
		}
	};

	// ギターのリフのリスト
	public final LinkedList<LinkedList<MidiEventWithTicktime>> riffLists = new LinkedList<LinkedList<MidiEventWithTicktime>>() {
		{
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
		}
	};

	public final LinkedList<LinkedList<MidiEventWithTicktime>> mainRiffList = new LinkedList<LinkedList<MidiEventWithTicktime>>() {
		{
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
		}
	};

	public final LinkedList<LinkedList<MidiEventWithTicktime>> mainBassRiffList = new LinkedList<LinkedList<MidiEventWithTicktime>>() {
		{
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
			add(new LinkedList<MidiEventWithTicktime>());
		}
	};

	// 　リフチェンジの際に入力される次のコード進行
	private LinkedList<String> nextChordProg = new LinkedList<String>();

	private List<LinkedList<String>> stockOfChordProg = new LinkedList<LinkedList<String>>();

	OutputBassNote(Sequence_manager psm, Struct pst, CMXController pcmx) {
		sm = psm;
		st = pst;
		cmx = pcmx;
	}

	public void execute(Object[] src, TimeSeriesCompatible[] dest)
			throws InterruptedException {
		try {
			MidiEventWithTicktime midievt = (MidiEventWithTicktime) src[0];
			byte[] data = midievt.getMessageInByteArray();

			// MIDIギターではノートオン、ノートオフとも-112でvelocityによって判断,さらに、常に変なMIDI信号
			if (data[0] == -112 && stateOfFree == false) {
				long quantizeMp = quantize(midievt, 16) - 1920; // カウント分マイナスして普通の状態に戻す
				MidiEventWithTicktime quantizeMidievt = new MidiEventWithTicktime(midievt.getMessage(), quantizeMp, quantizeMp);
				NoteList.add(quantizeMidievt);

				// If NoteOn
				if (data[2] > 0) {
					tmpNoteOnList.put(data[1], quantizeMidievt);
				}
				// NOTE_OFFでNoteListに加える
				else {
					ShortMessage onSmsg = new ShortMessage();
					ShortMessage offSmsg = new ShortMessage();
					MidiEventWithTicktime addOnMidievt = null;
					MidiEventWithTicktime addOffMidievt = null;

					// もしノートオンが見つからなくてノートオン、ノートオフがペアにならなかったら
					if (tmpNoteOnList.containsKey(data[1]) == false) {
						long length = 120L; // ノートオンが見つからない場合はlengthを120に設定
						long onMp = (quantizeMp - 120) % 7680; // ４小節ないに収める,
																// 長さを120に設定したのでその分マイナス
						long offMp = onMp + length;
						onSmsg.setMessage(ShortMessage.NOTE_ON, 2, data[1], 100); // Velocityが不明なのでとりあえず100に設定
						offSmsg.setMessage(ShortMessage.NOTE_OFF, 2, data[1], 0);
						addOnMidievt = new MidiEventWithTicktime(onSmsg, onMp, onMp);
						addOffMidievt = new MidiEventWithTicktime(offSmsg, offMp, offMp);
					}
					// ノートオンが見つかってペアで記録できる場合
					else if (tmpNoteOnList.containsKey(data[1]) == true) {
						MidiEventWithTicktime tmpOnMidievt = tmpNoteOnList.get(data[1]);
						byte[] tmpOnData = tmpOnMidievt.getMessageInByteArray();
						long length = quantizeMp - tmpOnMidievt.music_position;
						// ありえないと思うが長さが０の時
						if (length < 0) {
							length = 0;
						}
						long onMp = tmpOnMidievt.music_position % 7680;
						long offMp = onMp + length;
						onSmsg.setMessage(ShortMessage.NOTE_ON, 2, tmpOnData[1], tmpOnData[2]);
						offSmsg.setMessage(ShortMessage.NOTE_OFF, 2, tmpOnData[1], 0);
						addOnMidievt = new MidiEventWithTicktime(onSmsg, onMp, onMp);
						addOffMidievt = new MidiEventWithTicktime(offSmsg, offMp, offMp);
					}
					tmpNoteOnList.remove(data[1]);
					this.firstNoteList.add(addOnMidievt);
					this.firstNoteList.add(addOffMidievt);
				}
				dest[0].add(midievt);

			}

			// チャンネル１６の足鍵盤の時
			else if (data[0] == -97 && data[2] > 0) {
				String chordName = getChordNameFromNoteNumeber(data[1]);

				// コードが4つ入力されていなければ
				if (nextChordProg.size() <= 8) {
					nextChordProg.add(chordName);
				}

				if (nextChordProg.size() == 8) {
					for (int i = 0; i < nextChordProg.size(); i++) {
						System.out.println(nextChordProg.get(i));
					}

					int nowMeasure = (int) cmx.getTickPosition() / 1920; // 現在の小節
					newRiffPlay(nowMeasure, "Input");

				}
				// dest[0].add(midievt);

			}

			else if (data[0] == -65 && data[2] > 0) {
				int nowMeasure = (int) cmx.getTickPosition() / 1920; // 現在の小節
				this.newRiffPlay(nowMeasure, "mainRiff");
			}

		}
		catch (Exception e) {
			System.out.println("outputBassNote Error");
			e.printStackTrace();
		}
	}

	public Class[] getInputClasses() {
		return new Class[] { MidiEventWithTicktime.class };
	}

	public Class[] getOutputClasses() {
		return new Class[] { MidiEventWithTicktime.class };
	}

	void playMainRiff() {

	}

	private String getChordNameFromNoteNumeber(int nn) {
		final String[] chordName = { "C", "C#", "D", "D#", "Em", "F", "F#", "G", "G#", "Am", "A#", "Bmb5" };
		Map<Integer, String> nameMap = new HashMap<Integer, String>();
		String dest = null;

		for (int i = 0; i < 12; i++) {
			nameMap.put(i, chordName[i]);
		}

		if (nameMap.containsKey(nn % 12)) {
			dest = nameMap.get(nn % 12);
		}
		else {
			dest = "none";
		}

		return dest;
	}

	public long quantize(MidiEventWithTicktime midievt, int qon) {
		try {
			int qont;// クオンタイズ大きさ
			long quantize_note_position;
			byte[] data = midievt.getMessageInByteArray();

			qont = qon / 4;

			// ノートオンの場合
			if (data[2] > 0) {
				long tick_gap = cmx.getTickPosition() % (sm.getTpb() / qont);
				long note_gap = -1;

				if (tick_gap <= (sm.getTpb() / qont) / 2) {
					note_gap = -(tick_gap);
				}
				else {
					note_gap = (sm.getTpb() / qont) - tick_gap;
				}
				gapList.put(data[1], note_gap);
				quantize_note_position = midievt.music_position + note_gap;
				return quantize_note_position;
			}

			// ノートオフの場合
			else {

				// If NoteOn is not exist
				if (gapList.get(data[1]) == null) {
					quantize_note_position = 0; // とりあえず
				}
				else {
					quantize_note_position = midievt.music_position + gapList.get(data[1]);
				}

				gapList.remove(data[1]); // キーの削除

				return quantize_note_position;
			}

		}

		catch (NullPointerException e) {
			System.out.println("Quntaize Error");
			e.printStackTrace();
			return 0;
		}

	}

	public LinkedList<MidiEventWithTicktime> reviseFirstNoteList(
			LinkedList<MidiEventWithTicktime> inputList) {
		try {
			LinkedList<MidiEventWithTicktime> destRevisedNote = new LinkedList<MidiEventWithTicktime>();
			HashMap<Byte, MidiEventWithTicktime> tmpOnNote = new HashMap<Byte, MidiEventWithTicktime>();

			for (int i = 0; i < inputList.size(); i++) {
				MidiEventWithTicktime midievt = inputList.get(i);
				byte[] data = midievt.getMessageInByteArray();

				if (data[2] > 0) {
					tmpOnNote.remove(data[1]);
					tmpOnNote.put(data[1], midievt);
				}
				else {
					MidiEventWithTicktime tmpMidievt = tmpOnNote.get(data[1]);
					byte[] tmpData = tmpMidievt.getMessageInByteArray();
					long onMp = tmpMidievt.music_position;
					long offMp = midievt.music_position;
					long length = offMp - onMp;

					// "length"カットする長さの閾値
					if (length > 40) {
						ShortMessage on = new ShortMessage();
						ShortMessage off = new ShortMessage();
						on.setMessage(ShortMessage.NOTE_ON, 0, data[1], tmpData[2]);
						off.setMessage(ShortMessage.NOTE_OFF, 0, data[1], 0);
						MidiEventWithTicktime onMidievt = new MidiEventWithTicktime(on, onMp, onMp);
						MidiEventWithTicktime offMidievt = new MidiEventWithTicktime(off, onMp + length, onMp + length);

						destRevisedNote.add(onMidievt);
						destRevisedNote.add(offMidievt);
					}
				}
			}
			return destRevisedNote;
		}
		catch (Exception e) {
			System.out.println("ReviseNote Error");
			e.printStackTrace();
			return null;
		}

	}

	// 単音リフのためのbass生成
	private LinkedList<MidiEventWithTicktime> createBassForSingleNote(
			LinkedList<MidiEventWithTicktime> singleRiffList) {
		try {
			// singleRiffList.clear();
			LinkedList<MidiEventWithTicktime> destRiffList = new LinkedList<MidiEventWithTicktime>();

			for (int i = 0; i < singleRiffList.size(); i++) {
				MidiEventWithTicktime midievt = singleRiffList.get(i);
				byte[] data = midievt.getMessageInByteArray();

				if (data[2] > 0) {
					ShortMessage onSmsg = new ShortMessage();
					int onNote = data[1] - checkBottomOfBass(singleRiffList);
					long onMp = midievt.getTick();
					onSmsg.setMessage(ShortMessage.NOTE_ON, 1, onNote, data[2]);
					MidiEventWithTicktime addOnMidievt = new MidiEventWithTicktime(onSmsg, onMp, onMp);
					destRiffList.add(addOnMidievt);
				}
				else {
					ShortMessage offSmsg = new ShortMessage();
					int offNote = data[1] - checkBottomOfBass(singleRiffList);
					long offMp = midievt.getTick();
					offSmsg.setMessage(ShortMessage.NOTE_OFF, 1, offNote, 0);
					MidiEventWithTicktime addOffMidievt = new MidiEventWithTicktime(offSmsg, offMp, offMp);
					destRiffList.add(addOffMidievt);
				}
			}
			return destRiffList;
		}
		catch (Exception e) {
			System.out.println("createBassForSingleNote_Error");
			e.printStackTrace();
			return null;
		}
	}

	public int getChord(long mp) {

		int[] ach = st.getArrayOfChord();
		int nowBar = (int) (mp / st.getMeasureLength());

		return st.getArrayOfChord()[(nowBar - 1)];

	}

	public void createBassLine() {
		try {
			LinkedList<MidiEventWithTicktime> bassNoteList = st.getBassNoteList();
			bassNoteList.clear();

			for (int i = 1; i <= 4; i++) {
				for (int j = 0; j < st.getNumberOfRiffList(i).size(); j++) {
					byte[] data = st.getNumberOfRiffList(i).get(j).getMessageInByteArray();

					// 毎回インスタンスしないとsetMessageが変わらない
					if (data[2] > 0) {
						ShortMessage on = new ShortMessage();
						long mp = st.getNumberOfRiffList(i).get(j).music_position + (1920 * (i - 1));
						int minusBottomBass = checkBottomOfBass(st.getNumberOfRiffList(i));
						on.setMessage(ShortMessage.NOTE_ON, 1, data[1] - minusBottomBass, 127);
						MidiEventWithTicktime onMidievt = new MidiEventWithTicktime(on, mp, mp);
						st.getBassNoteList().add(onMidievt);

					}
					else {
						ShortMessage off = new ShortMessage();
						long mp = st.getNumberOfRiffList(i).get(j).music_position + (1920 * (i - 1));
						int minusBottomBass = checkBottomOfBass(st.getNumberOfRiffList(i));
						off.setMessage(ShortMessage.NOTE_OFF, 1, data[1] - minusBottomBass, 0);
						MidiEventWithTicktime offMidievt = new MidiEventWithTicktime(off, mp, mp);
						st.getBassNoteList().add(offMidievt);
					}
				}
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	void createBassRiff() {
		try {
			// final LinkedList<MidiEventWithTicktime> revisedFirstNoteList =
			// revisedFirstNoteList;
			// final LinkedList<MidiEventWithTicktime> bassNoteList =
			// st.getBassNoteList();
			clearBass(); // 呼び出すたびにベース類をカラにする
			OutputBassNote.revisedFirstNoteList = this.reviseFirstNoteList(OutputBassNote.firstNoteList);
			noteListDivideRiffList(this.revisedFirstNoteList);

			// 単音リフか複音リフ判定
			// 複音リフの時にコード判別
			for (int i = 0; i < riffLists.size(); i++) {
				singleOrChord[i] = Decide_SingleNote_Or_Chord(riffLists.get(i));

				// chordの時
				if (singleOrChord[i] == "chord") {
					String[] tmpChordName = newcheckChord(riffLists.get(i));
					chordArrayForDraw[i * 2] = tmpChordName[0];
					chordArrayForDraw[i * 2 + 1] = tmpChordName[1];

					List<MidiEventWithTicktime> nowRhythmList = createRhythmOfRiff(riffLists.get(i));
					Map<Byte, Long> tmpNoteOn = new HashMap<Byte, Long>();

					// コードネームとリズムの情報からベースリフを作成
					for (int j = 0; j < nowRhythmList.size(); j++) {
						MidiEventWithTicktime nowMidievt = nowRhythmList.get(j);
						byte[] data = nowMidievt.getMessageInByteArray();

						if (data[2] > 0) {
							tmpNoteOn.remove(data[1]);
							tmpNoteOn.put(data[1], nowMidievt.music_position);
						}
						else {
							long offMp = nowMidievt.music_position;
							long onMp = tmpNoteOn.get(data[1]);
							long length = offMp - onMp;
							ShortMessage on = new ShortMessage();
							ShortMessage off = new ShortMessage();

							// 小節前半
							if (onMp < 960) {
								on.setMessage(ShortMessage.NOTE_ON, 1, outputNoteNumberOfBass(tmpChordName[0]), 100);
								off.setMessage(ShortMessage.NOTE_OFF, 1, outputNoteNumberOfBass(tmpChordName[0]), 0);
							}
							else {
								on.setMessage(ShortMessage.NOTE_ON, 1, outputNoteNumberOfBass(tmpChordName[1]), 100);
								off.setMessage(ShortMessage.NOTE_OFF, 1, outputNoteNumberOfBass(tmpChordName[1]), 0);
							}
							bassRiffLists.get(i).add(new MidiEventWithTicktime(on, onMp, onMp));
							bassRiffLists.get(i).add(new MidiEventWithTicktime(off, offMp, offMp));
						}
					}
				}
				// singleの時
				else {
					// System.out.println("bottom:" + checkBottomOfBass(riffLists.get(i)));
					LinkedList<MidiEventWithTicktime> nowBassRiffList = createBassForSingleNote(riffLists.get(i)); //
					bassRiffLists.get(i).addAll(nowBassRiffList);
					// printMidiEventList(bassRiffLists.get(i));
				}
			}

			// 1小節単位だったリフから４小節単位に直す
			for (int i = 0; i < bassRiffLists.size(); i++) {
				LinkedList<MidiEventWithTicktime> nowBassRiffList = bassRiffLists.get(i);
				for (int j = 0; j < nowBassRiffList.size(); j++) {
					MidiEventWithTicktime midievt = nowBassRiffList.get(j);
					byte[] data = midievt.getMessageInByteArray();

					if (data[2] > 0) {
						ShortMessage on = new ShortMessage();
						long mp = midievt.music_position + (1920 * i);
						on.setMessage(ShortMessage.NOTE_ON, 1, data[1], data[2]);
						MidiEventWithTicktime onMidievt = new MidiEventWithTicktime(on, mp, mp);
						bassNoteList.add(onMidievt);
					}
					else {
						ShortMessage off = new ShortMessage();
						long mp = midievt.music_position + (1920 * i);
						off.setMessage(ShortMessage.NOTE_OFF, 1, data[1], data[2]);
						MidiEventWithTicktime offMidievt = new MidiEventWithTicktime(off, mp, mp);
						bassNoteList.add(offMidievt);
					}
				}
			}

			writeSMF();

		}
		catch (Exception e) {
			System.out.println("Create_BassLine_Error");
			e.printStackTrace();
		}
	}

	void createMainBassRiff() {
		this.bassNoteList.addAll(mainBassRiff);
		sm.createMainBassTrack(mainBassRiff);
	}

	// コードネームからベース音域内での最低音のnoteNumberを出力
	// 該当がなかったらA(３３）を出力
	byte outputNoteNumberOfBass(String chordName) {
		HashMap<String, Byte> chordMap = new HashMap<String, Byte>();
		HashMap<String, Byte> powerChordMap = new HashMap<String, Byte>();
		final String[] chordNameArray = { "C", "", "Dm", "", "Em", "F", "", "G", "", "Am", "", "Bmb5" };
		final String[] pwrChordNameArray = { "C5", "", "D5", "", "E5", "F5", "", "G5", "", "A5", "", "Bb5" };

		for (int i = 0; i < 12; i++) {
			chordMap.put(chordNameArray[i], (byte) (i + 24));
			powerChordMap.put(pwrChordNameArray[i], (byte) (i + 24));
		}

		byte dest;
		if (chordMap.containsKey(chordName)) {
			dest = chordMap.get(chordName);
		}
		else if (powerChordMap.containsKey(chordName)) {
			dest = powerChordMap.get(chordName);
		}
		else {
			dest = 33;
		}

		return dest;
	}

	// ベースリフのリズムを作成
	// SOlVEDFIXME:現在の使用では音長は平均値を取るだけなのでなにか工夫が必要
	LinkedList<MidiEventWithTicktime> createRhythmOfRiff(
			LinkedList<MidiEventWithTicktime> riffList) {
		try {
			LinkedList<MidiEventWithTicktime> destRhythmList = new LinkedList<MidiEventWithTicktime>();
			HashMap<Long, Long> tickMap = new HashMap<Long, Long>(); // tickを記録してどれだけ音長があるかも記録
			HashMap<Byte, Long> tmpNoteOn = new HashMap<Byte, Long>(); // ノートオンの記録

			List<Long> keyList = new LinkedList<Long>(); // 音がなったとこの記録
			HashMap<Long, Integer> numberOfNote = new HashMap<Long, Integer>(); // コードの音数のマップ

			// tickの大きさを記録するルーチン
			for (int i = 0; i < riffList.size(); i++) {
				MidiEventWithTicktime midievt = riffList.get(i);
				byte[] data = midievt.getMessageInByteArray();

				long maxNoteLength = -1;

				if (data[2] > 0) {
					long onMp = midievt.music_position;
					tmpNoteOn.remove(data[1]);
					tmpNoteOn.put(data[1], onMp);
				}
				// ノートオフの時記録
				else {
					long noteLength = midievt.music_position - tmpNoteOn.get(data[1]);
					long addTick = tmpNoteOn.get(data[1]);

					if (keyList.contains(addTick) == false) {
						keyList.add(addTick);
					}

					// すでにキーが存在
					if (tickMap.containsKey(addTick) == true && numberOfNote.containsKey(addTick) == true) {
						// long addLength = tickMap.get(addTick) + noteLength; // 存在しているものに足す

						if (maxNoteLength < noteLength) {
							tickMap.put(addTick, noteLength);
						}

						int addCount = numberOfNote.get(addTick) + 1;
						numberOfNote.put(addTick, addCount);
					}
					// キーが存在しない
					else {
						long addLength = noteLength;
						tickMap.put(addTick, addLength);

						numberOfNote.put(addTick, 1);
					}
				}
			}

			for (int i = 0; i < keyList.size(); i++) {
				long tickLength = tickMap.get((keyList.get(i))); // 蓄積された音長

				ShortMessage on = new ShortMessage();
				ShortMessage off = new ShortMessage();
				long addMp = keyList.get(i);
				long noteLength;
				int nmbOfNote = numberOfNote.get(addMp);

				on.setMessage(ShortMessage.NOTE_ON, 1, 33, 100); // とりあえずキーがAmなのでAをノートナンバーとする
				off.setMessage(ShortMessage.NOTE_OFF, 1, 33, 0);

				destRhythmList.add(new MidiEventWithTicktime(on, addMp, addMp));
				destRhythmList.add(new MidiEventWithTicktime(off, addMp + tickLength, addMp + tickLength));

				// System.out.println("tickLong:" + keyList.get(i) + " " +
				// tickMap.get(keyList.get(i)) + " " +
				// numberOfNote.get(keyList.get(i)));
			}

			return destRhythmList;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// ベース関係のクリア
	void clearBass() {
		this.bassNoteList.clear();

		for (int i = 0; i < bassRiffLists.size(); i++) {
			bassRiffLists.get(i).clear();
		}
	}

	// noteListを１小節単位のriffListに分ける
	// FIXME:小節をまたぐノートの処理をどうするか（裏で食っているノートなど）
	private void noteListDivideRiffList(LinkedList<MidiEventWithTicktime> list) {
		try {
			for (int i = 0; i < 4; i++) {
				LinkedList<MidiEventWithTicktime> tmpRiffList = riffLists.get(i);
				tmpRiffList.clear();
			}

			HashMap<Byte, MidiEventWithTicktime> tmpOnList = new HashMap<Byte, MidiEventWithTicktime>();
			for (int i = 0; i < list.size(); i++) {
				MidiEventWithTicktime nowMidievt = list.get(i);
				byte[] nowData = nowMidievt.getMessageInByteArray();
				long nowMp = nowMidievt.music_position; // FIXME: 小節にまたがるノートの処理

				if (nowData[2] > 0) {
					tmpOnList.put(nowData[1], nowMidievt);
				}
				else {
					long length = nowMp - tmpOnList.get(nowData[1]).music_position;
					long addOnMp = getReminderMp(tmpOnList.get(nowData[1]).music_position);
					int nowMeasure = getNowMeasure(tmpOnList.get(nowData[1]).music_position) - 1;// リストに記録するときにカウント分マイナスしてるのでマイナス１
					LinkedList<MidiEventWithTicktime> riffList = riffLists.get(nowMeasure);
					ShortMessage onSmsg = new ShortMessage();
					ShortMessage offSmsg = new ShortMessage();
					onSmsg.setMessage(ShortMessage.NOTE_ON, 1, nowData[1], 100);
					offSmsg.setMessage(ShortMessage.NOTE_OFF, 1, nowData[1], 0);
					MidiEventWithTicktime addOnMidievt = new MidiEventWithTicktime(onSmsg, addOnMp, addOnMp);
					MidiEventWithTicktime addOffMidievt = new MidiEventWithTicktime(offSmsg, addOnMp + length, addOnMp + length);
					riffList.add(addOnMidievt);
					riffList.add(addOffMidievt);
				}
			}
		}

		catch (Exception e) {
			System.out.println("DecideSingleOrChord");
			e.printStackTrace();
		}
	}

	// riffListの単位でベース音域の最低音とリフの最低音の差分をリターンする
	int checkBottomOfBass(LinkedList<MidiEventWithTicktime> riffList) {
		try {
			HashMap<Integer, Integer> bottomList = new HashMap<Integer, Integer>(); // ベース音域の最低音のハッシュマップ, <Byte, Integer>だとダメ
			MidiEventWithTicktime bottomMevt = null;
			byte[] bottomData = null;

			// ハッシュマップの作成
			for (int i = 0; i < 12; i++) {
				bottomList.put(i, i + 24);
			}

			for (int i = 0; i < riffList.size(); i++) {
				MidiEventWithTicktime nowMevt = riffList.get(i);
				byte[] nowData = nowMevt.getMessageInByteArray();

				// noteOnのときのみ比較処理
				if (nowData[2] > 0) {
					if (bottomMevt == null || nowData[1] < bottomData[1]) {
						bottomMevt = nowMevt;
						bottomData = nowData;
					}
				}
			}
			int dest = 0;
			if (bottomList.containsKey(bottomData[1] % 12) == true) {
				// System.out.println("contains.true");
				dest = bottomData[1] - bottomList.get(bottomData[1] % 12);
			}
			else {
				// System.out.println("contains.false");
				dest = 0;
			}
			return dest;
		}

		catch (Exception e) {
			System.out.println("Check bottom of bass Error");
			e.printStackTrace();
			return 0;
		}
	}

	private void printMidiEventList(LinkedList<MidiEventWithTicktime> list) {
		try {
			for (int i = 0; i < list.size(); i++) {
				MidiEventWithTicktime nowMidievt = list.get(i);
				byte[] nowData = nowMidievt.getMessageInByteArray();
				System.out.println("Status\t" + nowData[0] + "\tNoteNumber\t" + nowData[1] + "\tVelocity\t" + nowData[2] + "\tTick\t" + nowMidievt.getTick());
			}
		}
		catch (Exception e) {
			System.out.println("PrintListError");
			e.printStackTrace();
		}
	}

	// わかりやすくするためにMp(tick)を1小節単位になおす
	private long getReminderMp(long Mp) {
		if (Mp < 0) {
			long dist = 0;
			return dist;
		}
		else {
			long dist = Mp % 1920;
			return dist;
		}
	}

	private int getNowMeasure(long mp) {
		int dest = (int) (mp / 1920 + 1);// ノートリスト記録の際にカウント分引いているのでそのぶんプラス1

		if (dest > 0) {
			return dest;
		}
		else {
			return 1;
		}
	}

	private int chordOrsingle(LinkedList<MidiEventWithTicktime> lise) {
		return 0;
	}

	// １６部音符ごとに和音かどうか判断し50％以上だったら和音リフと判断
	// 以前の仕様 ： riffList単位で単音リフか複音リフか判定、一つでも複音があれば複音リフと判定する
	// SOLVED FIXME:なぜか maxCountOfNoteが更新されない
	private String Decide_SingleNote_Or_Chord(
			LinkedList<MidiEventWithTicktime> riffList) {
		int maxCountOfChord = 3; // 和音判定の際の閾値

		int[] chordFlag16 = new int[32]; //

		MidiEventWithTicktime preMidievt = null;
		byte[] preData = null;
		long preMp = -1;

		Map<Byte, Long> tmpNoteOn = new HashMap<Byte, Long>(); // ノートオンの一時保管
		Map<Long, Integer> noteMapOfCount = new HashMap<Long, Integer>(); // ノートオンがあったTIｃｋごとのカウント(和音判別のための）

		// riffListの展開
		for (int i = 0; i < riffList.size(); i++) {
			MidiEventWithTicktime nowMidievt = riffList.get(i);
			byte[] nowData = nowMidievt.getMessageInByteArray();
			long nowMp = nowMidievt.music_position;

			// ノートオンの場合
			if (nowData[2] > 0) {
				tmpNoteOn.put(nowData[1], nowMp);

				int addCount; // 追加のためのカウント
				// すでに存在
				if (noteMapOfCount.containsKey(nowData[1])) {
					addCount = noteMapOfCount.get(nowData[1]) + 1;
				}
				// 存在しない
				else {
					addCount = 1;
				}
				noteMapOfCount.put(nowMp, addCount);
			}

			// ノートオフ
			if (nowData[2] == 0) {
				long length = nowMp - tmpNoteOn.get(nowData[1]);
				int nmbOfFlag = (int) (length / 120); // いくつの１６フラグを通っているか
				int start = (int) (tmpNoteOn.get(nowData[1]) / 120);
				int end = start + nmbOfFlag;

				// 音が鳴っているところでカウント（音が鳴り終わるところはカウントしない）
				for (int flag_j = start; flag_j < end; flag_j++) {
					chordFlag16[flag_j]++;
				}
			}
		}

		// printFlagArray(chordFlag16);
		int countOfChord = 0;
		// 和音の場所がいくつあるか数える
		for (int i = 0; i < chordFlag16.length; i++) {
			if (chordFlag16[i] >= 2) {
				countOfChord += (chordFlag16[i] - 1);
			}
		}
		if (countOfChord >= maxCountOfChord) {
			return "chord";
		}
		else {
			return "singleNote";
		}

	}

	// checkChordのために１小節を２つに分割,小節の前半後半にまたがっているノートを分割
	LinkedList<MidiEventWithTicktime> divideRiffList(
			LinkedList<MidiEventWithTicktime> riffList) {
		try {
			LinkedList<MidiEventWithTicktime> destRiffList = new LinkedList<MidiEventWithTicktime>();
			HashMap<Byte, Long> tmpNoteOn = new HashMap<Byte, Long>();
			for (int i = 0; i < riffList.size(); i++) {
				MidiEventWithTicktime nowMidievt = riffList.get(i);
				byte[] nowData = nowMidievt.getMessageInByteArray();
				long nowMp = nowMidievt.music_position;

				if (nowData[2] > 0) {
					tmpNoteOn.remove(nowData[1]);
					tmpNoteOn.put(nowData[1], nowMp);
				}
				else {
					long length = -1;
					if (tmpNoteOn.containsKey(nowData[1]) != true) {
						length = 120L;
					}
					else {
						length = nowMp - tmpNoteOn.get(nowData[1]);
					}
					ShortMessage onSmsg = new ShortMessage();
					ShortMessage offSmsg = new ShortMessage();
					onSmsg.setMessage(ShortMessage.NOTE_ON, 1, nowData[1], 100);
					offSmsg.setMessage(ShortMessage.NOTE_OFF, 1, nowData[1], 0);
					long addOnMp = tmpNoteOn.get(nowData[1]);
					long addOffMp = addOnMp + length;
					// もし、ノートが小節の前半後半にまたがっていたら
					if (addOnMp < 960 && addOffMp > 960) {
						// 通常のノートを追加
						MidiEventWithTicktime addOnMidievt = new MidiEventWithTicktime(onSmsg, addOnMp, addOnMp);
						MidiEventWithTicktime addOffMidievt = new MidiEventWithTicktime(offSmsg, 960, 960);
						destRiffList.add(addOnMidievt);
						destRiffList.add(addOffMidievt);

						// 960で切ったノートを追加
						addOnMidievt = new MidiEventWithTicktime(onSmsg, 960, 960);
						addOffMidievt = new MidiEventWithTicktime(offSmsg, addOffMp, addOffMp);
						destRiffList.add(addOnMidievt);
						destRiffList.add(addOffMidievt);
					}
					// 通常の場合
					else {
						MidiEventWithTicktime addOnMidievt = new MidiEventWithTicktime(onSmsg, addOnMp, addOnMp);
						MidiEventWithTicktime addOffMidievt = new MidiEventWithTicktime(offSmsg, addOffMp, addOffMp);
						destRiffList.add(addOnMidievt);
						destRiffList.add(addOffMidievt);
					}
				}
			}

			return destRiffList;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// 1小節単位で前半後半の二部音符間でコード判定、2つのコードネームをリターン、コードを判別できない場合は"none"をリターン
	// FIXME:現在の仕様だと基本形のみしか認識できない、パワーコードの認識を至急可能にする必要あり、セブンスは対象外
	String[] newcheckChord(LinkedList<MidiEventWithTicktime> riffList) {
		String[] destChordListStrings = new String[6];
		final LinkedList<MidiEventWithTicktime> diviedRiffList = divideRiffList(riffList); // 小節を半分に分割して前半と後半にわける
		final HashMap<Byte, Long> tmpNoteOnList = new HashMap<Byte, Long>(); // ノートオンのハッシュテーブル
		final HashMap<Integer, Long> noteMap1 = new HashMap<Integer, Long>(); // コード判定のためのマップ、前半
		final HashMap<Integer, Long> noteMap2 = new HashMap<Integer, Long>(); // 後半
		final LinkedList<HashMap<Integer, Long>> noteMapList = new LinkedList<HashMap<Integer, Long>>() {
			{
				add(noteMap1);
				add(noteMap2);
			}
		};

		// 引数のriffListからnoteMapを作る
		for (int i = 0; i < diviedRiffList.size(); i++) {
			MidiEventWithTicktime nowMidievt = diviedRiffList.get(i);
			byte[] nowData = nowMidievt.getMessageInByteArray();
			long nowMp = nowMidievt.music_position;

			// ノートオン
			if (nowData[2] > 0) {
				tmpNoteOnList.remove(nowData[1]);
				tmpNoteOnList.put(nowData[1], nowMp);
			}
			// ノートオフの時noteMapに追加
			else {
				long length = nowMp - tmpNoteOnList.get(nowData[1]);
				int nowNoteNmb = nowData[1] % 12; // １オクターブ内に収める

				int firstOrLatterHalf = (int) (tmpNoteOnList.get(nowData[1]) / 960); // 小節の前半か後半か
				HashMap<Integer, Long> nowNoteMap = null;

				// 前半
				if (firstOrLatterHalf == 0) {
					nowNoteMap = noteMapList.get(0);
				}
				// 後半
				else {
					nowNoteMap = noteMapList.get(1);
				}

				long addSumLengh = -1;
				// keyが存在する場合は合計を追加
				if (nowNoteMap.containsKey(nowNoteNmb) == true) {
					addSumLengh = nowNoteMap.get(nowNoteNmb) + length;
				}
				else {
					addSumLengh = length;
				}
				nowNoteMap.put(nowNoteNmb, addSumLengh); // 12で割ったあまりでノートナンバーを1オクターブ内に
			}
		}

		final Integer[] chord1 = { -1, -1, -1, -1 }; // 前半のコードで上から4つ
		final Integer[] chord2 = { -1, -1, -1, -1 }; // 後半のコードで上から4つ
		ArrayList<Integer[]> destChordList = new ArrayList<Integer[]>() {
			{
				add(chord1);
				add(chord2);
			}
		};

		// １オクターブ内は１２、noteMapから上から３つを抽出、降順にソート
		// MEMO:コード認識できる範囲を広げるために、ノートの音長も記録したほうがいいかも
		for (int i = 0; i < destChordList.size(); i++) {
			Integer[] nowChord = destChordList.get(i);
			HashMap<Integer, Long> nowNoteMap = noteMapList.get(i);
			long[] max = { -1, -1, -1, -1 }; // 上から３つを抜き出すための音長の合計を配列
			// 3つを抽出し、降順にソート
			for (int j = 0; j < 12; j++) {
				if (nowNoteMap.containsKey(j) == true) {
					if (max[0] <= nowNoteMap.get(j)) {
						max[3] = max[2];
						max[2] = max[1];
						max[1] = max[0];
						max[0] = nowNoteMap.get(j);
						nowChord[3] = nowChord[2];
						nowChord[2] = nowChord[1];
						nowChord[1] = nowChord[0];
						nowChord[0] = j;
					}
					else if (max[1] <= nowNoteMap.get(j)) {
						max[3] = max[2];
						max[2] = max[1];
						max[1] = nowNoteMap.get(j);
						nowChord[3] = nowChord[2];
						nowChord[2] = nowChord[1];
						nowChord[1] = j;
					}
					else if (max[2] <= nowNoteMap.get(j)) {
						max[3] = max[2];
						max[2] = nowNoteMap.get(j);
						nowChord[3] = nowChord[2];
						nowChord[2] = j;
					}
					else if (max[2] <= nowNoteMap.get(j)) {
						max[3] = nowNoteMap.get(j);
						nowChord[3] = j;
					}
					else {
						// Do Nothing
					}
				}
			}
		}

		for (int i = 0; i < destChordList.size(); i++) {
			// printArray(destChordList.get(i));
		}

		final HashMap<String, Integer> pointOfChordList_1 = new HashMap<String, Integer>();
		final HashMap<String, Integer> pointOfChordList_2 = new HashMap<String, Integer>();
		ArrayList<HashMap<String, Integer>> pointOfChordList = new ArrayList<HashMap<String, Integer>>() {
			{
				add(pointOfChordList_1);
				add(pointOfChordList_2);
			}
		};

		// コードを確定するルーチン
		// destChordListのためのループ
		for (int destChordList_i = 0; destChordList_i < destChordList.size(); destChordList_i++) {
			Integer[] nowDestChord = destChordList.get(destChordList_i);

			String[] chordName = { "Am", "Bmb5", "C", "Dm", "Em", "F", "G" };// キー
			String[] powerChordName = { "A5", "Bb5", "C5", "D5", "E5", "F5", "G5" };// キー

			HashMap<String, Integer[]> chordListOfAm = createChordListOfAm();
			HashMap<String, Integer[]> powerChordListOfAm = createPowerChordListOfAm();

			// Amキーのダイアトニックコードを展開するためのループ
			for (int chordName_j = 0; chordName_j < chordName.length; chordName_j++) {
				Integer[] tmpChord = chordListOfAm.get(chordName[chordName_j]);
				Integer[] tmpPowerChord = powerChordListOfAm.get(powerChordName[chordName_j]);
				int chordMatchNoteCount = 0; // コードトーンにマッチしたカウンタ
				int powerMatchNoteCount = 0;

				// パワーコードの場合
				if (nowDestChord[2] == -1) {
					for (int tmpPowerChord_k = 0; tmpPowerChord_k < tmpPowerChord.length; tmpPowerChord_k++) {
						for (int nowChord_l = 0; nowChord_l < nowDestChord.length; nowChord_l++) {
							if (tmpPowerChord[tmpPowerChord_k] == nowDestChord[nowChord_l]) {
								powerMatchNoteCount++;
							}
						}
					}
				}
				else {
					// 現在のAmキーのコードトーンを展開
					for (int tmpChord_k = 0; tmpChord_k < tmpChord.length; tmpChord_k++) {
						// destコードを展開、比較してマッチしたらカウンタを増やす
						for (int nowChord_l = 0; nowChord_l < nowDestChord.length; nowChord_l++) {
							if (tmpChord[tmpChord_k] == nowDestChord[nowChord_l]) {
								chordMatchNoteCount++;
							}
						}
					}
				}

				pointOfChordList.get(destChordList_i).put(chordName[chordName_j], chordMatchNoteCount);
				pointOfChordList.get(destChordList_i).put(powerChordName[chordName_j], powerMatchNoteCount);
			}
		}

		// FIXME
		final List<Map.Entry<String, Integer>> sortedEntry1 = new ArrayList<Map.Entry<String, Integer>>(pointOfChordList.get(0).entrySet());
		final List<Map.Entry<String, Integer>> sortedEntry2 = new ArrayList<Map.Entry<String, Integer>>(pointOfChordList.get(1).entrySet());

		List<List<Map.Entry<String, Integer>>> sortedEntries = new ArrayList() {
			{
				add(sortedEntry1);
				add(sortedEntry2);
			}
		};

		// pointOfChordListをソート
		for (int i = 0; i < pointOfChordList.size(); i++) {
			List<Map.Entry<String, Integer>> nowEntry = sortedEntries.get(i);
			Collections.sort(nowEntry, new Comparator() {
				public int compare(Object o1, Object o2) {
					Map.Entry e1 = (Map.Entry) o1;
					Map.Entry e2 = (Map.Entry) o2;
					return ((Integer) e2.getValue()).compareTo((Integer) e1.getValue());
				}
			});
		}

		// 上から３つを取得
		for (int i = 0; i < sortedEntries.size(); i++) {
			for (int j = 0; j < 3; j++) {
				List<Map.Entry<String, Integer>> nowEntry = sortedEntries.get(i);
				System.out.println(nowEntry.get(j).getKey() + " " + nowEntry.get(j).getValue());
				destChordListStrings[i + j] = nowEntry.get(j).getKey();
			}
		}

		return destChordListStrings;

	}

	// キーAmのパワーコードのリストを作成
	HashMap<String, Integer[]> createPowerChordListOfAm() {
		final HashMap<String, Integer[]> destMap = new HashMap<String, Integer[]>();
		final String[] chordName = { "A5", "Bb5", "C5", "D5", "E5", "F5", "G5" };
		final Integer[][] chordTone = { { 9, 4 }, { 11, 2 }, { 0, 7 }, { 2, 9 }, { 4, 11 }, { 5, 0 }, { 7, 2 } };

		for (int i = 0; i < 7; i++) {
			Integer[] putChordTone = new Integer[2];
			// Integer[][]ではMapに追加できないのでInteger[]に変換
			for (int j = 0; j < putChordTone.length; j++) {
				putChordTone[j] = chordTone[i][j];
			}
			destMap.put(chordName[i], putChordTone);
		}

		return destMap;
	}

	// キーがコードネーム、値がコードトーンの配列のHashMapを作成
	HashMap<String, Integer[]> createChordListOfAm() {
		final HashMap<String, Integer[]> destMap = new HashMap<String, Integer[]>();
		final String[] chordName = { "Am", "Bmb5", "C", "Dm", "Em", "F", "G" };
		final Integer[][] chordTone = { { 9, 0, 4 }, { 11, 5, 2 }, { 0, 4, 7 }, { 2, 5, 9 }, { 4, 7, 11 }, { 5, 9, 0 }, { 7, 11, 2 } };

		for (int i = 0; i < 7; i++) {
			Integer[] putChordTone = new Integer[3];
			// Integer[][]ではMapに追加できないのでInteger[]に変換
			for (int j = 0; j < 3; j++) {
				putChordTone[j] = chordTone[i][j];
			}
			destMap.put(chordName[i], putChordTone);
		}

		return destMap;
	}

	void printArray(Object array[]) {
		for (int i = 0; i < array.length; i++) {
			System.out.println(i + " " + array[i]);
		}
	}

	void printIntArray(Integer[] integers) {
		for (int i = 0; i < integers.length; i++) {
			System.out.println(i + " " + integers[i]);
		}
	}

	// 和音判定の際のフラグの表示
	void printFlagArray(int[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.println(i + " " + array[i]);
		}
	}

	void changeBassPlay() {
		if (stateOfBassPlay == "Input") {
			stateOfBassPlay = "Create & Play";
		}
		else {
			stateOfBassPlay = "Input";
		}
	}

	void inputModeRiff(int nowInputMeasure) {
		inputMeasure = nowInputMeasure;
		createMeasure = inputMeasure + 1;
		// deleteRiff();
		LinkedList<MidiEventWithTicktime> tmp = sm.pickoutMidiMessegeOfSmf(new File("/home/masaki/Dropbox/midis/demoGuitar.mid"), 2, nowInputMeasure - 1, nowInputMeasure + 3);
		// OutputBassNote.firstNoteList.addAll(tmp);

		// 初期化
		for (int i = 0; i < singleOrChord.length; i++) {
			singleOrChord[i] = "";

		}
		// 初期化
		for (int i = 0; i < chordArrayForDraw.length; i++) {
			chordArrayForDraw[i] = "";
		}
	}

	void deleteRiff() {
		// ギターのリフリストとベースのリフリストをカラにする
		for (int i = 0; i < riffLists.size(); i++) {
			riffLists.get(i).clear();
			bassRiffLists.get(i).clear();
		}
		firstNoteList.clear();
		revisedFirstNoteList.clear();
		bassNoteList.clear();
	}

	void deleteRiffInvaildGt() {
		for (int i = 0; i < bassRiffLists.size(); i++) {
			bassRiffLists.get(i).clear();
		}
		firstNoteList.clear();
		revisedFirstNoteList.clear();
		bassNoteList.clear();
	}

	void writeSMF() {
		SCCDataSet SCC2 = new SCCDataSet(480);
		SCCDataSet SCC1 = new SCCDataSet(480);

		Part p1 = SCC2.addPart(2, 2, 1, 100, "Guitar");
		Part p2 = SCC2.addPart(2, 2, 1, 100, "Bass");

		HashMap<Byte, Long> bassTmpNoteOn = new HashMap<Byte, Long>();
		for (int i = 0; i < bassNoteList.size(); i++) {
			MidiEventWithTicktime midievt = bassNoteList.get(i);
			byte[] data = midievt.getMessageInByteArray();
			long mp = midievt.music_position;

			if (data[2] > 0) {
				bassTmpNoteOn.put(data[1], mp);
			}
			else if (data[2] == 0) {
				long onMp = bassTmpNoteOn.get(data[1]);
				long length = mp - onMp;

				if (onMp >= 0 && length >= 0) {
					p2.addNoteElement((int) onMp, (int) (onMp + length), (int) data[1], 100, 0);
				}
			}
		}

		HashMap<Byte, Long> gtTmpNoteOn = new HashMap<Byte, Long>();
		for (int i = 0; i < revisedFirstNoteList.size(); i++) {
			MidiEventWithTicktime midievt = revisedFirstNoteList.get(i);
			byte[] data = midievt.getMessageInByteArray();
			long mp = midievt.music_position;

			if (data[2] > 0) {
				gtTmpNoteOn.put(data[1], mp);
			}
			else if (data[2] == 0) {
				long onMp = gtTmpNoteOn.get(data[1]);
				long length = mp - onMp;

				if (onMp >= 0 && length >= 0) {
					p1.addNoteElement((int) onMp, (int) (onMp + length), (int) data[1], 100, 0);
				}

			}
		}

		try {
			SCCXMLWrapper sxw = SCC2.toWrapper();
			sxw.writefile("/home/masaki/xml/gtBass.xml");
			//SCCXMLWrapper sxw2 = SCC1.toWrapper();
			//sxw2.writefile("/home/masaki/xml/gt.xml");

			smfNum++;
			System.out.println("write");
		}

		catch (TransformerException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		catch (SAXException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	}

	// 状況に応じてリフプレイを選択
	// FIXME:stateOfBassPlayの状態をもっとわかりやすくすべき
	// もっと綺麗なやり方が有りそう
	void newRiffPlay(int inputNowMeasure, String foot) {
		int[] changingTiming = { 1, 9, 17, 25, 33, 41, 50, 58, 65, 72 }; // テスト用のインプットするタイミング

		/*
		 * for (int i = 0; i < changingTiming.length; i++) { if (inputNowMeasure == changingTiming[i] && stateOfBassPlay == "Input") { this.inputModeRiff(inputNowMeasure); changeBassPlay();
		 * sm.deleteBassTrack(); } }
		 */

		if (inputNowMeasure > 4 && stateOfBassPlay == "Input" && foot == "Input") {
			this.inputModeRiff(inputNowMeasure);
			changeBassPlay();
			// sm.deleteBassTrack();
			stateOfFoot = true;

			System.out.println("input" + " " + inputMeasure + " " + createMeasure);
		}

		if (inputNowMeasure > 4 && stateOfBassPlay == "Input" && foot == "mainRiff") {
			inputModeRiff(inputNowMeasure);
			this.changeBassPlay();
			System.out.println("mainRiffPlayInput");

			stateOfMain = true;
		}

		if (inputNowMeasure == createMeasure && stateOfBassPlay == "Create & Play" && stateOfFoot == true) {
			this.deleteRiff();
			this.createTmpBassRiff();
			// this.changeBassPlay();
			sm.createBassTrack(OutputBassNote.bassNoteList);

			this.stockOfChordProg.add(nextChordProg);
			nextChordProg.clear();

			stateOfFoot = false;
			stateOfFree = false;

			createMeasure = inputNowMeasure + 4;

			System.out.println("createFoot");
		}

		if (inputNowMeasure == createMeasure && stateOfBassPlay == "Create & Play" && stateOfMain == true) {
			this.deleteRiff();
			this.createMainBassRiff();
			this.changeBassPlay();
			sm.createBassTrack(OutputBassNote.bassNoteList);

			stateOfMain = false;

			System.out.println("createMainRiff");
		}

		if (inputNowMeasure == createMeasure && stateOfBassPlay == "Create & Play" && stateOfFree == false) {
			// this.deleteRiffInvaildGt();
			this.createBassRiff();
			this.changeBassPlay();
			sm.createBassTrack(OutputBassNote.bassNoteList);

			stateOfFree = true;

			System.out.println("createAfterFoot");
		}

		if (inputNowMeasure == 5 && rec == false) {
			recMainRiff();
			sm.createBassTrack(bassNoteList);
			stateOfFree = true;
		}
	}

	

	void recMainRiff() {
		this.createBassRiff();
		mainBassRiff.addAll(bassNoteList);
		rec = true;
		System.out.println("record");
	}

	void createTmpBassRiff() {

		try {
			for (int i = 0; i < bassRiffLists.size(); i++) {
				List nowBassRiff = bassRiffLists.get(i);

				long onMp1 = 0;
				long onMp2 = onMp1 + 960;
				long length = 480;
				ShortMessage on1 = new ShortMessage();
				ShortMessage off1 = new ShortMessage();
				ShortMessage on2 = new ShortMessage();
				ShortMessage off2 = new ShortMessage();

				int index = i * 2;

				this.chordArrayForDraw[index] = nextChordProg.get(index);
				this.chordArrayForDraw[index + 1] = nextChordProg.get(index + 1);

				on1.setMessage(ShortMessage.NOTE_ON, 1, outputNoteNumberOfBass(nextChordProg.get(index)),

				100);
				off1.setMessage(ShortMessage.NOTE_OFF, 1, outputNoteNumberOfBass(nextChordProg.get(index)), 0);

				on2.setMessage(ShortMessage.NOTE_ON, 1, outputNoteNumberOfBass(nextChordProg.get(index + 1)),

				100);
				off2.setMessage(ShortMessage.NOTE_OFF, 1, outputNoteNumberOfBass(nextChordProg.get(index + 1)), 0);

				bassRiffLists.get(i).add(new MidiEventWithTicktime(on1, onMp1, onMp1));
				bassRiffLists.get(i).add(new MidiEventWithTicktime(off1, onMp1 + length, onMp1 + length));

				bassRiffLists.get(i).add(new MidiEventWithTicktime(on2, onMp2, onMp2));
				bassRiffLists.get(i).add(new MidiEventWithTicktime(off2, onMp2 + length, onMp2 + length));

			}

			// 1小節単位だったリフから４小節単位に直す
			for (int i = 0; i < bassRiffLists.size(); i++) {
				LinkedList<MidiEventWithTicktime> nowBassRiffList = bassRiffLists.get(i);
				for (int j = 0; j < nowBassRiffList.size(); j++) {
					MidiEventWithTicktime midievt = nowBassRiffList.get(j);
					byte[] data = midievt.getMessageInByteArray();

					if (data[2] > 0) {
						ShortMessage on = new ShortMessage();
						long mp = midievt.music_position + (1920 * i);
						on.setMessage(ShortMessage.NOTE_ON, 1, data[1], data[2]);
						MidiEventWithTicktime onMidievt = new MidiEventWithTicktime(on, mp, mp);
						bassNoteList.add(onMidievt);
					}
					else {
						ShortMessage off = new ShortMessage();
						long mp = midievt.music_position + (1920 * i);
						off.setMessage(ShortMessage.NOTE_OFF, 1, data[1], data[2]);
						MidiEventWithTicktime offMidievt = new MidiEventWithTicktime(off, mp, mp);
						bassNoteList.add(offMidievt);
						
						writeSMF();
					}
				}
			}

		}
		catch (InvalidMidiDataException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	void initSingelOrChordAndChordArrayForDraw() {
		// 初期化
		for (int i = 0; i < singleOrChord.length; i++) {
			singleOrChord[i] = "";

		}
		// 初期化
		for (int i = 0; i < chordArrayForDraw.length; i++) {
			chordArrayForDraw[i] = "";
		}
	}

}
