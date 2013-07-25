package research;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.sound.midi.MidiEvent;

import jp.crestmuse.cmx.amusaj.sp.MidiEventWithTicktime;

public final class Struct {
	private static  long measureLength;
	private static HashSet<Long> bassNoteONCollision = new HashSet<Long>();// ノートオンの場合ベースノートの有無の判断
	private static HashSet<Long> bassNoteOFFCollision = new HashSet<Long>();// ノートオフの場合のベースノートの有無の判断
	private static LinkedList<MidiEventWithTicktime> NoteList = new LinkedList<MidiEventWithTicktime>();// ノートリスト
	private static LinkedList<MidiEventWithTicktime> bassNoteList = new LinkedList<MidiEventWithTicktime>();// ベースノートリスト
	private static LinkedList<MidiEventWithTicktime> firstNoteList = new LinkedList<MidiEventWithTicktime>();// 最初の四小節のノートリスト
	private static LinkedList<MidiEventWithTicktime> revisedFirstNoteList = new LinkedList<MidiEventWithTicktime>(); // 修正したノートリスト
	private static LinkedList<MidiEventWithTicktime> offbassNoteList = new LinkedList<MidiEventWithTicktime>();// ベースノートリスト
	private static HashMap<Byte, Float> NotePrint = new HashMap<Byte, Float>();// ピアノロール描画のための播種テーブル
	private static HashMap<Byte, Float> bassNotePrint = new HashMap<Byte, Float>();
	private static HashMap<Byte, Float> revisedNotePrint = new HashMap<Byte, Float>();
	private static LinkedList<MidiEventWithTicktime> riffList1 = new LinkedList<MidiEventWithTicktime>();
	private static LinkedList<MidiEventWithTicktime> riffList2 = new LinkedList<MidiEventWithTicktime>();
	private static LinkedList<MidiEventWithTicktime> riffList3 = new LinkedList<MidiEventWithTicktime>();
	private static LinkedList<MidiEventWithTicktime> riffList4 = new LinkedList<MidiEventWithTicktime>();
	private static boolean btFlg = false;

	int[] chord = { 36, 41, 43, 45 };// コード進行
	String[] noteName = { "C", "C#", "Dm", "D#", "Em", "F", "F#", "G", "G#",
			"Am", "A#", "Bm-5" };// key=C,Amの場合の音

	public long getMeasureLength() {
		return measureLength;
	}

	public void setMeasureLength(long measureLength) {
		this.measureLength = measureLength;
	}

	public HashSet<Long> getBassNoteONCollision() {
		return bassNoteONCollision;
	}

	public void setBassNoteONCollision(HashSet<Long> bassNoteONCollision) {
		this.bassNoteONCollision = bassNoteONCollision;
	}

	public HashSet<Long> getBassNoteOFFCollision() {
		return bassNoteOFFCollision;
	}

	public void setBassNoteOFFCollision(HashSet<Long> bassNoteOFFCollision) {
		this.bassNoteOFFCollision = bassNoteOFFCollision;
	}

	public LinkedList<MidiEventWithTicktime> getNoteList() {
		return NoteList;
	}

	public void setNoteList(LinkedList<MidiEventWithTicktime> noteList) {
		NoteList = noteList;
	}

	public LinkedList<MidiEventWithTicktime> getBassNoteList() {
		return bassNoteList;
	}

	public void setBassNoteList(LinkedList<MidiEventWithTicktime> bassNoteList) {
		this.bassNoteList = bassNoteList;
	}

	public LinkedList<MidiEventWithTicktime> getOffbassNoteList() {
		return offbassNoteList;
	}

	public void setOffbassNoteList(
			LinkedList<MidiEventWithTicktime> offbassNoteList) {
		this.offbassNoteList = offbassNoteList;
	}

	public HashMap<Byte, Float> getNotePrint() {
		return NotePrint;
	}

	public void setNotePrint(HashMap<Byte, Float> notePrint) {
		NotePrint = notePrint;
	}

	public int getChord(int i) {
		return chord[i];
	}
	
	public int[] getArrayOfChord () {
		return chord;
	}

	public void setChord(int[] chord) {
		this.chord = chord;
	}

	public String[] getNoteName() {
		return noteName;
	}

	public void setNoteName(String[] noteName) {
		this.noteName = noteName;
	}

	public HashMap<Byte, Float> getBassNotePrint() {
		return bassNotePrint;
	}

	public void setBassNotePrint(HashMap<Byte, Float> bassNotePrint) {
		this.bassNotePrint = bassNotePrint;
	}

	

	public boolean isBtFlg() {
		return btFlg;
	}

	public void setBtFlg(boolean btFlg) {
		this.btFlg = btFlg;
	}

	public LinkedList<MidiEventWithTicktime> getRiffList1()
	{
		return riffList1;
	}

	public void setRiffList1(LinkedList<MidiEventWithTicktime> riffList1)
	{
		this.riffList1 = riffList1;
	}

	public LinkedList<MidiEventWithTicktime> getRiffList2()
	{
		return riffList2;
	}

	public void setRiffList2(LinkedList<MidiEventWithTicktime> riffList2)
	{
		this.riffList2 = riffList2;
	}

	public LinkedList<MidiEventWithTicktime> getRiffList3()
	{
		return riffList3;
	}

	public void setRiffList3(LinkedList<MidiEventWithTicktime> riffList3)
	{
		this.riffList3 = riffList3;
	}

	public LinkedList<MidiEventWithTicktime> getRiffList4()
	{
		return riffList4;
	}

	public void setRiffList4(LinkedList<MidiEventWithTicktime> riffList4)
	{
		this.riffList4 = riffList4;
	}
	public LinkedList<MidiEventWithTicktime> getNumberOfRiffList(int i) {
		if (i == 1) return riffList1;
		else if (i == 2) return riffList2;
		else if (i == 3) return riffList3;
		else return riffList4;
	}
	
	
	public LinkedList<MidiEventWithTicktime> getFirstNoteList()
	{
		return firstNoteList;
	}

	public void setFirstNoteList(LinkedList<MidiEventWithTicktime> firstNoteList)
	{
		this.firstNoteList = firstNoteList;
	}

	public LinkedList<MidiEventWithTicktime> getRevisedFirstNoteList() {
		return revisedFirstNoteList;
	}

	public void setRevisedFirstNoteList(
			LinkedList<MidiEventWithTicktime> revisedFirstNoteList) {
		this.revisedFirstNoteList = revisedFirstNoteList;
	}

	public HashMap<Byte, Float> getRevisedNotePrint() {
		return revisedNotePrint;
	}

	public void setRevisedNotePrint(HashMap<Byte, Float> revisedNotePrint) {
		this.revisedNotePrint = revisedNotePrint;
	}
	

}
