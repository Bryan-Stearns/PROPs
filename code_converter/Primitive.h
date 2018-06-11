/*
 * Primitive.h
 *
 *  Created on: Jun 11, 2018
 *      Author: bryan
 */

#ifndef PRIMITIVE_H_
#define PRIMITIVE_H_

struct Primitive {
	std::string op,
		path1,
		path2;

	Primitive(std::string Op, std::string Path1, std::string Path2)
		: op(Op), path1(Path1), path2(Path2) { }
};

#endif /* PRIMITIVE_H_ */
