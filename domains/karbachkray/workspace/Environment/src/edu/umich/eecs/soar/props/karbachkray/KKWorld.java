package edu.umich.eecs.soar.props.karbachkray;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Collections;

import edu.umich.eecs.soar.propsutil.PROPsEnvironment;
import edu.umich.eecs.soar.propsutil.LearnConfig;

/**
 * This code replicates the lisp code written by Niels Taatgen for his Actransfer model of this task.
 */
public class KKWorld extends PROPsEnvironment {

	private static final ArrayList<String> baseNumStrings = new ArrayList<String>();
	static {
		baseNumStrings.add("zero"); baseNumStrings.add("one"); baseNumStrings.add("two"); baseNumStrings.add("three"); baseNumStrings.add("four"); 
		baseNumStrings.add("five"); baseNumStrings.add("six"); baseNumStrings.add("seven"); baseNumStrings.add("eight"); baseNumStrings.add("nine");
	}
	
	private boolean inDebug = false;
	private boolean printTaskProgress = true;

	private float task_reward = 10.0f;
	
	private int numSamples = 1;

	private String scheduledABCD = "nil"; 	// What is the current scheduled task: "A", "B", "AB", "C", "D", "CD"
	private boolean isTaskAB = true;		// Is the current task in the AB half (vs the CD half)
	
	private CSTask csTask;
	private StroopTask stroopTask;
	private SWTask swTask;
	
	KKWorld() {
		String proj_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/karbachkray/";
		String props_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/PROPsAgent/";
		//String proj_dir = "C:\\Users\\Bryan\\Documents\\GitHub_Bryan-Stearns\\PROPs\\domains\\karbachkray\\";
		//String props_dir = "C:\\Users\\Bryan\\Documents\\GitHub_Bryan-Stearns\\PROPs\\PROPsAgent\\";

		this.setAgentName("KKAgent");
		this.setPropsDir(props_dir);
		
		//this.setAddressChunkFile(proj_dir + "kk_agent_L1_chunks.soar");
		this.setInstructionsFile(proj_dir + "kk_agent3_instructions.soar");
		this.setSoarAgentFile(proj_dir + "kk_agent3.soar");
		
		this.setIOSize(3, 2);
		
		this.setUserAgentFiles(Arrays.asList("/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/lib_actransfer_prop3_interface.soar",
											 props_dir + "props_rl.soar",
											 proj_dir + "kk_agent_L1_chunks.soar",
											 proj_dir + "kk_agent_smem.soar"));
											 //proj_dir + "kk_agent_L1_chunks_stroop.soar"));
		//this.setUserAgentFiles(Arrays.asList("C:\\Users\\Bryan\\Documents\\GitHub_Bryan-Stearns\\PROPs\\domains\\lib_actransfer_interface.soar", 
		//		 proj_dir + "kk_agent_smem.soar"));


		csTask = new CSTask();
		swTask = new SWTask();
		stroopTask = new StroopTask();
	}
	
	public void setPrintProgress(boolean tf) { printTaskProgress = tf; }

	public void runKKExperiment(String taskName, int samples, ArrayList<LearnConfig> expList) {
		numSamples = samples;
		this.runExperiments(taskName, samples, expList);
	}
	
	public void runKKDebug(String task, int threshold, String mode) {
		inDebug = true;
		this.setVerbose(false);
		this.runDebug(task, task, new LearnConfig(mode, threshold));
		this.setVerbose(true);
		inDebug = false;
	}
	
	/*** Run code for the Countspan task ***/
	
	public void testCountSpan(boolean debug) {
		if (debug) {
			runKKDebug("count-span", 2, "12");
		}
		else {
			do_count_span(0, "TEST");
		}
	}
	
	private void initCountSpan() {
		task_reward = 10.0f;
		csTask.init();
	}
	
	private void setNewCountSpanTrial() {
		csTask.setNewTrial();
	}
	
	private ArrayList<String> toNumberStrings(ArrayList<Integer> numbers) {
		ArrayList<String> retval = new ArrayList<String>();
		for (int i : numbers) {
			retval.add(baseNumStrings.get(i));
		}
		return retval;
	}
	
	private int score_lists(List<String> l1, List<String> l2) {
		int l1size = l1.size();
		if (l1size == 0)
			return 0;
		
		if (l1.get(0).equals(l2.get(0))) 
			return 1 + score_lists(l1.subList(1, l1size), l2.subList(1, l2.size()));
		
		return score_lists(l1.subList(1, l1size), l2.subList(1, l2.size()));
	}

	/** Countspan has the following actions:
	 * say number: used to say the number of blue circles
	   report number: used to type in all the numbers seen
	   attend-next: look at the next visual stimulus on the screen and encode it
 	   suppress-pending: ignore the fixation cross on the screen
	 * @param action The first string output
	 * @param val2 The second string output
	 * @return Latency for the action, in seconds
	 */
	private double count_span_action(String action, String val2) {
		double latency = 0.0;
		
		if (action.equals("say") || action.equals("report")) {
			this.setReward(task_reward);
		}
		
		if (action.equals("say")) {
			// say number: used to say the number of blue circles
			if (csTask.nums.isEmpty()) {
				// We just said the last number, so now we have to report
				this.setPerception(Arrays.asList("report"));
				if (printTaskProgress) {
					System.out.print("# Report: ");
				}
			}
			else {
				// Otherwise, when waiting or saying start the next set of stimuli
				setNewCountSpanTrial();
				this.scheduleInput(0.5, csTask.getNextSymbol());
				this.setPerception(Arrays.asList("pending", null, null));
			}
			
			//System.out.println("# Say " + val2 + " #");
		}
		else if (action.equals("attend-next")) {
			// If there is a field of stimuli, we look at the next one
			latency = 0.15;
			if (csTask.numbers.isEmpty()) {
				this.setPerception(Arrays.asList("last", null, null));
			}
			else {
				this.setPerception(csTask.getNextSymbol());
			}
		}
		else if (action.equals("report")) {
			// If we report, than put the reported number in the responses variable
			csTask.responses.add(val2);
			latency = 0.3;
			if (printTaskProgress) {
				System.out.print(val2 + ", ");
			}
		}
		else if (action.equals("begin")) {
			// CHANGED: begin after the agent initializes count and is ready for the task
			this.scheduleInput(0.5, csTask.getNextSymbol());
			if (printTaskProgress) {
				System.out.println();
			}
		}
		else if (action.equals("suppress-pending")) {
			// suppress-pending: ignore the fixation cross on the screen
			if (this.getInput(0).equals("pending")) {
				this.setPerception(Arrays.asList("waiting"));
			}
			latency = 0.0;
		}
		
		return latency;
	}
	
	private void do_count_span(int day, String cnd) {
		this.setTask("count-span", "count-span");
		initCountSpan();
		
		for (ArrayList<Integer> x : csTask.trials) { // Each trial is a list of numbers to remember
			int l = x.size();
			csTask.nums = new ArrayList<Integer>();
			for (Integer xx : x) {
				csTask.nums.add(xx);
			}
			setNewCountSpanTrial();
			
			this.runAgent();

			//(clear-buffer 'goal)
			// results.add(l, x, csTask.responses.length(), csTask.responses.toString())
			Collections.reverse(csTask.responses); // csTask.trials are in reverse order, so reverse responses to match
			int score = score_lists(csTask.responses, toNumberStrings(x)); 
			this.addReport(String.format("%d %d %d %.3f", l, csTask.responses.size(), score, score / (float)l));
			csTask.responses.clear();
		}
		
		// Report
		this.setOutputFile("KK_digitspan_l"+this.getLearnMode().toString()+"_s"+numSamples+".txt");
		this.printReports(String.format("%d %s ", day, cnd));
		this.clearReports();
	}
	
	/*** Run code for the Stroop task ***/

	public void testStroop() {
		runKKDebug("stroop", 2, "12");
	}
	
	private void initStroop() {
		task_reward = 13.0f;
		stroopTask.init();
		this.clearReports();
		stroopTask.startTime = this.getElapsedTime()+this.secToMilli(1.0);  // "start at t=1 seconds"
		this.scheduleInput(1.0, Arrays.asList("yes", "red-color", (stroopTask.count % 2 == 0) ? "train-word" : "blue-word"));						 // Schedule first stimulus
	}
	
	/**
	 * The Stroop actions are the following:
		get-property: 	Can be called with color-property, in which case it only gives the color of the ink, otherwise it gives both word and color
		say: 			Say the answer
	 * @param action The first string output
	 * @param val2 The second string output
	 * @return Latency for the action, in seconds
	 */
	private double stroop_action(String action, String val2) {
		double latency = 0.05;
		
		switch (action)
		{
			case "say": {
				latency = 0.2;
				boolean correct = val2.equals(stroopTask.answer);
				this.setReward((correct ? task_reward : 0));
				this.addReport(String.format("%d %s %s %.3f", (1+stroopTask.count), stroopTask.type, correct ? "1" : "0", this.milliToSec(this.secToMilli(0.2)+this.getElapsedTime()-stroopTask.startTime)));
				String new_percept;
				if (++stroopTask.count > stroopTask.numTrials) {
					new_percept = "last";
				}
				else {
					stroopTask.startTime = this.getElapsedTime() + this.secToMilli(1.2);
					new_percept = "yes";
				}
				this.setPerception(Arrays.asList("pending", null, null));
				
				if (printTaskProgress) {
					System.out.print("*");
				}
				
				// Set up next trial
				String word_str = null; 			// "If we ask for color specifically leave out the distractor"
				if (stroopTask.count % 2 == 0) {
					stroopTask.type = "NEUTRAL";
					word_str = "train-word";
				}
				else {
					stroopTask.type = "INCONGRUENT";
					if (!val2.equals("color-property")) {
						word_str = "blue-word"; 
					}
				}
				stroopTask.answer = "red-concept";
				// Schedule it here
				this.scheduleInput(1.2, Arrays.asList(new_percept,"red-color",(stroopTask.count % 2 == 0) ? "train-word" : "blue-word"));
				
				break;
			}
			case "get-property":  {
				// CHANGED: This action is never used; made get-property a result of internal focusing rather than environment action
				String word_str = null; 			// "If we ask for color specifically leave out the distractor"
				if (stroopTask.count % 2 == 0) {
					stroopTask.type = "NEUTRAL";
					word_str = "train-word";
				}
				else {
					stroopTask.type = "INCONGRUENT";
					if (!val2.equals("color-property")) {
						word_str = "blue-word"; 
					}
				}
				stroopTask.answer = "red-concept";
				
				this.setInput(1, "red-color");	// Leave input slot 0 as "yes"
				this.setInput(2, word_str);
				
				latency = 0.2;
				break;
			}
		}
		
		return latency;
	}

	private void do_stroop(int day, String condition) {
		this.setTask("stroop", "stroop");
		this.setOutputFile("KK_stroop_l"+this.getLearnMode().toString()+"_s"+numSamples+".txt");
		
		for (int i=1; i<=4; ++i) {
			initStroop();
			
			if (printTaskProgress) {
				System.out.println();
			}
			this.runAgent();
			
			this.printReports(String.format("STROOP %s %d %d ", condition, i, day));
			this.clearReports();
		}
		
	}
	
	/*** Run code for task switching ***/
	
	public void testAB(String taskName) {
		Map<String, String> aMap = new HashMap<String, String>();
		aMap.put("A", "single-task-A");
		aMap.put("B", "single-task-B");
		aMap.put("AB", "task-switching-AB");
		aMap.put("C", "single-task-C");
		aMap.put("D", "single-task-D");
		aMap.put("CD", "task-switching-CD");
		
		isTaskAB = (taskName.contains("A") || taskName.contains("B"));
		scheduledABCD = taskName; // TODO: B,C,D individually for chunks
		String tsk = aMap.get(scheduledABCD);

		runKKDebug(tsk, 2, "12");

		//this.printReports(String.format("%s %s %s", "TEST", "0", scheduledABCD));
		this.clearReports();
	}
	
	private void initSingleTaskAB() {
		task_reward = 10.0f;
		swTask.init();
		this.clearReports();
		swTask.startTime = this.getElapsedTime()+this.secToMilli(1.425);	// We start after 1.425 seconds
		this.scheduleInput(1.425, Arrays.asList("yes"));					// Schedule the first stimulus
	}
	
	private void do_singleTaskAB(String switch_cond, int day, String sched_entry, String task) {
		this.setTask(task, task);
		initSingleTaskAB();

		if (printTaskProgress) {
			System.out.println();
		}
		this.runAgent();

		if (isTaskAB) {
			this.setOutputFile("KK_task-switching_l"+this.getLearnMode().toString()+"_s"+numSamples+".txt");
			this.printReports(String.format("%s %d %s ", switch_cond, day, sched_entry));
		}
		this.clearReports();
	}
	
	/**
	 * IF TASK AB: Do the p/m parts of task A and B. It should implement get-property food/size, press-key key and wait
	 * IF TASK CD: Do the p/m parts of task C and D. It should implement get-property food/size, press-key key and wait
	 * @param action The first string output
	 * @param val2 The second string output
	 * @return Latency for the action, in seconds
	 */
	private double singleTaskABCD_action(String action, String val2) {
		double latency = 0.05;
		
		switch (action) {
		case "get-property":
			if (isTaskAB) {
				if (val2.equals("food-property")) {
					this.setPerception(Arrays.asList("yes", new Random().nextBoolean() ? "fruit" : "vegetable"));
				}
				if (val2.equals("size-property")) {
					this.setPerception(Arrays.asList("yes", null, new Random().nextBoolean() ? "small" : "large"));
				}
			}
			else {
				if (val2.equals("transport-property")) {
					this.setPerception(Arrays.asList("yes", new Random().nextBoolean() ? "plane" : "car"));
				}
				if (val2.equals("number-property")) {
					this.setPerception(Arrays.asList("yes", null, new Random().nextBoolean() ? "one" : "two"));
				}
			}
			latency = 0.13;
			break;
		case "press-key":
			latency = 0.2;
			
			String tp = "SINGLE";
			if (scheduledABCD.equals("AB")) {
				tp = (swTask.count % 2 == 0) ? "SWITCH" : "REPEAT"; // Moved here from user_doExperiment
			}
			this.addReport(String.format("%s %d %.3f", tp, swTask.count+1, this.milliToSec(this.secToMilli(0.2)+this.getElapsedTime()-swTask.startTime)));
			
			String newPercept;
			if (++swTask.count > swTask.numTrials) {
				newPercept = "last";
			}
			else {
				swTask.startTime = this.getElapsedTime() + this.secToMilli(1.425); // Start after 1.425 sec
				newPercept = "yes";
			}
			this.scheduleInput(1.625, Arrays.asList(newPercept,null,null)); // This action is now asynchronous, triggers next stimulus at current time plus 1625 ms (1425 after key press)
			// In Actransfer, schedule-delayed-action sets percept to pending, and ?state> changing to t until the time has elapsed, after which it will revert to nil and the change is made.
			
			this.setReward(task_reward);
			this.setPerception(Arrays.asList("pending", null, null));
			
			//System.out.println("# Press-key " + val2 + " #");
			if (printTaskProgress) {
				System.out.print("*");
			}
			
			break;
		}
		
		return latency;
	}
	
	
	@Override
	/**
	 * Called during initAgent(), after sourcing agent files but before any custom cmd's are run.
	 */
	protected void user_createAgent() {
		/*lastDC = 0;*/
	}

	/**
	 * Defines one sample. Will be called repeatedly for each sample under this.runExperiments().
	 */
	@Override
	protected void user_doExperiment() {
		List<String> conditions = new ArrayList<>(Arrays.asList("SINGLE", "SWITCH"));
		Map<String, String> aMap = new HashMap<String, String>();
		aMap.put("A", "single-task-A");
		aMap.put("B", "single-task-B");
		aMap.put("AB", "task-switching-AB");
		aMap.put("C", "single-task-C");
		aMap.put("D", "single-task-D");
		aMap.put("CD", "task-switching-CD");
		
		/** The experiment is as follows:
		 * 		Blocks are always 17 trials
		 * 		Day 1, tasks A and B:
		 * 			Two single task blocks for training that are discarded
		 * 			2 single - 2 mixed - 2 single - 2 mixed - single - 2 mixed - single - 2 mixed - single - 2 mixed - single - 2 mixed
		 * 
		 * 		Days 2-5 (4 days), tasks C and D
		 * 			Both conditions:
		 * 			Two practice blocks followed by 24 experimental blocks
		 * 
		 * 		Day 6: same as day 1
		 **/
		
		for (String cnd : conditions) {
			if (!this.initAgent()) return;
			
			System.out.println("\n\n    *** Beginning " + cnd + " condition ***");
			
			//// Day 1 ////
			
			System.out.println("\n\n Day 1: Begin CountSpan test");
			do_count_span(1, cnd);
			if (!this.initAgent()) return;		// (reset) -- Why is this reset here, and why is count-span first here but second on day 6?
			
			System.out.println("\n\n Day 1: Begin Stroop test");
			do_stroop(1, cnd);
			if (!this.initAgent()) return;
			
			List<String> scheduleAB = new ArrayList<>(Arrays.asList("A","B","A","B","AB","AB","A","B","AB","AB","A","AB","AB","B","AB","AB","A","AB","AB","B","AB","AB"));

			System.out.println("\n\n Day 1: Begin Task-switching test");
			isTaskAB = true;			
			for (String x : scheduleAB) {
				scheduledABCD = x;
				String tsk = aMap.get(x);
				
				do_singleTaskAB(cnd,1,x,tsk);
			}

			if (!this.initAgent()) return;
			
			
			//// Day 2-5 ////
			
			List<String> scheduleCD = (cnd.equals("SINGLE")) ? 
						new ArrayList<>(Arrays.asList("C","D","C","D","C","D","C","D","C","D","C","D","C","D","C","D","C","D","C","D","C","D","C","D","C","D"))
						: new ArrayList<>(Arrays.asList("CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD","CD"));

			isTaskAB = false;
			for (int day=2; day<=5; ++day) {
				System.out.println("\n\n Day " + day + ": Begin CD training");
				for (String x : scheduleCD) {
					scheduledABCD = x;
					String tsk = aMap.get(x);

					do_singleTaskAB(cnd,day,x,tsk);
					// (clear-buffer 'goal)
				}
			}
			
			
			//// Day 6 ////

			System.out.println("\n\n Day 6: Begin Stroop test");
			do_stroop(6, cnd);
			System.out.println("\n\n Day 6: Begin CountSpan test");
			do_count_span(6, cnd);

			System.out.println("\n\n Day 6: Begin Task-switching test");
			isTaskAB = true;
			for (String x : scheduleAB) {
				scheduledABCD = x;
				String tsk = aMap.get(x);
				
				do_singleTaskAB(cnd,6,x,tsk);
			}

			scheduledABCD = "nil";
		}
		
	}

	@Override
	protected void user_outputListener(List<String> outputs) {
		// Get the output
		String action = outputs.get(0);
		String val2 = outputs.get(1);

		double latency = 0.0;

		if (this.getTask().equals("count-span")) {
			latency = count_span_action(action, val2);
		}
		else if (this.getTask().equals("stroop")) {
			latency = stroop_action(action, val2);
		}
		else {	// Assume that if it is not count-span or stroop it is one of the ABCD cases
			latency = singleTaskABCD_action(action, val2);
		}
		
		this.addAgentLatency(secToMilli(latency));

		/*	// Get the current number of chunks
			int chunkCount = agent.ExecuteCommandLine("p -c").split("\n").length;
			// Store the result
			int DC = agent.GetDecisionCycleCounter();
		*/

	}

	@Override
	/**
	 * Called just before every time the agent first runs from a call to a world function
	 */
	protected void user_agentStart() {
		this.clearReports();
		/*f (inDebug) {
			if (this.getTask().equals("count-span")) {
				initCountSpan();
				csTask.nums = new ArrayList<Integer>(Arrays.asList(3,4,5));
				setNewCountSpanTrial();
			}
		}*/
	}
	
	@Override
	/**
	 * Called after the agent stops due to finishing a task or to an error, after user_outputListener and user_errorListener
	 */
	protected void user_agentStop() {
		if (inDebug) {
			user_updateTask();
		}
		else {
			user_updateTask();
		}
	}

	@Override
	protected void user_errorListener(String err) {
		System.err.println("ERROR DETECTED!");
	}

	@Override
	/**
	 * Called after this.setTask()
	 */
	protected void user_updateTask() {
		if (inDebug) {
			if (this.getTask().equals("count-span")) {
				initCountSpan();
				csTask.nums = new ArrayList<Integer>(Arrays.asList(3,4,5));
				setNewCountSpanTrial();
			}
			else if (this.getTask().equals("stroop")) {
				initStroop();
			}
			else {
				initSingleTaskAB();
			}
		}
		// Start perception with "pending"
		this.setPerception(Arrays.asList("pending", null, null));
		this.applyNewInputs();
	}


}