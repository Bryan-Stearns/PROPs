package edu.umich.eecs.soar.props.cheinmorrison;

public class Stroop {
	public int count,
				numtrials;
	public long starttime;
	public String type,
				answer;
	
	Stroop() {
		count = 0;
		numtrials = 15;		// default
		starttime = System.nanoTime() + 1000000000l;
		type = "";
		answer = "";
		//schedule_delayed_action(1.0, "yes", "");
	}
}
