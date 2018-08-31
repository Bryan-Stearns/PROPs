package edu.umich.eecs.soar.props.cheinmorrison;

import java.util.ArrayList;

import edu.umich.eecs.soar.propsutil.LearnConfig;

public class SMLClient {
	
	
	public static void main(String[] args) {

		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		expList.add(new LearnConfig("12m", 48));
		expList.add(new LearnConfig("123m", 48));
		expList.add(new LearnConfig("12scm", 48));

		CheinMorrisonWorld world = new CheinMorrisonWorld();
		world.runExperiments("editors_props", 2, expList);
		
		//world.setConfig(new LearnConfig("scm123", 2));
		//world.runEditorsDebug("ed",2);
	}

}
