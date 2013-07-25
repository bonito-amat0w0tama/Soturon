package research;
import java.io.File;
import java.io.IOException;
import java.util.*;

import jp.crestmuse.cmx.processing.*;
import jp.crestmuse.cmx.amusaj.sp.*;
import jp.crestmuse.cmx.sound.*;
import javax.sound.midi.*;

import processing.core.*;

public class Sequence_manager implements TickTimer {

	private int tpb;
	private int bpm;
	private int bar;
	private Sequencer seqr = null;
	public Sequence seq = null;
	private int note_position;

	private long previous_note_gap;
	// private MidiEventWithTicktime[] note_list;
	private int sizeOfNote = -1;
	private long premp = -1;

	private int nn;
	private long preGap;
	Track bt = null;
	Track met = null;
	Struct st = null;
	CMXController cmx = null;
	OutputBassNote obn;

	public Sequence_manager(int t, int bp, int ba, Struct pst,
			CMXController pcmx) {
		try {
			setTpb(t);
			bpm = bp;
			bar = ba;

			st = pst;
			cmx = pcmx;

			st.setMeasureLength(getTpb() * 4);
			previous_note_gap = 0;
			// note_list = new MidiEventWithTicktime[4];
			nn = 0;
			// bass_instrument = 34;

			seqr = MidiSystem.getSequencer();
			seq = new Sequence(Sequence.PPQ, getTpb());
			seqr.setSequence(seq);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * public void setTpb_bpm_bar(int t, int bp, int ba) { tpb = t; bpm = bp; bar = ba; }
	 */

	// メトロノームのトラックを生成、追加
	public void dmetronome() {
		try {

			// Track met = seq.createTrack();
			met = seq.createTrack();
			bt = seq.createTrack();

			LinkedList<MidiEventWithTicktime> count = pickoutMidiMessegeOfSmf(new File("../midis/count.mid"), 11, 0);
			LinkedList<MidiEventWithTicktime> drm = pickoutMidiMessegeOfSmf(new File("../midis/doremi2.mid"), 11, 0);

			for (int i = 0; i < count.size(); i++) {
				byte[] data = count.get(i).getMessageInByteArray();
				ShortMessage smsg = new ShortMessage();
				smsg.setMessage(data[0], data[1], data[2]);

				met.add(new MidiEvent(smsg, count.get(i).music_position));

			}
			for (int i = 0; i < drm.size(); i++) {
				byte[] data = drm.get(i).getMessageInByteArray();
				ShortMessage smsg = new ShortMessage();
				smsg.setMessage(data[0], data[1], data[2]);

				for (int j = 0; j < 4 * bar; j++) {

					met.add(new MidiEvent(smsg, drm.get(i).music_position + 1920 + ((st.getMeasureLength() * 4) * j)));
				}
			}

			/*
			 * //トラックの表示 Track t = met; for (int i = 0; i < t.size(); i++) { MidiEvent e = t.get(i); byte[] m = e.getMessage().getMessage(); System.out.println(e.getTick() + ", " + m[1] + ", " +
			 * m[2]); }
			 */

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * public void metronome() { try {
	 * 
	 * // Track met = seq.createTrack(); met = seq.createTrack(); bt = seq.createTrack(); // 最初の小節 ShortMessage firstOn = new ShortMessage(); firstOn.setMessage(ShortMessage.NOTE_ON, 9, 42, 127);
	 * ShortMessage firstOff = new ShortMessage(); firstOff.setMessage(ShortMessage.NOTE_OFF, 9, 42, 0);
	 * 
	 * // 1拍目 ShortMessage on1 = new ShortMessage(); on1.setMessage(ShortMessage.NOTE_ON, 9, 46, 127); ShortMessage off1 = new ShortMessage(); off1.setMessage(ShortMessage.NOTE_OFF, 9, 46, 0);
	 * 
	 * // 2,3,4拍目 ShortMessage on2 = new ShortMessage(); on2.setMessage(ShortMessage.NOTE_ON, 9, 42, 127); ShortMessage off2 = new ShortMessage(); off2.setMessage(ShortMessage.NOTE_OFF, 9, 42, 0);
	 * 
	 * for (int i = 0; i < 4 * bar; i++) { note_position = getTpb() * i; if (i < 4) { met.add(new MidiEvent(firstOn, note_position)); met.add(new MidiEvent(firstOff, note_position)); } else if (i >= 4
	 * && i % 4 == 0) { met.add(new MidiEvent(on1, note_position)); met.add(new MidiEvent(off1, note_position)); } else { met.add(new MidiEvent(on2, note_position)); met.add(new MidiEvent(off2,
	 * note_position)); } }
	 * 
	 * } catch (Exception e) { e.printStackTrace(); } }
	 */

	public void newCreateBassTrack() {
		try {

			long nowTick = 0;
			long mp;
			byte[] data;
			ShortMessage smsg;

			for (int i = 0; i < bt.size(); i++) {
				MidiEvent evt = bt.get(i);
				bt.remove(evt);
			}
 
			// Chnge instrument
			ShortMessage message = new ShortMessage();
			message.setMessage(ShortMessage.PROGRAM_CHANGE, 1, 37, 0);
			bt.add(new MidiEvent(message, nowTick + 240));

			for (int i = 0; i < st.getBassNoteList().size(); i++) {
				data = st.getBassNoteList().get(i).getMessageInByteArray();
				smsg = new ShortMessage();
				if (data[2] > 0) {
					smsg.setMessage(ShortMessage.NOTE_ON, 1, data[1], data[2]);
				}
				else {
					smsg.setMessage(ShortMessage.NOTE_OFF, 1, data[1], 0);
				}
				for (int j = 0; j < 4 * bar; j++) {
					mp = st.getBassNoteList().get(i).music_position + 1920 + (st.getMeasureLength() * 4) * j; // 1920 カウント分
					if (mp >= nowTick)
						bt.add(new MidiEvent(smsg, mp));
				}

			}

			// btトラックの表示

			/*
			 * Track t = bt; for (int i = 0; i < t.size(); i++) { MidiEvent e = t.get(i); byte[] m = e.getMessage().getMessage(); System.out.println(e.getTick() + ", " + m[1] + ", " ); }
			 */

		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void play() {
		try {
			seqr.open();
			seqr.setTempoInBPM(bpm); // open後さらにstart前でなくればsetTempoInBPMできない
			// seqr.start();

			cmx.smfread(seq);

			// cmx.playMusic();

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public LinkedList<MidiEventWithTicktime> pickoutMidiMessegeOfSmf(File f,
			int trcNmb, int startMeasure) {
		try {
			int endMeasure = startMeasure + 4;
			Sequence seq = MidiSystem.getSequence(f);
			LinkedList<MidiEventWithTicktime> midimsgList = new LinkedList<MidiEventWithTicktime>();
			Track trc = seq.getTracks()[trcNmb];
			HashMap<Byte, MidiEventWithTicktime> tmpOnMevt = new HashMap<Byte, MidiEventWithTicktime>();
			MidiEventWithTicktime nowMevt = null;
			MidiEventWithTicktime addOnMevt = null;
			MidiEventWithTicktime addOffMevt = null;
			byte[] data = null;
			long nowMp = -1;
			long addMp = -1;
			ShortMessage onSmsg = null;
			ShortMessage offSmsg = null;

			for (int i = 0; i < trc.size(); i++) {
				nowMp = trc.get(i).getTick() - 1920;
				nowMevt = new MidiEventWithTicktime(trc.get(i).getMessage(), nowMp, nowMp);
				data = nowMevt.getMessageInByteArray();

				if (startMeasure * 1920 <= nowMp && nowMp <= endMeasure * 1920) {
					// when nowMp is out of end, End this method

					// Pattern of rythemTrack
					if (trcNmb == 11) {
						if (data[0] == -103 || data[0] == -119) {
							addOnMevt = new MidiEventWithTicktime(nowMevt.getMessage(), nowMp % 7680, nowMp % 7680);
							midimsgList.add(addOnMevt);
						}
					}
					else {
						if (data[0] == -112 && data[2] > 0) {
							tmpOnMevt.put(data[1], nowMevt);
						}
						else if (data[0] == -128 || (data[0] == -112 && data[0] == 0)) {
							try {
								addMp = nowMp - tmpOnMevt.get(data[1]).music_position;

								onSmsg = new ShortMessage();
								onSmsg.setMessage(ShortMessage.NOTE_ON, 0, data[1], tmpOnMevt.get(data[1]).data1());
								addOnMevt = new MidiEventWithTicktime(onSmsg, tmpOnMevt.get(data[1]).music_position % 7680, tmpOnMevt.get(data[1]).music_position % 7680);

								offSmsg = new ShortMessage();
								offSmsg.setMessage(ShortMessage.NOTE_OFF, 0, data[1], 0);
								addOffMevt = new MidiEventWithTicktime(offSmsg, tmpOnMevt.get(data[1]).music_position % 7680 + addMp, tmpOnMevt.get(data[1]).music_position % 7680 + addMp);

								midimsgList.add(addOnMevt);
								midimsgList.add(addOffMevt);
							}
							catch (Exception e) {
								e.printStackTrace();
								System.out.println("ErrorOfReadNonteOnOfNoteOff");
							}
						}
					}
				}
			}

			// Print the midimsgList
			for (int i = 0; i < midimsgList.size(); i++) {
				MidiEventWithTicktime m = midimsgList.get(i);

				byte[] d = m.getMessageInByteArray();

				System.out.println(d[0] + " " + d[1] + " " + d[2] + " " + m.music_position);
			}

			return midimsgList;

		}
		catch (IOException e) {
			System.out.println();
			e.printStackTrace();
			return null;
		}
		catch (InvalidMidiDataException e) {

			e.printStackTrace();
			return null;
		}
	}

	public void deleteBass() {
		for (int i = 0; i < bt.size(); i++) {
			MidiEvent evt = bt.get(i);
			bt.remove(evt);
		}
		st.getBassNoteList().clear();

	}

	// 以下setter, getter
	public void setTempo() {
		seqr.setTempoInBPM(bpm);
	}

	public int getTicksPerBeat() {
		// return getTpb();
		return cmx.getTicksPerBeat();
	}

	public long getTickPosition() {
		// return seqr.getTickPosition();
		return cmx.getTickPosition();
	}

	public long getMetroTickPosition() {
		return note_position;
	}

	public long getNowBar() {
		return (cmx.getTickPosition() / st.getMeasureLength());
	}

	public long getNowPosition() {
		return ((cmx.getTickPosition() % st.getMeasureLength()) / (getTpb() / 4));
	}

	public int getTpb() {
		return tpb;
	}

	public void setTpb(int tpb) {
		this.tpb = tpb;
	}
}
