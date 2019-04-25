package edu.umich.eecs.soar.props.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import edu.umich.eecs.soar.propsutil.LearnConfig;
import edu.umich.eecs.soar.propsutil.PROPsEnvironment;


public class EditorsWorld extends PROPsEnvironment {
	private static double STD_MOTOR_TIME = 0.25,
						  STD_VISUAL_TIME = 0.25;

	private ETask ed_task;
	private Report rep;
	
	//private int task_count;				// The index of edit_tasks to use next
	private String task_name;
	
	private List<ArrayList<String[]>> edit_tasks;
	private String[] numbers;
	
	private boolean vtarget_screen = true; 

	public boolean inDebug = false;
	
	EditorsWorld() {
		String proj_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/";
		String props_dir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/PROPsAgent/";
		
		this.setAgentName("EditorsAgent");
		this.setPropsDir(props_dir);
		
		this.setInstructionsFile(proj_dir + "editors_agent3_instructions.soar");
		this.setSoarAgentFile(proj_dir + "editors_agent3.soar");
		
		this.setIOSize(5, 3);
		
		this.setUserAgentFiles(Arrays.asList("/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/lib_actransfer_prop3_interface.soar", 
												proj_dir + "editors_agent_smem.soar"));
		
		this.edit_tasks = new LinkedList<ArrayList<String[]>>();
		
		this.edit_tasks.add(new ArrayList<String[]>(6));
		edit_tasks.get(0).add(new String[]{"instruction", "replace-word", "vader", "moeder", "one"});
		edit_tasks.get(0).add(new String[]{"instruction", "insert-word", "inhoud", "nieuwe", "three"});
		edit_tasks.get(0).add(new String[]{"instruction", "delete-word", "slome", "", "five"});
		edit_tasks.get(0).add(new String[]{"instruction", "replace-line", "ebooks en sociale medi", "electronisch boeken en andere vormen van sociale media", "eight"});
		edit_tasks.get(0).add(new String[]{"instruction", "delete-line", "of the rings trilogie", "", "fifteen"});
		edit_tasks.get(0).add(new String[]{"instruction", "insert-line", "Oscar of niet Rowling zal er niet om rouwen want de buit is al binnen", "net zo groot als tien jaar geleden", "eighteen"});
		
		this.edit_tasks.add(new ArrayList<String[]>(6));
		edit_tasks.get(1).add(new String[]{"instruction", "insert-word", "pers", "muskieten", "two"});
		edit_tasks.get(1).add(new String[]{"instruction", "replace-line", "fans mochten een blik op de inhoud werpen onder voorwaarde van strikte geheimhouding", "fans hadden de gelegenheid om alvast een kijkje te nemen", "three"});
		edit_tasks.get(1).add(new String[]{"instruction", "replace-word", "medi", "media", "eight"});
		edit_tasks.get(1).add(new String[]{"instruction", "delete-word", "eindelijk", "", "fourteen"});
		edit_tasks.get(1).add(new String[]{"instruction", "delete-line", "We all know what happened in the end but", "", "sixteen"});
		edit_tasks.get(1).add(new String[]{"instruction", "insert-line", "kassucces De spanning is daarom groot dit jaar", "succes Het zal Roling waarschijnlijk een worst wezen", "seventeen"});

		this.edit_tasks.add(new ArrayList<String[]>(6));
		edit_tasks.get(2).add(new String[]{"instruction", "replace-line", "Geestelijk vader van de tovenaarsleerling JK Rowling lanceert morgen de site pottermorecom", "Wederom is het tijd voor een nieuwe website over harry potter maar deze keer van Rowling zelf", "one"});
		edit_tasks.get(2).add(new String[]{"instruction", "insert-word", "paar", "klein", "two"});
		edit_tasks.get(2).add(new String[]{"instruction", "delete-word", "nieuwe", "", "five"});
		edit_tasks.get(2).add(new String[]{"instruction", "delete-line", "Op dit moment staat de laatste film in de serie op het punt om in de bioscoop", "", "twelve"});
		edit_tasks.get(2).add(new String[]{"instruction", "replace-word", "Oscar", "prijs", "thirteen"});
		edit_tasks.get(2).add(new String[]{"instruction", "insert-line", "kassucces De spanning is daarom groot dit jaar", "And here we have another meaningless line that makes this text een more unreadable", "seventeen"});


		numbers = new String[]{"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
				"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"}; 
		
	}
	
	public void runEditorsDebug(String task, int taskNum, LearnConfig config) {
		String taskSeq = task + "_" + (taskNum+1);
		task_name = task;
		//task_count = taskNum;
		inDebug = true;
		this.runDebug(task, taskSeq, config);
		inDebug = false;	// FIXME: This never is called, because ending debugger kills thread (fix environment)
	}
	
	private int getEditTaskIndex(String taskSeq) {
		if (taskSeq.contains("_1")) {
			return 0;
		}
		else if (taskSeq.contains("_2")) {
			return 1;
		}
		else if (taskSeq.contains("_3")) {
			return 2;
		}
		else return -1;	// Error, shouldn't happen
	}
	
	
	// Helper functions specifically for the Editors environment
	private void determine_v() {
		// Only update the perception for the screen if we're looking at the screen
		if (!vtarget_screen) 
			return;
		
		List<String> L = ed_task.text.get(ed_task.line_pos);
		String line = numbers[ed_task.line_pos];
		String word;
		if (L.size() <= ed_task.cursor_pos)
			word = "eol";
		else
			word = ed_task.text.get(ed_task.line_pos).get(ed_task.cursor_pos);
		
		ed_task.set_vlist("screen", "word", word, "", line);
		
	}
	
	private List<String> substitute_insert(String old_element, String new_element, List<String> l) {
		if (l == null || l.size() == 0)
			return l;
		return new LinkedList<String>(Arrays.asList(String.join(" ", l).replaceFirst(old_element, new_element).split("\\s+")));
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

		switch (rep.state) {
		case "ll-noread":
			if (action.equals("read-instruction")) {
				rep.state = "ll";
				rep.task = ed_task.edits.get(0)[1];
			}
			else {
				System.out.println("Something went wrong...");
			}
			break;
		case "ll":
			if (action.equals("number-p") || action.equals("enter") || action.equals("t-word")) {
				rep.ll = this.getElapsedTime() - rep.strt;
				rep.state = "ll-motor";
			}
			if (action.equals("read-instruction") && ed_task.edits.get(0)[4].equals(numbers[ed_task.line_pos])) {
				if (rep.state.equals("ll")) {
					rep.ll = this.getElapsedTime()-rep.strt;
					rep.state = "mt";
					rep.temp = this.getElapsedTime();
				}
			}
			break;
		case "ll-motor":
			if (action.equals("read-instruction")) {
				rep.state = "mt";
				rep.temp = this.getElapsedTime();
			}
			break;
		case "mt":
			temp = new ArrayList<String>(Arrays.asList("substitute-ed", "substitute-edt", "insert-ed", "insert-edt",
					"period-d", "type-text", "type-text-enter", "d", "control-k", "control-k-twice", "esc-d"));
			if (temp.contains(action)) {
				rep.state = "mt-motor";
				rep.mt = this.getElapsedTime() - rep.temp;
			}
			break;
		case "mt-motor":
			if (action.equals("next-instruction")) {
				rep.state = "ll";
				
				this.addReport(String.format("%1$s %2$d %3$s %4$d %5$-15s %6$.3f %7$.3f %8$.3f %9$.3f",
						rep.taskSetName, rep.trialNum, this.getTask().toUpperCase(), rep.editNum,
						rep.task.toUpperCase(), this.milliToSec(rep.ll), this.milliToSec(rep.mt), 
						this.milliToSec(this.getElapsedTime()-rep.strt), rep.latencies));
				
				if (ed_task.edits.size() <= 1)
					rep.task = "";
				else
					rep.task = ed_task.edits.get(1)[1];
				rep.strt = this.getElapsedTime();
				rep.temp = this.getElapsedTime();		// NOTE: Not in Taatgen's script
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
			vtarget_screen = true;
			determine_v();
			latency = STD_VISUAL_TIME;
			break;
		case "read-instruction":
			vtarget_screen = false;
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
				//double time = this.milliToSec(this.getElapsedTime());
				ed_task.set_vlist("end", "end", "end", "end", "end");
				rep.state = "end";
			}
			latency = STD_VISUAL_TIME;
			break;
		case "focus-on-word": case "focus-on-next-word":
			if (action.equals("focus-on-word")) {
				ed_task.line = new LinkedList<String>(Arrays.asList(ed_task.edits.get(0)[2].split("\\s")));
			}

			sTemp = "short";
			if (ed_task.line.size() == 1 || ed_task.line.get(0).length() > 4)	// If the word in the line is longer than 4 characters
				sTemp = "long";
			ed_task.set_vlist("instruction", "single-word", ed_task.line.get(0), sTemp, "");
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

		rep.latencies += latency;
		this.addAgentLatency(this.secToMilli(latency));

		for (int i=0; i<ed_task.vlist.length; ++i) {
			try {
				this.setInput(i,ed_task.vlist[i]);

			} catch (Exception e) {
				System.err.println("Wrong index....");
			}
		}
		
	}


	@Override
	protected void user_createAgent() {
		// Called from initAgent() and runDebug(), when the agent is created
		ed_task = new ETask();//(1,1,current_sample);
		rep = new Report();
		//rep.strt = this.getElapsedTime();
		vtarget_screen = true;
		
		this.clearReports();
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
			if (!this.initAgent())
				break;
			
			int task_count = 0;
			rep.taskSetName = sac.name;
			
			for (int i=0; i<6 && !this.hasError(); ++i) { // Each subject comes in for 6 'days'
				int j = (int)(1800.0 / (double)sac.trials[i] + 0.5); // How many trials can fit into the 'day'
				String condition = sac.conditions[i/2];

				for (int k=0; k<j && !this.hasError(); ++k) {
					task_name = condition;
					this.setTask(task_name, task_name + "_" + Integer.toString(task_count + 1));

					// Init report
					rep.init();
					rep.strt = this.getElapsedTime();
					rep.temp = this.getElapsedTime(); // NOTE: Not in Taatgen's model
					//rep.taskName = task_name;
					rep.trialNum = i+1;
					rep.editNum = k+1;
					
					this.runAgent();	// Run until receiving the finish command
					
					task_count = (task_count + 1) % 3;
					
					if (!this.hasError()) {	// abort potentially gets set in the updateEventHandler method
						this.printReports();
						this.clearReports();
						System.out.println("Done: " + sac.name + ", " + condition + " " + Integer.toString(i+1) + "," + Integer.toString(k+1));
					}
					else {
						System.out.println("ERROR RETURNED BY AGENT FOR " + condition + " " + Integer.toString(i+1) + "," + Integer.toString(k+1) + "!");
						break;
					}
				}
			}
			
			if (this.hasError()) 
				break;
		}

		//agent.ExecuteCommandLine("clog -c");
		//this.agentError = false;
	}


	@Override
	protected void user_errorListener(String arg0) {
		System.err.println("ERROR DETECTED!");
	}


	@Override
	protected void user_agentStart() {
		// Init
		//ed_task.init();
		//int taskInd = getEditTaskIndex(this.taskSequenceName);
		//if (taskInd != -1)
		//	ed_task.edits = new ArrayList<String[]>(edit_tasks.get(taskInd));
		this.clearReports();
		rep.init();
		rep.strt = this.getElapsedTime();
		determine_v();	
	}
	
	@Override
	protected void user_agentStop() {
		// Increment the task
		if (inDebug) {
			int currTaskInd = getEditTaskIndex(this.getTaskInstance());
			currTaskInd = (currTaskInd + 1) % 3;
			this.setTask(this.getTask(), this.getTask() + "_" + Integer.toString(currTaskInd + 1));
		}
		else {
			user_updateTask();
		}
	}

	@Override
	protected void user_updateTask() {
		ed_task.init();		// Resets the text to be edited
		ed_task.edits = new ArrayList<String[]>(edit_tasks.get(getEditTaskIndex(this.getTaskInstance())));
	}

}
