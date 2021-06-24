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
	
	public static void main(String[] args) {

		//ArrayList<Double> cmd1vals = generateParamSweep(0.02, 0.06, 0.02); 
		//ArrayList<Double> cmd2vals = generateParamSweep(0.775, 0.775, 0.05);
		
		CheinMorrisonWorld world = new CheinMorrisonWorld(true);
		//world.setUseProps(false);
		
		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		expList.add(new LearnConfig("2",1));
		/*for (Double val1 : cmd1vals) {
			for (Double val2 : cmd2vals) {
			ArrayList<String> cmds = new ArrayList<String>();
			
				cmds.add("rl --set learning-rate " + val1);
				cmds.add("rl --set discount-rate " + val2);

				// Was "12", 1 for 2020 paper, with (wait) operator
				expList.add(new LearnConfig("12", 1, cmds, String.format("_lr%02d_dr%03d", (int)(val1*100), (int)(val2*1000))));
			}
		}*/
		
		world.setVerbose(true);
		world.runCheinExperiment("stroopChein", 9, expList); // 9 samples
		
		//world.setSoarAgentFile("cheinNR_agent.soar");
		
		//world.runCheinDebug("verbal-CWM", 1, "1");
		//world.runCheinDebug("stroop", 1, "2");
		
		//world.testStroop();
		
		//world.makePreChunks();
	}

}
