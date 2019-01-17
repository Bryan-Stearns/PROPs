#include "props_instruction_parser.h"

#include <string>
#include <iostream>
#include <vector>
#include <sstream>
#include <map>
#include <cctype>
#include <algorithm>

#include "free_helper.h"


struct ArgIdChainHash
{
	std::size_t operator()(const arg_id_chain& k) const
	{
		std::size_t hash = 0;
		for (auto &i : k.first)
			hash_combine(hash, i);
		for (auto &i : k.second)
			for (auto &j : i)
				hash_combine(hash, j);
		return hash;
	}
};


props_instruction_parser::props_instruction_parser(std::string iPath, std::string oPath) : inPath(iPath), outPath(oPath) {
	inFile = std::ifstream();
	inFile.open(inPath.c_str());
	instNumber = 1;
	constNumber = 1;
	maxPropNumber = 1;
	chainNumber = 1;
	lineNumber = 1;	// unnecessary
	currRuleName = "";
	currRuleHasOpRef = false;
	currTaskName = "default";

	CONST_ID = "props$const";	// Used for internal intermediate referencing constants, and external printing of op-names in subchains
	BUFFER_ID = "B1";
	ROOT_MARKER = "props$rootstate";
	THRESHOLD_NUM = "2";
}


props_instruction_parser::~props_instruction_parser() {
	inFile.close();
}

// Custom tokenize separating a given 'ID.attr.attr.attr...' chain
std::vector<std::string> props_instruction_parser::tokenizeSlot(std::string s) {
	auto retval = std::vector<std::string>();
	size_t ind = s.find_first_of(".");
	// Replace the rootstate keyword in the ID if present
	std::string substr = s.substr(0, ind);
	if (substr.compare("s") == 0) {
		s.replace(0, ind, ROOT_MARKER);
		ind = s.find_first_of(".");
	}
	while (ind != std::string::npos) {
		retval.push_back(s.substr(0, ind));
		s = s.substr(ind + 1, s.size());
		ind = s.find_first_of(".");
	}
	retval.push_back(s);

	return retval;
}

// Recombines a {ID attr attr} vector into 'ID.attr.attr'
std::string props_instruction_parser::untokenize(std::vector<std::string> tokens) {
	std::string retval = "";
	for (size_t i = 0; i<tokens.size(); ++i) {
		retval += tokens.at(i);
		if (i < tokens.size() - 1)
			retval += ".";
	}
	return retval;
}

std::string props_instruction_parser::trim(std::string s) {
	int start = s.find_first_not_of(" #\n\r\t");
	int end = s.find_first_of("#");
	end = s.substr(0, end).find_last_not_of(" \n\r\t") + 1;
	if (start < 0)
		start = 0;
	if (end > start)
		return s.substr(start, end - start);
	else
		return "";
}

// Throws the next input line into the given stringstream and resets the stream for reading
std::string props_instruction_parser::nextLine(std::stringstream &ss) {
	std::string line = "";
	// Skip comments and whitespace
	while (!line.compare("")) {
		//inFile >> std::ws;
		getline(inFile, line);
		lineNumber++;
		if (line.compare(0,12, "# add-instr "))	// Allow special comment-nested meta data to pass
			line = trim(line);
	}
	ss.str(line);
	ss.clear();
	return line;
}

// Returns whether the given string is of an ID format (either A12 or @12, a letter or @ followed by numbers)
bool props_instruction_parser::isID(const std::string &str) {
	std::string s = str;
	if (s.substr(0, 1).compare("@") == 0 && std::isdigit(s.at(1)))
		//s = s.substr(1, std::string::npos);
		return true;
	if (!std::isalpha(s.at(0)))
		return false;
	try {
		std::stoi(s.substr(1, std::string::npos));
		return true;
	}
	catch (...) {
		return false;
	}
}

void props_instruction_parser::printParseError(std::string msg, bool showLine = true) {
	std::cout << "ERROR PARSING INPUT:" << std::endl << "  ";
	if (currRuleName.size() > 0)
		std::cout << "RULE: '" << currRuleName << "'" << std::endl << "  ";
	if (showLine)
		std::cout << "LINE " << lineNumber - 1 << ": ";
	std::cout << msg << std::endl;
}

// Replace any "-s.attr" conditions with "s.attr -"
// Returns success, or false if a bad negation command was found
bool props_instruction_parser::substituteConditionNegations(std::vector<arg_id_chain> &conditions) {
	for (auto &cond : conditions) {
		auto pCond = &(cond.first);
		if (pCond->at(0).substr(0, 1).compare("-") != 0)
			continue;
		// It starts with a "-".
		// Check for bad syntax (extra args)
		if (pCond->size() > 1) {
			printParseError("Negation '" + pCond->at(0) + "' may not have more than one argument.", false);
			return false;
		}

		std::string arg1 = pCond->at(0).substr(1, std::string::npos),
			op = "-";
		pCond->at(0) = arg1;
		pCond->push_back(op);
	}
	return true;
}

// Takes an arg_id_chain with raw tokens in the arg_chain, and replaces it with a formatted equivalent arg_id_chain
// Reformat from "ID1.attr1 = ID2.attr2" to "add ID1 attr1 ID2 attr2"
// Returns success. Fails if bad action symbol found.
bool props_instruction_parser::formatArgIdChain(std::vector<arg_id_chain> &chain, std::map<std::string, std::string> &ops) {
	auto newCs = std::vector<arg_id_chain>();
	for (auto &s : chain) {
		auto c = s.first;
		auto argEntry = arg_chain();
		auto idEntry = id_chain();

		// Extract the operator
		// first check for the existence-operator, which is the lack of an operator symbol (or "?")
		if (c.size() == 1 && ops.find("?") != ops.end()) {
			argEntry.push_back("existence");
		}
		else {
			try {
				argEntry.push_back(ops.at(c.at(1)));
			}
			catch (...) {
				printParseError("Invalid symbol: '" + c.at(1) + "'", false);
				return false;
			}
		}

		// Separate each ID.attr.attr... chain
		for (std::size_t i = 0; i < c.size(); ++i) {
			if (i == 1)		// skip the condition operator
				continue;
			// Check for bad ID:attr pair
			auto tokenized = tokenizeSlot(c.at(i));
			if (tokenized.size() < 2 && tokenized.at(0).substr(0, 1).compare("|") != 0) {
				printParseError("Invalid memory element '" + c.at(i) + "'. (Remember to separate your constants.)", false);
				return false;
			} 	// Allow constants at this point, since they indicate operator with the given name. Check that is a valid const name later.

			// Check for an operator condition
			if (std::find(tokenized.begin(), tokenized.end(), "operator") != tokenized.end()) {
				// We'll want to add a flag the instruction set
				currRuleHasOpRef = true;
			}

				// Make an id chain entry for this ID.attr.attr chain
			auto thisIdChain = arg_chain();
			// Put the ID on the arg chain if there is only one attribute, else put it in the id retrieval chain
			if (tokenized.size() <= 2) {
				argEntry.push_back(tokenized.at(0));
				thisIdChain.push_back("");
			}
			else {
				argEntry.push_back("");
				thisIdChain.push_back(tokenized.at(0));
			}
			// Put the last attribute on the arg chain, and push the remaining args into the id retrieval chain
			argEntry.push_back(tokenized.at(tokenized.size() - 1));
			for (std::size_t j = 1; j < tokenized.size() - 1; ++j) {
				thisIdChain.push_back(tokenized.at(j));
			}
			idEntry.push_back(thisIdChain);
		}

		newCs.push_back(arg_id_chain(argEntry, idEntry));
	}

	// 'return' the reformatted conditions
	chain = newCs;
	return true;
}

// Replace given constants with internal references: const1 -> C1.1
// Returns success, or false if modification of a constant was attempted in an action
bool props_instruction_parser::substituteConstants(std::vector<arg_chain> &constants, std::vector<arg_id_chain> &conditions, std::vector<arg_id_chain> &actions) {
	// Make a dictionary of given names to the index of the constant (and replace the constant names with the indices)
	auto dict = std::map<std::string, std::string>();
	for (int i = 0; i < (int)constants.size(); ++i) {
		std::string constName = constants.at(i).at(0);
		if (constName.compare(ROOT_MARKER) == 0 || constName.compare("s") == 0) {
			printParseError("Constant '" + constName + "' is a reserved keyword.", false);
			return false;
		}
		dict[constName] = toString(i + 1);
		constants.at(i).at(0) = toString(i + 1);
	}
	// Replace condition references
	for (auto &s : conditions) {
		auto c = &(s.first);
		for (std::size_t i = 0; i < c->size(); ++i) {
			if (dict.find(c->at(i)) != dict.end()) {
				c->at(i) = CONST_ID + "." + dict.at(c->at(i));
			}
		}
	}
	// Replace action references
	for (auto &s : actions) {
		auto a = &(s.first);
		/*if (dict.find(a->at(0)) != dict.end()) {
		printParseError("Attempt to modifiy constant in actions.", false);
		return false;
		}*/	// Allow modification of constants, since indicates operator whose name is the constant

		// Substitute on dot-separated tokens, since a constant may be referenced as a slot in the case of operator names
		for (std::size_t i = 0; i < a->size(); ++i) {
			auto tokens = tokenizeSlot(a->at(i));
			for (std::size_t j = 0; j < tokens.size(); ++j) {
				if (dict.find(tokens.at(j)) != dict.end()) {
					tokens.at(j) = CONST_ID + "." + dict.at(tokens.at(j));
				}
			}
			std::string repaired = untokenize(tokens);
			a->at(i) = repaired;
		}
	}

	return true;
}

// Reformat conditions from "ID1.attr1 = ID2.attr2" to "equality ID1 attr1 ID2 attr2"
// Returns success. Fails if bad condition symbol found.
bool props_instruction_parser::formatConditions(std::vector<arg_id_chain> &conditions) {
	std::map<std::string, std::string> ops = {
		{ "=", "equality" },
		{ "==", "equality" },
		{ "<>", "inequality" },
		{ "!=", "negation" },
		{ "<=>", "type-equality" },
		{ "<", "less-than" },
		{ ">", "greater-than" },
		{ "<=", "less-equal" },
		{ ">=", "greater-equal" },
		{ "-", "negation" },
		{ "--", "negation" },
		{ "?", "existence" }
	};

	currRuleHasOpRef = false;
	return formatArgIdChain(conditions, ops);
}

// Reformat actions from "ID1.attr1 = ID2.attr2" to "add ID1 attr1 ID2 attr2"
// Returns success. Fails if bad action symbol found.
bool props_instruction_parser::formatActions(std::vector<arg_id_chain> &actions) {
	std::map<std::string, std::string> ops = {
		{ "=", "add" },
		{ ":=", "add" },
		{ "-", "remove" },
		{ "+", "acceptable" },	// "-" for reject doubles as remove; "=" indifferent doubles as add
		//{ "==", "indifferent" },
		{ ">", "better" },
		{ "<", "worse" },
		{ "!", "require" }
	};

	return formatArgIdChain(actions, ops);
}

void props_instruction_parser::printSubChain(arg_id_chain &s, std::stringstream &instrs, std::string propID) {
	auto aic = s.second;
	for (std::size_t i = 0; i < aic.size(); ++i) {
		auto ai = aic.at(i);
		// Don't bother with subs if there is no attribute chain
		if (ai.at(0).compare("") == 0)
			continue;
		int chainRoot = chainNumber++;
		instrs << "(<" << propID << "> ^sub" << i + 1 << " |_U" << chainRoot << "|)" << std::endl;
		instrs << "(<" << propID << "> ^sub" << i + 1 << "-link <U" << chainRoot << ">)" << std::endl;
		instrs << "(<U" << chainRoot << "> ^lti-name |_U" << chainRoot << "|)" << std::endl;
		instrs << "(<U" << chainRoot << "> ^curr-id " << ai.at(0) << ")" << std::endl;
		instrs << "(<U" << chainRoot << "> ^target-arg id" << i + 1 << ")" << std::endl;
		//instrs << "(<U" << chainRoot << "> ^target-attr attr" << i + 1 << ")" << std::endl;	// TODO: this is only needed for operator name references
		instrs << "(<U" << chainRoot << "> ^depth " << ai.size() - 1 << ")" << std::endl;
		instrs << "(<U" << chainRoot << "> ^chain-attr " << ai.at(1) << ")" << std::endl;
		for (std::size_t j = 2; j < ai.size(); ++j) {
			instrs << "(<U" << chainNumber - 1 << "> ^chain-next <U" << chainNumber << ">)" << std::endl;
			instrs << "(<U" << chainNumber << "> ^chain-attr " << ai.at(j) << ")" << std::endl;
			chainNumber++;
		}
		instrs << "(<U" << chainNumber - 1 << "> ^chain-next |done|)" << std::endl;
	}
}

/**
 * If the given actions propose an operator, return true, and set the given string to the operator name.
 */
bool props_instruction_parser::getProposedOperator(const std::vector<arg_chain> &consts, const std::vector<arg_id_chain> &actions, std::string &op_name) {
	// Check each action for an operator proposal
	for (auto &aic : actions) {
		arg_chain ac = aic.first;	// ac:{"acceptable", "", <const-ind>}
		id_chain ic = aic.second;	// ic:{arg1:{ROOT_MARKER,CONST_ID}}
		if (!ic.at(0).at(0).compare(ROOT_MARKER) && !ic.at(0).at(1).compare(CONST_ID)
				&& !ac.at(0).compare("acceptable")) {
			op_name = consts.at(stoi(ac.at(2))-1).at(1);	// consts:{entry:{index, value}}
			return true;
		}
	}
	return false;
}

/**
 * If the given conditions test an operator, return true, and set the given string to the operator name.
 */
bool props_instruction_parser::getAppliedOperator(const std::vector<arg_chain> &consts, const std::vector<arg_id_chain> &conditions, std::string &op_name) {
	// Check each condition for an operator test
	for (auto &aic : conditions) {
		arg_chain ac = aic.first;	// ac:{"equality", "","name", CONST_ID,<attr2>}
		id_chain ic = aic.second;	// ic:{arg1:{ROOT_MARKER,operator}, arg2:{""}}
		if (!ic.at(0).at(0).compare(ROOT_MARKER) && !ic.at(0).at(1).compare("operator")
				&& !ac.at(0).compare("equality") && !ac.at(3).compare(CONST_ID)) {
			op_name = consts.at(stoi(ac.at(4))-1).at(1);	// consts:{entry:{index, value}}
			return true;
		}
	}
	return false;
}

bool props_instruction_parser::parsePropsFile(std::vector<arg_chain> &constants, std::vector<arg_id_chain> &conditions, std::vector<arg_id_chain> &actions) {

	std::stringstream ss;
	std::string sToken = "";
	char cToken;
	sToken = nextLine(ss);

	// Check for task name directive
	if (!sToken.compare(0,12, "# add-instr ")) {
		currTaskName = trim(sToken.substr(12));
		nextLine(ss);
	}
	else if (!sToken.compare(0,5, "# ;~ ")) {
		//currEpsetName = currTaskName;
		nextLine(ss);
	}
	else if (!sToken.compare(0,6, "# ;~~ ")) {
		nextLine(ss);
	}

	// Begin prop rule
	ss >> sToken;
	if (sToken.compare("pp") != 0) {
		printParseError("Begin instructions with 'pp'", true);
		return false;
	}
	ss >> std::ws >> cToken;
	if (cToken != '{') {
		printParseError("Open instructions with '{'", true);
		return false;
	}
	// Get instruction name
	currRuleName = "";
	ss >> std::ws >> currRuleName;

	// Get constants
	nextLine(ss);
	ss >> sToken;
	while (sToken.compare("--") != 0) {
		if (!sToken.compare("-->")) {
			printParseError("Expected '--'", true);
			return false;
		}

		// Read pair (eg: "const1 val1")
		std::string c1 = sToken,
			c2;
		ss >> std::ws >> c2;
		constants.push_back({ c1, c2 });

		nextLine(ss);
		ss >> sToken;
	}

	// Get conditions
	nextLine(ss);
	ss >> sToken;
	while (sToken.compare("-->") != 0) {
		if (!sToken.compare("}")) {
			printParseError("Expected '-->'", true);
			return false;
		}

		// Read unknown number of args
		auto args = arg_chain();
		args.push_back(sToken);
		while (ss.good()) {
			ss >> sToken;
			args.push_back(sToken);
		}
		auto res = std::pair<arg_chain, id_chain>();
		res.first = args;
		res.second = id_chain();
		conditions.push_back(res);

		nextLine(ss);
		ss >> sToken;
	}

	// Get actions
	nextLine(ss);
	ss >> sToken;
	while (sToken.compare("}") != 0) {
		// Read unknown number of args
		auto args = arg_chain();
		args.push_back(sToken);
		while (ss.good()) {
			ss >> sToken;
			args.push_back(sToken);
		}
		auto res = std::pair<arg_chain, id_chain>();
		res.first = args;
		res.second = id_chain();
		actions.push_back(res);

		nextLine(ss);
		ss >> sToken;
	}

	// Done reading file, now finish parsing what was just read

	// Replace "-wme" with "wme -" to represent negated conditions
	if (!substituteConditionNegations(conditions)) {
		return false;
	}

	// Replace given constants with internal references
	if (!substituteConstants(constants, conditions, actions)) {
		return false;
	}

	// Reformat conditions from "ID1.attr1 = ID2.attr2" to "equality ID1 attr1 ID2 attr2"
	if (!formatConditions(conditions)) {
		return false;
	}

	// TODO: Add ability to use instruction name as action; substitute with instruction ID

	// Reformat actions from "ID1.attr1 = ID2.attr2" to "add ID1 attr1 ID2 attr2"
	if (!formatActions(actions)) {
		return false;
	}

	return true;
}

std::string props_instruction_parser::makeDelta(const arg_id_chain &cond, const std::string deltaID) {
	std::stringstream ss;
	auto c = cond.first;

	// Add prop-type flag
	if (!c.at(0).compare("add") && c.size() < 4) {
		// "add" && -^attr2 --> "indifferent"
		ss << "(<" << deltaID << "> ^prop-type indifferent)" << std::endl;
	}
	else if (!c.at(0).compare("better") && c.size() < 4) {
		// "better" && -^attr2 --> "best"
		ss << "(<" << deltaID << "> ^prop-type best)" << std::endl;
	}
	else if (!c.at(0).compare("worse") && c.size() < 4) {
		// "worse" && -^attr2 --> "worst"
		ss << "(<" << deltaID << "> ^prop-type worst)" << std::endl;
	}
	else {
		// Default
		ss << "(<" << deltaID << "> ^prop-type " << c.at(0) << ")" << std::endl;
	}

	// Add attributes
	for (std::size_t i = 1; i < c.size(); i += 2) {
		ss << "(<" << deltaID << "> ^attr" << (i + 1) / 2 << " " << c.at(i + 1) << ")" << std::endl;
	}

	// Add the name of id1
	std::string tempStr;
	try {
		tempStr = cond.second.at(0).at(1); 	// Use ^sub1-link.chain-attr if it exists
	} catch (...) {
		tempStr = ROOT_MARKER;
	}
	ss << "(<" << deltaID << "> ^address1 " << tempStr << ")" << std::endl;

	// Add the name of id2, if applicable
	if (c.size() > 3) {
		try {
			if (!c.at(3).compare(CONST_ID))
				tempStr = CONST_ID;				// Use const if ^id2 is CONST_ID
			else
				tempStr = cond.second.at(1).at(1); // Use ^sub2-link.chain-attr if it exists
		} catch (...) {
			tempStr = ROOT_MARKER;
		}
		ss << "(<" << deltaID << "> ^address2 " << tempStr << ")" << std::endl;
	}

	return ss.str();
}

/**
 * Build smem instructions for PROP2 agents.
 * Instructions are for both proposal and application rules, of a particular format assumed by the PROP2 agent.
 */
std::vector<std::string> props_instruction_parser::buildProps2Instructions() {
	auto insList = std::vector<std::string>();		// The results for each input props instructions
	lineNumber = 1;
	std::string prevTaskName = "default";
	std::map<std::string, std::pair<std::string,std::string>> proposalMap;	// Maps operator names to their <proposal,application> rule instance numbers

	while (inFile.good()) {
		auto constants = std::vector<arg_chain>();
		auto conditions = std::vector<arg_id_chain>();
		auto actions = std::vector<arg_id_chain>();

		// Load the consts, conditions, and actions for the next rule in the file
		if (!parsePropsFile(constants, conditions, actions)) {
			insList.clear();
			return insList;
		}

		//// Convert these constants, conditions, and actions into SMEM instructions
		std::stringstream instrs;
		std::string str;
		// Get whether this rule applies an operator
		bool isProposal = false;
		if (getAppliedOperator(constants, conditions, str)) {
			// Save a reference to the epset delta for this proposal so it can link to the apply rule later
			if (proposalMap.find(str) == proposalMap.end()) {
				proposalMap.insert(std::make_pair(str, std::make_pair("",toString(instNumber))));
			}
			else {
				proposalMap.at(str).second = toString(instNumber);
			}
		}

		// Get whether this rule proposes an operator
		if (getProposedOperator(constants, actions, str)) {
			// Save a reference to the epset delta for this application so it can be linked from the proposal rule later
			if (proposalMap.find(str) == proposalMap.end()) {
				proposalMap.insert(std::make_pair(str, std::make_pair(toString(instNumber),"")));
			}
			else {
				proposalMap.at(str).first = toString(instNumber);
			}

			isProposal = true;
		}

		// Build the props-static instruction info and spreading network
		//instrs << "(<static> ^instructions <S" << instNumber << ">)" << std::endl;
		instrs << "(<S" << instNumber << "> ^name " << currRuleName << ")" << std::endl;

		// Add given name
		instrs << "(<Z" << instNumber << "> ^name " << currRuleName << ")" << std::endl;
		// Add ID name
		instrs << "(<Z" << instNumber << "> ^lti-name |_Z" << instNumber << "|)" << std::endl;
		// Add type (optional?)
		instrs << "(<Z" << instNumber << "> ^prop-type |instruction|)" << std::endl;
		// Add constants
		instrs << "(<Z" << instNumber << "> ^const <Q" << constNumber << ">)" << std::endl;
		// Add prop count meta-data
		instrs << "(<Z" << instNumber << "> ^cond-count " << conditions.size() << ")" << std::endl;
		instrs << "(<Z" << instNumber << "> ^act-count " << actions.size() << ")" << std::endl;
		// Add semantic knowledge of spread source
		instrs << "(<Z" << instNumber << "> ^spread-source <S" << instNumber << ">)" << std::endl;
		// Add any extra flags
		/*if (currRuleHasOpRef) {
			instrs << "(<Z" << instNumber << "> ^flag |o-supported|)" << std::endl;
		}*/
		for (auto &s : constants) {
			instrs << "(<Q" << constNumber << "> ^" << s.at(0) << " " << s.at(1) << ")" << std::endl;
		}

		// Add epset delta for this instruction if an operator proposal
		if (isProposal) {
			instrs << "(<epset-" << currTaskName << "> ^delta <d" << instNumber << ">)" << std::endl
					<< "(<d" << instNumber << "> ^name " << currRuleName << ")" << std::endl
					<< "(<d" << instNumber << "> ^const <Q" << constNumber << ">)" << std::endl;
		}

		// Add conditions
		for (auto &s : conditions) {
			int propNumber = maxPropNumber;
			bool skip = false;

			// Check for duplicate condition (same PROP)
			auto hash = ArgIdChainHash{}(s);
			if (propIds.find(hash) != propIds.end()) {
				propNumber = propIds.at(hash);
				skip = true;
			}
			else {
				propIds.emplace(hash, propNumber);
				maxPropNumber++;
			}

			// Add epset condition
			instrs << "(<d" << instNumber << "> ^condition <dc" << propNumber << ">)" << std::endl
					<< "(<dc" << propNumber << "> ^name |_P" << propNumber << "|)" << std::endl;

			// Add condition to instructions
			auto c = s.first;
			std::string ZPid = "Z" + toString(instNumber) + "P" + toString(propNumber);

			instrs << "(<Z" << instNumber << "> ^condition |_P" << propNumber << "|)" << std::endl;
			instrs << "(<Z" << instNumber << "> ^prop-link <P" << propNumber << ">)" << std::endl;
			instrs << "(<Z" << instNumber << "> ^cond-spread-source <Cue" << ZPid << ">)" << std::endl;

			instrs << "(<Cue" << ZPid << "> ^props-instruction-set <S" << instNumber << ">)" << std::endl;
			instrs << "(<Cue" << ZPid << "> ^name |_P" << propNumber << "|)" << std::endl;

			instrs << "(<S" << instNumber << "> ^condition <Scond" << ZPid << ">)" << std::endl
					<< "(<Scond" << ZPid << "> ^props-instructions <Z" << instNumber << ">)" << std::endl
					<< "(<Scond" << ZPid << "> ^name |_P" << propNumber << "|)" << std::endl;

			// Don't print PROP details if defined earlier
			if (skip)
				continue;

			// Add epset condition details
			if (isProposal) {
				instrs << "(<dc" << propNumber << "> ^prop-type " << c.at(0) << ")" << std::endl;
				for (std::size_t i = 1; i < c.size(); i += 2) {
					instrs << "(<dc" << propNumber << "> ^attr" << (i + 1) / 2 << " " << c.at(i + 1) << ")" << std::endl;
				}
				std::string tempStr;
				try {
					tempStr = s.second.at(0).at(1); 	// Use ^sub1-link.chain-attr if it exists
				} catch (...) {
					tempStr = ROOT_MARKER;
				}
				instrs << "(<dc" << propNumber << "> ^address1 " << tempStr << ")" << std::endl;
				if (c.size() > 3) {
					try {
						if (!c.at(3).compare(CONST_ID))
							tempStr = CONST_ID;				// Use const if ^id2 is CONST_ID
						else
							tempStr = s.second.at(1).at(1); // Use ^sub2-link.chain-attr if it exists
					} catch (...) {
						tempStr = ROOT_MARKER;
					}
					instrs << "(<dc" << propNumber << "> ^address2 " << tempStr << ")" << std::endl;
				}
			}

			instrs << "(<P" << propNumber << "> ^lti-name |_P" << propNumber << "|)" << std::endl;
			//instrs << "(<P" << propNumber << "> ^spread-source <Cue" << propNumber << ">)" << std::endl;
			instrs << "(<P" << propNumber << "> ^prop-type " << c.at(0) << ")" << std::endl;
			for (std::size_t i = 1; i < c.size(); i += 2) {
				// Replace C1 const references with the local constant ID
				// TODO: (this might be removed later) (it's also below under Add Actions)
				/*if (c.at(i).compare(CONST_ID) == 0) {
				c.at(i) = "<Q" + toString(constNumber) + ">";
				}*/

				// Add the id/attr prop args
				if (c.at(i).compare("") != 0)
					instrs << "(<P" << propNumber << "> ^id" << (i + 1) / 2 << " " << c.at(i) << ")" << std::endl;
				instrs << "(<P" << propNumber << "> ^attr" << (i + 1) / 2 << " " << c.at(i + 1) << ")" << std::endl;
			}

			// Add attribute chain
			printSubChain(s, instrs, "P" + propNumber);

		}

		/*
		// Add RHS
		instNumber++;
		//instrs << "}" << std::endl << "smem --add {" << std::endl;
		// Add given name
		instrs << "(<Z" << instNumber << "> ^name " << currRuleName << ")" << std::endl;
		// Add ID name
		instrs << "(<Z" << instNumber << "> ^lti-name |_Z" << instNumber << "|)" << std::endl;
		// Add type (optional?)
		instrs << "(<Z" << instNumber << "> ^prop-type |instruction-rhs|)" << std::endl;
		// Add constants
		instrs << "(<Z" << instNumber << "> ^const <Q" << constNumber << ">)" << std::endl;
		// Add prop count meta-data
		instrs << "(<Z" << instNumber << "> ^prop-count " << actions.size() << ")" << std::endl;
		*/
		// Add any extra flags
		if (currRuleHasOpRef) {
			instrs << "(<Z" << instNumber << "> ^flag |o-supported|)" << std::endl;
		}

		// Add actions
		for (auto &s : actions) {
			int propNumber = maxPropNumber;
			bool skip = false;

			// Check for duplicate action (same PROP)
			auto hash = ArgIdChainHash{}(s);
			if (propIds.find(hash) != propIds.end()) {
				propNumber = propIds.at(hash);
				skip = true;
			}
			else {
				propIds.emplace(hash, propNumber);
				maxPropNumber++;
			}

			auto a = s.first;
			instrs << "(<Z" << instNumber << "> ^action |_P" << propNumber << "|)" << std::endl;
			instrs << "(<Z" << instNumber << "> ^prop-link <P" << propNumber << ">)" << std::endl;

			// Don't print PROP details if defined earlier
			if (skip)
				continue;

			instrs << "(<P" << propNumber << "> ^lti-name |_P" << propNumber << "|)" << std::endl;
			instrs << "(<P" << propNumber << "> ^prop-type " << a.at(0) << ")" << std::endl;
			for (std::size_t i = 1; i < a.size(); i += 2) {
				// Replace C1 const references with the local constant ID
				// TODO: (this might be removed later) (it's also above under Add Conditions)
				/*if (a.at(i).compare(CONST_ID) == 0) {
				a.at(i) = "<Q" + toString(constNumber) + ">";
				}*/

				if (a.at(i).compare("") != 0)
					instrs << "(<P" << propNumber << "> ^id" << (i + 1) / 2 << " " << a.at(i) << ")" << std::endl;
				instrs << "(<P" << propNumber << "> ^attr" << (i + 1) / 2 << " " << a.at(i + 1) << ")" << std::endl;
			}

			// Add action attribute chain
			printSubChain(s, instrs, "P" + propNumber);

			propNumber++;
		}
		//instrs << "}" << std::endl;

		// Store converted instruction set for later printing
		insList.push_back(instrs.str());
		instNumber++;
		constNumber++;
		inFile >> std::ws;


		// Add task-level epset head structure
		if (currTaskName.compare(prevTaskName)) {
			insList.push_back("(<epset-" + currTaskName + "> ^props-epset-name " + currTaskName + ")");
			prevTaskName = currTaskName;
		}
	}
	// Connect the epset delta for the operator proposal to the application instructions
	std::stringstream instrs;
	for (auto mappings : proposalMap) {
		std::string proposalNumber = mappings.second.first;
		if (proposalNumber.compare("") && mappings.second.second.compare("")) {
			instrs << "(<d" << proposalNumber << "> ^instructions <dz" << proposalNumber << ">)" << std::endl
					<< "(<dz" << proposalNumber << "> ^op-name |" << mappings.first << "|)" << std::endl
					<< "(<dz" << proposalNumber << "> ^spread-id <Z" << mappings.second.second << ">)" << std::endl;
		}
		else {
			std::cerr << "WARNING: Operator found with only proposal or application, not both: " << mappings.first << "  Epset link not formed." << std::endl;
		}
	}
	proposalMap.clear();
	insList.push_back(instrs.str());

	return insList;
}

/**
 * Build smem instructions for PROP3 agents.
 * Instructions are structured in epsets, both for task decomposition and PROPs within instructions.
 * Proposals are encoded into epset children, applications are children of that proposal.
 */
std::vector<std::string> props_instruction_parser::buildProps3Instructions() {
	auto insList = std::vector<std::string>();		// The results for each input props instructions
	lineNumber = 1;
	std::string prevTaskName = "default";
	std::map<std::string, std::pair<std::string,std::string>> proposalMap;	// Maps operator names to their <proposal,application> rule instance numbers

	while (inFile.good()) {
		auto constants = std::vector<arg_chain>();
		auto conditions = std::vector<arg_id_chain>();
		auto actions = std::vector<arg_id_chain>();

		// Load the consts, conditions, and actions for the next rule in the file
		if (!parsePropsFile(constants, conditions, actions)) {
			insList.clear();
			return insList;
		}

		//// Convert these constants, conditions, and actions into SMEM instructions
		std::stringstream instrs;
		std::string currOpName;
		// Get whether this rule applies an operator
		bool isProposal = false;
		if (getAppliedOperator(constants, conditions, currOpName)) {
			// Save a reference to the epset delta for this proposal so it can link to the apply rule later
			if (proposalMap.find(currOpName) == proposalMap.end()) {
				proposalMap.insert(std::make_pair(currOpName, std::make_pair("",toString(instNumber))));
			}
			else {
				proposalMap.at(currOpName).second = toString(instNumber);
			}
		}

		// Get whether this rule proposes an operator
		if (getProposedOperator(constants, actions, currOpName)) {
			// Save a reference to the epset delta for this application so it can be linked from the proposal rule later
			if (proposalMap.find(currOpName) == proposalMap.end()) {
				proposalMap.insert(std::make_pair(currOpName, std::make_pair(toString(instNumber),"")));
			}
			else {
				proposalMap.at(currOpName).first = toString(instNumber);
			}

			isProposal = true;
		}

		// Add given name
		/*instrs << "(<Z" << instNumber << "> ^name " << currRuleName << ")" << std::endl;
		// Add ID name
		instrs << "(<Z" << instNumber << "> ^lti-name |_Z" << instNumber << "|)" << std::endl;
		// Add type (optional?)
		instrs << "(<Z" << instNumber << "> ^prop-type |instruction|)" << std::endl;
		// Add constants
		instrs << "(<Z" << instNumber << "> ^const <Q" << constNumber << ">)" << std::endl;
		// Add prop count meta-data
		instrs << "(<Z" << instNumber << "> ^cond-count " << conditions.size() << ")" << std::endl;
		instrs << "(<Z" << instNumber << "> ^act-count " << actions.size() << ")" << std::endl;
		// Add semantic knowledge of spread source
		instrs << "(<Z" << instNumber << "> ^spread-source <S" << instNumber << ">)" << std::endl;
		*/
		// Add const values
		for (auto &s : constants) {
			instrs << "(<Q" << constNumber << "> ^" << s.at(0) << " " << s.at(1) << ")" << std::endl;
		}

		// Add conditions
		if (isProposal) {
			// Add this operator as a delta in the task epset
			instrs << "(<epset-" << currTaskName << "> ^delta <d-op" << instNumber << ">)" << std::endl
					<< "(<d-op" << instNumber << "> ^name " << currRuleName << ")" << std::endl
					<< "(<d-op" << instNumber << "> ^const <Q" << constNumber << ">)" << std::endl;

			// Add each condition to the operator delta
			for (auto &s : conditions) {
				int propNumber = maxPropNumber;
				bool skip = false;

				// Check for duplicate condition (same PROP)
				auto hash = ArgIdChainHash{}(s);
				if (propIds.find(hash) != propIds.end()) {
					propNumber = propIds.at(hash);
					skip = true;
				}
				else {
					propIds.emplace(hash, propNumber);
					maxPropNumber++;
				}

				// Add epset condition
				instrs << "(<d-op" << instNumber << "> ^condition <dc" << propNumber << ">)" << std::endl;
						//<< "(<d-op" << instNumber << "> ^item-name |_PC" << propNumber << "|)" << std::endl;

				// Add condition to instructions

				// Don't print PROP details if defined earlier
				if (skip)
					continue;

				// Add epset condition details
				instrs << makeDelta(s, "dc" + toString(propNumber)) << std::endl
						<< "(<dc" << propNumber << "> ^name |_PC" << propNumber << "|)" << std::endl;

				/*auto c = s.first;
				instrs << "(<P" << propNumber << "> ^lti-name |_P" << propNumber << "|)" << std::endl;
				instrs << "(<P" << propNumber << "> ^prop-type " << c.at(0) << ")" << std::endl;
				for (std::size_t i = 1; i < c.size(); i += 2) {
					// Add the id/attr prop args
					if (c.at(i).compare("") != 0)
						instrs << "(<P" << propNumber << "> ^id" << (i + 1) / 2 << " " << c.at(i) << ")" << std::endl;
					instrs << "(<P" << propNumber << "> ^attr" << (i + 1) / 2 << " " << c.at(i + 1) << ")" << std::endl;
				}*/

				// Add attribute chain
				//printSubChain(s, instrs, "PC" + toString(propNumber));

			}
		}

		// Add actions
		if (!isProposal) {
			instrs << "(<epset-" << instNumber << "> ^props-epset-name " << currOpName << ")" << std::endl
					//<< "(<epset-" << instNumber << "> ^terminal |true|)" << std::endl
					<< "(<epset-" << instNumber << "> ^const <Q" << constNumber << ">)" << std::endl;

			for (auto &s : actions) {
				int propNumber = maxPropNumber;
				bool skip = false;

				// Check for duplicate action (same PROP)
				auto hash = ArgIdChainHash{}(s);
				if (propIds.find(hash) != propIds.end()) {
					propNumber = propIds.at(hash);
					skip = true;
				}
				else {
					propIds.emplace(hash, propNumber);
					maxPropNumber++;
				}

				//instrs << "(<Z" << instNumber << "> ^action |_P" << propNumber << "|)" << std::endl;
				//instrs << "(<Z" << instNumber << "> ^prop-link <P" << propNumber << ">)" << std::endl;


				// Add this action as a delta in the op-apply epset
				instrs << "(<epset-" << instNumber << "> ^delta <a-prop" << propNumber << ">)" << std::endl;


				// Don't print PROP details if defined earlier
				if (skip)
					continue;

				// Add delta condition for this action prop in the rule
				/*instrs << "(<d-prop" << propNumber << "> ^condition <da" << propNumber << ">)" << std::endl
						<< "(<d-prop" << propNumber << "> ^op-name " << "PROP" << propNumber << "-" << s.first.at(0) << "-" << s.first.at(2) << ")" << std::endl
						<< "(<d-prop" << propNumber << "> ^prop-apply |_PA" << propNumber << "|)" << std::endl
						<< "(<da" << propNumber << "> ^name |_PA" << propNumber << "|)" << std::endl;*/
				instrs //<< "(<a-prop" << propNumber << "> ^application <cbz" << propNumber << ">)" << std::endl
						//<< "(<a-prop" << propNumber << "> ^name " << "PROP" << propNumber << "-" << s.first.at(0) << "-" << s.first.at(2) << ")" << std::endl
						<< "(<a-prop" << propNumber << "> ^prop-apply |_PA" << propNumber << "|)" << std::endl;
				//		<< "(<cbz" << propNumber << "> ^unretrieved <cbzz" << propNumber << ">)" << std::endl
				//		<< "(<cbzz" << propNumber << "> ^spread-link <cbset-" << propNumber << ">)" << std::endl;

				instrs << "(<a-prop" << propNumber << "> ^condition <cba" << propNumber << ">)" << std::endl
					   << "(<a-prop" << propNumber << "> ^item-name |_PA" << propNumber << "|)" << std::endl
					   << "(<a-prop" << propNumber << "> ^op-name |_PA" << propNumber << "|)" << std::endl;
						//<< "(<cbset-" << propNumber << "> ^size 1)" << std::endl
						//<< "(<a-prop" << propNumber << "> ^name |_PA" << propNumber << "|)" << std::endl;

				//instrs << "(<cbset-" << propNumber << "> ^props-cbset-name |_PA" << propNumber << "|)" << std::endl
				//	   << "(<cbset-" << propNumber << "> ^op-name |_PA" << propNumber << "|)" << std::endl;

				// TODO: Make <PC_> complement condition per action
				//arg_id_chain acond = makeActionCond(s);

				// Add PROP action's condition delta
				instrs << makeDelta(s, "cba" + toString(propNumber))
						<< "(<cba" << propNumber << "> ^name |_PA" << propNumber << "|)" << std::endl
						<< "(<cba" << propNumber << "> ^action-name " << "PROP" << propNumber << "-" << s.first.at(0) << "-" << s.first.at(2) << ")" << std::endl;

				// Add complementing condition attribute chain
				//printSubChain(s, instrs, "PA" + toString(propNumber));


				// Print action prop
				/*auto a = s.first;
				instrs << "(<PA" << propNumber << "> ^lti-name |_PA" << propNumber << "|)" << std::endl;
				instrs << "(<PA" << propNumber << "> ^prop-type " << a.at(0) << ")" << std::endl;
				for (std::size_t i = 1; i < a.size(); i += 2) {
					if (a.at(i).compare("") != 0)
						instrs << "(<PA" << propNumber << "> ^id" << (i + 1) / 2 << " " << a.at(i) << ")" << std::endl;
					instrs << "(<PA" << propNumber << "> ^attr" << (i + 1) / 2 << " " << a.at(i + 1) << ")" << std::endl;
				}

				// Add action attribute chain
				printSubChain(s, instrs, propNumber);*/

				propNumber++;
			}
		}

		// Store converted instruction set for later printing
		insList.push_back(instrs.str());
		instNumber++;
		constNumber++;
		inFile >> std::ws;


		// Add task-level epset head structure
		if (currTaskName.compare(prevTaskName)) {
			insList.push_back("(<epset-" + currTaskName + "> ^props-epset-name " + currTaskName + ")");
			prevTaskName = currTaskName;
		}
	}
	// Connect the epset delta for the operator proposal to the application instructions
	std::stringstream instrs;
	for (auto mappings : proposalMap) {
		std::string proposalNumber = mappings.second.first;
		if (proposalNumber.compare("") && mappings.second.second.compare("")) {
			instrs << "(<d-op" << proposalNumber << "> ^application <dz" << proposalNumber << ">)" << std::endl
					<< "(<d-op" << proposalNumber << "> ^op-name |" << mappings.first << "|)" << std::endl
					<< "(<dz" << proposalNumber << "> ^unretrieved <dzz" << proposalNumber << ">)" << std::endl
					<< "(<dzz" << proposalNumber << "> ^spread-link <epset-" << mappings.second.second << ">)" << std::endl;
		}
		else {
			std::cerr << "WARNING: Operator found with only proposal or application, not both: " << mappings.first << "  Epset link not formed." << std::endl;
		}
	}
	proposalMap.clear();
	insList.push_back(instrs.str());

	return insList;
}

// Translates the input file into SMEM instructions
// Returns 0 for success status
int props_instruction_parser::convert() {
	// Check if file is open
	if (!inFile.is_open()) {
		std::cout << "ERROR: Could not open input file." << std::endl;
		return -1;
	}

	// Get the SMEM-formatted instructions
	std::vector<std::string> insList = buildProps3Instructions();

	// Quit on error. Error message already given.
	if (insList.size() == 0) {
		return -1;
	}

	// Save the parsed contents
	std::ofstream outFile;
	try {
		// Print header
		outFile.open(outPath.c_str(), std::ios::trunc);
		outFile << "#####" << std::endl
			<< "# THIS FILE TRANSLATES THE PROPS INSTRUCTIONS" << std::endl
			<< "# FROM '" << inPath << "' INTO SMEM INSTRUCTIONS USABLE" << std::endl
			<< "# BY A PROPS SOAR AGENT." << std::endl
			<< "#####" << std::endl << std::endl;

		// Print reservation of const ID (for holding combo pairs)
		//outFile << "smem --add { (<" << BUFFER_ID << "> ^temp 0) }" << std::endl
		//        << "smem --remove { (<" << BUFFER_ID << "> ^temp 0) }" << std::endl << std::endl;

		// Print storage of props static data container
		/*outFile << "smem --add { " << std::endl
			<< "(<" << BUFFER_ID << "> ^props-buffer props-buffer)" << std::endl
			<< "(<X1> ^props-static props-static)" << std::endl
			<< "(<X1> ^prop-counts <X2>)" << std::endl
			<< "(<X1> ^prop-prohibits <X3>)" << std::endl
			<< "(<X2> ^threshold " << THRESHOLD_NUM << ")" << std::endl
			<< "}" << std::endl << std::endl;*/

		// Print instruction contents
		outFile << "# BEGIN INSTRUCTIONS" << std::endl << std::endl;
		outFile << "smem --add {" << std::endl;
		//for (std::string &s : insList) {
		for (int i=insList.size()-1; i>=0; --i) {	// Iterate backwards so first rules recall first in agent
			outFile << insList.at(i) << std::endl;
		}
		outFile << "}" << std::endl;
	}
	catch (...) {
		std::cout << "ERROR: Could not open the output file. Aborting." << std::endl;
		return -1;
	}

	outFile.close();

	return 0;
}
