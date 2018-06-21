package edu.umich.eecs.soar.props.cheinmorrison;

import java.util.ArrayList;

public class VCWM {
	public String state;
	public Boolean last_correct;
	public ArrayList<Integer> spans;
	public ArrayList<String> responses,
							stimuli;
	public int current_span,
				trials,
				count;
	public long start;
	
	VCWM() {
		state = "lexical";
		current_span = 4;
		last_correct = null;
		trials = 16;
		spans = new ArrayList<Integer>();
		responses = new ArrayList<String>();
		stimuli = new ArrayList<String>();
		count = current_span - 1;
		start = System.nanoTime() + 365000000l;
		//schedule_delayed_action(0.5, "word", get_rand_word());
	}

	@Override
	public String toString() {
		return "FIXME ";
	}
}
