package edu.umich.eecs.soar.props.elio;

import java.util.ArrayList;

import edu.umich.eecs.soar.propsutil.LearnConfig;

public class SMLClient {
	
	public static void main(String[] args) {
		
		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		expList.add(new LearnConfig("12m", 10));
		expList.add(new LearnConfig("12smc", 10));
		expList.add(new LearnConfig("123m", 10));
		expList.add(new LearnConfig("123smc", 10));

		ElioWorld world = new ElioWorld();
		world.runExperiments("elio_props", 8, expList);
		//world.runDebug("procedure-a","procedure-a", 1, "12scm");
		
	}

}
