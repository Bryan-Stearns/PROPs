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
		line = trim(line);
	}
	ss.str(line);
	ss.clear();
	return line;
}

// Returns whether the given string is of an ID format (either A12 or @12, a letter followed by numbers)
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

void props_instruction_parser::printSubChain(arg_id_chain &s, std::stringstream &instrs, int propNumber) {
	auto aic = s.second;
	for (std::size_t i = 0; i < aic.size(); ++i) {
		auto ai = aic.at(i);
		// Don't bother with subs if there is no attribute chain
		if (ai.at(0).compare("") == 0)
			continue;
		int chainRoot = chainNumber++;
		instrs << "(<P" << propNumber << "> ^sub" << i + 1 << " |_U" << chainRoot << "|)" << std::endl;
		instrs << "(<P" << propNumber << "> ^sub" << i + 1 << "-link <U" << chainRoot << ">)" << std::endl;
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

std::vector<std::string> props_instruction_parser::parsePropsFile() {
	std::stringstream ss;
	std::vector<std::string> insList = std::vector<std::string>();		// The results for each input props instructions
	lineNumber = 1;

	while (inFile.good()) {
		auto constants = std::vector<arg_chain>();
		auto conditions = std::vector<arg_id_chain>();
		auto actions = std::vector<arg_id_chain>();

		std::string sToken = "";
		char cToken;
		nextLine(ss);

		// Begin prop rule
		ss >> sToken;
		if (sToken.compare("pp") != 0) {
			printParseError("Begin instructions with 'pp'", true);
			insList.clear();
			return insList;
		}
		ss >> std::ws >> cToken;
		if (cToken != '{') {
			printParseError("Open instructions with '{'", true);
			insList.clear();
			return insList;
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
				insList.clear();
				return insList;
			}

			// Read pair (eg: "const1 val1")
			std::string c1 = sToken,
				c2;
			ss >> std::ws;
			std::getline(ss, c2);	// Let the value be multiple tokens long; assume a line break after the value
			constants.push_back({ c1, "|" + c2 + "|" });

			nextLine(ss);
			ss >> sToken;
		}

		// Get conditions
		nextLine(ss);
		ss >> sToken;
		while (sToken.compare("-->") != 0) {
			if (!sToken.compare("}")) {
				printParseError("Expected '-->'", true);
				insList.clear();
				return insList;
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

		// Replace "-wme" with "wme -" to represent negated conditions
		if (!substituteConditionNegations(conditions)) {
			insList.clear();
			return insList;
		}

		// Replace given constants with internal references
		if (!substituteConstants(constants, conditions, actions)) {
			insList.clear();
			return insList;
		}

		// Reformat conditions from "ID1.attr1 = ID2.attr2" to "equality ID1 attr1 ID2 attr2"
		if (!formatConditions(conditions)) {
			insList.clear();
			return insList;
		}

		// TODO: Add ability to use instruction name as action; substitute with instruction ID

		// Reformat actions from "ID1.attr1 = ID2.attr2" to "add ID1 attr1 ID2 attr2"
		if (!formatActions(actions)) {
			insList.clear();
			return insList;
		}

		//// Convert these constants, conditions, and actions into SMEM instructions
		std::stringstream instrs;

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
			printSubChain(s, instrs, propNumber);
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
			printSubChain(s, instrs, propNumber);

			propNumber++;
		}
		//instrs << "}" << std::endl;

		// Store converted instruction set for later printing
		insList.push_back(instrs.str());
		instNumber++;
		constNumber++;
		inFile >> std::ws;
	}

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
	std::vector<std::string> insList = parsePropsFile();

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
