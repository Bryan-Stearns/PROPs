package edu.umich.eecs.soar.props.karbachkray;

public class StroopTask {
	public int taskID;
	public long startTime;
	public int count;
	public int numTrials;
	public String type;
	public String answer;
	
	StroopTask() {
		init();
	}
	
	public void init() {
		taskID = 0;
		startTime = 0;
		count = 0;
		numTrials = 24;		// default
		type = "NEUTRAL";	// first run
		answer = "";
	}
	
	
}
