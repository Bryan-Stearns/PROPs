package edu.umich.eecs.soar.props.cheinmorrison;

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
	
	private static ArrayList<LearnConfig> generateCRLSweep(String configStr, int lr) {
		ArrayList<LearnConfig> retList = new ArrayList<LearnConfig>();
		
		ArrayList<Double> cmdVals = generateParamSweep(0.1, 0.3, 0.05);
		for (Double val : cmdVals) {
			ArrayList<String> cmds = new ArrayList<String>();
			// Set the learning rate by overwriting the rule in props_crl.soar that defines it.
			cmds.add("sp {elaborate*props*pref-weight*learning-rate\n    (state <s> ^superstate nil)\n-->\n    (<s> ^props-pref-learning-rate " 
					+ val + ")\n}");

			retList.add(new LearnConfig(configStr, lr, cmds, String.format("_clr%02d", (int)(val*100))));
		}

		return retList;
	}
	
	public static void main(String[] args) {

		CheinMorrisonWorld world = new CheinMorrisonWorld(true);
		//world.setUseProps(false);
		
		//ArrayList<LearnConfig> expList = generateRLSweep("12",1);
		ArrayList<LearnConfig> expList = generateCRLSweep("12",1);
		//ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		//expList.add(new LearnConfig("12",1));
		
		//world.runCheinExperiment("stroopChein", 3, expList); // 9 samples
		
		//world.setSoarAgentFile("cheinNR_agent.soar");
		
		//world.runCheinDebug("verbalCWM", 1, "1");
		world.runCheinDebug("stroop", 1, "1");
		
		//world.testStroop();
		
		//world.makePreChunks();
	}

}
