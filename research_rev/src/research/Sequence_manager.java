package research;

import java.io.File;
import java.io.IOException;
import java.util.*;

import jp.crestmuse.cmx.processing.*;
import jp.crestmuse.cmx.amusaj.sp.*;
import jp.crestmuse.cmx.sound.*;
import javax.sound.midi.*;

import processing.core.*;

public final class Sequence_manager implements TickTimer {

	private int tpb;
	private int bpm;
	private int bar;
	public Sequence seq = null;
	private int note_position;

	private int sizeOfNote = -1;
	private long premp = -1;

	private int nn;
	private long preGap;
	private Track bt = null;
	private Track met = null;
	private Struct st = null;
	private CMXController cmx = null;
	private OutputBassNote obn = null;

	public Sequence_manager(int t, int bp, int ba, Struct pst, CMXController pcmx) {
		try {
			setTpb(t);
			bpm = bp;
			bar = ba;

			st = pst;
			cmx = pcmx;

			st.setMeasureLength(getTpb() * 4);
			seq = new Sequence(Sequence.PPQ, getTpb());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// メトロノームのトラックを生成、追加
	public void dmetronome() {
		try {
			met = seq.createTrack();
			bt = seq.createTrack();

			final LinkedList<MidiEventWithTicktime> count = pickoutMidiMessegeOfSmf(new File("/home/masaki/Dropbox/midis/count.mid"), 11, 0, 1);
			final LinkedList<MidiEventWithTicktime> bassDrum = pickoutMidiMessegeOfSmf(new File("/home/masaki/Dropbox/midis/4bassDrum.mid"), 11, 0, 4);
			final LinkedList<MidiEventWithTicktime> drm = pickoutMidiMessegeOfSmf(new File("/home/masaki/Dropbox/midis/doremi2.mid"), 11, 0, 4);

			for (int i = 0; i < count.size(); i++) {
				byte[] data = count.get(i).getMessageInByteArray();
				ShortMessage smsg = new ShortMessage();
				smsg.setMessage(data[0], data[1], data[2]);
				MidiEvent midievt = new MidiEvent(smsg, count.get(i).music_position);
				met.add(midievt);

			}
			for (int i = 0; i < bassDrum.size(); i++) {
				byte[] data = bassDrum.get(i).getMessageInByteArray();
				ShortMessage smsg = new ShortMessage();
				smsg.setMessage(data[0], data[1], data[2]);
				MidiEvent midievt = new MidiEvent(smsg, bassDrum.get(i).music_position + 1920);
				met.add(midievt);

			}
			for (int i = 0; i < drm.size(); i++) {
				byte[] data = drm.get(i).getMessageInByteArray();
				ShortMessage smsg = new ShortMessage();
				smsg.setMessage(data[0], data[1], data[2]);
				long measureLength = st.getMeasureLength();
				for (int j = 0; j < 4 * bar; j++) {
					long mp = drm.get(i).music_position + ((measureLength * 4) * j) + (measureLength * 5);

					met.add(new MidiEvent(smsg, mp));
				}
			}

			/*
			 * //トラックの表示 Track t = met; for (int i = 0; i < t.size(); i++) {
			 * MidiEvent e = t.get(i); byte[] m = e.getMessage().getMessage();
			 * System.out.println(e.getTick() + ", " + m[1] + ", " + m[2]); }
			 */

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// MEMO:まだテストしていない
	public void createBassTrack(LinkedList<MidiEventWithTicktime> inputBassNoteList) {
		try {
			// Initialize the btTrack
			for (int i = 0; i < bt.size(); i++) {
				MidiEvent evt = bt.get(i);
				bt.remove(evt);
			}
			
			this.deleteBassTrack();

			long nowTick = cmx.getTickPosition(); // 呼び出された時のtick

			// Change the instrument of bassTrack
			ShortMessage message = new ShortMessage();
			message.setMessage(ShortMessage.PROGRAM_CHANGE, 1, 37, 0);
			bt.add(new MidiEvent(message, nowTick + 240));

			for (int i = 0; i < inputBassNoteList.size(); i++) {
				MidiEventWithTicktime nowMidievt = inputBassNoteList.get(i);
				byte[] data = nowMidievt.getMessageInByteArray();
				ShortMessage smsg = new ShortMessage();
				if (data[2] > 0) {
					smsg.setMessage(ShortMessage.NOTE_ON, 1, data[1], data[2]);
				} else {
					smsg.setMessage(ShortMessage.NOTE_OFF, 1, data[1], 0);
				}
				
				for (int j = 0; j < 4 * bar; j++) {
					long mp = nowMidievt.getTick() + 1920 + (st.getMeasureLength() * 4) * j; // 1920カウント分
					if (mp >= nowTick) { // 呼び出されたTicK以降のトラックを作る
						bt.add(new MidiEvent(smsg, mp));
					}
				}

			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void createMainBassTrack(LinkedList<MidiEventWithTicktime> inputBassNoteList) {
		try {
			// Initialize the btTrack
			for (int i = 0; i < bt.size(); i++) {
				MidiEvent evt = bt.get(i);
				bt.remove(evt);
			}
			
			this.deleteBassTrack();

			long nowTick = cmx.getTickPosition(); // 呼び出された時のtick

			// Change the instrument of bassTrack
			ShortMessage message = new ShortMessage();
			message.setMessage(ShortMessage.PROGRAM_CHANGE, 1, 37, 0);
			bt.add(new MidiEvent(message, nowTick + 240));

			for (int i = 0; i < inputBassNoteList.size(); i++) {
				MidiEventWithTicktime nowMidievt = inputBassNoteList.get(i);
				byte[] data = nowMidievt.getMessageInByteArray();
				ShortMessage smsg = new ShortMessage();
				if (data[2] > 0) {
					smsg.setMessage(ShortMessage.NOTE_ON, 1, data[1], data[2]);
				} else {
					smsg.setMessage(ShortMessage.NOTE_OFF, 1, data[1], 0);
				}
				
				for (int j = 0; j < 4 * bar; j++) {
					long mp = nowMidievt.getTick() + 1920 + (st.getMeasureLength() * 4) * j; // 1920カウント分
					if (mp >= nowTick) { // 呼び出されたTicK以降のトラックを作る
						bt.add(new MidiEvent(smsg, mp));
					}
				}

			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void play() {
		try {
			cmx.smfread(seq);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// MEMO: まだテストしていない
	private void printTrack(Track trc) {
		for (int i = 0; i < trc.size(); i++) {
			MidiEventWithTicktime nowMidievt = (MidiEventWithTicktime) trc.get(i);
			byte[] nowData = nowMidievt.getMessageInByteArray();
			System.out.println("Status\t" + nowData[0] + "\tNoteNumber\t" + nowData[1] + "\tVelocity\t" + nowData[2] + "\tTick\t" + nowMidievt.getTick());
			//byte[] m = e.getMessage().getMessage();
			//System.out.println(e.getTick() + ", " + m[1] + ", ");
		}
	
	}

	public LinkedList<MidiEventWithTicktime> pickoutMidiMessegeOfSmf(File f, int trcNmb, int startMeasure, int endMeasure) {
		try {
			Sequence seq = MidiSystem.getSequence(f);
			Track trc = seq.getTracks()[trcNmb];
			LinkedList<MidiEventWithTicktime> distMidimsgList = new LinkedList<MidiEventWithTicktime>();

			HashMap<Byte, MidiEventWithTicktime> tmpOnMevt = new HashMap<Byte, MidiEventWithTicktime>();
			for (int i = 0; i < trc.size(); i++) {
				long nowMp = trc.get(i).getTick() - 1920;
				MidiEventWithTicktime nowMevt = new MidiEventWithTicktime(trc.get(i).getMessage(), nowMp, nowMp);
				byte[] data = nowMevt.getMessageInByteArray();
				// int endMeasure = startMeasure + 4;

				if (startMeasure * 1920 <= nowMp && nowMp <= endMeasure * 1920) {

					// Pattern of rythemTrack
					if (trcNmb == 11) {
						if (data[0] == -103 || data[0] == -119) {
							MidiEventWithTicktime addOnMevt = new MidiEventWithTicktime(nowMevt.getMessage(), nowMp % 7680, nowMp % 7680);
							distMidimsgList.add(addOnMevt);
						}
					} else {
						if (data[0] == -112 && data[2] > 0) {
							tmpOnMevt.put(data[1], nowMevt);
						} else if ((data[0] == -128 && data[2] == 0) || (data[0] == -112 && data[2] == 0)) {

							if (tmpOnMevt.containsKey(data[1]) == true) {
								long length = nowMp - tmpOnMevt.get(data[1]).music_position;

								ShortMessage onSmsg = new ShortMessage();
								onSmsg.setMessage(ShortMessage.NOTE_ON, 0, data[1], 100);
								MidiEventWithTicktime addOnMevt = new MidiEventWithTicktime(onSmsg, tmpOnMevt.get(data[1]).music_position % 7680, tmpOnMevt.get(data[1]).music_position % 7680);

								ShortMessage offSmsg = new ShortMessage();
								offSmsg.setMessage(ShortMessage.NOTE_OFF, 0, data[1], 0);
								MidiEventWithTicktime addOffMevt = new MidiEventWithTicktime(offSmsg, tmpOnMevt.get(data[1]).music_position % 7680 + length, tmpOnMevt.get(data[1]).music_position % 7680 + length);

								distMidimsgList.add(addOnMevt);
								distMidimsgList.add(addOffMevt);

							}

						}
					}
				}
			}

			// Print the midimsgList
			/*for (int i = 0; i < distMidimsgList.size(); i++) {
				MidiEventWithTicktime m = distMidimsgList.get(i);

				byte[] d = m.getMessageInByteArray();

				System.out.println(d[0] + " " + d[1] + " " + d[2] + " " + m.music_position);
			}
			*/
			return distMidimsgList;

		} catch (Exception e) {
			System.out.println("ReadSMF Error {");
			e.printStackTrace();
			return null;
		}
	}
	public void deleteBassTrack() {
		for (int i = 0; i < bt.size(); i++) {
			MidiEvent evt = bt.get(i);
			bt.remove(evt);
		}
	}

	// 以下setter, getter

	public int getTicksPerBeat() {
		return cmx.getTicksPerBeat();
	}

	public long getTickPosition() {
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
