package edu.umich.eecs.soar.props.karbachkray;

import java.util.ArrayList;
import edu.umich.eecs.soar.propsutil.LearnConfig;
import javafx.util.Pair;

@SuppressWarnings("restriction")
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
		
		ArrayList<LearnConfig> expList = new ArrayList<LearnConfig>();
		
		//ArrayList<Double> cmd1vals = generateParamSweep(0.02, 0.02, 0.02);
		//ArrayList<Double> cmd2vals = generateParamSweep(3, 3, 0.05);
		//cmd1vals.add(0.02); // cmd1vals.add(0.1); cmd1vals.add(0.2); cmd1vals.add(0.3);
		//cmd2vals.add(0.775);
		/*for (Double val1 : cmd1vals) {
			for (Double val2 : cmd2vals) {
			ArrayList<String> cmds = new ArrayList<String>();
			
				cmds.add("rl --set learning-rate " + val1);
				cmds.add("rl --set discount-rate " + val2);

				expList.add(new LearnConfig("2", 1, cmds, String.format("_lr%02d_dr%02d", (int)(val1*100), (int)(val2*100))));
			}
		}*/
		//ArrayList<String> cmds = new ArrayList<String>();
		//cmds.add("rl --set learning-rate 0.3");
		//expList.add(new LearnConfig("12", 1, cmds, "_nocondrl_lr30_dr775"));
		expList.add(new LearnConfig("2", 1));

		KKWorld world = new KKWorld();
		
		//world.testCountSpan(true);
		//world.testStroop();
		//world.testAB("A");
		
		world.setVerbose(false);
		world.setPrintProgress(false);
		world.runKKExperiment("kk_props", 21, expList);
		
		ArrayList<Pair<String,String>> chunkTrainList = new ArrayList<Pair<String,String>>();
		chunkTrainList.add(new Pair<String,String>("count-span","count-span"));
		chunkTrainList.add(new Pair<String,String>("stroop","stroop"));
		chunkTrainList.add(new Pair<String,String>("task-switching-AB","task-switching-AB"));
		//chunkTrainList.add(new Pair<String,String>("task-switching-CD","task-switching-CD"));
		chunkTrainList.add(new Pair<String,String>("single-task-A","single-task-A"));
		
		//world.makeChunkingRuns(chunkTrainList, "kk_agent_L1_chunks.soar");
		
		//world.setSavePercepts(true);
		//world.runDebug("procedure-a","procedure-a", new LearnConfig("2", 2));

	}

}
