/*
 * actransfer_operator_parser.cpp
 *
 *  Created on: Apr 3, 2017
 *      Author: bryan
 */

#include "actransfer_operator_parser.h"

#include <string>
#include <iostream>
#include <vector>
#include <sstream>
#include <map>
#include <algorithm>

#include "free_helper.h"


actransfer_operator_parser::actransfer_operator_parser(std::string in_path, std::string out_path, std::string projName) : inPath(in_path), outPath(out_path), projectName(projName)
{
	try {
		inFile = std::ifstream();
		inFile.open(inPath.c_str());
	}
	catch (...) {
		std::cout << "ERROR: Could not open the lisp input file. Aborting." << std::endl;
	}

	lineNumber = 1;
	currRule_h1 = "";
	currRule_h2 = "";
	currTaskName = "";

	initSlotRefMap();
}

actransfer_operator_parser::~actransfer_operator_parser() {
}
const std::string actransfer_operator_parser::CONST_NAME = "const";

void actransfer_operator_parser::printParseError(std::string msg, bool showLine = true) {
	std::cout << "ERROR PARSING INPUT:" << std::endl << "  ";
	if (showLine)
		std::cout << "LINE " << lineNumber - 1 << ": ";
	std::cout << msg << std::endl;
}

// Trim a line, removing whitespace and trailing comments
std::string actransfer_operator_parser::trim(std::string s) {
	// Remove leading spaces
	s.erase(0, s.find_first_not_of(" \n\r\t"));

	// Accept rule-name directive comments
	if (!s.compare(0,2,";~"))
		return s;
	// Accept lines that specify Actransfer instructions
	if (!s.compare(0,6,"(ins :"))
		return s;
	if (!s.compare(0,11,"(add-instr "))
		return s;
	if (!s.compare(0,1,":"))
		return s;

	return "";
}

std::string actransfer_operator_parser::separateTokens(const std::string &str) {
	// Add spaces between parentheses and quotation marks
	std::string s = str;
	if (s.size() > 1) {
		// Scan backwards through string
		for (int i=s.length()-1; i>=0; --i){
			if (s.at(i) == '(' || s.at(i) == ')' || s.at(i) == '"') {
				s.insert(i+1, " ");
				s.insert(i, " ");
			}
		}
	}

	return s;
}

// Throws the next input line into the given stringstream and resets the stream for reading
std::string actransfer_operator_parser::nextLine(std::stringstream &ss) {
	std::string line = "";
	// Skip comments and whitespace
	try {
	while (!line.compare("") && inFile.good()) {
		std::getline(inFile, line);
		lineNumber++;
		line = trim(line);
	}
	// Separate parentheses and quotation marks
	line = separateTokens(line);
	} catch (...) {
		std::cout << "ERROR: Could not read next line from file." << std::endl;
		throw;
	}

	ss.str(line);
	ss.clear();
	return line;
}

std::string actransfer_operator_parser::nextToken(std::stringstream &ss) {
	std::string token = "";
	if (!ss.good()) {
		nextLine(ss);
	}
	ss >> token;
	return token;
}

void actransfer_operator_parser::initSlotRefMap() {
	slotRefMap = {
		{"Gcontrol","s.G.Gcontrol"},
		{"Gtop","s.G.Gtop"},
		{"Gtask", "s.G.Gtask"},
		{"Gparent", "s.G.Gparent"},
		{"WMid", "s.WM"},
		{"WMprev", "s.WM.WMprev"},
		{"WMnext", "s.WM.WMnext"},
		{"RTprev", "s.RT.WMprev"},
		{"RTnext", "s.RT.WMnext"},
		{"RT1", "s.smem.rt-result"},
		{"RTid", "s.RT"},
		{"AC1", "s.AC.action.slot1"}
	};
}

// Assuming the ss given is just before a "(...)" of slot names, add to the given map the mappings of these slots to addresses in the given slot path
void actransfer_operator_parser::getSlotNames(std::stringstream &ss, std::map<std::string, std::string> &nameMap, std::string slotPath) {
	std::string sToken = nextToken(ss); // Should be "("
	if (sToken.compare("(")) {
		printParseError("Invalid instruction. Expected '(' and slot names for '" + slotPath, true);
	}

	bool inParen = false;
	int addrInd = 1;
	sToken = nextToken(ss);

	if (!sToken.compare("(")) {
		inParen = true;
		sToken = nextToken(ss);
	}

	while (sToken.compare(")")) {
		// Add this token to the map
		nameMap.insert(std::make_pair(sToken, slotPath + "slot" + toString(addrInd++)));

		// Get the next token
		sToken = nextToken(ss);
		if (!sToken.compare(")") && inParen) {
			addrInd = 1;				// Reset the addressing index
			sToken = nextToken(ss);		// Get the next token
			// Pass a newline if it exists
			if (sToken.length() == 0)
				sToken = nextToken(ss);
			if (!sToken.compare("("))
				sToken = nextToken(ss);
		}
	}
}

// Assuming the ss given is pointing to the middle of the add-instr directive, after the instruction name,
// return a map of slot names to slot addresses.
// Leave the stream at the beginning of the operator descriptions (which follow the add-instr setup).
void actransfer_operator_parser::readInstrSettings(std::stringstream &ss, std::map<std::string, std::string> &nameMap) {
	std::string slotPath = "";
	std::string sToken = nextToken(ss);

	// Read until finding the comment symbol
	while (ss.good()) {
		if (!sToken.compare(":input")) {
			getSlotNames(ss, nameMap, "s.V.");
		}
		else if (!sToken.compare(":variables") || !sToken.compare(":working-memory")) {
			getSlotNames(ss, nameMap, "s.WM.");
		}
		else if (!sToken.compare(":declarative")) {
			getSlotNames(ss, nameMap, "s.RT.");
		}
		else if (!sToken.compare(":pm-function") || !sToken.compare(":init") || !sToken.compare(":parameters") || !sToken.compare(":reward")) {
			// End after finding any post-slot parameters
			nextLine(ss);
			while (ss.peek() == ':')
				nextLine(ss);
			return;
		}

		sToken = nextToken(ss);

		// Pass a newline if it exists
		if (sToken.length() == 0)
			sToken = nextToken(ss);
	}
}

std::string actransfer_operator_parser::makeActionRef(const std::string &condRef) {
	if (condRef.compare("s.RT")) {
		// If it's not an RT ref, no need to change it
		return condRef;
	}

	if (condRef.length() == 4) {
		// Special case: replace "s.RT" with "s.Q.retrieve"
		return "s.Q.retrieve";
	}

	std::string retval = condRef;
	retval.replace(2,2, "Q.query");
	return retval;
}

std::string actransfer_operator_parser::getBuffSlot(const std::string &buff, const int slot) {
	if (!buff.compare("AC")) {
		return "s.AC.action.slot" + toString(slot);
	}
	if (!buff.compare("RT") || !buff.compare("RTid")) {
		return "s.Q.query.slot" + toString(slot);
	}
	if (!buff.compare("Gtop")) {
		return "s.G.Gtop.slot" + toString(slot);
	}
	if (!buff.compare("newWM")) {
		return "s.NW.wm.slot" + toString(slot);
	}
	return "";
}

// Get the conditions and actions of the next rule in the file in raw format.
// Constants will still have raw values.
// Returns success, and modifies the given stuctures.
bool actransfer_operator_parser::getRawProps(std::stringstream & ss, std::vector<std::string>& condConsts, std::vector<std::string>& actConsts, std::vector<Primitive>& conditions, std::vector<Primitive>& actions)
{
	// TODO: Make automatic using "(add-instr"
	const int wmprev_ind = 2;	// starting from 0
	/*const std::map<std::string, std::string> condSlotRefs = {
			{"Gcontrol","s.G.Gcontrol"},
			{"Gtop","s.G.Gtop"},
			{"Gtask", "s.G.Gtask"},
			{"Gparent", "s.G.Gparent"},
			//{"Vlabel", "s.V.in1"},
			//{"Vvalue", "s.V.in2"},
			//{"Vtype", "s.V.in1"},
			//{"Vword1", "s.V.in2"},
			//{"Vword2", "s.V.in3"},
			//{"Vline", "s.V.in4"},
			{"WMid", "s.WM"},
			//{"WMatt", "s.WM.slot1"},
			//{"WMvalue", "s.WM.slot2"},
			{"WMprev", "s.WM.WMprev"},
			{"WMnext", "s.WM.WMnext"},
			//{"WMcurline", "s.WM.slot1"},
			//{"WMsearch-goal", "s.WM.slot2"},
			//{"RTtype", "s.RT.slot1"},
			//{"RTarg1", "s.RT.slot2"},
			//{"RTarg2", "s.RT.slot3"},
			//{"RTans", "s.RT.slot4"},
			//{"RTatt", "s.RT.slot1"},
			//{"RTvalue", "s.RT.slot2"},
			{"RTprev", "s.RT.WMprev"},
			{"RTnext", "s.RT.WMnext"},
			{"RT1", "s.smem.rt-result"},
			//{"RTcount-fact", "s.RT.slot1"},
			//{"RTfirst", "s.RT.slot2"},
			//{"RTsecond", "s.RT.slot3"},
			//{"RTdiff-3-fact", "s.RT.slot1"},
			//{"RTnum1", "s.RT.slot2"},
			//{"RTnum2", "s.RT.slot3"},
			{"RTid", "s.RT"}
	};
	const std::map<std::string, std::string> actSlotRefs = {
			{"Gcontrol","s.G.Gcontrol"},
			{"Gtop","s.G.Gtop"},
			{"Gtask", "s.G.Gtask"},
			{"Gparent", "s.G.Gparent"},
			{"WMid", "s.WM"},
			//{"WMatt", "s.WM.slot1"},
			//{"WMvalue", "s.WM.slot2"},
			{"WMprev", "s.WM.WMprev"},
			//{"WMcurline", "s.WM.slot1"},
			//{"WMsearch-goal", "s.WM.slot2"},
			//{"RTtype", "s.Q.query.slot1"},
			//{"RTarg1", "s.Q.query.slot2"},
			//{"RTarg2", "s.Q.query.slot3"},
			//{"RTans", "s.Q.query.slot4"},
			//{"RTatt", "s.Q.query.slot1"},
			//{"RTvalue", "s.Q.query.slot2"},
			{"RTprev", "s.Q.query.WMprev"},
			//{"RT1", "s.Q.query.slot1"},
			//{"RTcount-fact", "s.Q.query.slot1"},
			//{"RTfirst", "s.Q.query.slot2"},
			//{"RTsecond", "s.Q.query.slot3"},
			//{"RTdiff-3-fact", "s.Q.query.slot1"},
			//{"RTnum1", "s.Q.query.slot2"},
			//{"RTnum2", "s.Q.query.slot3"},
			{"AC1", "s.AC.action.slot1"},
			{"RTid", "s.Q.retrieve"}
	};*/



	// Special case negation conditions
	//const std::vector<std::string> specNegs = {"s.V.Vlabel", "s.V.Vvalue"/*, "s.RT.slot2", "s.RT.slot3", "s.RT.slot4"*/};

	// Special case clear-rt conditions
	//const std::vector<std::string> specClears = {"s.RT.result", "s.RT.slot1", "s.RT.slot2", "s.RT.slot3", "s.RT.slot4"};
	Primitive clearAction("","","");

	// Read first task name
	std::string sToken = nextToken(ss);	// "(" or ";~"

	// Handle directives if any
	while (!sToken.compare("(") || !sToken.compare("add-instr") || !sToken.compare(";~") || !sToken.compare(";~~") || sToken.at(0) == ':') {
		if (!sToken.compare("add-instr")) {
			initSlotRefMap();
			currTaskName = nextToken(ss);
			readInstrSettings(ss, slotRefMap);	// These will add task-specific refs to the master refs, without overwriting
		}
		else if (!sToken.compare(";~")) {
			currRule_h1 = nextToken(ss);
			currRule_h2 = "";
		}
		else if (!sToken.compare(";~~")) {
			currRule_h2 = nextToken(ss);
		}
		sToken = nextToken(ss);
	}

	// Begin instruction

	// Skip next "ins"
	sToken = nextToken(ss);


	// *** CONDITIONS ***

	// Check syntax (okay, so we do do a little of that)
	if (sToken.compare(":condition")) {
		printParseError("Invalid instruction. Expected ':condition'.", true);
	}
	nextToken(ss); 			// read "("
	sToken = nextToken(ss);	// read first condition reference

	// Add universal condition testing the current task
	conditions.push_back(Primitive("==", "s.G.Gtask", currTaskName));
	condConsts.push_back(currTaskName);

	// Read conditions until finding closing parenthesis
	while (sToken.compare(")") != 0) {
		std::string p1;
		std::string p2;
		std::string op;

		auto it = slotRefMap.find(sToken);	// Look
		if (it == slotRefMap.end()) {
			printParseError("Unrecognized condition token '" + sToken + "'");
			throw;
		}

		// Get path to of first arg
		p1 = it->second;

		// Read op ("=" or "<>");
		op = nextToken(ss);
		if (op.compare("<>") != 0)
			op = "==";

		// Read arg2
		sToken = nextToken(ss);


		// Check special negation/existence cases
		if (!sToken.compare("nil") /*&& std::find(specNegs.begin(), specNegs.end(), p1) != specNegs.end()*/ )
		{
			if (!op.compare("==")) {
				// Negation test (== nil)
				p2 = "";
				op = "-";
			}
			else {
				// Existence test (<> nil)
				p2 = "";
				op = "";
			}
		}
		else {
			it = slotRefMap.find(sToken);
			if (it != slotRefMap.end()) {
				p2 = it->second;
			}
			else {
				// else (should only happen for constants)
				p2 = sToken;
				condConsts.push_back(p2);
			}
		}

		conditions.push_back(Primitive(op, p1, p2));

		// Check RT test conditions, indicating need to clear RT after reading
		if (/*sToken.compare("nil") && sToken.compare("error") && std::find(specClears.begin(), specClears.end(), p1) != specClears.end()*/
				!p1.compare(0,5, "s.RT.") || (!p1.compare(0,16, "s.smem.rt-result") && op.compare("-")) ) {
			clearAction = (Primitive("=", "s.G.clear-rt", "const1"));
		}



		// Read token for next iteration
		sToken = nextToken(ss);

	}

	// *** ACTIONS ***

	// Check syntax
	sToken = nextToken(ss);
	if (sToken.compare(":action")) {
		printParseError("Invalid instruction. Expected ':action'.", true);
	}
	nextToken(ss); 			// read "("
	sToken = nextToken(ss);	// read first condition reference

	bool usedAC = false;	// Whether we've added a special "AC.action <a>" action
	bool usedQ = false;		// Whether we've added a special "Q.query <q>" action
	bool usedNW = false;	// Whether we've added a special "NW.wm <q>" action
	std::string specialRT = "";	// If not "", replace a query with a retrieve for the referenced slot

	// Define buffer collections (buffer in the act-r sense)
	std::vector<std::string> src_bufferIDs = {
			{"s.RT"},
			{"s.WM"},
			//{"RTid", {"s.RT.slot4.slot1", "s.RT.slot4.slot2", "s.RT.slot4.slot3", "s.RT.slot4.slot4"}},    // This is hacky specific for Elio.
			//{"WMprev", {"s.WM.slot3.slot1", "s.WM.slot3.slot2", "s.WM.slot3.slot3", "s.WM.slot3.slot4"}},  // This is hacky specific for Elio.
			{"s.G.Gtop"}
	};
	/*std::map<std::string, std::vector<std::string>> dst_buffers = {
			{"AC", {"s.AC.action.slot1", "s.AC.action.slot2", "s.AC.action.slot3"}},
			{"RT", {"s.Q.query.slot1", "s.Q.query.slot2", "s.Q.query.slot3", "s.Q.query.slot4"}},
			{"RTid", {"s.Q.query.slot1", "s.Q.query.slot2", "s.Q.query.slot3", "s.Q.query.slot4"}},
			//{"RTprev", {"s.RT.slot4.slot1", "s.RT.slot4.slot2", "s.RT.slot4.slot3", "s.RT.slot4.slot4"}},  // This is hacky specific for Elio.
			//{"WMprev", {"s.WM.slot3.slot1", "s.WM.slot3.slot2", "s.WM.slot3.slot3", "s.WM.slot3.slot4"}},  // This is hacky specific for Elio.
			{"newWM", {"s.NW.wm.slot1", "s.NW.wm.slot2", "s.NW.wm.slot3", "s.NW.wm.slot4"}},
			{"Gtop", {"s.G.Gtop.slot1", "s.G.Gtop.slot2", "s.G.Gtop.slot3", "s.G.Gtop.slot4"}}
	};*/

	while (sToken.compare(")")) {
		// Check for group action (e.g., (div RTvalue WMvalue) -> RT)
		if (!sToken.compare("(")) { // || src_buffers.find(sToken) != src_buffers.end()) {

			// Get source list
			auto sources = std::vector<std::string>();
			std::string pRef, op, buff;

			//if (src_buffers.find(sToken) == src_buffers.end()) {
				sToken = nextToken(ss); // read first item

				while (sToken.compare(")")) {
					// Add each item within the ()'s to the source list
					auto it = slotRefMap.find(sToken);
					if (it != slotRefMap.end()) {
						if (sToken.compare("RTid") == 0) {
							// Special case: If (? ? RTid) -> RT, replace RT with RT.WMnext
							specialRT = it->second;
						}
						pRef = it->second;
					}
					else {
						// (should only happen for constants)
						pRef = sToken;
						/*if (!pRef.compare("?"))
							pRef = "nil";*/
						if (pRef.compare("?") != 0)
							actConsts.push_back(pRef);
					}
					sources.push_back(pRef);
					sToken = nextToken(ss);
				}
			/*}
			else {
				// Use the buffer id to infer the source list
				for (std::string next : src_buffers.at(sToken)) {
					sources.push_back(next);
				}
			}*/

			nextToken(ss);	// read "->"
			op = "=";
			// Read destination buffer
			buff = nextToken(ss);
			/*if (dst_buffers.find(buff) == dst_buffers.end()) {
				std::cout << "ERROR: Unexpected target buffer '" + buff + "' for group value assignment." << std::endl;
				throw;
			}

			auto buffList = dst_buffers.at(buff);*/

			// Check for use of queries - if so, don't clear-rt
			if (!buff.compare("RT"))
				clearAction.path1 = "";

			// Check for the special case of backtracing a pointer ((? ? WMid) -> RTid)
			if (buff.compare("RT") == 0 && specialRT.compare("") != 0) {
				// Add the id attribute being queried
				actions.push_back(Primitive("=","s.Q.wm-query.root",specialRT));
				// Add any extra attributes the id should have
				for (size_t i=0; i<sources.size(); ++i) {
					if (sources.at(i).compare(specialRT) == 0 || sources.at(i).compare("?") == 0)
						continue;
					actions.push_back(Primitive("=","s.Q.wm-query.slot"+toString(i+1),sources.at(i)));
				}
				// Read token for next iteration
				sToken = nextToken(ss);
				continue;
			}

			// Check for need for action that creates an AC action cluster
			if (!usedAC && !buff.compare("AC")) {
				actions.push_back(Primitive("+","s.AC.action",""));
				usedAC = true;
			}
			// Check for need for query that creates an Q query cluster
			if (!usedQ && !buff.compare("RT")) {
				actions.push_back(Primitive("+","s.Q.query",""));
				usedQ = true;
			}
			// Check for a newWM wm cluster
			if (!usedQ && !buff.compare("newWM")) {
				actions.push_back(Primitive("+","s.NW.wm",""));
				usedNW = true;
			}

			// Add action for each corresponding source->dest pair
			for (size_t i=0; i<sources.size(); ++i) {
				std::string src = sources.at(i),
						dst;
				if (!src.compare("?"))
					continue;

				// If this is making a newWM, check for the WMprev value if any
				if (i == wmprev_ind && buff.compare("newWM") == 0 && std::find(src_bufferIDs.begin(), src_bufferIDs.end(), src) != src_bufferIDs.end()) {
					dst = "s.NW.wm.WMprev";
				}
				else {
					dst = getBuffSlot(buff,i+1); //buffList.at(i);
				} /*catch (...) {
					printParseError("Too many values sent to buffer!", true);
					throw;
				}*/	 // If this fails, it's either a bad instruction or the buffers map is bad.


				if (!src.compare("nil")) {
					actions.push_back(Primitive("-", dst, ""));		// Remove WME rather than set to nil
				}
				else {
					actions.push_back(Primitive(op, dst, src));
				}
			}
		}
		else {
			// Normal action
			std::string p1;
			std::string p2;
			std::string op;

			auto it = slotRefMap.find(sToken);
			if (it != slotRefMap.end()) {
				p1 = it->second;
			}
			else {
				// (should only happen for constants)
				p1 = sToken;
				actConsts.push_back(p1);
			}

			nextToken(ss);	// read "->"
			op = "=";

			// Read arg2
			sToken = nextToken(ss);
			it = slotRefMap.find(sToken);
			if (it != slotRefMap.end()) {
				p2 = makeActionRef(it->second);

				// Check for use of queries - if so, don't clear-rt
				if (!p2.compare(0,4,"s.Q."))
					clearAction.path1 = "";
				else if (!p2.compare("s.G.Gtask"))
					clearAction = (Primitive("=", "s.G.clear-rt", "const1"));
			}
			else {
				printParseError("Unrecognized action token '" + sToken + "'");
				throw;
			}

			// Check for need for action that creates an AC action cluster
			if (!usedAC && (!p1.compare(0,5, "s.AC.") || !p2.compare(0,5, "s.AC."))) {
				actions.push_back(Primitive("+","s.AC.action",""));
				usedAC = true;
			}
			// Check for need for action that creates an Q query cluster
			if (!usedQ && (!p1.compare(0,4, "s.Q.") || !p2.compare(0,4, "s.Q."))) {
				actions.push_back(Primitive("+","s.Q.query",""));
				usedQ = true;
			}
			// Check for a newWM cluster
			if (!usedNW && (!p1.compare(0,5, "s.NW.") || !p2.compare(0,5, "s.NW."))) {
				actions.push_back(Primitive("+","s.NW.wm",""));
				usedNW = true;
			}

			actions.push_back(Primitive(op, p2, p1));
		}

		// Read token for next iteration
		sToken = nextToken(ss);
	}

	if (clearAction.path1.compare(""))
		actions.push_back(clearAction);
	else if (!currRule_h2.compare("pcmd") || !currRule_h2.compare("read-instruction") ||
			((!currRule_h2.compare("for-replace")
			|| !currRule_h2.compare("for-delete")
			|| !currRule_h2.compare("for-insert"))
			&& (!currTaskName.compare("ed") || !currTaskName.compare("edt"))))
	{
		actions.push_back(Primitive("=", "s.G.clear-rt", "const1"));
	}

	ss.str(""); 	// clear any remaining tokens from this line
	ss.clear();

	return true;
}

// Builds and returns a whole pp rule
void actransfer_operator_parser::buildPropCode(std::string &retval, const std::string ruleName, const std::vector<std::string>& consts, const std::vector<Primitive>& conditions, const std::vector<Primitive>& actions)
{
	// Begin with the normal header
	retval = "pp {" + ruleName + "\n";

	// Add constants
	for (auto &c : consts) {
		retval += "\t" + c + "\n";
	}
	retval += "\t--\n";

	// Add conditions
	for (const Primitive &p : conditions) {
		if (!p.path2.compare("")) {
			// Single argument like existence or negation
			retval += "\t" + p.path1 + " " + p.op + "\n";
		}
		else {
			retval += "\t" + p.path1 + " " + p.op + " " + p.path2 + "\n";
		}
	}

	retval += "-->\n";

	// Add actions
	for (const Primitive &p : actions) {
		if (!p.op.compare("=")) {
			if (!p.path2.compare("")) {
				// Single argument like existence or negation
				retval += "\t" + p.path1 + " " + p.op + "\n";
			}
			else {
				retval += "\t" + p.path1 + " " + p.op + " " + p.path2 + "\n";
			}
		}
		else if (!p.op.compare("-")) {
			retval += "\t" + p.path1 + " " + p.op + "\n";
		}
		else {
			// Is an operator preference
			if (p.path2.size() > 0)
				retval += "\t" + p.path1 + "." + p.path2 + " " + p.op + "\n";
		}
	}

	// Add closer
	retval += "}\n";

}

// Split a given dot-notation string at the last dot and return the segments
bool splitDotChain(std::string const &original, std::string &front, std::string &back) {
	int dotPos = original.find_last_of('.');
	if (dotPos == std::string::npos) {
		dotPos = original.size();
		front = original;
		back = "";
		return false;
	}
	else {
		front = original.substr(0, dotPos);
		back = original.substr(dotPos+1);
		return true;
	}
}

// Convert set of primitives (with original const values) into "<s> ^attr1.attr2 <id>" strings.
// Stores list of converted reference declarations in retRefs.
// Returns map from old dot notation source to new reference "s.attr1.attr2":"<id>".
// Map also includes consts mapped to themselves.
std::map<std::string, std::string> actransfer_operator_parser::buildSoarIdRefs(std::vector<std::string> &retRefs, const std::vector<Primitive> &conditions, std::vector<Primitive> &actions, std::vector<std::string> &negTestRefs) {
	auto retval = std::map<std::string, std::string>();
	//auto ids = std::vector<std::string>();
	std::string id1 = "",
				id2 = "",
				p11, p12, p21, p22;
	int idnum = 1;

	auto newActions = actions;

	retval["s"] = "<s>";

	// Conditions
	for (const Primitive &c : conditions) {
		splitDotChain(c.path1, p11, p12);
		//splitDotChain(p11, p111, p112);

		// Do arg1
		if (!p11.compare(0,2, "s.")) {
			if (!retval.count(p11)) {
				id1 = "<c" + toString(idnum++) + ">";
				retval[p11] = id1;

				retRefs.push_back("state <s> ^" + p11.substr(2) + " " + id1);
			}
		}
		else {
			std::cout <<"ERROR: invalid condition path '" + c.path1 + "'\n";
			throw;
		}

		// Do arg2 (might be a const)
		if (!c.op.compare("-") && !c.path2.compare("")) {
			negTestRefs.push_back(c.path1);
		}
		if (!splitDotChain(c.path2, p21, p22)) {
			retval[p21] = p21;	// is a const
			id2 = p21;
		}
		else {
			if (!retval.count(p21)) {
				id2 = "<c" + toString(idnum++) + ">";
				retval[p21] = id2;
				retRefs.push_back("state <s> ^" + p21.substr(2) + " " + id2);
			}
			else
				id2 = retval[p21];
			if (!retval.count(c.path2)) {
				id1 = "<c" + toString(idnum++) + ">";
				retval[c.path2] = id1;
				retRefs.push_back(id2 + " ^" + p22 + " " + id1);
			}
		}
		//Add the actual condition test
		//retRefs.push_back(id1 + " ^" + p12 + " " + id2);
	}

	// Actions
	for (const Primitive &a : actions) {
		splitDotChain(a.path1, p11, p12);

		// Do arg1
		if (!a.path1.compare("s.operator") || !a.path1.compare("s.operator.name")) {
			// Do nothing, leave for action id creation, not for condition side
			/*id1 = "<c" + toString(idnum++) + ">";
			retval[a.path1] = id1;
			retRefs.push_back("<s> ^operator " + id1);*/
		}
		else if (!a.path1.compare("s.AC") || !p11.compare(0,11, "s.AC.action")
				|| !a.path1.compare("s.Q") || !p11.compare(0,9, "s.Q.query")) {
			/*if (!retval.count("s.AC.action")) {
				retval["a"] = "<a>";
				retRefs.push_back("state <s> ^AC.action <a>");
			}*/
		}
		else if (!a.path1.compare("s.G.clear-rt")) {
			if (!retval.count("s.G")) {
				id1 = "<c" + toString(idnum++) + ">";
				retval["s.G"] = id1;

				retRefs.push_back("state <s> ^G " + id1);
			}
		}
		else if (!p11.compare(0,2, "s.")) {
			if (!retval.count(p11)) {
				// If an action of the form "s.foo.ey bar" and s.foo hasn't been tracked yet
				id1 = "<c" + toString(idnum++) + ">";
				retval[p11] = id1;

				retRefs.push_back("state <s> ^" + p11.substr(2) + " " + id1);
			}
		}
		else if (!p11.compare("s")) {
			if (!retval.count(p11)) {
				// If an action of the form "s.foo bar", and <s> hasn't been tracked yet
				id1 = "<c" + toString(idnum++) + ">";
				retval[p11] = id1;

				retRefs.push_back("state <s> ^" + p12.substr(2) + " " + id1);
			}
		}
		/*else if () {
			if (!retval.count(p11)) {
				id1 = "<c" + toString(idnum++) + ">";
				retval[a.path1] = id1;

				//retRefs.push_back("<a> ^" + p11.substr(2) + " " + id1);
			}
		}*/
		else {
			std::cout << "ERROR: invalid action path '" + a.path1 + "'\n";
			throw;
		}

		if (a.path2.size() == 0)	// creating new ID (ie, operator or AC.action)
			continue;

		// Do arg2 (might be a const)
		if (!splitDotChain(a.path2, p21, p22)) {
			retval[p21] = p21;	// is a const
		}
		else {
			// Add the parent id ref
			if (!retval.count(p21)) {
				id2 = "<c" + toString(idnum++) + ">";
				retval[p21] = id2;
				retRefs.push_back("state <s> ^" + p21.substr(2) + " " + id2);
			}
			// Add the end value ref
			if (!retval.count(a.path2)) {
				id2 = "<c" + toString(idnum++) + ">";
				retval[a.path2] = id2;
				id1 = retval[p21];
				retRefs.push_back(id1 + " ^" + p22 + " " + id2);
			}
		}
		// Add action to remove the old value
		if (std::find(negTestRefs.begin(), negTestRefs.end(), a.path1) == negTestRefs.end()
				&& a.path1.compare(0,10, "s.operator") && a.path1.compare(0,4, "s.Q.") && a.path1.compare(0,4, "s.AC") && a.path1.compare(0,3, "s.Q") && a.path1.compare("s.G.clear-rt")) {
			// Add reference to the old value
			id2 = "<c" + toString(idnum++) + ">";
			retRefs.push_back(retval.at(p11) + " ^" + p12 + " " + id2);
			// Add remove action
			retval[id2] = id2;
			newActions.push_back(Primitive("-", a.path1, id2));
		}
	}

	actions = newActions;

	return retval;
}

// Builds and returns a whole sp rule
void actransfer_operator_parser::buildSoarCode(std::string &retval, const std::string ruleName, const std::vector<Primitive>& conditions, const std::vector<Primitive>& actions, std::vector<std::string> &negTestRefs)
{
	// Build soar code id references
	std::vector<std::string> condRefs;
	int idnum = 1;
	auto soarActions = actions;	// make a copy that will be modified to include remove actions in the next line:
	auto refMap = buildSoarIdRefs(condRefs, conditions, soarActions, negTestRefs);

	// Begin with the normal header
	retval = "sp {" + ruleName + "\n";

	// Add condition references
	for (const std::string &ref : condRefs) {
		retval += "\t(" + ref + ")\n";
	}

	// Add condition tests
	std::string str, p11, p12, p2, p21, p22;

	for (const Primitive &c : conditions) {
		splitDotChain(c.path1, p11, p12);
		splitDotChain(c.path2, p21, p22);

		if (!refMap.count(p11)) {
			std::cout << "ERROR: couldn't parse the condition reference '" + p11 + "'" << std::endl;
			throw;
		}
		if (!refMap.count(p21)) {
			std::cout << "ERROR: couldn't parse the condition reference '" + p21 + "'" << std::endl;
			throw;
		}
		p11 = refMap.at(p11);
		p2 = refMap.at(c.path2);
		if (!c.op.compare("<>")) {
			p2 = "<> " + p2;
		}

		if (!c.op.compare("-"))
			retval += "\t(" + p11 + " -^" + p12 + " " + p2 + ")\n";
		else
			retval += "\t(" + p11 + " ^" + p12 + " " + p2 + ")\n";

	}

	retval += "\t-->\n";

	// Add actions
	for (const Primitive &a : soarActions) {
		splitDotChain(a.path1, p11, p12);

		if (!refMap.count(p11)) {
			std::cout << "ERROR: couldn't parse the action reference '" + p11 + "'" << std::endl;
			throw;
		}
		p11 = refMap.at(p11);
		if (a.path2.size() == 0) {
			p2 = "<n" + toString(idnum++) + ">";
			refMap[a.path1] = p2;
		}
		else {
			splitDotChain(a.path2, p21, p22);
			if (!refMap.count(a.path2)) {
				std::cout << "ERROR: unsupported action dot reference." << std::endl;
				throw;
			}
			p2 = refMap.at(a.path2);
			if (!a.op.compare("-")) {
				p2 = p2 + " -";
			}
		}

		retval += "\t(" + p11 + " ^" + p12 + " " + p2 + ")\n";

	}

	// Add closer
	retval += "}\n";

}

// Build a pair of pp rules for an operator proposal+apply
void actransfer_operator_parser::buildPropOperator(std::string &proposeRule, std::string &applyRule,
		std::vector<std::string>& condConsts, std::vector<std::string>& actConsts,
		const std::vector<Primitive>& conditions, const std::vector<Primitive>& actions)
{
	std::string currRuleName;
	std::string h2 = "";
	if (currRule_h2.length() > 0)
		h2 = "*" + currRule_h2;

	// *** PROPOSE RULE ***
	currRuleName = "propose*" + projectName + "*" + currTaskName + "*" + currRule_h1 + h2;
	//std::replace(currRuleName.begin(), currRuleName.end(), '-', '*');
	// Make the operator-proposal action
	std::vector<Primitive> opActions = {Primitive("+", "s", "const1")};
	buildPropCode(proposeRule, currRuleName, condConsts, conditions, opActions);

	// *** APPLY RULE ***
	currRuleName = "apply*" + projectName + "*" + currTaskName + "*" + currRule_h1 + h2;
	//std::replace(currRuleName.begin(), currRuleName.end(), '-', '*');
	// Make the operator-proposal action
	std::vector<Primitive> opConds = {Primitive("==", "s.operator.name", "const1")};
	buildPropCode(applyRule, currRuleName, actConsts, opConds, actions);
}

// Build a pair of sp rules for an operator proposal+apply
void actransfer_operator_parser::buildSoarOperator(std::string &proposeRule, std::string &applyRule,
		const std::vector<Primitive> &conditions, const std::vector<Primitive> &actions)
{
	std::string currRuleName;
	std::string h2 = "";
	std::string opName = projectName + "-" + currTaskName + "-" + currRule_h1;
	if (currRule_h2.length() > 0) {
		h2 = "*" + currRule_h2;
		opName += "-" + currRule_h2;
	}

	auto negTestRefs = std::vector<std::string>();
	// *** PROPOSE RULE ***
	currRuleName = "propose*" + projectName + "*" + currTaskName + "*" + currRule_h1 + h2;
	//std::replace(currRuleName.begin(), currRuleName.end(), '-', '*');
	// Make the operator-proposal action
	std::vector<Primitive> opActions = {Primitive("+", "s.operator", ""), Primitive("+", "s.operator.name", opName)};
	buildSoarCode(proposeRule, currRuleName, conditions, opActions, negTestRefs);

	// *** APPLY RULE ***
	currRuleName = "apply*" + projectName + "*" + currTaskName + "*" + currRule_h1 + h2;
	//std::replace(currRuleName.begin(), currRuleName.end(), '-', '*');
	// Make the operator-proposal action
	std::vector<Primitive> opConds = {Primitive("==", "s.operator.name", opName)};
	buildSoarCode(applyRule, currRuleName, opConds, actions, negTestRefs);
}

std::vector<std::string> actransfer_operator_parser::refineConsts(std::vector<std::string> rawConsts, const std::vector<Primitive> &primitives, std::vector<Primitive> &newprimitives) {
	int i = 2;
	auto constIds = std::map<std::string, int>();
	auto constDecls = std::vector<std::string>();	// Pre-generate the output const declarations
	newprimitives = primitives;

	// Insert the operator name constant - it's used by all rules we generate
	std::string h2 = "";
	if (currRule_h2.length() > 0)
		h2 = "-" + currRule_h2;
	std::string opName = projectName + "-" + currTaskName + "-" + currRule_h1 + h2;
	constDecls.push_back(CONST_NAME + "1 " + opName);
	constIds[opName] = 1;

	for (std::string &s : rawConsts) {
		if (!constIds.count(s)) {
			// Register a new const reference
			std::string cname = CONST_NAME + toString(i);
			constDecls.push_back(cname + " " + s);
			constIds[s] = i++;
			// Replace any instance of the const in the instructions with the const reference
			for (Primitive &p : newprimitives) {
				if (!p.path1.compare(s))
					p.path1 = cname;
				if (!p.path2.compare(s))
					p.path2 = cname;
			}
		}
	}
	return constDecls;
}

// Convert the object's given actransfer lisp file into sets of props instructions.
// Returns a list of strings, each corresponding to one production / instruction set
// Note: This function does not do syntax error checking.
void actransfer_operator_parser::parseActransferFile(std::vector<std::string> &propRules, std::vector<std::string> &soarRules) {
	std::stringstream ss;
	lineNumber = 1;

	nextLine(ss);	// needed to init nextToken usage

	// For each production:
	while (inFile.good()) {
		auto condRawConsts = std::vector<std::string>(),			// Ordered list of const values
			 actRawConsts = std::vector<std::string>();
		auto rawConditions = std::vector<Primitive>();
		auto rawActions = std::vector<Primitive>();
		auto conditions = std::vector<Primitive>();
		auto actions = std::vector<Primitive>();

		// Get the conditions and actions
		if (!getRawProps(ss, condRawConsts, actRawConsts, rawConditions, rawActions)) {
			propRules.clear();
			return;
		}

		// Skip whitespace so the inFile.good() test is correct
		inFile >> std::ws;
		nextLine(ss);

		// Find ordering of non-duplicate constants
		auto condConsts = refineConsts(condRawConsts, rawConditions, conditions);
		auto actConsts = refineConsts(actRawConsts, rawActions, actions);

		// Separate id chains to

		// Construct PROPs code
		std::string proposeRule = "", applyRule = "";
		buildPropOperator(proposeRule, applyRule, condConsts, actConsts, conditions, actions);
		propRules.push_back(proposeRule);
		propRules.push_back(applyRule);

		// Construct Soar code
		proposeRule = "";
		applyRule = "";
		try {
			buildSoarOperator(proposeRule, applyRule, rawConditions, rawActions);
		} catch (...) {
			std::cout << "ERROR: Couldn't build Soar code.\n" << std::endl;
		}
		soarRules.push_back(proposeRule);
		soarRules.push_back(applyRule);

	}	// end while reading file

}

// Public runner for the conversion process
int actransfer_operator_parser::convertToPropsInstructions() {
	// Check if file is open
	if (!inFile.is_open()) {
		std::cout << "ERROR: Could not open input file for conversion." << std::endl;
		return -1;
	}

	// Get the PROP-formatted instructions
	std::vector<std::string> propRules,
							 soarRules;
	parseActransferFile(propRules, soarRules);

	// Quit on error. Error message already given.
	if (propRules.size() == 0) {
		std::cout << "No rules found. Done." << std::endl;
		return -1;
	}

	// Save the prop file
	std::ofstream outFile;
	std::string outPathFull = outPath + ".prop";

	try {
		// Print header
		outFile.open(outPathFull.c_str(), std::ios::trunc);
		outFile << "#####" << std::endl
				<< "# THIS FILE TRANSLATES THE ACTRANSFER PRODUCTIONS" << std::endl
				<< "# FROM '" << inPath << "' INTO INTERMEDIATE PROP INSTRUCTIONS." << std::endl
				<< "#####" << std::endl << std::endl;

		// Print instructions
		for (std::string &str : propRules) {
			outFile << str;
			std::printf(str.c_str());
		}
	}
	catch (...) {
		std::cout << "ERROR: Could not open the prop output file. Aborting." << std::endl;
		return -1;
	}
	outFile.close();


	// Save the soar file
	try {
		outPathFull = outPath + ".soar";
		// Print header
		outFile.open(outPathFull.c_str(), std::ios::trunc);
		outFile << "#####" << std::endl
				<< "# THIS FILE TRANSLATES THE ACTRANSFER PRODUCTIONS" << std::endl
				<< "# FROM '" << inPath << "' INTO SOURCEABLE SOAR PRODUCTIONS." << std::endl
				<< "#####" << std::endl << std::endl;

		// Print instructions
		for (std::string &str : soarRules) {
			outFile << str;
			std::printf(str.c_str());
		}
	}
	catch (...) {
		std::cout << "ERROR: Could not open the soar output file. Aborting." << std::endl;
		return -1;
	}
	outFile.close();

	return 0;
}
