package edu.umich.eecs.soar.props.elio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umich.eecs.soar.propsutil.PROPsEnvironment;

/**
 * This code replicates the lisp code written by Niels Taatgen for his Actransfer model of this task.
 */
public class ElioWorld extends PROPsEnvironment {
	
	private int lastDC = 0, current_sample = 0;
	private ETask etask;
	
	// We'll use the same numbers on the screen over and over again. The model will not notice.
	private static final Map<String, Integer> inputs;
	static {
		Map<String, Integer> aMap = new HashMap<String, Integer>();
		aMap.put("solid", 6);
		aMap.put("algae", 2);
		aMap.put("lime1", 3);
		aMap.put("lime2", 5);
		aMap.put("lime3", 1);
		aMap.put("lime4", 9);
		aMap.put("limemax", 2);
		aMap.put("limemin", 1);
		aMap.put("toxin1", 4);
		aMap.put("toxin2", 8);
		aMap.put("toxin3", 7);
		aMap.put("toxin4", 2);
		aMap.put("toxinmin", 2);
		aMap.put("toxinmax", 8);
		inputs = Collections.unmodifiableMap(aMap);
	}

	
	ElioWorld() {
		String proj_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/";
		String props_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/PROPsAgent/";
		//String proj_dir = "/home/bryan/Dropbox/UM_misc/Soar/Research/PROPs/PRIMs_Duplications/Elio/";
		//String props_dir = "/home/bryan/Dropbox/UM_misc/Soar/Research/PROPs/PROPs Project/";
		
		this.setAgentName("ElioAgent");
		this.setPropsDir(props_dir);
		this.setProjDir(proj_dir);
		
		this.setCondChunkFile("elio_agent_condspread_chunks.soar");
		this.setAddressChunkFile("elio_agent_L1_chunks.soar");
		this.setFetchSeqFile("elio_agent_fetch_procedures.soar");
		this.setInstructionsFile("elio_agent_instructions.soar");//"prims_elio01_agent_smem.soar");
		this.setSoarAgentFile("elio_agent.soar");
		/*this.setCondChunkFile("prims_elio02_condspread-chunks.soar");
		this.setAddressChunkFile("prims_elio02_L1-chunks.soar");
		this.setFetchSeqFile("prims_elio_procedures_smem.soar");
		this.setInstructionsFile("test_elio_agent_PROP.soar");//"prims_elio01_agent_smem.soar");
		this.setSoarAgentFile("test_elio_agent.soar");*/
		
		this.setIOSize(2, 2);
		
		this.setUserAgentFiles(Arrays.asList("/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/lib_actransfer_interface.soar", 
											 proj_dir + "elio_agent_smem.soar"));
		

		etask = new ETask("", 1,1,0);
	}
	
	@Override
	protected void user_createAgent() {
		lastDC = 0;
	}

	@Override
	protected void user_outputListener(List<String> outputs) {
		// Get the output
		String val1 = outputs.get(0);
		String val2 = outputs.get(1);

		// Generate the corresponding input
		int input2;
		if (val1.compareTo("read") == 0) {
			input2 = inputs.get(val2);
			try {
				this.setInput(0, val2);
				this.setInput(1, input2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (val1.compareTo("enter") == 0) {
			// (Inputs already destroyed, leave it that way)
			// Get the current number of chunks
			int chunkCount = agent.ExecuteCommandLine("p -c").split("\n").length;
			// Store the result
			int DC = agent.GetDecisionCycleCounter();
			//results.add(new Result(etask, val2, DC - lastDC, chunkCount));
			this.addReport(String.format("%1$s \t%2$d \t%3$d \t%4$.3f \t%5$s \t%6$d \t%7$d",
					this.taskName.toUpperCase(), etask.trial, etask.line, (System.nanoTime() - etask.start)/1000000000.0, val2, (DC - lastDC), chunkCount, etask.sample));
			
			lastDC = DC;
			// Start next task
			etask.line++;
			etask.start = System.nanoTime();

			System.out.println("# Enter " + val2 + " #");
		}

	}

	@Override
	protected void user_agentStart() {
		this.clearReports();
	}
	
	@Override
	protected void user_agentStop() {
		// Reset for next trial
		etask.line = 1;
		etask.trial++;
		etask.start = System.nanoTime();
	}

	@Override
	protected void user_doExperiment() {
		List<String> tasks = new ArrayList<>(Arrays.asList("procedure-b", "procedure-c", "procedure-d"));
		int NUM_TRIALS = 6;
		
		
		etask.sample = current_sample++;
		lastDC = 0;
		
		// Alternate NUM_TRIALS trials of procedure-A with NUM_TRIALS trials of each of the others
		for (String task : tasks) {
	
			try {this.initAgent();} catch (Exception e) {e.printStackTrace();}
			
			this.setTask("procedure-a", "procedure-a");
			etask.init("procedure-a");
			for (int i=0; i<NUM_TRIALS; ++i) {
				this.runAgent();
			}

			this.setTask(task, task);
			etask.init(task);
			for (int i=0; i<NUM_TRIALS; ++i) {
				this.runAgent();
			}
			
			this.printReports();
			
		}
	}

	@Override
	protected void user_errorListener(String err) {
		
	}

	@Override
	protected void user_updateTask() {
		// TODO Auto-generated method stub
		
	}


}
