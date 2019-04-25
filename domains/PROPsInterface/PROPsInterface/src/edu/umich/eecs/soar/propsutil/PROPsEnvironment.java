package edu.umich.eecs.soar.propsutil;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import javafx.util.Pair;

import sml.Identifier;
import sml.Kernel;
import sml.Kernel.UpdateEventInterface;
import sml.WMElement;
import sml.smlUpdateEventId;
import sml.Agent;
import edu.umich.soar.debugger.SWTApplication;

public abstract class PROPsEnvironment implements UpdateEventInterface/*, RunEventInterface*/ {
	private boolean using_props = true;
	private boolean verbose = true;
	private boolean save_percepts = false;
	private boolean testing = false;
	private boolean running = false;
	private boolean agentError = false;
	private boolean initOkay = false;
	private String props_dir = "";
	
	private String agentName = "PROPsAgent";
	private int agent_num = 0;
	final private String CMD_SAY = "say",
							CMD_FINISH = "finish",
							//CMD_INSTR_NEXT = "props-command",
							CMD_ERROR = "error",
							ATTR_INPUT_CHANGED = "input-changed";
	private String outFileName = "AgentOutput.txt",
					agent_instruction_file = "",
					agent_genericsoar_file = "";
	private String taskName = "TEST",
					taskSequenceName = "TEST";
	
	private LearnConfig currentLearnMode;
	private int currentSampleNum;
	
	private Identifier input_link;
	protected Kernel kernel;
	protected Agent agent = null;
	private WMElement inTask,
					  inTaskSeqName,
					  inputChangedWME,
					  rewardWME;
	private boolean inputChanged;
	private float rewardInput = 0.0f;
	
	private long elapsedAgentMSEC = 0;
	private long elapsedAgentDCs = 0;
	private double msecPerDecision = 50;
	
	private int numAgentInputs = 3,
				  numAgentOutputs = 3;
	private ArrayList<WMElement> inputWMEs;
	private ArrayList<GhostWME> newInputs;	// a buffer for the user-made environment input
	private ArrayList<String> inputs;
	private ArrayList<String> outputs;
	private PriorityQueue<ScheduledInput> delayedInputs;

	private ArrayList<String> userAgentFiles;
	private ArrayList<String> reports;

	//abstract protected void user_initEnvironment();
	abstract protected void user_doExperiment();
	abstract protected void user_outputListener(List<String> outputs);
	abstract protected void user_createAgent();
	abstract protected void user_agentStart();
	abstract protected void user_agentStop();
	abstract protected void user_updateTask();
	abstract protected void user_errorListener(String err);
	
	public PROPsEnvironment() {
		kernel = Kernel.CreateKernelInNewThread();
		kernel.SetAutoCommit(true);
		kernel.RegisterForUpdateEvent(smlUpdateEventId.smlEVENT_AFTER_ALL_OUTPUT_PHASES, this, null);
		
		userAgentFiles = new ArrayList<String>();
		reports = new ArrayList<String>();
		
		inputWMEs = new ArrayList<WMElement>(numAgentInputs);
		newInputs = new ArrayList<GhostWME>(numAgentInputs);
		inputs = new ArrayList<String>(numAgentInputs);
		outputs = new ArrayList<String>(numAgentOutputs);
		delayedInputs = new PriorityQueue<ScheduledInput>(Comparator.comparing(ScheduledInput::getMoment));
		
		currentLearnMode = new LearnConfig(false, true, false, false);	// "12"
		currentSampleNum = 0;
		
		//user_initEnvironment();
		//   User needs to set: 
		// agentName, PROPS_DIR, PROJ_DIR, 
		// agent_condchunk_file, agent_addresschunk_file, agent_instruction_file, agent_fetchseq_file, agent_genericsoar_file,
		// numAgentInputs, numAgentOutputs, 
		// userAgentFiles
	}
	

	private boolean loadConfigProductions(LearnConfig mode) {
				
		// Check '3', w/ no '1' or '2'
		/*if (mode.learnsAuto() && !mode.learnsCognitive() && !mode.learnsAssociative()) {
			agent.LoadProductions(props_dir + "props_learn_l3only.soar");
			return true;
		}*/
		
		// Check '1' - whether addressing chunks should be sourced
		if (mode.learnsCognitive()) {
			agent.LoadProductions(props_dir + "props_learn_l1.soar");
		}
		
		// Check '2'
		if (mode.learnsAssociative()) {
			agent.LoadProductions(props_dir + "props_learn_l2.soar");
		}
		
		// Check '3'
		if (mode.learnsAuto()) {
			agent.LoadProductions(props_dir + "props_learn_l3.soar");
		}
		
		return true;
	}
	
	private void loadUserAgentFiles() {
		// Load the list of user-provided agent files
		for (String s : userAgentFiles) {
			agent.LoadProductions(s);
		}
		
	}
	
	public boolean initAgent() {
		if (agent != null) {
			if (verbose)
				agent.ExecuteCommandLine("clog -c");
			kernel.DestroyAgent(agent);
			// Clear the input WMEs, since the agent was destroyed
			for (int i=0; i<inputWMEs.size(); ++i) {
				inputWMEs.set(i, null);
			}
			inTask = null;
			inTaskSeqName = null;
			inputChangedWME = null;
		}
		
		agent = kernel.CreateAgent(agentName + (agent_num++));
		//agent.RegisterForRunEvent(smlRunEventId.smlEVENT_BEFORE_INPUT_PHASE, this, null);
		agent.SetBlinkIfNoChange(false);
		agentError = false;
		
		// Reset IO
		input_link = agent.GetInputLink();
		
		setIOSize(numAgentInputs, numAgentOutputs);	// Resets input lists
		running = false;
		
		elapsedAgentMSEC = 0;
		elapsedAgentDCs = 0;

		// Load the agent files
		if (using_props) {

			if (props_dir == "") {
				System.err.println("ERROR: User did not provide the PROPS directory! Aborting.");
				initOkay = false;
				return false;
			}

			if (agent_instruction_file == "") {
				System.err.println("ERROR: User did not provide the SMEM instruction file! Aborting.");
				initOkay = false;
				return false;
			}
			
			agent.LoadProductions(props_dir + "_firstload_props.soar");			// props library
			agent.LoadProductions(agent_instruction_file);		// The props instructions

			agent.ExecuteCommandLine("chunk confidence-threshold " + currentLearnMode.getChunkThreshold());
			
			if (!loadConfigProductions(currentLearnMode)) {
				initOkay = false;
				return false;
			}
		}
		else if (agent_genericsoar_file != "") {
			agent.LoadProductions(agent_genericsoar_file);	// editors agent to be learned (for testing)
		}
		else {
			System.err.println("ERROR: User did not provide the generic Soar agent file! Aborting.");
			initOkay = false;
			return false;
		}
		
		loadUserAgentFiles();
		user_createAgent();
		
		// Run any extra Soar commands for this agent configuration
		if (currentLearnMode.commands != null) {
			for (String cmd : currentLearnMode.commands)
			agent.ExecuteCommandLine(cmd);
		}

		reports = new ArrayList<String>();
		
		if (verbose) {
			agent.ExecuteCommandLine("watch 1");
			if (!testing)
				agent.ExecuteCommandLine("watch --learn 1");
			agent.ExecuteCommandLine("clog -A verbose_" + outFileName);
		}
		else if (!testing){
			agent.ExecuteCommandLine("watch 0");
			agent.ExecuteCommandLine("output enabled off");
			agent.ExecuteCommandLine("output agent-writes off");
		}
		
		if (save_percepts) {
			agent.ExecuteCommandLine("save percepts -o " + agentName + "Percepts.spr -f");
			agent.ExecuteCommandLine("save agent " + agentName + "_pre.soar");
		}
		
		initOkay = true;
		return true;
	}
	
	public void setUserAgentFiles(List<String> filenames) {
		userAgentFiles = new ArrayList<String>(filenames);
	}


	public boolean isUsingProps() { return using_props; }
	public boolean isVerbose() { return verbose; }
	public boolean isSavingPercepts() { return save_percepts; }
	public boolean hasError() { return agentError; }
	
	public LearnConfig getLearnMode() { return currentLearnMode; }
	public int getCurrentSample() { return currentSampleNum; }
	public String getTask() { return taskName; }
	public String getTaskInstance() { return taskSequenceName; }
	
	public long secToNano(double sec) {return (long) (sec*1000000000l);}
	public long secToMilli(double sec) {return (long) (sec*1000l);}
	public double nanoToSec(long nano) {return (double) nano/1000000000.0;}
	public double nanoToMilli(long nano) {return (double) nano/1000000.0;}
	public double milliToSec(long milli) {return (double) milli/1000.0;}
	
	public void setPropsDir(String dir) { props_dir = dir; }
	
	public void setInstructionsFile(String filename) { agent_instruction_file = filename; }
	public void setSoarAgentFile(String filename) { agent_genericsoar_file = filename; }
	
	public void setAgentName(String name) { agentName = name; }
	public void setConfig(LearnConfig config) { currentLearnMode = config; }
	
	public void setUseProps(boolean mode) { using_props = mode; }
	public void setVerbose(boolean mode) { verbose = mode; }
	public void setSavePercepts(boolean mode) { save_percepts = mode; }
	
	public void setOutputFile(String filename) { outFileName = filename; }
	
	/**
	 * Configures input and output slots, and clears any existing input.
	 * @param inputSize The max number of inputs
	 * @param outputSize The max number of outputs
	 */
	public void setIOSize(int inputSize, int outputSize) {
		numAgentInputs = inputSize;
		numAgentOutputs = outputSize;
		
		for (int i=0; i<inputWMEs.size(); ++i) {
			WMElement in = inputWMEs.get(i);
			if (in != null) {
				in.DestroyWME();
				inputWMEs.set(i, null);
			}
		}
		
		inputWMEs = new ArrayList<WMElement>(numAgentInputs);
		newInputs = new ArrayList<GhostWME>(numAgentInputs);
		inputs = new ArrayList<String>(numAgentInputs);
		outputs = new ArrayList<String>(numAgentOutputs);
		
		inputChanged = false;
		if (inputChangedWME != null) {
			inputChangedWME.DestroyWME();
			inputChangedWME = null;
		}
		
		for (int i=0; i<numAgentInputs; ++i) {
			inputWMEs.add(null);
		}
		for (int i=0; i<numAgentInputs; ++i) {
			newInputs.add(null);
		}
		for (int i=0; i<numAgentInputs; ++i) {
			inputs.add(null);
		}
		for (int i=0; i<numAgentOutputs; ++i) {
			outputs.add(null);
		}
	}
	
	public String getInput(int slot) {
		if (slot < 0 || slot >= numAgentInputs)
			return null;
		return inputs.get(slot);
	}
	
	/**
	 * @return The ordered list of the contents of the input-link slots.
	 */
	public List<String> getPerception() {
		return inputs;
	}

	/**
	 * Set the agent's input-link slots 
	 * @param perception An ordered list of inputs for each slot
	 */
	protected void setPerception(List<String> perception) {
		int lim = numAgentInputs;
		if (perception.size() < numAgentInputs)
			lim = perception.size();
		
		for (int i=0; i<lim; ++i) {
			try {
				this.setInput(i,perception.get(i));
			} catch (Exception e) {
				System.err.println("Indexing error when setting perception.");
			}
		}
	}
	
	protected void setInput(int slot, String input) {
		if (slot < 0 || slot >= numAgentInputs) {
			throw new RuntimeException("ERROR in setInput(): Input slot must be between 0 and " + (numAgentInputs-1));
		}
		if (input != null) {
			newInputs.set(slot, new GhostWME(GhostWME.Type.STRING, "slot" + (slot+1), input));
		}
		else {
			newInputs.set(slot, null);
		}
	}
	protected void setInput(int slot, int input) {
		if (slot < 0 || slot >= numAgentInputs) {
			throw new RuntimeException("ERROR in setInput(): Input slot must be between 0 and " + (numAgentInputs-1));
		}
		newInputs.set(slot, new GhostWME(GhostWME.Type.INT, "slot" + (slot+1), Integer.toString(input)));
	}
	protected void setInput(int slot, double input) {
		if (slot < 0 || slot >= numAgentInputs) {
			throw new RuntimeException("ERROR in setInput(): Input slot must be between 0 and " + (numAgentInputs-1));
		}
		newInputs.set(slot, new GhostWME(GhostWME.Type.FLOAT, "slot" + (slot+1), Double.toString(input)));
	}
	/*protected void setInput(int slot, boolean input) throws Exception {
		if (slot < 0 || slot >= numAgentInputs) {
			throw new Exception("ERROR: Input slot must be between 1 and " + numAgentInputs);
		}
		newInputs.set(slot, new GhostWME(GhostWME.Type.BOOLEAN, "slot" + (slot+1), Boolean.toString(input)));
	}*/
	
	private WMElement makeInputWME(GhostWME w) {
		if (w.type == GhostWME.Type.INT) {
			return input_link.CreateIntWME(w.attribute, w.i_val);
		}
		else if (w.type == GhostWME.Type.FLOAT){
			return input_link.CreateFloatWME(w.attribute, w.f_val);
		}
		else { // Assume STRING
			return input_link.CreateStringWME(w.attribute, w.s_val);
		}
	}
	
	/** 
	 * Flush all new inputs to the agent's input-link
	 * @return Whether the input-link changes at all.
	 */
	protected boolean applyNewInputs() {
		boolean didChange = false;
		// Only change WMEs if they are different (blinking can cause problems)
		for (int i=0; i<numAgentInputs; ++i) {
			GhostWME newIn = newInputs.get(i);
			WMElement in = inputWMEs.get(i);
			// Test if the wmes are equal
			if (in == null && newIn == null) {
				continue;
			}
			else if (newIn == null) {
				didChange = true;
				in.DestroyWME();
				inputWMEs.set(i, null);
				inputs.set(i, null);
			}
			else if ((in == null) || (newIn.blink_new || !newIn.equals(in))) {
				didChange = true;
				if (in != null)
					in.DestroyWME();
				inputWMEs.set(i, makeInputWME(newIn));
				inputs.set(i, newIn.toString());
				//newInputs.set(i, null);
				newIn.blink_new = false;		// Don't recreate this input unless told otherwise
			} 
		}
		
		if (didChange) {
			inputChanged = true;
		}
		
		return didChange;
	}
	
	/**
	 * Remove all input WMEs
	 */
	protected void clearPerception() {
		for (int i=0; i<inputWMEs.size(); ++i) {
			try {
				setInput(i, null);
			} catch (Exception e) {
				e.printStackTrace();	// Shouldn't happen
			}
		}
		applyNewInputs();	// Apply the clearing
		for (int i=0; i<numAgentInputs; ++i) {
			if (inputWMEs.get(i) != null) {
				inputWMEs.get(i).DestroyWME();
				inputWMEs.set(i, null);
			}
			inputs.set(i, null);
		}
	}
	
	/**
	 * Send the given actions to the input link after the given delay, in real time.
	 * @param delaysec The number of seconds to delay until the inputs are made
	 * @param actions The inputs to deliver, in slot order
	 */
	protected void scheduleInput(double delaysec, List<String> actions) {
		// Add the new schedule entry
		long moment = elapsedAgentMSEC + secToMilli(delaysec);
		delayedInputs.add(new ScheduledInput(moment, actions));	// PriorityQueue sorting recalculates when to trigger the next scheduled entry
	}
	
	/**
	 * Clear the list of existing delayed inputs
	 */
	protected void clearScheduledInputs() {
		delayedInputs.clear();
	}
	
	/**
	 * Set the reward value for the agent for this cycle.
	 * @param reward The reward sent after the output cycle, for this cycle only
	 */
	protected void setReward(float reward) {
		rewardInput = reward;
	}
	
	/**
	 * Notify the world of additional elapsed time used by the agent (besides normal decision cycle time). 
	 * @param msec The extra time taken, in milliseconds.
	 */
	protected void addAgentLatency(long msec) {
		elapsedAgentMSEC += msec;
	}
	
	/**
	 * @return The elapsed agent time in milliseconds
	 */
	public long getElapsedTime() {
		return elapsedAgentMSEC;
	}
	
	/**
	 * @return The elapsed agent time in milliseconds
	 */
	public long getElapsedCycles() {
		return elapsedAgentDCs;
	}
	
	public void setTask(String task, String taskInstance) {
		if (agent == null) {
			if (!initAgent())
				throw new RuntimeException();
		}
		
		// Clear input if any
		//clearPerception();
		clearScheduledInputs();
		
		// Reset task commands
		if (inTask != null) {
			if (!inTask.GetValueAsString().equals(task)) {
				inTask.DestroyWME();
				inTask = input_link.CreateStringWME("task", task);		// for lib-actr to set Gtask
				inputChanged = true;
			}
		}
		else 
			inTask = input_link.CreateStringWME("task", task);
		
		if (inTaskSeqName != null) {
			if (!inTaskSeqName.GetValueAsString().equals(taskInstance)) {
				inTaskSeqName.DestroyWME();
				inTaskSeqName = input_link.CreateStringWME("task-sequence-name", taskInstance);	// for specifying a known PROPs instruction sequence
				inputChanged = true;
			}
		}
		else
			inTaskSeqName = input_link.CreateStringWME("task-sequence-name", taskInstance);
		
		this.taskName = task;
		this.taskSequenceName = taskInstance;
		
		user_updateTask();
	}


	/** 
	 * Clear the output file if it already exists, create it if it doesn't.
	 * @return True if successful, false if an filesystem error occurred.
	 */
	public boolean clearOutputFile() {
		if (!verbose) {
			try {
				PrintWriter writer;
				writer = new PrintWriter(outFileName);
				writer.print("");
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * Add a string to the list of reports
	 * @param r The report entry to store
	 */
	public void addReport(String r) {
		reports.add(r);
	}
	public void clearReports() {
		reports.clear();
	}
	
	/**
	 * Print all current reports
	 */
	public void printReports() {
		printReports("");
	}
	/**
	 * Print all current reports
	 * @param header A header to include in front of each report line
	 */
	public void printReports(String header) {
		try(FileWriter fw = new FileWriter(outFileName, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			for (String s : reports) {
				String str = header + s;
				out.println(str);
				if (verbose)
					agent.ExecuteCommandLine("echo\t" + str);
			}
		}
		catch (IOException e) {
			System.err.println("ERROR: Unable to append to file '" + outFileName + "'");
		}
	}
	
	/**
	 * Run a task in the Soar Java Debugger
	 * @param task The name of the task
	 * @param taskSeq The name of the task instance (make same as task if no instance variation)
	 * @param threshold The chunking threshold to use
	 * @param learnMode The learning configuration to use
	 */
	public void runDebug(String task, String taskSeq, LearnConfig mode) {
		testing = true;
		currentLearnMode = mode;
		currentSampleNum = 1;
		
		if (task.compareTo(taskSeq) != 0)
			setOutputFile(task + "_" + taskSeq + "_debug_out.txt");
		else
			setOutputFile(task + "_debug_out.txt");
		
		initAgent();

		if (!initOkay)
			return;
		
		clearOutputFile();
		setTask(task, taskSeq);

		user_agentStart();
		
		try {
			SWTApplication swtApp = new SWTApplication();
			swtApp.startApp(new String[]{"-remote"});
			
			agent.KillDebugger();
			testing = false;
			System.exit(0);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		testing = false;
	}
	
	public void runAgent(int steps) {
		if (!initOkay)
			return;
		if (!running) {
			user_agentStart();
			running = true;
		}
		agent.RunSelf(steps);   // Run for the given number of decision cycles
	}
	public void runAgent() {
		if (!initOkay)
			return;
		if (!running) {
			user_agentStart();
			running = true;
		}
		agent.RunSelfForever(); // Run until receiving the finish command
	}
	

	/*public void makeSpreadingChunks(List<Pair<String,String>> taskSeqs, String fname) { makeSpreadingChunks(taskSeqs, fname, true); }
	public void makeSpreadingChunks(List<Pair<String,String>> taskSeqs, String fname, boolean useManualSeq) {
		if (taskSeqs.size() == 0) {
			System.err.println("No tasks given to makeSpreadingChunks. Nothing to do.");
			return;
		}
		
		testing = true;
		outFileName = "make_condition_chunks.txt";
		currentLearnMode.set(true, false, false, true, true, useManualSeq, false, false, false);
		currentLearnMode.setChunkThreshold(1);

		System.out.println("Starting condition chunking run...");
		makeChunkingRuns(taskSeqs, fname);
		testing = false;
	}
	public void makeAddressingChunks(List<Pair<String,String>> taskSeqs, String fname) { makeAddressingChunks(taskSeqs, fname, true); }
	public void makeAddressingChunks(List<Pair<String,String>> taskSeqs, String fname, boolean useManualSeq) {
		if (taskSeqs.size() == 0) {
			System.err.println("No tasks given to makeAddressingChunks. Nothing to do.");
			return;
		}
		
		testing = true;
		outFileName = "make_address_chunks.txt";
		currentLearnMode.set(false, false, false, false, false, useManualSeq, false, true,false);
		currentLearnMode.setChunkThreshold(1);
		
		System.out.println("Starting address chunking run...");
		makeChunkingRuns(taskSeqs, fname);
		testing = false;
	}*/
	
	@SuppressWarnings("restriction")
	public void makeChunkingRuns(List<Pair<String,String>> taskSeqs, String fname) {

		try {
			initAgent();
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		clearOutputFile();
		
		int chunksDelta = 0,
			chunksCurr = 0,
			chunksPrev = 0;
		int cont = currentLearnMode.getChunkThreshold() + 2;
		int count = 0;
		
		for (Pair<String,String> task : taskSeqs) {
			setTask(task.getKey(), task.getValue());
			
			while (cont > 0) {
				currentSampleNum = ++count;
				runAgent();
				
				// Check how many chunks were learned this past run
				chunksPrev = chunksCurr;
				chunksCurr = agent.ExecuteCommandLine("p -c").split("\n").length;
				chunksDelta = chunksCurr - chunksPrev;
				
				if (chunksCurr == 0 || chunksPrev == 0)
					continue;
				
				if (chunksDelta == 0)
					cont--;
				else
					cont = currentLearnMode.getChunkThreshold() + 2;
			}
			cont = currentLearnMode.getChunkThreshold() + 1;
			
			System.out.println("Completed " + task.getKey() + " : " + task.getValue() + ", " + count + " runs, " + chunksCurr + " chunks.");
			count = 0;
		}
		
		System.out.println("\nDone! Saving chunks to: " + fname);
		
		// Save chunks
		try(FileWriter fw = new FileWriter(fname, false);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.println(agent.ExecuteCommandLine("p -fc"));
		}
		catch (IOException e) {
			System.err.println("ERROR: Unable to write to file '" + fname + "'");
		}
	}
	
	/**
	 * Return the given string, with the span of [first,last] inclusive removed, if it exists.
	 * The first instance of <first> and the last instance of <last> are used as bounds.
	 * If one bound exists but not the other, no extraction is made.
	 * @param str The string to use for returning
	 * @param first The character for the beginning of the extract
	 * @param last The character for the end of the extract
	 * @return Same as <str>, minus the extract
	 */
	/*private String removeStringComment(String str, char first, char last) {
		int firstInd = str.indexOf(first);
		int lastInd = str.lastIndexOf(last);
		
		if (firstInd == -1 || lastInd == -1) {
			return str;
		}
		
		return str.substring(0, firstInd) + str.substring(lastInd+1, str.length());
	}

	private void saveSmemSnip(List<String> toSave, int start) {
		List<String> smem = Arrays.asList(agent.ExecuteCommandLine("p @").split("\n"));
		smem = smem.subList(start, smem.size());
		
		Map<String, String> idMap = new HashMap<String, String>();
		
		toSave.add("smem --add {");
		for (String s : smem) {
			// Remove activation printout
			String line = removeStringComment(s, '[', ']');
			if (!line.contains("^")) {
				continue;
			}
			
			// Replace IDs with variables
			int nextID = 1;
			int atInd = line.indexOf('@');
			while (atInd != -1) {
				String id = line.substring(atInd).split(" ")[0];
				if (!idMap.containsKey(id)) {
					idMap.put(id, "<E" + nextID++ + ">");
				}
				line = line.replace(id, idMap.get(id));
				
				atInd = line.indexOf('@');
			}
			
			// Add this smem entry
			toSave.add(line);
		}
		toSave.add("}");
		
	}
	
	@SuppressWarnings("restriction")
	public void makeFetchSets(List<Pair<String,String>> taskSeqs, String fname, boolean useSpreading) {
		if (taskSeqs.size() == 0) {
			System.err.println("No tasks given to makeFetchSets. Nothing to do.");
			return;
		}
		
		testing = true;
		outFileName = "makeFetchSets_log.txt";
		currentLearnMode.set(false, false, false, false, useSpreading, false, false, false, true);
		currentLearnMode.setChunkThreshold(1);
		clearOutputFile();

		ArrayList<String> toSave = new ArrayList<String>();
		
		System.out.println("Starting fetch-set learning run...");
		
		// Get the end line for initial smem
		int smemLen = agent.ExecuteCommandLine("p @").split("\n").length;
		
		for (Pair<String,String> task : taskSeqs) {
			if (!initAgent()) {
				System.err.println("ERROR: Could not init agent for task '" + task + "'");
				return;
			}

			setTask(task.getKey(), task.getValue());
			
			runAgent();
			
			System.out.println("Completed " + task.getKey() + " : " + task.getValue() );
			
			// Get the new smem contents
			saveSmemSnip(toSave, smemLen);
		}
		
		// Save to file
		try(FileWriter fw = new FileWriter(fname, false);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			for (String s : toSave) {
				out.println(s);
			}
		}
		catch (IOException e) {
			System.err.println("ERROR: Unable to write to file '" + fname + "'");
		}
		
		System.out.println("\nDone! Fetch sets saved to: " + fname);
		
	}*/
	
	public void runExperiments(String exp_name, int samples, List<LearnConfig> modes) {
		// For each parameter combination
		for (LearnConfig m : modes) {
			currentLearnMode = m;
				
			setOutputFile(exp_name + "_l" + m + "_s" + samples + ".dat");
			clearOutputFile();

			long startTime, endTime;
			for (int i=0; i<samples; ++i) {
				currentSampleNum = i+1;
				startTime = System.nanoTime();
				user_doExperiment();
				endTime = System.nanoTime();
				System.out.println("\nCompleted sample " + (i+1));
				System.out.println(String.format("World ran at x%.3f real time on average.", elapsedAgentMSEC/nanoToMilli(endTime-startTime)));
			}

			if (verbose)
				agent.ExecuteCommandLine("clog -c");
				
			System.out.println("\nCOMPLETED MODE " + m);
		}
	}
	
	/**
	 * This is called every decision cycle, after the output phase.
	 * It is used to react whenever the agent updates the output link.
	 */
	private void update_readOutput() {
		if (agentError) {
			return;
		}
		
		int C = agent.GetNumberCommands();
		if (C <= 0) {
			return;
		}
		
		// Clear old input
		//clearPerception();
		
		// Note the availability of this function for reporting:  int DC = agent.GetDecisionCycleCounter();
		// Seemed broken when tried to use it before. Can't ignore impasse DCs in any case.

		boolean stopAgent = false;
		for (int c = 0; c < C; ++c) {
			final Identifier id = agent.GetCommand(c);
			if (id != null && id.IsJustAdded()) {
				//int nChildren = id.GetNumberChildren();
				//if (nChildren != numAgentOutputs)
					//return;
				
				// Handle agent-reported errors
				if (id.GetCommandName().compareTo(CMD_ERROR) == 0) {
					agentError = true;
					stopAgent = true;
					String msg = id.GetParameterValue("msg");
					
					user_errorListener(msg);
					
					// Write note to results that they are corrupted
					try(FileWriter fw = new FileWriter(outFileName, true);
							BufferedWriter bw = new BufferedWriter(fw);
							PrintWriter out = new PrintWriter(bw))
					{
						out.println("ERROR - AGENT CRASH: " + msg);
					}
					catch (IOException e) {
						System.err.println("ERROR: Unable to append to file '" + outFileName + "'");
					}
					
					id.AddStatusComplete();
				}
				// Handle Soar agent commands
				else if (id.GetCommandName().compareTo(CMD_SAY) == 0) {
						//&& nChildren == numAgentOutputs) {

					// Get the output
					for (int i=0; i<numAgentOutputs; ++i) {
						outputs.set(i,id.GetParameterValue("slot" + Integer.toString(i+1)));
					}
					
					if (outputs.get(0) == null) {
						System.err.println("WARNING: output 'slot1' missing.");
						id.AddStatusComplete();
						agent.Commit();
						break;
					}
					
					// Update the environment using the output
					user_outputListener(outputs);
					
					// Update the agent inputs according to environment responses
					applyNewInputs();
					
					id.AddStatusComplete();
				}
				else if (id.GetCommandName().compareTo(CMD_FINISH) == 0) {
					stopAgent = true;
					id.AddStatusComplete();
				}
				else {
					id.AddStatusError();
				}
				
				//if (!(id.GetCommandName().compareTo(CMD_SAY) == 0))
						//&& nChildren != numAgentOutputs))
					//id.AddStatusComplete();
			}
		}

		if (stopAgent) {
			// Clear old input
			clearPerception();
			agent.StopSelf();
			
			// Reset for next trial
			user_agentStop();
			
		}
	}
	
	private void update_sendInput() {
		// Update reward signals
		if (rewardWME != null) {
			rewardWME.DestroyWME();
			rewardWME = null;
		}
		if (rewardInput != 0) {
			rewardWME = input_link.CreateFloatWME("reward", rewardInput);
			rewardInput = 0.0f;
		}
		
		// Check for delayed inputs
		if (delayedInputs.size() != 0) {
			// Only look at 1 delayed input per decision, since each affects all input slots
			// Note, this could overwrite inputs set earlier this decision by update_readOutput().
			if (delayedInputs.peek().msecMoment <= elapsedAgentMSEC) {
				ScheduledInput input = delayedInputs.poll();
				setPerception(input.inputs);
				applyNewInputs();
			}
		}

		// Send a generic flag to the agent of whether environment input has changed
		if (inputChanged) {
			if (inputChangedWME != null)
				inputChangedWME.DestroyWME();
			inputChangedWME = input_link.CreateStringWME(ATTR_INPUT_CHANGED, "true");
			inputChanged = false;	// Remove the string next cycle
		}
		else {
			if (inputChangedWME != null) {
				inputChangedWME.DestroyWME();
				inputChangedWME = null;
			}
		}
	}
	
	@Override
	public void updateEventHandler(int eventID, Object data, Kernel kernel, int runFlags) {
		if (eventID == smlUpdateEventId.smlEVENT_AFTER_ALL_OUTPUT_PHASES.swigValue()) {
			// Increase the elapsed time each decision cycle
			elapsedAgentMSEC += msecPerDecision;
			elapsedAgentDCs++;
			// Manage agent output
			update_readOutput();
			// Manage environment input
			update_sendInput();
		}
		
	}
	
	/*@Override
	public void runEventHandler(int eventID, Object data, Agent agent, int runFlags) {
		if (eventID == smlRunEventId.smlEVENT_BEFORE_INPUT_PHASE.swigValue()) {
			update_beforeInput();
		}
	}*/
}
