package edu.umich.eecs.soar.propsutil;

import java.util.ArrayList;

public class LearnConfig {
	private boolean cognitive,
					associative,
				    auto,
				    spreading;
	private int chunkThreshold = 2;
	public ArrayList<String> commands = null;
	private String commandName = "";
	

	/**
	 * Initialize a learning configuration. 
	 * Learn flags:
	 *   'cognitive'     - Learn cognitive phase: choosing decisions appropriately for task structure
	 *   'associative'   - Learn associative phase: hierarchical PROP composition
	 *   'auto'          - Learn autonomous phase: fully proceduralized (autonomous) rules for each PROP instruction set
	 *   'spreading'     - Enable spreading to bias fetching 
	 */
	public LearnConfig(boolean cognitive, boolean associative, boolean auto, boolean spreading) {
		set(cognitive, associative, auto, spreading);
	}

	/**
	 * Initialize a learning configuration. 
	 * Learn flags:
	 *   '1' - Learn cognitive phase: choosing decisions appropriately for task structure
	 *   '2' - Learn associative phase: hierarchical PROP composition
	 *   '3' - Learn autonomous phase: fully proceduralized (autonomous) rules for each PROP instruction set
	 *   's' - Enable spreading to bias fetching 
	 */
	public LearnConfig(String str) {
		set(str);
	}
	/**
	 * Initialize a learning configuration. 
	 * Learn flags:
	 *   '1' - Learn cognitive phase: choosing decisions appropriately for task structure
	 *   '2' - Learn associative phase: hierarchical PROP composition
	 *   '3' - Learn autonomous phase: fully proceduralized (autonomous) rules for each PROP instruction set
	 *   's' - Enable spreading to bias fetching 
	 * @param str A string of learning mode flags
	 * @param t The chunking threshold
	 * @param cmds A ordered list of any extra Soar commands to issue the agent after creation
	 * @param cmdName What to insert into the agent output file name corresponding to these commands
	 */
	public LearnConfig(String str, int t, ArrayList<String> cmds, String cmdName) {
		this(str);
		chunkThreshold = t;
		this.commands = cmds;
		this.commandName = cmdName;
	}
	/**
	 * Initialize a learning configuration. 
	 * Learn flags:
	 *   '1' - Learn cognitive phase: choosing decisions appropriately for task structure
	 *   '2' - Learn associative phase: hierarchical PROP composition
	 *   '3' - Learn autonomous phase: fully proceduralized (autonomous) rules for each PROP instruction set
	 *   's' - Enable spreading to bias fetching 
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
	 *   '1' - Learn cognitive phase: choosing decisions appropriately for task structure
	 *   '2' - Learn associative phase: hierarchical PROP composition
	 *   '3' - Learn autonomous phase: fully proceduralized (autonomous) rules for each PROP instruction set
	 *   's' - Enable spreading to bias fetching 
	 * @param str The string of learning mode flags
	 */
	public void set(String str) {
		if (str.contains("1")) {this.cognitive = true;} else {this.cognitive = false;}
		if (str.contains("2")) {this.associative = true;} else {this.associative = false;}
		if (str.contains("3")) {this.auto = true;} else {this.auto = false;}
		if (str.contains("s")) {this.spreading = true;} else {this.spreading = false;}
	}
	
	/**
	 * Reset the learning configuration. 
	 * Learn flags:
	 *   'cognitive'     - Learn cognitive phase: choosing decisions appropriately for task structure
	 *   'associative'   - Learn associative phase: hierarchical PROP composition
	 *   'auto'          - Learn autonomous phase: fully proceduralized (autonomous) rules for each PROP instruction set
	 *   'spreading'     - Enable spreading to bias fetching 
	 */
	public void set(boolean cognitive, boolean associative, boolean auto, boolean spreading) {
		this.cognitive = cognitive;
		this.associative = associative;
		this.auto = auto;
		this.spreading = spreading;
	}
	
	public void setChunkThreshold(int t) { chunkThreshold = t; }
	
	public boolean learnsCognitive() { return cognitive; }
	public boolean learnsAssociative() { return associative; }
	public boolean learnsAuto() { return auto; }
	public boolean usesSpreading() { return spreading; }
	public int getChunkThreshold() { return chunkThreshold; }
	
	@Override
	public String toString() {
		String retVal = "";
		if (cognitive) {retVal += "1";}
		if (associative) {retVal += "2";}
		if (auto) {retVal += "3";}
		if (spreading) {retVal += "s";}
		retVal += "_t" + chunkThreshold;
		if (commandName != "") {retVal += commandName;}
		return retVal;
	}
}
