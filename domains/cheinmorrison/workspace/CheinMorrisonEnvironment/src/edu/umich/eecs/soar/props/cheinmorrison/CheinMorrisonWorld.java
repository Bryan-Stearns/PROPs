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

public class CheinMorrisonWorld implements UpdateEventInterface {

	private static final boolean USING_PROPS = true;
	private static final boolean VERBOSE = true;
	private static boolean COND_SPREAD = true;
	private static boolean TESTING = false;
	private static String PROJ_DIR;
	private static String PROPS_DIR;

	private static final String AGENT_NAME = "EditorsAgent";
	private static int agent_num = 0,
			currentPropsThreshold = 2;
	final protected String CMD_SAY = "say",
			CMD_FINISH = "finish",
			CMD_INSTR_NEXT = "props-command",
			CMD_ERROR = "error";
	protected String outFileName = "editors_props.dat",
			currentTaskName,
			currentLearnMode = "c123m";
	private Identifier input_link;
	private Kernel kernel;
	private Agent agent = null;
	private WMElement in1, 
	in2,
	in3,
	in4,
	inTask,
	inTaskSeqName;
	private boolean abort = false;

	private static double STD_MOTOR_TIME = 0.25,
			STD_VISUAL_TIME = 0.25;

	private ETask ed_task;
	private Report rep;
	private List<String> reports;
	private int task_count;

	private List<ArrayList<String[]>> edit_tasks;
	private String[] numbers;


	EditorsWorld(Kernel kernel) {
		PROJ_DIR = "/home/bryan/Dropbox/UM_misc/Soar/Research/PROPs/PRIMs_Duplications/Editors/";
		PROPS_DIR = "/home/bryan/Dropbox/UM_misc/Soar/Research/PROPs/PROPs Project/";

		this.kernel = kernel;
		this.kernel.RegisterForUpdateEvent(smlUpdateEventId.smlEVENT_AFTER_ALL_OUTPUT_PHASES, this, this);

		this.edit_tasks = new LinkedList<ArrayList<String[]>>();

		this.edit_tasks.add(new ArrayList<String[]>(6));
		edit_tasks.get(0).add(new String[]{"replace-word", "vader", "moeder", "one"});
		edit_tasks.get(0).add(new String[]{"insert-word", "inhoud", "nieuwe", "three"});
		edit_tasks.get(0).add(new String[]{"delete-word", "slome", "", "five"});
		edit_tasks.get(0).add(new String[]{"replace-line", "ebooks en sociale medi", "electronisch boeken en andere vormen van sociale media", "eight"});
		edit_tasks.get(0).add(new String[]{"delete-line", "of the rings trilogie", "", "fifteen"});
		edit_tasks.get(0).add(new String[]{"insert-line", "Oscar of niet Rowling zal er niet om rouwen want de buit is al binnen", "net zo groot als tien jaar geleden", "eighteen"});

		this.edit_tasks.add(new ArrayList<String[]>(6));
		edit_tasks.get(1).add(new String[]{"insert-word", "pers", "muskieten", "two"});
		edit_tasks.get(1).add(new String[]{"replace-line", "fans mochten een blik op de inhoud werpen onder voorwaarde van strikte geheimhouding", "fans hadden de gelegenheid om alvast een kijkje te nemen", "three"});
		edit_tasks.get(1).add(new String[]{"replace-word", "medi", "media", "eight"});
		edit_tasks.get(1).add(new String[]{"delete-word", "eindelijk", "", "fourteen"});
		edit_tasks.get(1).add(new String[]{"delete-line", "We all know what happened in the end but", "", "sixteen"});
		edit_tasks.get(1).add(new String[]{"insert-line", "kassucces De spanning is daarom groot dit jaar", "succes Het zal Roling waarschijnlijk een worst wezen", "seventeen"});

		this.edit_tasks.add(new ArrayList<String[]>(6));
		edit_tasks.get(2).add(new String[]{"replace-line", "Geestelijk vader van de tovenaarsleerling JK Rowling lanceert morgen de site pottermorecom", "Wederom is het tijd voor een nieuwe website over harry potter maar deze keer van Rowling zelf", "one"});
		edit_tasks.get(2).add(new String[]{"insert-word", "paar", "klein", "two"});
		edit_tasks.get(2).add(new String[]{"delete-word", "nieuwe", "", "five"});
		edit_tasks.get(2).add(new String[]{"delete-line", "Op dit moment staat de laatste film in de serie op het punt om in de bioscoop", "", "twelve"});
		edit_tasks.get(2).add(new String[]{"replace-word", "Oscar", "prijs", "thirteen"});
		edit_tasks.get(2).add(new String[]{"insert-line", "kassucces De spanning is daarom groot dit jaar", "And here we have another meaningless line that makes this text een more unreadable", "seventeen"});


		numbers = new String[]{"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
				"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"}; 

	}

	private void loadLearnProductions(String mode) {
		if (COND_SPREAD && !mode.contains("c")) {
			agent.LoadProductions(PROJ_DIR + "prims_editors01_condspread_chunks.soar");
		}
		if (mode.contains("3only")) {
			agent.LoadProductions(PROPS_DIR + "props_learn_l3only.soar");
			return;
		}

		if (!mode.contains("1")) {
			agent.LoadProductions(PROJ_DIR + "prims_editors01_L1-chunks.soar");
		}
		if (mode.contains("2")) {
			agent.LoadProductions(PROPS_DIR + "props_learn_l2.soar");
		}
		if (mode.contains("3")) {
			agent.LoadProductions(PROPS_DIR + "props_learn_l3.soar");
		}
	}

	private void createAgent() {
		if (agent != null) {
			if (VERBOSE)
				agent.ExecuteCommandLine("clog -c");
			kernel.DestroyAgent(agent);
		}

		agent = kernel.CreateAgent(AGENT_NAME + (agent_num++));

		input_link = agent.GetInputLink();
		in1 = null;
		in2 = null;
		in3 = null;
		in4 = null;
		inTask = null;
		inTaskSeqName = null;

		agent.SetBlinkIfNoChange(false);
		kernel.SetAutoCommit(true);


		// Load the agent files
		if (USING_PROPS) {
			agent.LoadProductions(PROPS_DIR + "load_config_props.soar");			// props library

			agent.LoadProductions(PROJ_DIR + "prims_editors_agent_smem.soar");		// editors props instructions
			if (currentLearnMode.contains("m"))
				agent.LoadProductions(PROJ_DIR + "prims_editors_tasks_smem.soar");	// manual instruction sequences for tasks
			if (COND_SPREAD)
				agent.LoadProductions(PROPS_DIR + "props_spread_conds.soar");

			agent.ExecuteCommandLine("chunk confidence-threshold " + currentPropsThreshold);

			loadLearnProductions(currentLearnMode);
		}
		else {
			agent.LoadProductions(PROJ_DIR + "prims_editors_agent.soar");	// editors agent to be learned (for testing)
		}

		agent.LoadProductions(PROJ_DIR + "lib_actr_interface.soar");		// actransfer memory interface
		agent.LoadProductions(PROJ_DIR + "smem_editors.soar");				// smem memory used by editors agent


		ed_task = new ETask();//(1,1,current_sample);
		rep = new Report();

		reports = new LinkedList<String>();
		task_count = 1;

		if (VERBOSE) {
			agent.ExecuteCommandLine("watch 1");
			agent.ExecuteCommandLine("watch --learn 1");
			agent.ExecuteCommandLine("clog -A verbose_"+outFileName);
		}
		else if (!TESTING){
			agent.ExecuteCommandLine("watch 0");
			agent.ExecuteCommandLine("output enabled off");
			agent.ExecuteCommandLine("output agent-writes off");
		}

		//agent.ExecuteCommandLine("save percepts -o EditorsAgentPercepts.spr -f");
		//agent.ExecuteCommandLine("save agent EditorsAgent_pre.soar");
	}

	public void setOutputFile(String filename) {
		outFileName = filename;
	}

	public void initTask(String task) {
		currentTaskName = task;

		// Init task
		task_count = task_count % 3;	// Safety constraint
		ed_task.init();
		ed_task.edits = new ArrayList<String[]>(edit_tasks.get(task_count));
		rep.init();
		rep.taskName = task;
		reports.clear();

		// Clear input if any
		if (in1 != null) {
			in1.DestroyWME();
			in1 = null;
		}
		if (in2 != null) {
			in2.DestroyWME();
			in2 = null;
		}
		if (in3 != null) {
			in3.DestroyWME();
			in3 = null;
		}
		if (in4 != null) {
			in4.DestroyWME();
			in4 = null;
		}
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
			if (!inTaskSeqName.GetValueAsString().equals(task + "_" + Integer.toString(task_count+1))) {
				inTaskSeqName.DestroyWME();
				inTaskSeqName = input_link.CreateStringWME("task-sequence-name", task + "_" + Integer.toString(task_count+1));	// for specifying a known PROPs instruction sequence
			}
		}
		else
			inTaskSeqName = input_link.CreateStringWME("task-sequence-name", task + "_" + Integer.toString(task_count+1));

		determine_v();

		task_count = (task_count + 1) % 3;
	}

	// Helper functions specifically for the Editors environment
	private void determine_v() {
		List<String> L = ed_task.text.get(ed_task.line_pos);
		String line = numbers[ed_task.line_pos];
		String word;
		if (L.size() <= ed_task.cursor_pos)
			word = "eol";
		else
			word = ed_task.text.get(ed_task.line_pos).get(ed_task.cursor_pos);

		ed_task.set_vlist("word", word, "nil", line);
	}

	private List<String> substitute_insert(String old_element, String new_element, List<String> l) {
		if (l == null || l.size() == 0)
			return l;
		return new LinkedList<String>(Arrays.asList(String.join(" ", l).replaceFirst(old_element, new_element).split("\\s+")));
	}

	public void runTest(int trials) {
		TESTING = true;
		outFileName = "editors_props_test.dat";
		createAgent();
		clearOutputFile();
		task_count = 1;

		List<String> tasks = new ArrayList<>(Arrays.asList("edt", "ed", "emacs"));
		for (String task : tasks) {
			initTask(task);

			for (int i=0; i<trials; ++i) {
				agent.RunSelfForever();
			}

			printResults();
		}

		agent.ExecuteCommandLine("p -fc");

	}
	public void runTest(int trials, int steps, String task) {
		TESTING = true;
		outFileName = "editors_props_test.dat";
		createAgent();
		clearOutputFile();

		for (int i=0; i<trials; ++i) {
			if (steps <= 0) {
				initTask(task);
				rep.editNum = i+1;
				agent.RunSelfForever();		// until receiving the finish command
			}
			else {
				initTask(task);
				rep.editNum = i+1;
				agent.ExecuteCommandLine("watch 4");
				agent.ExecuteCommandLine("watch --learn 2");
				agent.RunSelf(steps);
				agent.RunSelfForever();
			}

		}

		agent.ExecuteCommandLine("clog -c");

		printResults();
	}

	public void runDebug() {
		TESTING = true;
		createAgent();
		clearOutputFile();
		task_count = 0;
		initTask("edt");

		try {
			SWTApplication swtApp = new SWTApplication();
			swtApp.startApp(new String[]{"-remote"});

			printResults();
			agent.KillDebugger();
			System.exit(0);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void doLevelThresholdSweep(String exp_name, int samples) {
		List<String> levelTrials = new ArrayList<>(Arrays.asList("c12m","c123m"));// ,"23","23m","123m", "2","m3only"));
		List<Integer> thresholds = new ArrayList<>(Arrays.asList(48));
		String spreadString = (COND_SPREAD) ? "_scu" : "";
		// For each parameter combination
		for (String l : levelTrials) {
			currentLearnMode = l;
			for (int t : thresholds) {
				currentPropsThreshold = t;
				setOutputFile(exp_name + spreadString + "_l" + l + "_t" + t + "_s" + samples + ".dat");
				doExperiment(samples);
			}
			System.out.println("\nCOMPLETED MODE " + l);
		}
	}

	public void doThresholdExperiment(String exp_name, int samples) {
		// NOTE: These integers must correspond to valid filename affixes:
		List<Integer> thresholds = new ArrayList<>(Arrays.asList(1, 2, 4, 8, 16, 32));

		// For each parameter combination
		for (int i : thresholds) {
			currentPropsThreshold = i;
			setOutputFile(exp_name + "_t" + i + ".dat");
			doExperiment(samples);
		}
	}

	public void doExperiment(int samples) {
		clearOutputFile();

		for (int i=0; i<samples; ++i) {
			do_sa();
			System.out.println("\nCompleted sample " + i);
		}

		if (VERBOSE)
			agent.ExecuteCommandLine("clog -c");

		System.out.println("\nDONE!");
	}

	public void do_sa() {
		List<SaCondition> sa_conditions = new ArrayList<SaCondition>();
		sa_conditions.add(new SaCondition("ED-ED-EMACS", new String[]{"ed", "ed", "emacs"}, new int[]{115, 54, 44, 42, 43, 28}));
		sa_conditions.add(new SaCondition("EDT-EDT-EMACS", new String[]{"edt", "edt", "emacs"}, new int[]{115, 54, 55, 49, 43, 28}));
		sa_conditions.add(new SaCondition("ED-EDT-EMACS", new String[]{"ed", "edt", "emacs"}, new int[]{115, 54, 63, 44, 41, 26}));
		sa_conditions.add(new SaCondition("EDT-ED-EMACS", new String[]{"edt", "ed", "emacs"}, new int[]{115, 54, 46, 37, 41, 26}));
		sa_conditions.add(new SaCondition("EMACS-EMACS-EMACS", new String[]{"emacs", "emacs", "emacs"}, new int[]{77, 37, 29, 23, 23, 21}));

		for (SaCondition sac : sa_conditions) {
			createAgent();
			rep.taskSetName = sac.name;

			for (int i=0; i<6 && !abort; ++i) { // Each subject comes in for 6 'days'
				int j = (int)(1800.0 / (double)sac.trials[i] + 0.5); // How many trials can fit into the 'day'
				String condition = sac.conditions[i/2];

				for (int k=0; k<j && !abort; ++k) {
					initTask(condition);
					rep.trialNum = i+1;
					rep.editNum = k+1;

					agent.RunSelfForever();	// Run until receiving the finish command

					if (!abort) {	// abort potentially gets set in the updateEventHandler method
						printResults();
						System.out.println("Done: " + sac.name + ", " + condition + " " + Integer.toString(i+1) + "," + Integer.toString(k+1));
					}
					else {
						System.out.println("ERROR RETURNED BY AGENT FOR " + condition + " " + Integer.toString(i+1) + "," + Integer.toString(k+1) + "!");
						break;
					}
				}
			}

			if (abort) break;
		}

		//agent.ExecuteCommandLine("clog -c");
		abort = false;
	}

	// Clear the output file if it already exists
	public boolean clearOutputFile() {
		if (!VERBOSE) {
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

	// Print all current reports
	public void printResults() {
		try(FileWriter fw = new FileWriter(outFileName, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			for (String s : reports) {
				out.println(s);
				if (VERBOSE)
					agent.ExecuteCommandLine("echo\t" + s);
			}
		}
		catch (IOException e) {
			System.err.println("ERROR: Unable to append to file '" + outFileName + "'");
		}
	}

	@Override
	public void updateEventHandler(int arg0, Object data, Kernel kernel, int arg3) {

		int C = agent.GetNumberCommands();
		if (C <= 0) {
			return;
		}

		// Note the availability of this function for reporting:  int DC = agent.GetDecisionCycleCounter();
		// Seemed broken when tried to use it before. Can't ignore impasse DCs in any case.

		boolean stopAgent = false;
		for (int c = 0; c < C; ++c) {
			final Identifier id = agent.GetCommand(c);
			if (id != null && id.IsJustAdded()) {

				// Handle agent-reported errors
				if (id.GetCommandName().compareTo(CMD_ERROR) == 0) {
					abort = true;
					stopAgent = true;
					String msg = id.GetParameterValue("msg");

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
				else if (id.GetCommandName().compareTo(CMD_SAY) == 0) {
					// Get the output
					String action = id.GetParameterValue("out1");
					String val2 = id.GetParameterValue("out2");
					String val3 = id.GetParameterValue("out3");

					double latency = 0.05;

					if (action == null) {
						System.err.println("WARNING: Command missing action. Also received: " + val2 + " : " + val3);
						id.AddStatusComplete();
						agent.Commit();
						break;
					}

					// Generate the corresponding input
					int iTemp;
					String sTemp;
					List<String> temp;

					switch (rep.state) {
					case "ll-noread":
						if (action.equals("read-instruction")) {
							rep.state = "ll";
							rep.task = ed_task.edits.get(0)[0];
						}
						else {
							System.out.println("Something went wrong...");
						}
						break;
					case "ll":
						if (action.equals("number-p") || action.equals("enter") || action.equals("t-word")) {
							rep.ll = System.nanoTime() - rep.strt;
							rep.state = "ll-motor";
						}
						if (action.equals("read-instruction") && ed_task.edits.get(0)[3].equals(numbers[ed_task.line_pos])) {
							if (rep.state.equals("ll")) {
								rep.ll = System.nanoTime()-rep.strt;
								rep.state = "mt";
								rep.temp = System.nanoTime();
							}
						}
						break;
					case "ll-motor":
						if (action.equals("read-instruction")) {
							rep.state = "mt";
							rep.temp = System.nanoTime();
						}
						break;
					case "mt":
						temp = new ArrayList<String>(Arrays.asList("substitute-ed", "substitute-edt", "insert-ed", "insert-edt",
								"period-d", "type-text", "type-text-enter", "d", "control-k", "control-k-twice", "esc-d"));
						if (temp.contains(action)) {
							rep.state = "mt-motor";
							rep.mt = System.nanoTime() - rep.temp;
						}
						break;
					case "mt-motor":
						if (action.equals("next-instruction")) {
							rep.state = "ll";
							//rep.addLatency(STD_VISUAL_TIME); // Could move latency for 'next-instruction' here so it ends up in the report, but Taatgen doesn't?
							reports.add(rep.toString());
							if (ed_task.edits.size() <= 1)
								rep.task = "nil";
							else
								rep.task = ed_task.edits.get(1)[0];
							rep.strt = System.nanoTime();
							rep.latencies = 0.0;
						}
						break;
					case "end":
						break;	// Do nothing (not in original lisp code - never gets called anyway)
					}

					switch (action) {
					case "enter": case "control-n":
						ed_task.line_pos++;
						ed_task.cursor_pos = 0;
						latency = (action.equals("enter")) ? (STD_MOTOR_TIME + STD_VISUAL_TIME) : STD_MOTOR_TIME;
						determine_v();
						break;
					case "esc-f": case "move-attention-right":
						latency = (action.equals("esc-f")) ? (2.0 * STD_MOTOR_TIME) : STD_VISUAL_TIME;
						ed_task.cursor_pos++;
						determine_v();
						break;
					case "read-screen":
						determine_v();
						latency = STD_VISUAL_TIME;
						break;
					case "read-instruction":
						ed_task.set_vlist(ed_task.edits.get(0));
						latency = STD_VISUAL_TIME;
						break;
					case "next-instruction":
						if (ed_task.edits.size() == 0)
							break;	// end
						ed_task.edits.remove(0);
						if (ed_task.edits.size() > 0) {
							ed_task.set_vlist(ed_task.edits.get(0));
						}
						else {
							ed_task.set_vlist("end", "end2", "end3", "end4");
							rep.state = "end";
						}
						latency = STD_VISUAL_TIME;
						break;
					case "focus-on-word": case "focus-on-next-word":
						if (action.equals("focus-on-word")) {
							ed_task.line = new LinkedList<String>(Arrays.asList(ed_task.edits.get(0)[1].split("\\s")));
						}

						sTemp = "short";
						if (ed_task.line.size() == 1 || ed_task.line.get(0).length() > 4)	// If the word in the line is longer than 4 characters
							sTemp = "long";
						ed_task.set_vlist("single-word", ed_task.line.get(0), sTemp, "");
						ed_task.line.remove(0);
						latency = STD_VISUAL_TIME;
						break;
					case "esc-d":	// Delete an element in the line
						ed_task.text.get(ed_task.line_pos).remove(ed_task.cursor_pos);
						latency = STD_MOTOR_TIME;
						determine_v();
						break;
					case "type-text": case "type-text-enter": case "period-a": case "i":	// Insert text into the line
						if (!action.equals("type-text")) {
							int pos = ed_task.line_pos;
							for (int i=0; i<19-pos; ++i) {
								ed_task.text.set(19-i, ed_task.text.get(18-i));
							}
							ed_task.text.set(pos, new LinkedList<String>());
						}
						else {
							latency = val2.length() * STD_MOTOR_TIME;
						}

						if (!action.equals("period-a") && !action.equals("i")) {
							List<String> text = ed_task.text.get(ed_task.line_pos);
							temp = new LinkedList<String>(text.subList(0, ed_task.cursor_pos));
							temp.addAll(Arrays.asList(val2.split("\\s")));
							temp.addAll(text.subList(ed_task.cursor_pos, text.size()));
							ed_task.text.set(ed_task.line_pos, temp);

							if (action.equals("type-text-enter")) {
								ed_task.cursor_pos = 0;
								latency = (1 + val2.length()) * STD_MOTOR_TIME;
							}
						}
						else {
							latency = STD_VISUAL_TIME + ((action.equals("period-a") ? 3.0 : 2.0) * STD_MOTOR_TIME);
						}

						determine_v();
						break;
					case "substitute-ed": case "substitute-edt":
						ed_task.text.set(ed_task.line_pos, substitute_insert(val2, val3, ed_task.text.get(ed_task.line_pos)));
						latency = (val2.length() + val3.length() + (action.equals("substitute-ed") ? 5.0 : 4.0)) * STD_MOTOR_TIME;
						ed_task.cursor_pos = 0;
						determine_v();
						break;
					case "insert-ed": case "insert-edt":
						sTemp = val3 + " " + val2;
						ed_task.text.set(ed_task.line_pos, substitute_insert(val2, sTemp, ed_task.text.get(ed_task.line_pos)));
						latency = (2.0*val2.length() + val3.length() + 1.0 + (action.equals("insert-ed") ? 5.0 : 4.0)) * STD_MOTOR_TIME;
						ed_task.cursor_pos = 0;
						determine_v();
						break;
					case "period-d": case "d": case "control-k-twice": case "control-k":
						boolean wasEmpty = ed_task.text.get(ed_task.line_pos).size() == 0;
						if (!action.equals("control-k") || wasEmpty) {
							iTemp = ed_task.line_pos;
							for (int i=0; i<19-iTemp; ++i) {
								ed_task.text.set(i+iTemp, ed_task.text.get(i+iTemp+1));
							}
							ed_task.cursor_pos = 0;
						}

						if (action.equals("control-k") && wasEmpty) {
							ed_task.text.set(ed_task.line_pos, new LinkedList<String>());
						}

						latency = action.equals("control-k") ? STD_MOTOR_TIME : 
							(action.equals("control-k-twice") ? (2.0 * STD_MOTOR_TIME) : (STD_VISUAL_TIME + (action.equals("period-d") ? 3.0 : 2.0) * STD_MOTOR_TIME));
						determine_v();
						break;
					case "period-c": case "r":
						ed_task.text.set(ed_task.line_pos, new LinkedList<String>());
						latency = STD_VISUAL_TIME + ((action.equals("period-c") ? 3.0 : 2.0) * STD_MOTOR_TIME);
						determine_v();
						break;
					case "period": case "control-z":
						latency = STD_VISUAL_TIME + ((action.equals("period") ? 2.0 : 1.0) * STD_MOTOR_TIME);
						break;
					case "number-p":
						ed_task.line_pos = Arrays.asList(numbers).indexOf(val2);
						ed_task.cursor_pos = 0;
						determine_v();
						latency = STD_VISUAL_TIME + (2.0 * STD_MOTOR_TIME);
						break;
					case "t-word":
						iTemp = ed_task.line_pos;
						while (ed_task.text.get(iTemp).indexOf(val2) < 0) {
							iTemp++;
						}
						ed_task.line_pos = iTemp;
						ed_task.cursor_pos = 0;
						latency = (5.0 + val2.length()) * STD_MOTOR_TIME;
						determine_v();
						break;
					}

					rep.addLatency(latency);	// When a report ends from next-instruction, does not include the latency for next-instruction

					if (ed_task.vlist_changed()) {
						// Clear old input
						if (in1 != null) {
							in1.DestroyWME();
							in1 = null;
						}
						if (in2 != null) {
							in2.DestroyWME();
							in2 = null;
						}
						if (in3 != null) {
							in3.DestroyWME();
							in3 = null;
						}
						if (in4 != null) {
							in4.DestroyWME();
							in4 = null;
						}

						in1 = input_link.CreateStringWME("in1", ed_task.vlist[0]);
						in2 = input_link.CreateStringWME("in2", ed_task.vlist[1]);
						in3 = input_link.CreateStringWME("in3", ed_task.vlist[2]);
						in4 = input_link.CreateStringWME("in4", ed_task.vlist[3]);

						ed_task.vlist_changed = false;

					}

					//System.out.println(val1 + " " + val2);	// For debugging
				}
				else if (id.GetCommandName().compareTo(CMD_FINISH) == 0) {
					stopAgent = true;
				}
				else {
					id.AddStatusError();
				}

				id.AddStatusComplete();
			}
		}

		if (stopAgent) {
			// Clear old input
			if (in1 != null) {
				in1.DestroyWME();
				in1 = null;
			}
			if (in2 != null) {
				in2.DestroyWME();
				in2 = null;
			}
			if (in3 != null) {
				in3.DestroyWME();
				in3 = null;
			}
			if (in4 != null) {
				in4.DestroyWME();
				in4 = null;
			}
			agent.StopSelf();

			// Reset for next trial
			ed_task.init();
			ed_task.edits = new ArrayList<String[]>(edit_tasks.get(task_count));
			rep.init();
			determine_v();

		}

	}

}
