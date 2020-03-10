/*
 * free_helper.cpp
 *
 *      Author: bryan
 */

#include <string>
#include <vector>

#include "free_helper.h"



// Trim "|foobar|" into "foobar"
std::string trim_bars(std::string str) {
	if (str.front() == '|' && str.back() == '|')
		return str.substr(1, str.size() - 2);
	return str;
}

// Custom tokenize separating a given 'ID.attr.attr.attr...' chain
std::vector<std::string> tokenizeSlot(std::string s, std::string root_marker) {
	auto retval = std::vector<std::string>();
	size_t ind = s.find_first_of(".");
	// Replace the rootstate keyword in the ID if present
	std::string substr = s.substr(0, ind);
	if (substr.compare("s") == 0) {
		s.replace(0, ind, root_marker);
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
std::string untokenize(std::vector<std::string> tokens) {
	std::string retval = "";
	for (size_t i = 0; i<tokens.size(); ++i) {
		retval += tokens.at(i);
		if (i < tokens.size() - 1)
			retval += ".";
	}
	return retval;
}
