package edu.umich.eecs.soar.props.elio;

public class ETask {
	public int line, trial;//, sample=1;
	public long start;
	public String task;
	
	ETask(String task, int line, int trial, /*int sample,*/ long start) {
		this.line = line;
		this.trial = trial;
		//this.sample = sample;
		this.start = start;//System.nanoTime();
	}
	
	public void init(String task) {
		this.task = task;
		line = 1;
		trial = 1;
		start = 0;//System.nanoTime();
	}
}
