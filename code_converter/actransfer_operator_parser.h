/*
 * actransfer_operator_parser.h
 *
 *  Created on: Apr 3, 2017
 *      Author: bryan
 */

#ifndef ACTRANSFER_OPERATOR_PARSER_H_
#define ACTRANSFER_OPERATOR_PARSER_H_

#include <string>
#include <fstream>
#include <vector>
#include <map>

#include "Primitive.h"


class actransfer_operator_parser {
public:
	actransfer_operator_parser(std::string inPath, std::string outPath, std::string projName);
	virtual ~actransfer_operator_parser();

	int convertToPropsInstructions();

private:
	const static std::string CONST_NAME;
	std::string inPath,
		outPath;
	std::ifstream inFile;
	std::string projectName;
	std::string currTaskName;
	std::string currRule_h1;	// rule name attribute 1
	std::string currRule_h2;	// rule name attribute 2
	int lineNumber;

	void printParseError(std::string msg, bool showLine);
	std::string trim(std::string s);
	std::string separateTokens(const std::string &str);
	std::string nextLine(std::stringstream & ss);
	std::string nextToken(std::stringstream & ss);
	bool getRawProps(std::stringstream &ss, std::vector<std::string> &condConsts, std::vector<std::string> &actConsts, std::vector<Primitive> &conditions, std::vector<Primitive> &actions);
	std::map<std::string, std::string> buildSoarIdRefs(std::vector<std::string> &retRefs, const std::vector<Primitive> &conditions, std::vector<Primitive> &actions);
	void buildPropCode(std::string &retval, const std::string rulename, const std::vector<std::string> &consts, const std::vector<Primitive> &conditions, const std::vector<Primitive> &actions);
	void buildSoarCode(std::string &retval, const std::string rulename, const std::vector<Primitive> &conditions, const std::vector<Primitive> &actions);
	void buildPropOperator(std::string &prpRule, std::string &aplRule, std::vector<std::string> &condConsts, std::vector<std::string> &actConsts, const std::vector<Primitive> &conditions, const std::vector<Primitive> &actions);
	void buildSoarOperator(std::string &prpRule, std::string &aplRule, const std::vector<Primitive> &conditions, const std::vector<Primitive> &actions);
	std::vector<std::string> refineConsts(std::vector<std::string> rawConsts, const std::vector<Primitive> &primitives, std::vector<Primitive> &newprimitives);
	void parseActransferFile(std::vector<std::string> &propRules, std::vector<std::string> &soarRules);
};

#endif /* ACTRANSFER_OPERATOR_PARSER_H_ */
