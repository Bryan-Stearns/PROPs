package edu.umich.eecs.soar.props.cheinmorrison;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umich.eecs.soar.propsutil.LearnConfig;
import edu.umich.eecs.soar.propsutil.PROPsEnvironment;
import javafx.util.Pair;


public class CheinMorrisonWorld extends PROPsEnvironment {
	//private static double STD_MOTOR_TIME = 0.25,
	//					  STD_VISUAL_TIME = 0.25;

	//private Report rep;
	private VCWM cstask;
	private Stroop sttask;

	//private String taskMode = "";
	private boolean inDebug = false;
	private int numSamples = 1;
	
	private boolean rehearseMode = false;
	
	CheinMorrisonWorld(boolean rehearse) {
		String proj_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/cheinmorrison/";
		String props_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/PROPsAgent/";
		
		this.setAgentName("CheinMorrisonAgent");
		this.setPropsDir(props_dir);
		
		if (rehearse)  {
			this.setCondChunkFile(proj_dir + "chein_agent_condspread_chunks.soar");
			this.setAddressChunkFile(proj_dir + "chein_agent_L1_chunks.soar");
			//this.setFetchSeqFile(proj_dir + "chein_agent_fetch_procedures.soar");
			this.setInstructionsFile(proj_dir + "chein_agent_instructions.soar");
			this.setSoarAgentFile(proj_dir + "chein_agent.soar");
		}
		else {
			this.setCondChunkFile(proj_dir + "cheinNR_agent_condspread_chunks.soar");
			this.setAddressChunkFile(proj_dir + "cheinNR_agent_L1_chunks.soar");
			//this.setFetchSeqFile(proj_dir + "cheinNR_agent_fetch_procedures.soar");
			this.setInstructionsFile(proj_dir + "cheinNR_agent_instructions.soar");
			this.setSoarAgentFile(proj_dir + "cheinNR_agent.soar");
		}
		
		this.setIOSize(3, 2);

		this.setUserAgentFiles(Arrays.asList("/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/lib_actransfer_interface.soar", 
												proj_dir + "chein_agent_smem.soar"));
		
		cstask = new VCWM();
		sttask = new Stroop();
		rehearseMode = rehearse;
	}
	
	public void runCheinDebug(String task, int threshold, String mode) {
		inDebug = true;
		//taskMode = task;
		this.runDebug(task, task, new LearnConfig(mode, threshold));
		inDebug = false;
	}
	
	// Get a random integer within [0,max)
	private int get_rand_int(int max) {
		return (int)(Math.random()*(max));
	}
	private String get_rand_word() {
		List<String> words = Arrays.asList("umbrella","tantrum","xobos","fartnot");
		return words.get(get_rand_int(words.size()));
	}
	// Return a random letter within [a-j]
	private String get_rand_letter() {
		//List<String> words = Arrays.asList("a","b","c","d","e","f","g","h","i","j");
		return Character.toString((char)(get_rand_int((int)('h'-'a'))+'a'));
	}
	
	private void set_perception(String in1, String in2, String in3) {
		this.setPerception(Arrays.asList(in1, in2, in3));
	}
	
	private double VCWM_action(String action, String out1) {
		double latency = 0.05;
		
		if (action.equals("type") && cstask.state.equals("lexical") && (this.getElapsedTime() - cstask.start) < secToMilli(4.0)) {
			// 4 seconds are not yet up
			set_perception("pending", null, null);	// CUSTOM: acknowledge the agent's response
			this.scheduleInput(0.5, Arrays.asList("word", get_rand_word()));	// 0.5
		} else if (action.equals("type") && cstask.state.equals("lexical")) {
			// 4 seconds are up
			// trigger_reward(TASK_REWARD);
			String next_letter = get_rand_letter();
			/*if (cstask.count == 0) {
				cstask.state = "report";
				this.scheduleInput(1.135, Arrays.asList("report","")); // last letter, schedule a report	// 1.135
			} else {
				cstask.count--;
				cstask.start = this.getElapsedTime() + this.secToMilli(1.635);
				this.scheduleInput(1.135, Arrays.asList("word", get_rand_word()));	// 1.135
			}*/
			set_perception("letter", next_letter, "");
			cstask.stimuli.add(0, next_letter);
			latency = 0.135;
		} else if (action.equals("type") && cstask.state.equals("report")) {
			cstask.responses.add(0, out1);
			set_perception("report", "", "");
			latency = 0.2;
		} else if (action.equals("enter")) {
			//trigger_reward(TASK_REWARD);
			Boolean correct = cstask.responses.equals(cstask.stimuli);
			this.addReport(String.format("%1d %2d", cstask.current_span, correct ? 1 : 0));
			if (cstask.last_correct != null && cstask.current_span > 1) {
				if (correct && cstask.last_correct) {
					cstask.current_span++;
					correct = null;
				} else if (!correct && !cstask.last_correct) {
					cstask.current_span--;
					correct = null;
				}
			}
			cstask.last_correct = correct;
			cstask.state = "lexical";
			this.clearPerception();
			cstask.trials--;
			// Done with entering the stimuli
			// Handle this later. Let's test this stuff first.
			//System.out.println("Enter has been pushed, current span " + cstask.current_span);
			//System.out.println(cstask.toString());
			
			// Set up the next trial
			if (cstask.trials > 0) {
				cstask.responses.clear();
				cstask.stimuli.clear();
				cstask.count = cstask.current_span - 1;		// keep a counter
				cstask.start = this.getElapsedTime() + this.secToMilli(0.365);
				this.scheduleInput(0.5, Arrays.asList("word", get_rand_word()));
			}
		} else if (action.equals("wait")) {
			set_perception("pending", null, null);
			
			// Moved from "type""lexical" to allow agent time to remember-letter.
			if (cstask.state.equals("lexical")) {
				if (cstask.count == 0) {
					cstask.state = "report";
					this.scheduleInput(1.135, Arrays.asList("report","")); // last letter, schedule a report	// 1.135
				} else {
					cstask.count--;
					cstask.start = this.getElapsedTime() + this.secToMilli(1.635);
					this.scheduleInput(1.135, Arrays.asList("word", get_rand_word()));	// 1.135
				}
			}
		}
		
		return latency;
		
	}
	
	private double stroop_action(String action, String out1) {
		double latency = 0.05;
		
		if (action.equals("get-property")) {
			if (sttask.count % 2 == 0) {
				sttask.type = "congruent";
				sttask.answer = "red-concept";
				set_perception("rred","red-color","red-word");
			} else {
				sttask.type = "incongruent";
				sttask.answer = "red-concept";
				set_perception("rblue","red-color","blue-word");
			}
			
			if (out1.equals("color-property")) {
				set_perception("rblue","red-color", null);
			}
			
			latency = 0.2;
		} else if (action.equals("say")) {
			latency = 0.2;
			
			Boolean correct = sttask.answer.equals(out1);
			//trigger_reward( (correct) ? TASK_REWARD : 0.0 );
			this.addReport(String.format("%d \t%s \t%d \t%.3f", 
					sttask.count+1, sttask.type.toUpperCase(), correct ? 1 : 0, this.milliToSec(this.getElapsedTime()+this.secToMilli(0.2)-sttask.starttime)));
			
			String new_percept = (++sttask.count > sttask.numtrials) ? "last" : "yes";
			if (new_percept.equals("yes")) {
				sttask.starttime = this.getElapsedTime() + this.secToMilli(1.2);
			}
			set_perception("pending", null, null);
			this.scheduleInput(1.2, Arrays.asList(new_percept, null, null));
		}
		
		return latency;
	}
	
	private void CM_setTask(String task) {
		//taskMode = task;
		this.setTask(task, task); 	// TODO: CM uses random inputs. Will need to make fixed inputs for training sequence!
	}
	
	private void reset_VCWM() {
		cstask.start = this.getElapsedTime() + this.secToMilli(0.365);
		this.scheduleInput(0.5, Arrays.asList("word", get_rand_word(), null));
	}
	private void init_VCWM() {
		cstask.init();
		reset_VCWM();
	}
	private void init_stroop() {
		sttask.init();
		sttask.starttime = this.getElapsedTime() + 1000l;
		this.scheduleInput(1.0, Arrays.asList("yes", null, null));
	}
	
	private void do_stroop(int day, String condition) {
		if (rehearseMode) {
			this.setOutputFile("stroopChein_" + this.getLearnMode().toString() + "_s" + numSamples + ".txt");
		}
		else {
			this.setOutputFile("stroopCheinNR_" + this.getLearnMode().toString() + "_s" + numSamples + ".txt");
		}
		CM_setTask("stroop");
		for (int i=0; i<12 && !this.hasError(); ++i) {
			init_stroop();
			
			this.runAgent();
			
			this.printReports(String.format("STROOP %s %d %d ", condition, i+1, day));
			this.clearReports();
		}
	}

	public void runCheinExperiment(String taskName, int samples, ArrayList<LearnConfig> expList) {
		numSamples = samples;
		this.runExperiments(taskName, samples, expList);
	}
	
	public void makePreChunks() {
		List<Pair<String,String>> trainList = new ArrayList<Pair<String,String>>();
		trainList.add(new Pair<String,String>("verbal-CWM","verbal-CWM"));
		trainList.add(new Pair<String,String>("stroop","stroop"));
		
		String task = "cheinNR";
		if (rehearseMode)
			task = "chein";
		
		inDebug = true;
		makeSpreadingChunks(trainList, task + "_agent_condspread_chunks.soar", false);
		makeAddressingChunks(trainList, task + "_agent_L1_chunks.soar", false);
		inDebug = false;
	}
	

	@Override
	protected void user_doExperiment() {
		// Run the control agent
		if (!this.initAgent()) return;
		/*do_stroop(1, "CONTROL");
		// WEIRD: Taatgen reinits here in rehearsal case
		do_stroop(21, "CONTROL");

		// Run the transfer agent
		if (!this.initAgent()) return;
		do_stroop(1, "EXP");*/
		// WEIRD: Taatgen reinits here in rehearsal case
		
		CM_setTask("verbal-CWM");
		for (int i=0; i<2 && !this.hasError(); ++i) {
			// Practice the verbal task
			System.out.println("*** Practice Session " + (i+1));
			init_VCWM();

			for (int j=0; j<16 && !this.hasError(); ++j) {
				reset_VCWM();

				this.runAgent();	// Run until receiving the finish command
			}
		}
		
		this.clearReports(); 	// We don't use the practice results
		if (rehearseMode) {
			this.setOutputFile("WMChein_" + this.getLearnMode().toString() + "_s" + numSamples + ".txt");
		}
		else {
			this.setOutputFile("WMCheinNR_" + this.getLearnMode().toString() + "_s" + numSamples + ".txt");
		}
		
		for (int i=0; i<20 && !this.hasError(); ++i) {
			// Test the verbal task
			System.out.println("*** Session " + (i+1));
			init_VCWM();

			for (int j=0; j<16 && !this.hasError(); ++j) {
				reset_VCWM();

				this.runAgent();	// Run until receiving the finish command
			}
			this.printReports(String.format("%1d  ", i+1));
			this.clearReports();
		}
		
		if (this.hasError()) {
			System.out.println("ERROR RETURNED BY AGENT " + "!");
			return;
		}
		
		
		do_stroop(21, "EXP");
		
		System.out.println("\nDone!");

	}

	@Override
	protected void user_outputListener(List<String> outputs) {

		// Get the output
		String action = outputs.get(0);
		String val2 = outputs.get(1);

		double latency = 0.0;

		if (this.getTask().equals("stroop")) {
			latency = stroop_action(action, val2);
		}
		else if (this.getTask().equals("verbal-CWM")) {
			latency = VCWM_action(action, val2);
		}
		else {
			// Unknown task - do nothing
		}
		
		this.addAgentLatency(secToMilli(latency));

		//rep.addLatency(latency);
		
	}


	@Override
	protected void user_agentStart() {
		// Called from runAgent() the first time it is run for an agent
		
	}

	@Override
	protected void user_agentStop() {
		if (inDebug) {
			/*if (this.taskName.equals("verbal-CWM")) {
				this.scheduleInput(0.5, Arrays.asList("word", get_rand_word(), null));
			}
			else */if (this.getTask().equals("stroop")) {
				init_stroop();
			}
		}
		else {
			user_updateTask();
		}
	}

	@Override
	protected void user_createAgent() {
		// Called from initAgent() and runDebug(), when the agent is created, usually before the task is set
	}

	@Override
	protected void user_updateTask() {

		if (inDebug) {
			if (this.getTask().equals("verbal-CWM")) {
				init_VCWM();
			}
			else if (this.getTask().equals("stroop")) {
				init_stroop();
			}
		}
		// Start perception with "pending"
		this.setInput(0, "pending");
		this.applyNewInputs();
	}

	@Override
	protected void user_errorListener(String arg0) {
		System.err.println("ERROR DETECTED!");
	}

}
