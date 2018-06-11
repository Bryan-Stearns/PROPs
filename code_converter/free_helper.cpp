/*
 * free_helper.cpp
 *
 *      Author: bryan
 */

#include <string>

#include "free_helper.h"



// Trim "|foobar|" into "foobar"
std::string trim_bars(std::string str) {
	if (str.front() == '|' && str.back() == '|')
		return str.substr(1, str.size() - 2);
	return str;
}

