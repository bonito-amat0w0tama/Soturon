package research;
import java.util.*;
import jp.crestmuse.cmx.processing.*;
import jp.crestmuse.cmx.amusaj.sp.*;
import jp.crestmuse.cmx.sound.*;
import javax.sound.midi.*;
import processing.core.*;

public class PrintModule extends SPModule {
	Sequence_manager sm = null;
	CMXController cmx = null;
	PrintModule(Sequence_manager psm, CMXController pcmx) {
		sm = psm;
		cmx = pcmx;
	}

	public void execute(Object[] src, TimeSeriesCompatible[] dest)
			throws InterruptedException {
		MidiEventWithTicktime midievt = (MidiEventWithTicktime) src[0];
		// String[] mm = {"Status", "NoteNumber", "Velocity", "Music_Positio"};
		byte[] data = midievt.getMessageInByteArray();
		// ノートオンの場合のみ
		if (((data[0] & 0xF0) == 0x90) || (data[0] & 0xF0) == 0x9F || data[0] == -65){// && data[2] > 0) {
			System.out.println(data[0] + " " + data[1] + " " + data[2] + " "
					+ midievt.music_position);
			/*System.out.println("Tick : " + sm.getTickPosition()
					% (sm.getTicksPerBeat() / 4) + "\nbar : " + sm.getNowBar()
					+ "\nposition : " + sm.getNowPosition() + "\n");*/
		}
		dest[0].add(midievt);
	}

	public Class[] getInputClasses() {
		return new Class[] { MidiEventWithTicktime.class };
	}

	public Class[] getOutputClasses() {
		return new Class[] { MidiEventWithTicktime.class };
	}

}
