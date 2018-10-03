package edu.umich.eecs.soar.propsutil;

public class LearnConfig {
	private boolean addresses,
				    proposals,
				    autos,
				    conditions,
				    spreading,
				    manual,
				    seqs,
				    addressChunks;
	private int chunkThreshold = 2;

	/**
	 * Initialize a learning configuration. 
	 * Learn flags:
	 *   'addresses'     - Learn memory addresses (source address chunks if not enabled)
	 *   'proposals'     - Learn hierarchical PROP composition
	 *   'autos'         - Learn fully proceduralized (autonomous) rules for each PROP instruction set
	 *   'conditions'    - Learn condition spread chunks from scratch (source condition chunks if not enabled)
	 *   'spreading'     - Learn chunks that cause spread from true conditions to their instructions.
	 *                     This flag also causes the agent to start each fetch with free spread-based recall.
	 *   'manual'        - Use a manual fetch ordering for training the task (source the fetch sequence file)
	 *   'seq'           - If using 'm', also learn to update the sequence pointer after a correct free recall. 
	 *   'addressChunks' - Learn memory addressing chunks (should only be used for generating the source file for '1')
	 */
	public LearnConfig(boolean addresses, boolean proposals, boolean autos, boolean conditions, boolean spreading, boolean manual, boolean seqs, boolean addressChunks) {
		set(addresses, proposals, autos, conditions, spreading, manual, seqs, addressChunks);
	}

	/**
	 * Initialize a learning configuration. 
	 * Learn flags:
	 *   '1' - Learn memory addresses (source address chunks if not enabled)
	 *   '2' - Learn hierarchical PROP composition
	 *   '3' - Learn fully proceduralized (autonomous) rules for each PROP instruction set
	 *   'a' - Learn memory addressing chunks (should only be used for generating the source file for '1')
	 *   's' - Learn chunks that cause spread from true conditions to their instructions.
	 *         This flag also causes the agent to start each fetch with free spread-based recall.
	 *   'c' - Learn condition spread chunks from scratch (source condition chunks if not enabled)
	 *   'm' - Use a manual fetch ordering for training the task (source the fetch sequence file)
	 *   'q' - If using 'm', also learn to update the sequence pointer after a correct free recall. 
	 */
	public LearnConfig(String str) {
		set(str);
	}
	/**
	 * Initialize a learning configuration. 
	 * Learn flags:
	 *   '1' - Learn memory addresses (source address chunks if not enabled)
	 *   '2' - Learn hierarchical PROP composition
	 *   '3' - Learn fully proceduralized (autonomous) rules for each PROP instruction set
	 *   'a' - Learn memory addressing chunks (should only be used for generating the source file for '1')
	 *   's' - Learn chunks that cause spread from true conditions to their instructions.
	 *         This flag also causes the agent to start each fetch with free spread-based recall.
	 *   'c' - Learn condition spread chunks from scratch (source condition chunks if not enabled)
	 *   'm' - Use a manual fetch ordering for training the task (source the fetch sequence file)
	 *   'q' - If using 'm', also learn to update the sequence pointer after a correct free recall.  
	 * @param str A string of learning mode flags
	 * @param t The chunking threshold
	 */
	public LearnConfig(String str, int t) {
		this(str);
		chunkThreshold = t;
	}

	/**
	 * Reset the learning configuration. 
	 * Learn flags:
	 *   '1' - Learn memory addresses (source address chunks if not enabled)
	 *   '2' - Learn hierarchical PROP composition
	 *   '3' - Learn fully proceduralized (autonomous) rules for each PROP instruction set
	 *   'a' - Learn memory addressing chunks (should only be used for generating the source file for '1')
	 *   's' - Learn chunks that cause spread from true conditions to their instructions.
	 *         This flag also causes the agent to start each fetch with free spread-based recall.
	 *   'c' - Learn condition spread chunks from scratch (source condition chunks if not enabled)
	 *   'm' - Use a manual fetch ordering for training the task (source the fetch sequence file)
	 *   'q' - If using 'm', also learn to update the sequence pointer after a correct free recall.
	 * @param str The string of learning mode flags
	 */
	public void set(String str) {
		if (str.contains("1")) {this.addresses = true;} else {this.addresses = false;}
		if (str.contains("2")) {this.proposals = true;} else {this.proposals = false;}
		if (str.contains("3")) {this.autos = true;} else {this.autos = false;}
		if (str.contains("a")) {this.addressChunks = true;} else {this.addressChunks = false;}
		if (str.contains("s")) {this.spreading = true;} else {this.spreading = false;}
		if (str.contains("c")) {this.conditions = true;} else {this.conditions = false;}
		if (str.contains("m")) {this.manual = true;} else {this.manual = false;}
		if (str.contains("q")) {this.seqs = true;} else {this.seqs = false;}
	}
	
	/**
	 * Reset the learning configuration. 
	 * Learn flags:
	 *   'addresses'     - Learn memory addresses (source address chunks if not enabled)
	 *   'proposals'     - Learn hierarchical PROP composition
	 *   'autos'         - Learn fully proceduralized (autonomous) rules for each PROP instruction set
	 *   'conditions'    - Learn condition spread chunks from scratch (source condition chunks if not enabled)
	 *   'spreading'     - Learn chunks that cause spread from true conditions to their instructions.
	 *                     This flag also causes the agent to start each fetch with free spread-based recall.
	 *   'manual'        - Use a manual fetch ordering for training the task (source the fetch sequence file)
	 *   'seq'           - If using 'm', also learn to update the sequence pointer after a correct free recall. 
	 *   'addressChunks' - Learn memory addressing chunks (should only be used for generating the source file for '1')
	 */
	public void set(boolean addresses, boolean proposals, boolean autos, boolean conditions, boolean spreading, boolean manual, boolean seqs, boolean addressChunks) {
		this.addresses = addresses;
		this.proposals = proposals;
		this.autos = autos;
		this.conditions = conditions;
		this.spreading = spreading;
		this.manual = manual;
		this.seqs = seqs;
		this.addressChunks = addressChunks;
	}
	
	public void setChunkThreshold(int t) { chunkThreshold = t; }
	
	public boolean learnsAddresses() { return addresses; }
	public boolean learnsProposals() { return proposals; }
	public boolean learnsAutos() { return autos; }
	public boolean learnsAllConditions() { return conditions; }
	public boolean learnsSpreading() { return spreading; }
	public boolean usesManual() { return manual; }
	public boolean learnsManualSeqs() { return seqs; }
	public boolean learnsAddressChunks() { return addressChunks; }
	public int getChunkThreshold() { return chunkThreshold; }
	
	@Override
	public String toString() {
		String retVal = "";
		if (addresses) {retVal += "1";}
		if (proposals) {retVal += "2";}
		if (autos) {retVal += "3";}
		if (addressChunks) {retVal += "a";}
		if (spreading) {retVal += "s";}
		if (conditions) {retVal += "c";}
		if (manual) {retVal += "m";}
		if (seqs) {retVal += "q";}
		retVal += "_t" + chunkThreshold;
		return retVal;
	}
}
