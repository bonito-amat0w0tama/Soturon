package research;
import java.util.*;

import jp.crestmuse.cmx.processing.*;
import jp.crestmuse.cmx.amusaj.sp.*;
import jp.crestmuse.cmx.sound.*;
import javax.sound.midi.*;

import processing.core.*;

public class OutputBassNote extends SPModule {
	private HashMap<Byte, Long> gapList = new HashMap<Byte, Long>();// クオンタイズの際のノートのズレ
	Sequence_manager sm = null;
	Struct st = null;
	CMXController cmx;

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
			// long mp = midievt.music_position;
			long newmp;

			// MIDIギターではノートオン、ノートオフとも-112でvelocityによって判断,さらに、常に変なMIDI信号
			if (data[0] == -112) {

				newmp = quantize(midievt, 16) - 1920;
				ShortMessage message = new ShortMessage();
				// message.setMessage(1, 1, 1);
				MidiEventWithTicktime newMidievt = new MidiEventWithTicktime(midievt.getMessage(), newmp, newmp);
				newMidievt.createShortMessageEvent(newmp, 0, data[0], data[1], data[2]);

				/*
				 * if (3 < sm.getNowBar() && sm.getNowBar() < 8) {
				 * 
				 * 
				 * st.getNoteList().add(newMidievt);
				 * 
				 * }
				 */

				st.getNoteList().add(newMidievt);
				st.getFirstNoteList().add(new MidiEventWithTicktime(midievt.getMessage(), newmp % 7680, newmp % 7680));

				dest[0].add(newMidievt);
			}
		}

		catch (Exception e) {
			e.printStackTrace();

		}

	}

	public Class[] getInputClasses() {
		return new Class[] { MidiEventWithTicktime.class };
	}

	public Class[] getOutputClasses() {
		return new Class[] { MidiEventWithTicktime.class };
	}

	public long quantize(MidiEventWithTicktime midievt, int qon) {
		long tick_gap;
		long note_gap;
		int qont;// クオンタイズ大きさ
		long quantize_note_position;
		byte[] data = midievt.getMessageInByteArray();

		qont = qon / 4;

		// if (midievt.music_position < 1 * bar_length) quantize_note_position =
		// 1 * bar_length;

		// ノートオンの場合
		if (data[2] > 0) {
			tick_gap = cmx.getTickPosition() % (sm.getTpb() / qont);

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
			if (gapList.get(data[1]) == null) {
				quantize_note_position = midievt.music_position + 120;
			}
			else {
				quantize_note_position = midievt.music_position + gapList.get(data[1]);
			}

			gapList.remove(data[1]);

			return quantize_note_position;
		}
	}

	public LinkedList<MidiEventWithTicktime> reviseFirstNoteList() {
		try {
			LinkedList<MidiEventWithTicktime> revisedNote = new LinkedList<MidiEventWithTicktime>();
			MidiEventWithTicktime midievt = null;
			byte[] data = null;
			byte[] tmpData = null;
			HashMap<Byte, MidiEventWithTicktime> tmpOnNote = new HashMap<Byte, MidiEventWithTicktime>();
			long onMp = -1;
			long offMp = -1;
			long length = -1;

			MidiEventWithTicktime tmpMidievt = null;
			MidiEventWithTicktime onMidievt = null;
			MidiEventWithTicktime offMidievt = null;
			ShortMessage on = null;
			ShortMessage off = null;

			for (int i = 0; i < st.getFirstNoteList().size(); i++) {
				midievt = st.getFirstNoteList().get(i);
				data = midievt.getMessageInByteArray();

				if (data[2] > 0) {
					tmpOnNote.remove(data[1]);
					tmpOnNote.put(data[1], midievt);
				}
				else {
					tmpMidievt = tmpOnNote.get(data[1]);
					tmpData = tmpMidievt.getMessageInByteArray();
					onMp = tmpMidievt.music_position;
					offMp = midievt.music_position;
					length = offMp - onMp;

					// "length"カットする長さの閾値
					if (length > 40) {
						on = new ShortMessage();
						off = new ShortMessage();
						on.setMessage(ShortMessage.NOTE_ON, 0, data[1], tmpData[2]);
						off.setMessage(ShortMessage.NOTE_OFF, 0, data[1], 0);
						onMidievt = new MidiEventWithTicktime(on, onMp, onMp);
						offMidievt = new MidiEventWithTicktime(off, onMp + length, onMp + length);

						revisedNote.add(onMidievt);
						revisedNote.add(offMidievt);

					}
				}
			}
			/*
			 * // 表示 for (int i = 0; i < revisedNote.size(); i++ ) { byte[] tmpdata = revisedNote.get(i).getMessageInByteArray(); System.out.println(revisedNote.get(i).music_position + " " +
			 * tmpdata[0] + " " + tmpdata[1] + " " + tmpdata[2]);
			 * 
			 * }
			 */
			return revisedNote;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public void checkRoute() {

		try {
			st.getRiffList1().clear();
			st.getRiffList2().clear();
			st.getRiffList3().clear();
			st.getRiffList4().clear();

			long premp = -1;
			long nowmp = -1;
			byte[] nowData;
			byte[] preData;
			MidiEventWithTicktime nowMevt = null;
			MidiEventWithTicktime preMevt = null;
			MidiEventWithTicktime addOnMevt = null;
			MidiEventWithTicktime addOffMevt = null;
			MidiEventWithTicktime tmpMevt = null;
			preData = st.getFirstNoteList().get(0).getMessageInByteArray();
			premp = st.getFirstNoteList().get(0).music_position;
			preMevt = st.getFirstNoteList().get(0);

			long length = -1;
			HashMap<Byte, MidiEventWithTicktime> tmpOffBassNote = new HashMap<Byte, MidiEventWithTicktime>();
			HashMap<Byte, MidiEventWithTicktime> tmpOnBassNote = new HashMap<Byte, MidiEventWithTicktime>();

			for (int i = 0; i < st.getRevisedFirstNoteList().size(); i++) {
				nowMevt = st.getRevisedFirstNoteList().get(i);
				nowData = nowMevt.getMessageInByteArray();
				// System.out.println(data[0] + " " + data[1] + " " + data[2] + " " + st.getNoteList().get(i).music_position + " " + (st.getNoteList().get(i).music_position / 1920));

				if (nowData[2] > 0) {
					nowmp = nowMevt.music_position;

					if ((nowmp - premp) < 2) {
						if (nowData[1] < preData[1]) {
							preMevt = nowMevt;
							preData = preMevt.getMessageInByteArray();
						}
					}
					else {
						int nowBar = (int) (premp / 1920);
						long mp = premp % 1920;
						tmpMevt = tmpOffBassNote.get(preData[1]);
						tmpOffBassNote.remove(preData[1]);
						length = tmpMevt.music_position - premp;
						addOnMevt = new MidiEventWithTicktime(preMevt.getMessage(), mp, mp);
						addOffMevt = new MidiEventWithTicktime(tmpMevt.getMessage(), mp + length, mp + length);

						if (nowBar == 0) {
							st.getRiffList1().add(addOnMevt);
							st.getRiffList1().add(addOffMevt);
						}
						else if (nowBar == 1) {
							st.getRiffList2().add(addOnMevt);
							st.getRiffList2().add(addOffMevt);

						}
						else if (nowBar == 2) {
							st.getRiffList3().add(addOnMevt);
							st.getRiffList3().add(addOffMevt);

						}
						else {
							st.getRiffList4().add(addOnMevt);
							st.getRiffList4().add(addOffMevt);

						}

						// 初期化
						premp = nowmp;
						preData = nowData;
						preMevt = nowMevt;
					}
				}
				else {
					tmpOffBassNote.remove(nowData[1]);
					tmpOffBassNote.put(nowData[1], nowMevt);
				}

			}

			// 最後のノート用
			int nowBar = (int) (premp / 1920);
			long mp = premp % 1920;
			tmpMevt = tmpOffBassNote.get(preData[1]);
			tmpOffBassNote.remove(preData[1]);
			length = tmpMevt.music_position - premp - 1;
			addOnMevt = new MidiEventWithTicktime(preMevt.getMessage(), mp, mp);
			addOffMevt = new MidiEventWithTicktime(tmpMevt.getMessage(), mp + length, mp + length);

			if (nowBar == 0) {
				st.getRiffList1().add(addOnMevt);
				st.getRiffList1().add(addOffMevt);
			}
			else if (nowBar == 1) {
				st.getRiffList2().add(addOnMevt);
				st.getRiffList2().add(addOffMevt);

			}
			else if (nowBar == 2) {
				st.getRiffList3().add(addOnMevt);
				st.getRiffList3().add(addOffMevt);

			}
			else {
				st.getRiffList4().add(addOnMevt);
				st.getRiffList4().add(addOffMevt);

			}
		}
		catch (Exception e) {
			System.out.println("error");
			e.printStackTrace();
		}
	}

	public int getChord(long mp) {

		int[] ach = st.getArrayOfChord();
		int nowBar = (int) (mp / st.getMeasureLength());

		return st.getArrayOfChord()[(nowBar - 1)];

	}

	public void createBassLine() {
		try {
			st.getBassNoteList().clear();
			MidiEventWithTicktime onMidievt = null;
			MidiEventWithTicktime offMidievt = null;
			ShortMessage on = null;
			ShortMessage off = null;
			byte[] data;
			long mp;
			int count = 0;
			int minusBottomBass = 0;

			for (int i = 1; i <= 4; i++) {
				minusBottomBass = checkBottomOfBass(st.getNumberOfRiffList(i));
				for (int j = 0; j < st.getNumberOfRiffList(i).size(); j++) {
					mp = st.getNumberOfRiffList(i).get(j).music_position + (1920 * (i - 1));
					data = st.getNumberOfRiffList(i).get(j).getMessageInByteArray();

					// 毎回インスタンスしないとsetMessageが変わらない
					on = new ShortMessage();
					off = new ShortMessage();

					if (data[2] > 0) {
						on.setMessage(ShortMessage.NOTE_ON, 1, data[1] - minusBottomBass, 127);
						onMidievt = new MidiEventWithTicktime(on, mp, mp);
						st.getBassNoteList().add(onMidievt);

					}
					else {
						off.setMessage(ShortMessage.NOTE_OFF, 1, data[1] - minusBottomBass, 0);
						offMidievt = new MidiEventWithTicktime(off, mp, mp);
						st.getBassNoteList().add(offMidievt);
					}

				}

			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	int checkBottomOfBass(LinkedList<MidiEventWithTicktime> list) {
		HashMap<Integer, MidiEventWithTicktime> bottomOfmidievt = new HashMap<Integer, MidiEventWithTicktime>();
		MidiEventWithTicktime nowMevt = null;
		MidiEventWithTicktime bottomMevt = null;
		byte[] bottomData = null;
		byte[] nowData = null;
		long nowmp = 0;
		int nowBar = -1;
		int dist = 0;
		HashMap<Integer, Integer> bottomList = new HashMap<Integer, Integer>();
		
		for (int i = 0; i < 12; i++) {
			bottomList.put(i, i + 24);
		}
		

		for (int i = 0; i < list.size(); i++) {
			nowMevt = list.get(i);
			nowData = nowMevt.getMessageInByteArray();
			nowBar = (int) (nowmp / 1920);

			if (nowData[2] > 0) {
				nowmp = nowMevt.music_position;
				if (bottomMevt == null || nowData[1] < bottomData[1]) {
					bottomMevt = nowMevt;
					bottomData = nowData;
				}
			}

		}
		
		dist = bottomData[1] - bottomList.get(bottomData[1] % 12);
		return dist;
	}
		
		
	
}
