package edu.umich.eecs.soar.props.elio;

import java.util.ArrayList;

import edu.umich.eecs.soar.propsutil.LearnConfig;

public class SMLClient {
	
	public static void main(String[] args) {
		
		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		expList.add(new LearnConfig("12m", 10));
		expList.add(new LearnConfig("123m", 10));

		ElioWorld world = new ElioWorld();
		//world.runExperiments("elio_props", 2, expList);
		world.runDebug("procedure-a","procedure-a");
		
	}

}
