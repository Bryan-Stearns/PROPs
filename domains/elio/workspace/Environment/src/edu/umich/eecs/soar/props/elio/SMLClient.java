package edu.umich.eecs.soar.props.elio;

import java.util.ArrayList;
import java.util.List;

import edu.umich.eecs.soar.propsutil.LearnConfig;
import javafx.util.Pair;

public class SMLClient {
	
	public static void main(String[] args) {
		
		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		/*expList.add(new LearnConfig("12m", 1));
		expList.add(new LearnConfig("12smc", 1));
		expList.add(new LearnConfig("123m", 1));
		expList.add(new LearnConfig("123smc", 1));*/
		expList.add(new LearnConfig("1e", 10));
		//expList.add(new LearnConfig("123se", 10));

		ElioWorld world = new ElioWorld();
		//world.runExperiments("elio_props", 2, expList);
		
		world.setVerbose(false);
		//world.setSavePercepts(true);
		world.runDebug("procedure-c","procedure-c", new LearnConfig("1e", 2));

		List<Pair<String,String>> trainList = new ArrayList<Pair<String,String>>();
		trainList.add(new Pair<String, String>("procedure-a","procedure-a"));
		trainList.add(new Pair<String, String>("procedure-b","procedure-b"));
		trainList.add(new Pair<String, String>("procedure-c","procedure-c"));
		trainList.add(new Pair<String, String>("procedure-d","procedure-d"));

		//world.makeSpreadingChunks(trainList, "elio_agent_condspread_chunks.soar");
		//world.makeAddressingChunks(trainList, "elio_agent_L1_chunks.soar", true);
		//world.makeFetchSets(trainList, "elio_agent_epsets.soar", true);
	}

}
