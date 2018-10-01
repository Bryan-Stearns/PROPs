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
import sml.smlRunEventId;
import sml.smlUpdateEventId;
import sml.Agent;
import sml.Agent.RunEventInterface;
import edu.umich.soar.debugger.SWTApplication;

public abstract class PROPsEnvironment implements UpdateEventInterface/*, RunEventInterface*/ {
	private boolean using_props = true;
	private boolean verbose = true;
	private boolean save_percepts = false;
	private boolean testing = false;
	private boolean running = false;
	protected boolean agentError = false;
	private boolean initOkay = false;
	private String props_dir = "";
	
	protected String agentName = "PROPsAgent";
	private int agent_num = 0;
	final protected String CMD_SAY = "say",
							CMD_FINISH = "finish",
							CMD_INSTR_NEXT = "props-command",
							CMD_ERROR = "error";
	protected String outFileName = "AgentOutput.txt",
					agent_condchunk_file = "",
					agent_addresschunk_file = "",
					agent_instruction_file = "",
					agent_fetchseq_file = "",
					agent_genericsoar_file = "";
	protected String taskName = "TEST",
					 taskSequenceName = "TEST";
	
	protected LearnConfig currentLearnMode;
	
	protected Identifier input_link;
	protected Kernel kernel;
	protected Agent agent = null;
	private WMElement inTask,
					  inTaskSeqName;
	
	private long elapsedAgentMSEC = 0;
	protected double msecPerDecision = 50;
	
	protected int numAgentInputs = 3,
				  numAgentOutputs = 3;
	private ArrayList<WMElement> inputWMEs;
	private ArrayList<GhostWME> newInputs;	// a buffer for the user-made environment input
	private ArrayList<String> inputs;
	private ArrayList<String> outputs;
	private PriorityQueue<ScheduledInput> delayedInputs;

	protected ArrayList<String> userAgentFiles;
	protected ArrayList<String> reports;

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
		
		currentLearnMode = new LearnConfig(false, true, false, true, true, true, false, false);	// Learn associative combos and conditions by default, using spreading and deliberate fetch sequences
		
		//user_initEnvironment();
		//   User needs to set: 
		// agentName, PROPS_DIR, PROJ_DIR, 
		// agent_condchunk_file, agent_addresschunk_file, agent_instruction_file, agent_fetchseq_file, agent_genericsoar_file,
		// numAgentInputs, numAgentOutputs, 
		// userAgentFiles
	}
	

	private boolean loadLearnProductions(LearnConfig mode) {
		if (mode.spreading() && !mode.learnsConditions()) {
			if (agent_condchunk_file == "") {
				System.err.println("ERROR: User did not provide the condition chunk file! Aborting.");
				return false;
			}
			agent.LoadProductions(agent_condchunk_file);
		}
		
		if (mode.learnsAddressChunks()) {
			// Learn addressing chunks only (and save them for later sourcing)
			agent.LoadProductions(props_dir + "props_learn_l1.soar");
			return true;
		}
		
		if (mode.seqs()) {
			agent.LoadProductions(props_dir + "props_learn_seqlinks.soar");
		}
		
		if (mode.learnsAutos() && !mode.learnsAddresses() && !mode.learnsProposals()) {
			agent.LoadProductions(props_dir + "props_learn_l3only.soar");
			return true;
		}
		
		if (!mode.learnsAddresses()) {
			if (agent_addresschunk_file == "") {
				System.err.println("ERROR: User did not provide the addressing chunk file! Aborting.");
				return false;
			}
			agent.LoadProductions(agent_addresschunk_file);
		}
		if (mode.learnsProposals()) {
			agent.LoadProductions(props_dir + "props_learn_l2.soar");
		}
		if (mode.learnsAutos()) {
			agent.LoadProductions(props_dir + "props_learn_l3.soar");
		}
		
		return true;
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
		}
		
		agent = kernel.CreateAgent(agentName + (agent_num++));
		//agent.RegisterForRunEvent(smlRunEventId.smlEVENT_BEFORE_INPUT_PHASE, this, null);
		agent.SetBlinkIfNoChange(false);
		agentError = false;
		
		// Reset IO
		input_link = agent.GetInputLink();
		inTask = null;
		inTaskSeqName = null;
		
		setIOSize(numAgentInputs, numAgentOutputs);	// Resets input lists
		running = false;
		
		elapsedAgentMSEC = 0;

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
			
			if (currentLearnMode.manual()) {
				if (agent_fetchseq_file == "") {
					System.err.println("ERROR: User did not provide the fetch sequence SMEM file! Aborting.");
					initOkay = false;
					return false;
				}
				agent.LoadProductions(agent_fetchseq_file);	// manual instruction sequences for tasks
			}
			if (currentLearnMode.spreading()) {
				agent.LoadProductions(props_dir + "props_learn_conds.soar");
			}

			agent.ExecuteCommandLine("chunk confidence-threshold " + currentLearnMode.getChunkThreshold());
			
			if (!loadLearnProductions(currentLearnMode)) {
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
	
	private void loadUserAgentFiles() {
		// Load the list of user-provided agent files
		for (String s : userAgentFiles) {
			agent.LoadProductions(s);
		}
		
	}
	
	public void setUserAgentFiles(List<String> filenames) {
		userAgentFiles = new ArrayList<String>(filenames);
	}


	public long secToNano(double sec) {return (long) (sec*1000000000l);}
	public long secToMilli(double sec) {return (long) (sec*1000l);}
	public double nanoToSec(long nano) {return (double) nano/1000000000.0;}
	public double nanoToMilli(long nano) {return (double) nano/1000000.0;}
	public double milliToSec(long milli) {return (double) milli/1000.0;}
	
	public void setCondChunkFile(String filename) { agent_condchunk_file = filename; }
	public void setAddressChunkFile(String filename) { agent_addresschunk_file = filename; }
	public void setInstructionsFile(String filename) { agent_instruction_file = filename; }
	public void setFetchSeqFile(String filename) { agent_fetchseq_file = filename; }
	public void setSoarAgentFile(String filename) { agent_genericsoar_file = filename; }
	
	public void setUseProps(boolean mode) { using_props = mode; }
	public void setVerbose(boolean mode) { verbose = mode; }
	public void setSavePercepts(boolean mode) { save_percepts = mode; }
	
	public void setPropsDir(String dir) { props_dir = dir; }
	
	public void setAgentName(String name) { agentName = name; }
	public void setConfig(LearnConfig config) { currentLearnMode = config; }
	
	public void setOutputFile(String filename) { outFileName = filename; }
	
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
	
	public List<String> getPerception() {
		return inputs;
	}

	public void setInput(int slot, String input) {
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
	public void setInput(int slot, int input) {
		if (slot < 0 || slot >= numAgentInputs) {
			throw new RuntimeException("ERROR in setInput(): Input slot must be between 0 and " + (numAgentInputs-1));
		}
		newInputs.set(slot, new GhostWME(GhostWME.Type.INT, "slot" + (slot+1), Integer.toString(input)));
	}
	public void setInput(int slot, double input) {
		if (slot < 0 || slot >= numAgentInputs) {
			throw new RuntimeException("ERROR in setInput(): Input slot must be between 0 and " + (numAgentInputs-1));
		}
		newInputs.set(slot, new GhostWME(GhostWME.Type.FLOAT, "slot" + (slot+1), Double.toString(input)));
	}
	/*public void setInput(int slot, boolean input) throws Exception {
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
	
	// Copy the new inputs from the buffer to the agent
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
		return didChange;
	}
	
	public void clearPerception() {
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
	
	public void setPerception(List<String> perception) {
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
	
	/**
	 * Send the given actions to the input link after the given delay, in real time.
	 * @param delaysec The number of seconds to delay until the inputs are made
	 * @param actions The inputs to deliver, in slot order
	 */
	public void scheduleInput(double delaysec, List<String> actions) {
		// Add the new schedule entry
		long moment = elapsedAgentMSEC + secToMilli(delaysec);
		delayedInputs.add(new ScheduledInput(moment, actions));	// PriorityQueue sorting recalculates when to trigger the next scheduled entry
	}
	
	protected void addAgentLatency(long msec) {
		elapsedAgentMSEC += msec;
	}
	
	/**
	 * @return The elapsed agent time in milliseconds
	 */
	public long getElapsedTime() {
		return elapsedAgentMSEC;
	}
	
	public void setTask(String task, String taskSeqName) {
		// Clear input if any
		//clearPerception();
		if (agent == null) {
			if (!initAgent())
				throw new RuntimeException();
		}
		
		// Reset task commands
		if (inTask != null) {
			if (!inTask.GetValueAsString().equals(task)) {
				inTask.DestroyWME();
				inTask = input_link.CreateStringWME("task", task);		// for lib-actr to set Gtask
			}
		}
		else 
			inTask = input_link.CreateStringWME("task", task);
		
		if (inTaskSeqName != null) {
			if (!inTaskSeqName.GetValueAsString().equals(taskSeqName)) {
				inTaskSeqName.DestroyWME();
				inTaskSeqName = input_link.CreateStringWME("task-sequence-name", taskSeqName);	// for specifying a known PROPs instruction sequence
			}
		}
		else
			inTaskSeqName = input_link.CreateStringWME("task-sequence-name", taskSeqName);
		
		this.taskName = task;
		this.taskSequenceName = taskSeqName;
		
		user_updateTask();
	}


	// Clear the output file if it already exists
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

	public void addReport(String r) {
		reports.add(r);
	}
	public void clearReports() {
		reports.clear();
	}
	
	// Print all current reports
	public void printReports() {
		printReports("");
	}
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
	
	public void runDebug(String task, String taskSeq, int threshold, String learnMode) {
		testing = true;
		currentLearnMode.set(learnMode);
		currentLearnMode.setChunkThreshold(threshold);
		
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
	

	public void makeSpreadingChunks(List<Pair<String,String>> taskSeqs, String fname) {
		testing = true;
		outFileName = "make_condition_chunks.txt";
		currentLearnMode.set(true, false, false, true, true, true, false, false);
		currentLearnMode.setChunkThreshold(1);
		
		makeChunkingRuns(taskSeqs, fname);
		testing = false;
	}
	public void makeAddressingChunks(List<Pair<String,String>> taskSeqs, String fname) {
		testing = true;
		outFileName = "make_address_chunks.txt";
		currentLearnMode.set(false, false, false, false, false, true, false, true);
		currentLearnMode.setChunkThreshold(1);
		
		makeChunkingRuns(taskSeqs, fname);
		testing = false;
	}
	
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
				runAgent();
				count++;
				
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

	public void runExperiments(String exp_name, int samples, List<LearnConfig> modes) {
		// For each parameter combination
		for (LearnConfig m : modes) {
			currentLearnMode = m;
				
			setOutputFile(exp_name + "_l" + m + "_s" + samples + ".dat");
			clearOutputFile();

			long startTime, endTime;
			for (int i=0; i<samples; ++i) {
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
	private void update_afterOutput() {
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
	
	@Override
	public void updateEventHandler(int eventID, Object data, Kernel kernel, int runFlags) {
		if (eventID == smlUpdateEventId.smlEVENT_AFTER_ALL_OUTPUT_PHASES.swigValue()) {
			// Increase the elapsed time each decision cycle
			elapsedAgentMSEC += msecPerDecision;
			// Manage agent output
			update_afterOutput();
		}
		
		// Check for delayed inputs
		if (delayedInputs.size() == 0) {
			return;
		}
		// Only look at 1 delayed input per decision, since each affects all input slots
		// Note, this could overwrite inputs set earlier this decision by update_afterOutput().
		if (delayedInputs.peek().msecMoment <= elapsedAgentMSEC) {
			ScheduledInput input = delayedInputs.poll();
			setPerception(input.inputs);
			applyNewInputs();
		}
	}
	
	/*@Override
	public void runEventHandler(int eventID, Object data, Agent agent, int runFlags) {
		if (eventID == smlRunEventId.smlEVENT_BEFORE_INPUT_PHASE.swigValue()) {
			update_beforeInput();
		}
	}*/
}
