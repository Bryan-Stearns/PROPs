package edu.umich.eecs.soar.props.editors;

import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;

import edu.umich.eecs.soar.propsutil.LearnConfig;

public class SMLClient {
	
	private static ArrayList<Double> generateParamSweep(double start, double end, double step) {
		ArrayList<Double> retval = new ArrayList<Double>();
		double eps = 0.00001;
		
		// Sweep through cmd1
		double curPos = start;
		while (curPos <= end + eps) {
			retval.add(curPos);
			curPos += step;
		}
		
		return retval;
	}
	
	// Sweep over values for RL parameters
	private static ArrayList<LearnConfig> generateRLSweep(String configStr, int lr) {
		ArrayList<LearnConfig> retList = new ArrayList<LearnConfig>();
		
		ArrayList<Double> cmd1vals = generateParamSweep(0.0025, 0.02, 0.0025);
		ArrayList<Double> cmd2vals = generateParamSweep(0.7, 0.8, 0.025);
		
		for (Double val1 : cmd1vals) {
			for (Double val2 : cmd2vals) {
			ArrayList<String> cmds = new ArrayList<String>();
				cmds.add("rl --set learning-rate " + val1);
				cmds.add("rl --set discount-rate " + val2);

				retList.add(new LearnConfig(configStr, lr, cmds, String.format("_lr%04d_dr%03d", (int)(val1*10000), (int)(val2*1000))));
			}
		}
		
		return retList;
	}

	// Sweep over values for operator return reward and decision temperature
	private static ArrayList<LearnConfig> generateOPRSweep(String configStr, int chunk_thresh) {
		ArrayList<LearnConfig> retList = new ArrayList<LearnConfig>();
		
		ArrayList<Double> cmd1vals = generateParamSweep(0.15, 0.175, 0.005);
		ArrayList<Double> cmd2vals = generateParamSweep(0.14, 0.16, 0.02);
		
		for (Double val1 : cmd1vals) {
			for (Double val2 : cmd2vals) {
			ArrayList<String> cmds = new ArrayList<String>();
				cmds.add("sp {apply*props*retrieve-epset*success*return-reward (state <s> ^operator.name props-load-epset ^superstate.reward-link <srl>) --> (<srl> ^reward.value " + val1 + ")}");
				cmds.add("decide indifferent-selection -t " + val2);

				retList.add(new LearnConfig(configStr, chunk_thresh, cmds, String.format("_lr02_dr775_opr%03d_tmp%02d", (int)(val1*1000), (int)(val2*100))));
			}
		}
		
		return retList;
	}
	
	//@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		//ArrayList<LearnConfig> expList = generateOPRSweep("12", 48); //new ArrayList<LearnConfig>();
		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		expList.add(new LearnConfig("2", 48));
		

		EditorsWorld world = new EditorsWorld();
		world.setVerbose(false);
		world.runExperiments("editors_prop3", 12, expList);
		//world.runEditorsDebug("ed",0, new LearnConfig("1",1));
		
		/*List<Pair<String,String>> trainList = new ArrayList<Pair<String,String>>();
		trainList.add(new Pair<String, String>("ed","ed_1")); trainList.add(new Pair<String, String>("ed","ed_2")); trainList.add(new Pair<String, String>("ed","ed_3"));
		trainList.add(new Pair<String, String>("edt","edt_1")); trainList.add(new Pair<String, String>("edt","edt_2")); trainList.add(new Pair<String, String>("edt","edt_3"));
		trainList.add(new Pair<String, String>("emacs","emacs_1")); trainList.add(new Pair<String, String>("emacs","emacs_2")); trainList.add(new Pair<String, String>("emacs","emacs_3"));
		*/
		
		//world.makeAddressingChunks(trainList, "editors_agent_L1_chunks.soar");
		//world.makeSpreadingChunks(trainList, "editors_agent_condspread_chunks.soar");
		
	}

}
