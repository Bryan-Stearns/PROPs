#ifndef SOAR_TO_PROPS_H
#define SOAR_TO_PROPS_H

#include <string>
#include <fstream>
#include <vector>
#include <map>

#include "Primitive.h"

class soar_to_props
{
public:
	soar_to_props(std::string inPath, std::string outPath);
	~soar_to_props();

	int convertToPropsInstructions();

private:
	const static std::string CONST_NAME;
	std::string inPath,
		outPath;
	std::ifstream inFile;
	std::string currRuleName;
	int lineNumber;

	static std::map<std::string, std::string> cond_ops;
	static std::map<std::string, std::string> act_ops;

	void printParseError(std::string msg, bool showLine);
	std::string trim(std::string s);
	std::string nextLine(std::stringstream & ss);
	std::string nextToken(std::stringstream & ss);
	std::string replacementPath(const std::string &str, std::map<std::string, std::string> &varToPath, bool &did_change);
	std::string replacementPath(const std::string &str, std::map<std::string, int> &const_ids, bool &did_change);
	bool getRawProps(std::stringstream &ss, std::map<std::string, std::string> &varToPath, std::vector<std::string> &constants, std::vector<Primitive> &conditions, std::vector<Primitive> &actions);
	bool fillPaths(std::map<std::string, std::string> &varToPath, std::map<std::string, int> &constIds, std::vector<Primitive> &conditions, std::vector<Primitive> &actions);
	void setReplaceProps(std::vector<Primitive> &actions);
	void buildPropCode(std::string &retval, const std::vector<std::string> &consts, const std::vector<Primitive> &conditions, const std::vector<Primitive> &actions);
	std::vector<std::string> parseSoarFile();
};

#endif
