
#include <string>
#include <iostream>

#include "props_instruction_parser.h"
#include "soar_to_props.h"
#include "actransfer_operator_parser.h"

// Given a filename, return the front and back.
// Ex:	Given fuzzy.exe, returns [fuzzy] [.exe]
//		Given fuzzy, returns [fuzzy] []
void splitFilename(std::string const &original, std::string &front, std::string &back) {
	int dotPos = original.find_last_of('.');
	if (dotPos == std::string::npos) {
		dotPos = original.size();
		front = original;
		back = "";
	}
	else {
		front = original.substr(0, dotPos);
		back = original.substr(dotPos);
	}
}

int main(int argc, char** argv) {
	// Check for arguments
	if (argc <= 2) {
		std::cout << "USAGE: Your first two arguments must be a path to the input file and a path to an output file." << std::endl;
		return 1;
	}

	std::string inPath = std::string(argv[1]);
	std::string outPath = std::string(argv[2]);

	// Determine which conversion to do based on file extension
	std::string inFront, inExt, outFront, outExt;
	splitFilename(inPath, inFront, inExt);
	splitFilename(outPath, outFront, outExt);

	if (!inExt.compare(".lisp") || !inExt.compare(".delta")) {
		// Require a 3rd argument for the project name (used for naming generated prop/soar rules)
		if (argc <= 3) {
			std::cout << "USAGE: Your third argument must be the name to use for this model (no spaces)." << std::endl;
			return 1;
		}

		// Convert the Actransfer instruction file into a .prop instruction file
		try {
			std::string projName = std::string(argv[3]);
			actransfer_operator_parser parser(inPath, outFront, projName);

			if (parser.convertToPropsInstructions() != 0) {
				return 1;
			}

			// Adjust so we next generate the smem .soar file from the .prop file just created
			inExt = ".prop";
			inFront = outFront;

			std::cout << "DONE!" << std::endl;
		}
		catch (...) {
			std::cout << "ERROR in conversion. Aborting." << std::endl;
		}
	}

	if (!inExt.compare(".soar")) {
		// Convert the soar productions file into a prop instructions file
		try {
			soar_to_props parser(inPath, outPath);
			if (parser.convertToPropsInstructions() != 0) {
				return 1;
			}

			// Adjust so we next generate the smem .soar file from the .prop file just created
			inExt = ".prop";
			inFront = outFront;
		}
		catch (...) {
			std::cout << "ERROR: Could not open the soar input file. Aborting." << std::endl;
		}
	}

	if (!inExt.compare(".prop")) {
		// Convert the prop file into an smem source file
		try {
			props_instruction_parser parser(inFront+inExt, outFront+"_instructions.soar");
			if (parser.convert() != 0) {
				return 1;
			}

		}
		catch (...) {
			std::cout << "ERROR: Could not open the props input file. Aborting." << std::endl;
		}
	}


	return 0;
}
