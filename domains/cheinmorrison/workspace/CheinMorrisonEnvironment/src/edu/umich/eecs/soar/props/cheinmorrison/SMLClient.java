package edu.umich.eecs.soar.props.cheinmorrison;

import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;

import edu.umich.eecs.soar.propsutil.LearnConfig;

public class SMLClient {
	
	
	public static void main(String[] args) {

		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		//expList.add(new LearnConfig("123", 10));
		expList.add(new LearnConfig("12", 1));

		CheinMorrisonWorld world = new CheinMorrisonWorld(true);
		//world.setUseProps(false);
		
		//world.runCheinExperiment("chein", 4, expList); // 9 samples
		
		//world.setSoarAgentFile("cheinNR_agent.soar");
		
		//world.runCheinDebug("verbal-CWM", 1, "1");
		world.runCheinDebug("stroop", 1, "1");
		
		//world.testStroop();
		
		//world.makePreChunks();
	}

}
