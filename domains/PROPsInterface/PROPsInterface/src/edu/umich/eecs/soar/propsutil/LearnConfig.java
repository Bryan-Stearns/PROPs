package edu.umich.eecs.soar.propsutil;

public class LearnConfig {
	private boolean addresses,
				    proposals,
				    autos,
				    conditions,
				    spreading,
				    manual,
				    seqs;
	private int chunkThreshold = 2;
	
	public LearnConfig(boolean addresses, boolean proposals, boolean autos, boolean conditions, boolean spreading, boolean manual, boolean seqs) {
		this.addresses = addresses;
		this.proposals = proposals;
		this.autos = autos;
		this.conditions = conditions;
		this.spreading = spreading;
		this.manual = manual;
		this.seqs = seqs;
	}
	public LearnConfig(String str) {
		if (str.contains("1")) {
			this.addresses = true;
		}
		if (str.contains("2")) {
			this.proposals = true;
		}
		if (str.contains("3")) {
			this.autos = true;
		}
		if (str.contains("c")) {
			this.conditions = true;
		}
		if (str.contains("s")) {
			this.spreading = true;
		}
		if (str.contains("m")) {
			this.manual = true;
		}
		if (str.contains("q")) {
			this.seqs = true;
		}
	}
	public LearnConfig(String str, int t) {
		this(str);
		chunkThreshold = t;
	}
	
	public void set(boolean addresses, boolean proposals, boolean autos, boolean conditions, boolean spreading, boolean manual, boolean seqs) {
		this.addresses = addresses;
		this.proposals = proposals;
		this.autos = autos;
		this.conditions = conditions;
		this.spreading = spreading;
		this.manual = manual;
		this.seqs = seqs;
	}
	
	public void setChunkThreshold(int t) { chunkThreshold = t; }
	
	public boolean learnsAddresses() { return addresses; }
	public boolean learnsProposals() { return proposals; }
	public boolean learnsAutos() { return autos; }
	public boolean learnsConditions() { return conditions; }
	public boolean spreading() { return spreading; }
	public boolean manual() { return manual; }
	public boolean seqs() { return seqs; }
	public int getChunkThreshold() { return chunkThreshold; }
	
	@Override
	public String toString() {
		String retVal = "";
		if (addresses) {retVal += "1";}
		if (proposals) {retVal += "2";}
		if (autos) {retVal += "3";}
		if (spreading) {retVal += "s";}
		if (manual) {retVal += "m";}
		if (conditions) {retVal += "c";}
		if (seqs) {retVal += "q";}
		retVal += "_t" + chunkThreshold;
		return retVal;
	}
}
