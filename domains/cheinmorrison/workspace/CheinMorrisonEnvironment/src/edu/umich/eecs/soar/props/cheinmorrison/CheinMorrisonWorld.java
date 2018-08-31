package edu.umich.eecs.soar.props.cheinmorrison;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import sml.Identifier;
import sml.Kernel;
import sml.Kernel.UpdateEventInterface;
import sml.WMElement;
import sml.smlUpdateEventId;
import sml.Agent;
import edu.umich.soar.debugger.SWTApplication;

import edu.umich.eecs.soar.propsutil.PROPsEnvironment;


public class CheinMorrisonWorld extends PROPsEnvironment {
	private static double STD_MOTOR_TIME = 0.25,
						  STD_VISUAL_TIME = 0.25;

	private Report rep;
	private int task_count;				// The index of edit_tasks to use next
	private VCWM cstask;
	private Stroop sttask;

	
	CheinMorrisonWorld() {
		String proj_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/cheinmorrison/";
		String props_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/PROPsAgent/";
		
		this.setAgentName("CheinMorrisonAgent");
		this.setPropsDir(props_dir);
		this.setProjDir(proj_dir);
		
		this.setCondChunkFile("prims_cheinmorrison_condspread_chunks.soar");
		this.setAddressChunkFile("prims_cheinmorrison_L1-chunks.soar");
		this.setFetchSeqFile("prims_cheinmorrison_tasks_smem.soar");
		this.setInstructionsFile("prims_cheinmorrison_agent_smem.soar");
		this.setSoarAgentFile("prims_cheinmorrison_agent.soar");
		
		this.setIOSize(4, 3);
		
		this.setUserAgentFiles(Arrays.asList("lib_actr_interface.soar", 
											 "smem_editors.soar"));
		
		cstask = new VCWM();
		sttask = new Stroop();
	}
	
	public void runEditorsDebug(String task, int taskNum) {
		String taskSeq = task + "_" + (taskNum+1);
		task_count = taskNum;
		this.runDebug(task, taskSeq);
	}
	
	
	private long secToNano(double sec) {
		return (long) sec*1000000000l;
	}
	
	private List<String> substitute_insert(String old_element, String new_element, List<String> l) {
		if (l == null || l.size() == 0)
			return l;
		return new LinkedList<String>(Arrays.asList(String.join(" ", l).replaceFirst(old_element, new_element).split("\\s+")));
	}
	
	private void schedule_delayed_action(double delaysec, String action, String arg) {
		
	}
	
	private String get_rand_word() {
		return "umbrella"; // tantrum xobos fartnot
	}
	private String get_rand_letter() {
		return "a"; // a-j
	}
	
	private void set_perception(String in1, String in2, String in3) {
		this.setPerception(Arrays.asList(in1, in2, in3));
	}
	
	private double VCWM_action(String action, String out1, String out2) {
		double latency = 0.05;
		
		if (action.equals("type") && cstask.state.equals("lexical") && (cstask.start - System.nanoTime()) < secToNano(4.0)) {
			// 4 seconds are not yet up
			schedule_delayed_action(0.5, "word", get_rand_word());
		} else if (action.equals("type") && cstask.state.equals("lexical")) {
			// 4 seconds are up
			// trigger_reward(TASK_REWARD);
			String next_letter = get_rand_letter();
			if (cstask.count == 0) {
				cstask.state = "report";
				schedule_delayed_action(1.135, "report",""); // last letter, schedule a report
			} else {
				cstask.count--;
				cstask.start = System.nanoTime() + secToNano(1.635);
				schedule_delayed_action(1.135, "word", get_rand_word());
			}
			set_perception("letter", "next-letter", "");
			cstask.stimuli.add(0, next_letter);
			latency = 0.135;
		} else if (action.equals("type") && cstask.state.equals("report")) {
			cstask.responses.add(0, out1);
			latency = 0.2;
		} else if (action.equals("enter")) {
			//trigger_reward(TASK_REWARD);
			Boolean correct = cstask.responses.equals(cstask.stimuli);
			cstask.spans.add(0, correct ? 1 : 0);
			cstask.spans.add(0, cstask.current_span);
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
			System.out.println("Enter has been pushed, current span " + cstask.current_span);
			System.out.println(cstask.toString());
			// Set up the next trial
			if (cstask.trials > 0) {
				cstask.responses.clear();
				cstask.stimuli.clear();
				cstask.count = cstask.current_span - 1;		// keep a counter
				cstask.start = System.nanoTime() + secToNano(0.365);
				schedule_delayed_action(0.5, "word", get_rand_word());
			}
		}
		
		return latency;
		
	}
	
	private double stroop_pm(String action, String out1, String out2) {
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
				set_perception("rblue",this.getInput(1), "");
			}
			
			latency = 0.2;
		} else if (action.equals("say")) {
			latency = 0.2;
			
			Boolean correct = sttask.answer.equals(out1);
			//trigger_reward( (correct) ? TASK_REWARD : 0.0 );
			this.addReport(String.format("%1$d \t%2$d \t%s \t%d", 
					sttask.count+1, System.nanoTime()+secToNano(0.2)-sttask.starttime, sttask.type, correct));
			
			String new_percept = (++sttask.count > sttask.numtrials) ? "last" : "yes";
			if (new_percept.equals("yes")) {
				sttask.starttime = System.nanoTime() + secToNano(1.2);
			}
			schedule_delayed_action(1.2, new_percept, "");
		}
		
		return latency;
	}
	
	
	@Override
	protected void user_outputListener(List<String> outputs) {

		// Get the output
		String action = outputs.get(0);
		String val2 = outputs.get(1);
		String val3 = outputs.get(2);

		double latency = 0.05;

		// Generate the corresponding input
		int iTemp;
		String sTemp;
		List<String> temp;

		

		rep.addLatency(latency);	// When a report ends from next-instruction, does not include the latency for next-instruction

		if (ed_task.vlist_changed()) {
			for (int i=0; i<ed_task.vlist.length; ++i) {
				try {
					this.setInput(i,ed_task.vlist[i]);
					
				} catch (Exception e) {
					System.err.println("Wrong index....");
				}
			}

			ed_task.vlist_changed = false;
		}
		
		
	}


	@Override
	protected void user_createAgent() {
		// Called from initAgent() and runDebug(), when the agent is created
		ed_task = new ETask();//(1,1,current_sample);
		rep = new Report();
		
		this.reports = new ArrayList<String>();
	}


	@Override
	protected void user_doExperiment() {
		List<SaCondition> sa_conditions = new ArrayList<SaCondition>();
		sa_conditions.add(new SaCondition("ED-ED-EMACS", new String[]{"ed", "ed", "emacs"}, new int[]{115, 54, 44, 42, 43, 28}));
		sa_conditions.add(new SaCondition("EDT-EDT-EMACS", new String[]{"edt", "edt", "emacs"}, new int[]{115, 54, 55, 49, 43, 28}));
		sa_conditions.add(new SaCondition("ED-EDT-EMACS", new String[]{"ed", "edt", "emacs"}, new int[]{115, 54, 63, 44, 41, 26}));
		sa_conditions.add(new SaCondition("EDT-ED-EMACS", new String[]{"edt", "ed", "emacs"}, new int[]{115, 54, 46, 37, 41, 26}));
		sa_conditions.add(new SaCondition("EMACS-EMACS-EMACS", new String[]{"emacs", "emacs", "emacs"}, new int[]{77, 37, 29, 23, 23, 21}));

		for (SaCondition sac : sa_conditions) {
			try {
				this.initAgent();
			} 
			catch (Exception e) {
				System.err.println(e.getMessage());
				return;
			}
			
			task_count = 0;
			rep.taskSetName = sac.name;
			
			for (int i=0; i<6 && !this.agentError; ++i) { // Each subject comes in for 6 'days'
				int j = (int)(1800.0 / (double)sac.trials[i] + 0.5); // How many trials can fit into the 'day'
				String condition = sac.conditions[i/2];

				for (int k=0; k<j && !this.agentError; ++k) {
					this.initTask(condition, condition + "_" + Integer.toString(task_count + 1));
					rep.trialNum = i+1;
					rep.editNum = k+1;
					
					this.runAgent();	// Run until receiving the finish command
					
					if (!this.agentError) {	// abort potentially gets set in the updateEventHandler method
						this.printReports();
						System.out.println("Done: " + sac.name + ", " + condition + " " + Integer.toString(i+1) + "," + Integer.toString(k+1));
					}
					else {
						System.out.println("ERROR RETURNED BY AGENT FOR " + condition + " " + Integer.toString(i+1) + "," + Integer.toString(k+1) + "!");
						break;
					}
				}
			}
			
			if (this.agentError) 
				break;
		}

		//agent.ExecuteCommandLine("clog -c");
		this.agentError = false;
	}


	@Override
	protected void user_errorListener(String arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected void user_initTask(String taskName, String taskSeqName) {
		// Init
		ed_task.init();
		ed_task.edits = new ArrayList<String[]>(edit_tasks.get(task_count));
		rep.init();
		if (taskName != "") {
			// Reset reports for next trial
			rep.taskName = taskName;
			this.reports.clear();
			task_count = (task_count + 1) % 3;
		}
		determine_v();
		
	}

}
