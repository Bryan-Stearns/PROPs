package edu.umich.eecs.soar.propsutil;

import java.util.List;

public class ScheduledInput {
	public long moment = -1;
	public List<String> inputs;
	
	public ScheduledInput(long moment, List<String> inputs) {
		this.moment = moment;
		this.inputs = inputs;
	}
	
	public long getMoment() {return moment;}
}
