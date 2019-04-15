package edu.umich.eecs.soar.props.editors;

import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;

import edu.umich.eecs.soar.propsutil.LearnConfig;

public class SMLClient {
	
	//@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		expList.add(new LearnConfig("1", 96));

		EditorsWorld world = new EditorsWorld();
		//world.setVerbose(false);
		world.runExperiments("editors_props", 2, expList);
		//world.runEditorsDebug("emacs",0, new LearnConfig("1",1));
		
		List<Pair<String,String>> trainList = new ArrayList<Pair<String,String>>();
		trainList.add(new Pair<String, String>("ed","ed_1")); trainList.add(new Pair<String, String>("ed","ed_2")); trainList.add(new Pair<String, String>("ed","ed_3"));
		trainList.add(new Pair<String, String>("edt","edt_1")); trainList.add(new Pair<String, String>("edt","edt_2")); trainList.add(new Pair<String, String>("edt","edt_3"));
		trainList.add(new Pair<String, String>("emacs","emacs_1")); trainList.add(new Pair<String, String>("emacs","emacs_2")); trainList.add(new Pair<String, String>("emacs","emacs_3"));
		
		//world.makeAddressingChunks(trainList, "editors_agent_L1_chunks.soar");
		//world.makeSpreadingChunks(trainList, "editors_agent_condspread_chunks.soar");
		
	}

}
