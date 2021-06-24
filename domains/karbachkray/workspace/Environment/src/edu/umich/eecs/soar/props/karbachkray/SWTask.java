package edu.umich.eecs.soar.props.karbachkray;

public class SWTask {
	public int taskID;
	public long startTime;
	public int count;
	public int numTrials;

	SWTask() {
		init();
	}
	
	public void init() {
		taskID = 0;
		startTime = 0;
		count = 0;
		numTrials = 17;		// default
	}
}
