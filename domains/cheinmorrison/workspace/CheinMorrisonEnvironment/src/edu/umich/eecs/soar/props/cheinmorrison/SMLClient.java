package edu.umich.eecs.soar.props.cheinmorrison;

import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;

import edu.umich.eecs.soar.propsutil.LearnConfig;

public class SMLClient {
	
	
	public static void main(String[] args) {

		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		expList.add(new LearnConfig("12m", 48));
		expList.add(new LearnConfig("123m", 48));
		expList.add(new LearnConfig("12scm", 48));

		CheinMorrisonWorld world = new CheinMorrisonWorld();
		//world.runExperiments("chein", 2, expList);
		world.runEditorsDebug("verbal-CWM",0, 1, "12");
		
		//world.setConfig(new LearnConfig("scm123", 2));
		//world.runEditorsDebug("ed",2);
		
		List<Pair<String,String>> trainList = new ArrayList<Pair<String,String>>();
		
		/*world.makeAddressingChunks(trainList, "chein_agent_L1_chunks.soar");
		world.makeSpreadingChunks(trainList, "chein_agent_condspread_chunks.soar");*/
	}

}
