#include "soar_to_props.h"

#include <string>
#include <iostream>
#include <vector>
#include <sstream>
#include <map>

#include "free_helper.h"


soar_to_props::soar_to_props(std::string in_path, std::string out_path) : inPath(in_path), outPath(out_path)
{
	inFile = std::ifstream();
	inFile.open(inPath.c_str());

	lineNumber = 1;
	currRuleName = "";
}
soar_to_props::~soar_to_props()
{
}
const std::string soar_to_props::CONST_NAME = "const";

std::map<std::string, std::string> soar_to_props::cond_ops = std::map<std::string, std::string>{
	//{ "equality", "=" },
	{ "equality", "==" },
	{ "inequality", "<>" },
	//{ "inequality", "!=" },
	{ "type-equality", "<=>" },
	{ "less-than", "<" },
	{ "greater-than", ">" },
	{ "less-equal", "<=" },
	{ "greater-equal", ">=" },
	{ "negation", "-" },
	//{ "negation", "--" },
	{ "existence", "?" }
};
std::map<std::string, std::string> soar_to_props::act_ops = std::map<std::string, std::string>{
	{ "add", "=" },
	//{ "add", ":=" },
	{ "remove", "-" },
	{ "acceptable", "+" },	// "-" for reject doubles as remove; "=" indifferent doubles as add
	{ "better", ">" },
	{ "worse", "<" },
	{ "require", "!" }
};


void soar_to_props::printParseError(std::string msg, bool showLine = true) {
	std::cout << "ERROR PARSING INPUT:" << std::endl << "  ";
	if (currRuleName.size() > 0)
		std::cout << "RULE: '" << currRuleName << "'" << std::endl << "  ";
	if (showLine)
		std::cout << "LINE " << lineNumber - 1 << ": ";
	std::cout << msg << std::endl;
}

// Trim a line, removing whitespace and trailing comments
std::string soar_to_props::trim(std::string s) {
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
std::string soar_to_props::nextLine(std::stringstream &ss) {
	std::string line = "";
	// Skip comments and whitespace
	while (!line.compare("") && inFile.good()) {
		std::getline(inFile, line);
		lineNumber++;
		line = trim(line);
	}

	// Add spaces between opening and closing parentheses/brackets
	if (line.size() > 1) {
		if (line.front() == '(')
			line.insert(1, " ");
		if (line.back() == ')' || line.back() == '}')
			line.insert(line.end() - 1, ' ');
	}
	ss.str(line);
	ss.clear();
	return line;
}

std::string soar_to_props::nextToken(std::stringstream &ss) {
	std::string token = "";
	if (!ss.good()) {
		nextLine(ss);
	}
	ss >> token;
	return token;
}

// Returns: Replace a leading variable with the attribute path found in the given dictionary, if any
// E.g., replaces <g>.foo.bar with s.arg1.foo.bar
std::string soar_to_props::replacementPath(const std::string &str, std::map<std::string, std::string>& varToPath, bool & did_change)
{
	int i = str.find_first_of('>');
	if (i == std::string::npos)
		return str;

	std::string var = str.substr(0, i + 1);
	if (varToPath.count(var)) {
		var = varToPath[var] + str.substr(i + 1);
		did_change = true;
	}
	return var;
}
// Returns: Replace the given constant with its corresponding const name, if any
std::string soar_to_props::replacementPath(const std::string & str, std::map<std::string, int>& constIds, bool & did_change)
{
	if (!constIds.count(str))
		return str;
	return CONST_NAME + toString(constIds[str]);
}

// Get the conditions and actions of the next rule in the file in raw format.
// Paths will still have "<var" elements, and constants' raw values.
// Returns success, and modifies the given stuctures.
bool soar_to_props::getRawProps(std::stringstream & ss, std::map<std::string, std::string>& varToPath, std::vector<std::string>& constants, std::vector<Primitive>& conditions, std::vector<Primitive>& actions)
{
	std::string sToken = "";
	char cToken;
	bool hasOpNameCond = false;
	std::string opStr = "";
	std::string path2 = "";

	sToken = nextToken(ss);

	// Skip config options (TODO: this only covers options at the top of the file)
	do {
		hasOpNameCond = false;
		if (!sToken.compare("source") || !sToken.compare("set") || !sToken.compare("smem")
			|| !sToken.compare("chunk") || !sToken.compare("learn")) {
			ss.str();
			ss.clear();
			nextLine(ss);
			sToken = nextToken(ss);
			hasOpNameCond = true;		// Reuse the bool as a loop-exit indicator, just because
		}
	} while (hasOpNameCond);

	hasOpNameCond = false;
	// Begin soar rule
	if (sToken.compare("sp") != 0) {
		printParseError("Unrecognized command '" + sToken + "'. Begin instructions with 'sp'", true);
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

	// Get conditions
	sToken = nextToken(ss);

	if (!sToken.compare("}")) {
		printParseError("Expected '-->'", true);
		return false;
	}
	if (!sToken.compare(":o-support")) {
		printParseError("The :o-support directive is not supported!", true);
		return false;
	}
	// Skip ignorable directives
	if (!sToken.compare(":i-support") || !sToken.compare(":chunk")) {
		sToken = nextToken(ss);
	}

	// Read each condition () block
	while (sToken.compare("-->") != 0) {
		// Read a group of augmentations under an ID
		// Begin with '('
		if (sToken.compare("(")) {
			printParseError("Expected '('", true);
			return false;
		}

		// Check ID
		sToken = nextToken(ss);
		if (!sToken.compare("state")) {
			// Mark the root
			ss >> sToken;	// get actual ID
			varToPath[sToken] = "s";
		}
		std::string id = sToken;

		// Read attr:val pairs until reaching a ')'
		sToken = nextToken(ss);
		while (sToken.compare(")")) {
			opStr = "existence";
			path2 = "";

			// Get attribute chain
			if (sToken.at(0) == '-') {
				// Attribute begins with "-^", is a negation condition
				opStr = "negation";
				sToken = sToken.substr(1);
			}
			if (sToken.at(0) != '^') {
				printParseError("Expected '^' after ID", true);
				return false;
			}
			std::string attr = sToken.substr(1);

			// If no value, then it's an existence op
			if (ss.good()) {
				// Get value
				sToken = nextToken(ss);
				if (sToken.front() == '<' && sToken.back() == '>') {
					// Check for unequal op
					if (!sToken.compare("<>")) {
						opStr = "inequality";
						// Get the actual value
						sToken = nextToken(ss);
						path2 = sToken;
						if (sToken.front() != '<' || sToken.back() != '>') {
							constants.push_back(path2);		// We'll need to remove duplicates later
						}
					}
					else {
						// Is a variable
						opStr = "equality";
						path2 = sToken;
						varToPath[sToken] = id + "." + attr;
					}
				}
				else if (sToken.compare(")")) {
					// Is a const
					if (!opStr.compare("negation"))
						opStr = "inequality";
					else
						opStr = "equality";
					path2 = trim_bars(sToken);
					constants.push_back(path2);		// We'll need to remove duplicates later
				}
			}

			// TODO vars not claimed in actions or other conditions are also existence op
			// If an apply rule for the Actransfer conversion, only positively test the operator name
			if (!hasOpNameCond || !opStr.compare("negation") || !opStr.compare("inequality")) {
				conditions.push_back(Primitive(cond_ops.at(opStr), id + "." + attr, path2));
				// FIXME: this is hacky and only works if specific string used
				if (!attr.compare("operator.name"))
					hasOpNameCond = true;
			}

			if (sToken.compare(")") != 0)
				sToken = nextToken(ss);
		}
		sToken = nextToken(ss);
	}	// end while (sToken != "-->")

	// Get actions
	sToken = nextToken(ss);
	while (sToken.compare("}") != 0) {
		// Read each action () block
		while (sToken.compare("}")) {
			// Read a group of augmentations under an ID
			// Begin with '('
			if (sToken.compare("(") != 0) {
				printParseError("Expected '('", true);
				return false;
			}

			// Check ID
			sToken = nextToken(ss);
			std::string id = sToken;

			if (!id.compare("write")) {
				// RHS function: skip this line
				ss.str();
				ss.clear();
				nextLine(ss);
				sToken = nextToken(ss);
				continue;
			}

			// Read attr:val pairs until reaching a ')'
			sToken = nextToken(ss);
			while (sToken.compare(")")) {
				opStr = "existence";
				std::string path1 = "";
				path2 = "";

				// Read attribute chain
				std::string attr = sToken.substr(1);
				path1 = id + "." + attr;
				// Get value
				sToken = nextToken(ss);
				path2 = trim_bars(sToken);
				if (sToken.front() != '<' || sToken.back() != '>')
					constants.push_back(path2);
				else if (!varToPath.count(sToken) && attr.compare("operator") != 0) {
					// A var without LHS definition - we must be adding it here
					varToPath[sToken] = path1;
					// FIXME: In this version, this means don't create the id, but assume its augmentations do that
					//		This really should make the id though. That PROP hasn't been implemented yet though.
					sToken = nextToken(ss);
					continue;
				}

				// If no '-' or other second command, is an add action
				if (!ss.good()) {
					opStr = "add";
				}
				else {
					// Get cmd
					sToken = nextToken(ss);
					if (!sToken.compare(")")) {
						// There was no cmd after all
						opStr = "add";
					}
					else if (!sToken.compare("-")) {
						opStr = "remove";
					}
					else if (!sToken.compare("+") && !attr.compare("operator")) {
						opStr = "acceptable";
						// FIXME: check for indifferent/best/require/etc
						// A proposal requires an operator name next:
						for (int i = 0; i < 4; ++i)
							nextToken(ss);		// Assume the next line is "(<o> ^name |name|)". Get the name.
						sToken = nextToken(ss);

						path2 = trim_bars(sToken);
						constants.push_back(path2);
						path1 = id;
					}
				}

				actions.push_back(Primitive(act_ops.at(opStr), path1, path2));

				if (sToken.compare(")") != 0)
					sToken = nextToken(ss);
			}	// end while in ( )
			sToken = nextToken(ss);
		}	// end while () sets remain

	}	// end while in {}

	return true;
}

bool soar_to_props::fillPaths(std::map<std::string, std::string>& varToPath, std::map<std::string, int>& constIds, std::vector<Primitive>& conditions, std::vector<Primitive>& actions)
{
	// Do conditions: iteratively replace opening <var>'s in paths, or constants with const name
	bool madeChange = true;
	while (madeChange) {
		madeChange = false;
		for (Primitive &p : conditions) {
			// Replace path leading variables
			p.path1 = replacementPath(p.path1, varToPath, madeChange);
			p.path2 = replacementPath(p.path2, varToPath, madeChange);
			// Replace any path2 constants  FIXME: if "s" was a constant, it would replace a single S1 id
			p.path2 = replacementPath(p.path2, constIds, madeChange);
		}
	}
	// Do actions: iteratively replace opening <var>'s in paths, or constants with const name
	madeChange = true;
	while (madeChange) {
		madeChange = false;
		for (Primitive &p : actions) {
			// Replace path leading variables
			p.path1 = replacementPath(p.path1, varToPath, madeChange);
			p.path2 = replacementPath(p.path2, varToPath, madeChange);
			// Replace any path2 constants  FIXME: if "s" was a constant, it would replace a single S1 id
			p.path2 = replacementPath(p.path2, constIds, madeChange);
		}
	}

	return true;
}

void soar_to_props::setReplaceProps(std::vector<Primitive>& actions)
{
	/*bool advance = true;
	auto p = actions.begin();
	for (; p != actions.end(); ) {
		advance = true;
		if (!p->op.compare(act_ops.at("remove"))) {
			// Look for any corresponding "add" actions, including those that add an id via a longer path
			int slen = p->path1.size();
			for (auto &p2 : actions) {
				if (!p2.op.compare(act_ops.at("add")) && !p2.path1.substr(0,slen).compare(p->path1)) {
					p = actions.erase(p);
					advance = false;
					break;
				}
			}
		}
		if (advance)
			++p;
	}*/
}

// Builds and returns the whole pp rule
void soar_to_props::buildPropCode(std::string &retval, const std::vector<std::string>& consts, const std::vector<Primitive>& conditions, const std::vector<Primitive>& actions)
{
	// Begin with the normal header
	retval = "pp {" + currRuleName + "\n";
	
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
		if (!p.op.compare(act_ops["add"])) {
			if (!p.path2.compare("")) {
				// Single argument like existence or negation
				retval += "\t" + p.path1 + " " + p.op + "\n";
			}
			else {
				retval += "\t" + p.path1 + " " + p.op + " " + p.path2 + "\n";
			}
		}
		else if (!p.op.compare(act_ops["remove"])) {
			retval += "\t" + p.path1 + " " + p.op + "\n";
		}
		else {
			// Is an operator preference
			retval += "\t" + p.path1 + "." + p.path2 + " " + p.op + "\n";
		}
	}

	// Add closer
	retval += "}\n";

}

// Convert the object's given soar production file into sets of instructions.
// Returns a list of strings, each corresponding to one production / instruction set
// Currently only supports basic eq/add/remove wme conditions/actions.
// Note: This function assumes the given file sources without error into a Soar agent, 
// and does not do that flavor of syntax error checking.
std::vector<std::string> soar_to_props::parseSoarFile()
{
	std::stringstream ss;
	std::vector<std::string> retList = std::vector<std::string>();		// The results for each input soar production
	lineNumber = 1;

	nextLine(ss);	// needed to init nextToken usage

	// For each production:
	while (inFile.good()) {
		auto varToPath = std::map<std::string, std::string>();	// Map from <varName> to s.foo.bar path, for the current sp
		auto constants = std::vector<std::string>();			// Ordered list of const values
		auto conditions = std::vector<Primitive>();
		auto actions = std::vector<Primitive>();

		// Get the conditions and actions
		if (!getRawProps(ss, varToPath, constants, conditions, actions)) {
			retList.clear();
			return retList;
		}

		// Skip whitespace so the inFile.good() test is correct
		inFile >> std::ws;

		// Find ordering of non-duplicate constants
		int i = 1;
		auto constIds = std::map<std::string, int>();
		auto constDecls = std::vector<std::string>();	// Pregenerate the output const declarations
		for (std::string &s : constants) {
			if (!constIds.count(s)) {
				constDecls.push_back(CONST_NAME + toString(i) + " " + s);
				constIds[s] = i++;
			}
		}

		// Replace variables with full path references
		fillPaths(varToPath, constIds, conditions, actions);

		// Remove any "remove" props which are actually part of a replace operation
		//setReplaceProps(actions);
		{
			bool advance = true;
			for (auto p = actions.begin(); p != actions.end(); ) {
				advance = true;
				if (!p->op.compare(act_ops.at("remove"))) {
					// Look for any corresponding "add" actions, including those that add an id via a longer path
					int slen = p->path1.size();
					for (auto &p2 : actions) {
						if (!p2.op.compare(act_ops.at("add")) && !p2.path1.substr(0,slen).compare(p->path1)) {
							p = actions.erase(p);
							advance = false;
							break;
						}
					}
				}
				if (advance)
					++p;
			}
		}

		// Construct PROPs code
		std::string propRule = "";
		buildPropCode(propRule, constDecls, conditions, actions);
		retList.push_back(propRule);

	}	// end while reading file

	return retList;
}

int soar_to_props::convertToPropsInstructions()
{
	// Check if file is open
	if (!inFile.is_open()) {
		std::cout << "ERROR: Could not open input file." << std::endl;
		return -1;
	}

	// Get the PROP-formatted instructions
	std::vector<std::string> ruleList = parseSoarFile();

	// Quit on error. Error message already given.
	if (ruleList.size() == 0) {
		return -1;
	}

	// Save the parsed contents
	std::ofstream outFile;
	try {
		// Print header
		outFile.open(outPath.c_str(), std::ios::trunc);
		outFile << "#####" << std::endl
			<< "# THIS FILE TRANSLATES THE SOAR PRODUCTIONS" << std::endl
			<< "# FROM '" << inPath << "' INTO INTERMEDIATE PROP INSTRUCTIONS." << std::endl
			<< "#####" << std::endl << std::endl;
		
		// Print instructions
		for (std::string &str : ruleList) {
			outFile << str;
			std::printf(str.c_str());
		}
	}
	catch (...) {
		std::cout << "ERROR: Could not open the output file. Aborting." << std::endl;
		return -1;
	}

	outFile.close();

	return 0;
}
