package edu.umich.eecs.soar.props.editors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.List;

public class ETask {
	public int line_pos, cursor_pos;
	public String[] vlist;
	public String[] old_vlist;
	public boolean vlist_changed;
	public List<String> line;
	public List<String[]> edits;
	public List<List<String>> text;
	
	ETask() {
		this.line_pos = 0;
		this.cursor_pos = 0;
	}
	
	public void init() {
		line_pos = 0;
		cursor_pos = 0;
		vlist = new String[]{"","","",""};
		vlist_changed = false;
		
		text = new ArrayList<List<String>>(20);
		text.add(new LinkedList<String>(Arrays.asList("Geestelijk vader van de tovenaarsleerling JK Rowling lanceert morgen de site pottermorecom".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("Rowling laat de pers in het duister tasten over de inhoud ervan Alleen een paar van de grootste".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("fans mochten een blik op de inhoud werpen onder voorwaarde van strikte geheimhouding".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("Ondertussen wordt er gespeculeerd over de inhoud van de website".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("nu is alleen nog een slome voorpagina te zien Fans snakken naar nieuwe avonturen".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("van het ongekend populaire personage Maar volgens een woordvoerder zal op de".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("site geen nieuw boek worden aangekondigd Dus wordt er gefluisterd over online spelletjes".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("ebooks en sociale medi".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("De zeven Potter boeken zijn wereldwijd meer dan 450 miljoen keer over de toonbank gegaan".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("Samen met de recordopbrengsten van de filmreeks hebben ze Rowling tot de rijkste auteur".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("ter wereld gemaakt".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("Op dit moment staat de laatste film in de serie op het punt om in de bioscoop".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("te komen De verwachtingen zijn hooggespannen Zal deze keer de Harry Potter".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("film eindelijk een Oscar krijgen net als bij het laatste deel van de lord".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("of the rings trilogie".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("We all know what happened in the end but".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("Of zullen de leden van de academy voorbijgaan aan dit hollywood".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("kassucces De spanning is daarom groot dit jaar".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList("Oscar of niet Rowling zal er niet om rouwen want de buit is al binnen".split("\\s"))));
		text.add(new LinkedList<String>(Arrays.asList(new String[]{""})));
	}
	
	public boolean set_vlist(String v1, String v2, String v3, String v4) {
		old_vlist = new String[]{vlist[0], vlist[1], vlist[2], vlist[3]};
		vlist = new String[]{v1, v2, v3, v4};
		vlist_changed = !Arrays.equals(vlist, old_vlist);
		return vlist_changed;
	}
	public boolean set_vlist(String[] list) {
		old_vlist = new String[]{vlist[0], vlist[1], vlist[2], vlist[3]};
		vlist = list;
		vlist_changed = !Arrays.equals(vlist, old_vlist);
		return vlist_changed;
	}
	
	public boolean vlist_changed() {return vlist_changed;}
}
