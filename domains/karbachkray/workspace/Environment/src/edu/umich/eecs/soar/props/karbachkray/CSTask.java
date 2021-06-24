package edu.umich.eecs.soar.props.karbachkray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class CSTask {
	public List<ArrayList<Integer>> trials;	// The initialized list of trials, each trial a sequence of numbers to remember
	public ArrayList<Integer> nums;			// The current trial (while running)
	public ArrayList<String> numbers;	// Why is this called "numbers"?
	public ArrayList<String> responses;		// The current list of responses (while running)
	
	CSTask() {
		init();
	}
	
	public void init() {
		trials = new ArrayList<ArrayList<Integer>>();
		nums = new ArrayList<Integer>();
		numbers = new ArrayList<String>();
		responses = new ArrayList<String>();
		
		// Set up trials
		trials.clear();
		for (int i=0; i<5; ++i) {
			for (int j=0; j<3; ++j) {
				ArrayList<Integer> trial = new ArrayList<Integer>();
				for (int k=0; k<i+2; ++k) {
					trial.add(3+get_rand_int(7));
				}
				trials.add(trial);
			}
		}
		Collections.shuffle(trials);
	}
	
	// Get a random integer within [0,max)
	private int get_rand_int(int max) {
		return (int)(Math.random()*(max));
	}
	
	public ArrayList<String> getNextSymbol() {
		if (numbers.size() == 0)
			return null;
		String obj = numbers.get(numbers.size()-1);
		numbers.remove(numbers.size()-1);
		if (obj.equals("dbc"))
			return new ArrayList<String>(Arrays.asList("yes","blue","circle"));
		if (obj.equals("lgc"))
			return new ArrayList<String>(Arrays.asList("yes","green","circle"));
		if (obj.equals("dbs"))
			return new ArrayList<String>(Arrays.asList("yes","blue","square"));
		return null;
	}
	
	public void setNewTrial() {
		numbers.clear();
		// Add darkbluecircle samples
		for (int i=nums.get(nums.size()-1); i>0; --i) {
			numbers.add("dbc");
		}
		nums.remove(nums.size()-1);
		// Add lightgreencircle samples
		for (int i=1 + get_rand_int(5); i>0; --i) {
			numbers.add("lgc");
		}
		// Add darkbluesquare samples
		for (int i=1 + 2*get_rand_int(5); i>0; --i) {
			numbers.add("dbs");
		}
		
		Collections.shuffle(numbers);
	}
}