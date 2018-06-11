/*
 * free_helper.h
 *
 *  Created on: Jun 11, 2018
 *      Author: bryan
 */

#ifndef FREE_HELPER_H_
#define FREE_HELPER_H_

#include <string>
#include <sstream>

// Custom to_string
template <typename T>
inline std::string toString(const T& n) {
	std::ostringstream ss;
	ss << n;
	return ss.str();
}

std::string trim_bars(std::string str);


// Stolen from Boost
template <class T>
inline void hash_combine(std::size_t& seed, const T& v) {
	seed ^= std::hash<T>{}(v)+0x9e3779b9 + (seed << 6) + (seed >> 2);
}


#endif /* FREE_HELPER_H_ */
