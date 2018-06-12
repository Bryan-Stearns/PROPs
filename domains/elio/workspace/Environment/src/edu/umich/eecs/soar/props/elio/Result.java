package edu.umich.eecs.soar.props.elio;

public class Result {
	public int line, trial, decision, chunks, sample;
	public long time;
	public String action;
	
	Result(ETask etask, String action, int decision, int chunks) {
		this.line = etask.line;
		this.trial = etask.trial;
		this.sample = etask.sample;
		this.time = System.nanoTime() - etask.start;
		this.action = action;
		this.decision = decision;
		this.chunks = chunks;
	}

	@Override
	public String toString() {
		return trial + "\t" + line + "\t" + time/1000000000.0 + "\t" + action + "\t" + decision + "\t" + chunks + "\t" + sample;
	}
	
}
