#ifndef PARSER_H
#define PARSER_H

#include <string>
#include <fstream>
#include <vector>
#include <map>
#include <list>

typedef std::vector<std::string> arg_chain;
typedef std::vector<std::vector<std::string> > id_chain;
typedef std::pair<arg_chain, id_chain> arg_id_chain;

// s.attr.attr1 = s.attr2.attr3.attr4 -> arg_chain({op, "", "attr1", "", "attr4"}) + id_chain({ {"props~rootstate", "attr"}, {"props~rootstate", "attr2", "attr3"} })
// O3.attr1.attr2 = C1.3 -> arg_chain({op, "", "attr2", "C1", "3"}) + id_chain({ {O3, "attr1"}, {} })
// s.attr1 = O3.attr2 -> arg_chain({op, "props~rootstate", "attr1", "O3", "attr2"}) + id_chain({ {}, {} })

class props_instruction_parser
{
public:
	props_instruction_parser(std::string inPath, std::string outPath);
	~props_instruction_parser();

	int convert();

	enum rule_type {
		NONE = 0,
		ELABORATION,
		PROPOSAL,
		APPLICATION
	};

private:

	std::vector<std::string> tokenizeSlot(std::string s);
	std::string untokenize(std::vector<std::string> tokens);
	std::string trim(std::string str);
	std::string nextLine(std::stringstream &ss);
	bool isID(const std::string &str);
	void printParseError(std::string msg, bool showLine);
	bool formatArgIdChain(std::vector<arg_id_chain> &chain, std::map<std::string, std::string> &ops);
	bool substituteConditionNegations(std::vector<arg_id_chain> &conditions);
	bool substituteConstants(std::vector<arg_chain> &constants, std::vector<arg_id_chain> &conditions, std::vector<arg_id_chain> &actions);
	bool formatConditions(std::vector<arg_id_chain> &conditions);
	bool formatActions(std::vector<arg_id_chain> &actions);
	void printSubChain(arg_id_chain &s, std::stringstream &instrs, std::string propID);
	bool getProposedOperator(const std::vector<arg_chain> &consts, const std::vector<arg_id_chain> &actions, std::string &op_name);
	bool getAppliedOperator(const std::vector<arg_chain> &consts, const std::vector<arg_id_chain> &conditions, std::string &op_name);
	bool parsePropsFile(std::vector<arg_chain> &constants, std::vector<arg_id_chain> &conditions, std::vector<arg_id_chain> &actions);
	std::string makeProp(const arg_id_chain &cond, const std::string propID, const std::string propName);
	std::string makeActionDeltaHierarchy(std::map<std::string, std::vector<std::string>> &ruleDeltas);
	std::vector<std::string> buildProps2Instructions();
	std::vector<std::string> buildProps3Instructions();

	std::string inPath,
		outPath;
	std::ifstream inFile;
	int lineNumber;
	int instNumber;
	int constNumber;
	int maxPropNumber;
	int chainNumber;
	std::string currRuleName;
	std::string currRule_h1;
	std::list<std::string> currTaskList;
	//std::string currEpsetName;
	bool currRuleHasOpRef;

	std::map<size_t, int> propIds;
	std::vector<std::string> epsetLocks;
	std::map<std::string, std::vector<std::string>> taskOperators;

	std::string CONST_ID;
	std::string BUFFER_ID;
	std::string ROOT_MARKER;
	std::string THRESHOLD_NUM;
};

#endif
