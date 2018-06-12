package edu.umich.eecs.soar.props.editors;

public class Report {
	public String state, 
				  task;		// low level task
	public String taskSetName, taskName;	// editor tasks
	public int trialNum, editNum;
	public long time, strt, ll, mt, temp;
	public double latencies;
	
	Report() {
		state = "ll-noread";
		strt = System.nanoTime();
		taskSetName = "--";
		taskName = "--";
	}
	
	public void init() {
		trialNum = 1;
		editNum = 1;
		state = "ll-noread";
		strt = System.nanoTime();
		latencies = 0.0;
	}
	
	public void addLatency(double l) {latencies += l;}
	
	@Override
	public String toString() {
		double s2 = ll / 1000000000.0,
				s3 = mt / 1000000000.0,
				s4 = (System.nanoTime()-strt) / 1000000000.0;
		return String.format("%1$s %2$d %3$s %4$d %5$-15s %6$.3f %7$.3f %8$.3f %9$.3f",
				taskSetName, trialNum, taskName.toUpperCase(), editNum,
				task.toUpperCase(), s2, s3, s4, latencies);
	}
	
}
