package edu.umich.eecs.soar.propsutil;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import sml.Identifier;
import sml.Kernel;
import sml.Kernel.UpdateEventInterface;
import sml.WMElement;
import sml.smlUpdateEventId;
import sml.Agent;
import edu.umich.soar.debugger.SWTApplication;

public abstract class PROPsEnvironment implements UpdateEventInterface {
	private boolean using_props = true;
	private boolean verbose = true;
	private boolean save_percepts = false;
	private boolean testing = false;
	private boolean running = false;
	protected boolean agentError = false;
	private String proj_dir = "";
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
	
	protected int numAgentInputs = 3,
				  numAgentOutputs = 3;
	private ArrayList<WMElement> inputWMEs;
	private ArrayList<String> inputs;
	private ArrayList<String> outputs;


	protected ArrayList<String> userAgentFiles;
	protected ArrayList<String> reports;

	//abstract protected void user_initEnvironment();
	abstract protected void user_createAgent();
	abstract protected void user_doExperiment();
	abstract protected void user_agentStart();
	abstract protected void user_agentStop();
	abstract protected void user_outputListener(List<String> outputs);
	abstract protected void user_errorListener(String err);
	
	public PROPsEnvironment() {
		kernel = Kernel.CreateKernelInNewThread();
		kernel.SetAutoCommit(true);
		kernel.RegisterForUpdateEvent(smlUpdateEventId.smlEVENT_AFTER_ALL_OUTPUT_PHASES, this, this);
		
		userAgentFiles = new ArrayList<String>();
		reports = new ArrayList<String>();
		
		inputWMEs = new ArrayList<WMElement>(numAgentInputs);
		inputs = new ArrayList<String>(numAgentInputs);
		outputs = new ArrayList<String>(numAgentOutputs);
		
		currentLearnMode = new LearnConfig(false, true, false, true, true, true, false, false);	// Learn associative combos and conditions by default, using spreading and deliberate fetch sequences
		
		//user_initEnvironment();
		//   User needs to set: 
		// agentName, PROPS_DIR, PROJ_DIR, 
		// agent_condchunk_file, agent_addresschunk_file, agent_instruction_file, agent_fetchseq_file, agent_genericsoar_file,
		// numAgentInputs, numAgentOutputs, 
		// userAgentFiles
	}


	private void loadLearnProductions(LearnConfig mode) throws Exception {
		if (mode.spreading() && !mode.learnsConditions()) {
			if (agent_condchunk_file == "") {
				throw new Exception("ERROR: User did not provide the condition chunk file!");
			}
			agent.LoadProductions(proj_dir + agent_condchunk_file);
		}
		
		if (mode.learnsAddressChunks()) {
			// Learn addressing chunks only (and save them for later sourcing)
			agent.LoadProductions(props_dir + "props_learn_l1.soar");
			return;
		}
		
		if (mode.seqs()) {
			agent.LoadProductions(props_dir + "props_learn_seqlinks.soar");
		}
		
		if (mode.learnsAutos() && !mode.learnsAddresses() && !mode.learnsProposals()) {
			agent.LoadProductions(props_dir + "props_learn_l3only.soar");
			return;
		}
		
		if (!mode.learnsAddresses()) {
			if (agent_addresschunk_file == "") {
				throw new Exception("ERROR: User did not provide the addressing chunk file!");
			}
			agent.LoadProductions(proj_dir + agent_addresschunk_file);
		}
		if (mode.learnsProposals()) {
			agent.LoadProductions(props_dir + "props_learn_l2.soar");
		}
		if (mode.learnsAutos()) {
			agent.LoadProductions(props_dir + "props_learn_l3.soar");
		}
	}
	
	public void initAgent() throws Exception {
		if (agent != null) {
			if (verbose)
				agent.ExecuteCommandLine("clog -c");
			kernel.DestroyAgent(agent);
		}
		
		agent = kernel.CreateAgent(agentName + (agent_num++));
		agent.SetBlinkIfNoChange(false);
		agentError = false;
		
		// Reset IO
		input_link = agent.GetInputLink();
		inTask = null;
		inTaskSeqName = null;
		
		setIOSize(numAgentInputs, numAgentOutputs);
		running = false;

		
		if (props_dir == "") {
			throw new Exception("ERROR: User did not provide the PROPS directory!");
		}
		if (proj_dir == "") {
			throw new Exception("ERROR: User did not provide the domain project directory!");
		}

		// Load the agent files
		if (using_props) {
			if (agent_instruction_file == "") {
				throw new Exception("ERROR: User did not provide the SMEM instruction file!");
			}
			
			agent.LoadProductions(props_dir + "_firstload_props.soar");			// props library
			agent.LoadProductions(proj_dir + agent_instruction_file);		// The props instructions
			
			if (currentLearnMode.manual()) {
				if (agent_fetchseq_file == "") {
					throw new Exception("ERROR: User did not provide the fetch sequence SMEM file!");
				}
				agent.LoadProductions(proj_dir + agent_fetchseq_file);	// manual instruction sequences for tasks
			}
			if (currentLearnMode.spreading()) {
				agent.LoadProductions(props_dir + "props_learn_conds.soar");
			}

			agent.ExecuteCommandLine("chunk confidence-threshold " + currentLearnMode.getChunkThreshold());
			
			loadLearnProductions(currentLearnMode);
		}
		else if (agent_genericsoar_file != "") {
			agent.LoadProductions(proj_dir + agent_genericsoar_file);	// editors agent to be learned (for testing)
		}
		else {
			throw new IllegalArgumentException("ERROR: User did not provide the generic Soar agent file!");
		}
		
		loadUserAgentFiles();
		user_createAgent();
		
		if (verbose) {
			agent.ExecuteCommandLine("watch 1");
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
	}
	
	private void loadUserAgentFiles() {
		// Load the list of user-provided agent files
		for (String s : userAgentFiles) {
			agent.LoadProductions(proj_dir + s);
		}
		
	}
	
	public void setUserAgentFiles(List<String> filenames) {
		userAgentFiles = new ArrayList<String>(filenames);
	}

	public void setCondChunkFile(String filename) { agent_condchunk_file = filename; }
	public void setAddressChunkFile(String filename) { agent_addresschunk_file = filename; }
	public void setInstructionsFile(String filename) { agent_instruction_file = filename; }
	public void setFetchSeqFile(String filename) { agent_fetchseq_file = filename; }
	public void setSoarAgentFile(String filename) { agent_genericsoar_file = filename; }
	
	public void setUseProps(boolean mode) { using_props = mode; }
	public void setVerbose(boolean mode) { verbose = mode; }
	public void setSavePercepts(boolean mode) { save_percepts = mode; }
	
	public void setPropsDir(String dir) { props_dir = dir; }
	public void setProjDir(String dir) { proj_dir = dir; }
	
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
		inputs = new ArrayList<String>(numAgentInputs);
		outputs = new ArrayList<String>(numAgentOutputs);
		
		for (int i=0; i<numAgentInputs; ++i) {
			inputWMEs.add(null);
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

	public void setInput(int slot, String input) throws Exception {
		if (slot < 0 || slot >= numAgentInputs) {
			throw new Exception("ERROR: Input slot must be between 1 and " + numAgentInputs);
		}
		
		// Establish the given data on the input link
		WMElement in = inputWMEs.get(slot);
		if (in != null) {
			in.DestroyWME();
		}
		if (input != null && !input.equals("")) {
			inputWMEs.set(slot, input_link.CreateStringWME("in" + (slot+1), input));
		}
		else {
			inputWMEs.set(slot, null);
		}
		
		// Save for later internally
		inputs.set(slot, input);
	}
	public void setInput(int slot, int input) throws Exception {
		if (slot < 0 || slot >= numAgentInputs) {
			throw new Exception("ERROR: Input slot must be between 1 and " + numAgentInputs);
		}
		
		// Establish the given data on the input link
		WMElement in = inputWMEs.get(slot);
		if (in != null) {
			in.DestroyWME();
		}
		inputWMEs.set(slot, input_link.CreateIntWME("in" + (slot+1), input));
		
		// Save for later internally
		inputs.set(slot, Integer.toString(input));
	}
	public void setInput(int slot, double input) throws Exception {
		if (slot < 0 || slot >= numAgentInputs) {
			throw new Exception("ERROR: Input slot must be between 1 and " + numAgentInputs);
		}
		
		// Establish the given data on the input link
		WMElement in = inputWMEs.get(slot);
		if (in != null) {
			in.DestroyWME();
		}
		inputWMEs.set(slot, input_link.CreateFloatWME("in" + (slot+1), input));
		
		// Save for later internally
		inputs.set(slot, Double.toString(input));
	}
	
	public void clearPerception() {
		for (int i=0; i<inputWMEs.size(); ++i) {
			try {
				setInput(i, null);
			} catch (Exception e) {
				e.printStackTrace();	// Shouldn't happen
			}
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
	
	public void setTask(String task, String taskSeqName) {
		// Clear input if any
		//clearPerception();
		
		// Reset task commands
		if (inTask != null) {
			if (!inTask.GetValueAsString().equals(task)) {
				inTask.DestroyWME();
				inTask = null;
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
		try(FileWriter fw = new FileWriter(outFileName, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			for (String s : reports) {
				out.println(s);
				if (verbose)
					agent.ExecuteCommandLine("echo\t" + s);
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
		
		try {
			initAgent();
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
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
		if (!running) {
			user_agentStart();
			running = true;
		}
		agent.RunSelf(steps);   // Run for the given number of decision cycles
	}
	public void runAgent() {
		if (!running) {
			user_agentStart();
			running = true;
		}
		agent.RunSelfForever(); // Run until receiving the finish command
	}
	

	public void makeSpreadingChunks(String taskName, List<String> seqNames, String fname) {
		testing = true;
		outFileName = "make_chunks.txt";
		currentLearnMode.set(true, false, false, true, true, false, false, false);
		currentLearnMode.setChunkThreshold(1);
		
		makeChunkingRuns(taskName, seqNames, fname);
		testing = false;
	}
	public void makeAddressingChunks(String taskName, List<String> seqNames, String fname) {
		testing = true;
		outFileName = "make_chunks.txt";
		currentLearnMode.set(false, false, false, false, false, true, false, true);
		currentLearnMode.setChunkThreshold(1);
		
		makeChunkingRuns(taskName, seqNames, fname);
		testing = false;
	}
	
	public void makeChunkingRuns(String taskName, List<String> taskSeqs, String fname) {

		try {
			initAgent();
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		clearOutputFile();

		user_agentStart();
		
		int chunksDelta = 0,
			chunksCurr = 0,
			chunksPrev = 0;
		int cont = currentLearnMode.getChunkThreshold() + 2;
		
		for (String task : taskSeqs) {
			setTask(taskName, task);
			
			while (cont > 0) {
				agent.RunSelfForever();
				
				if (chunksCurr == 0 || chunksPrev == 0)
					continue;
				// Check how many chunks were learned this past run
				chunksPrev = chunksCurr;
				chunksCurr = agent.ExecuteCommandLine("p -c").split("\n").length;
				chunksDelta = chunksCurr - chunksPrev;
				
				if (chunksDelta == 0)
					cont--;
				else
					cont = currentLearnMode.getChunkThreshold() + 2;
			}
			cont = currentLearnMode.getChunkThreshold() + 1;
		}
		
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

			for (int i=0; i<samples; ++i) {
				user_doExperiment();
				System.out.println("\nCompleted sample " + i);
			}

			if (verbose)
				agent.ExecuteCommandLine("clog -c");
				
			System.out.println("\nCOMPLETED MODE " + m);
		}
	}
	
	@Override
	public void updateEventHandler(int arg0, Object arg1, Kernel kernel, int arg3) {
		
		if (agentError) {
			return;
		}
		
		int C = agent.GetNumberCommands();
		if (C <= 0) {
			return;
		}
		
		// Clear old input
		clearPerception();
		
		// Note the availability of this function for reporting:  int DC = agent.GetDecisionCycleCounter();
		// Seemed broken when tried to use it before. Can't ignore impasse DCs in any case.

		boolean stopAgent = false;
		for (int c = 0; c < C; ++c) {
			final Identifier id = agent.GetCommand(c);
			if (id != null && id.IsJustAdded()) {
				int nChildren = id.GetNumberChildren();
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
				}
				// Handle Soar agent commands
				else if (id.GetCommandName().compareTo(CMD_SAY) == 0
						&& nChildren == numAgentOutputs) {

					// Get the output
					for (int i=0; i<numAgentOutputs; ++i) {
						outputs.set(i,id.GetParameterValue("out" + Integer.toString(i+1)));
					}
					
					if (outputs.get(0) == null) {
						System.err.println("WARNING: 'out1' command missing.");
						id.AddStatusComplete();
						agent.Commit();
						break;
					}
					
					// Update the environment using the output
					user_outputListener(outputs);
					
					
				}
				else if (id.GetCommandName().compareTo(CMD_FINISH) == 0) {
					stopAgent = true;
				}
				else {
					id.AddStatusError();
				}
				
				if (!(id.GetCommandName().compareTo(CMD_SAY) == 0
						&& nChildren != numAgentOutputs))
					id.AddStatusComplete();
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
}
