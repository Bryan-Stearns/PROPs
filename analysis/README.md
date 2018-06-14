This folder contains generic code useful for analyzing PROPs agent output.

* **parse_props_verbose.py** This script takes a verbose Soar output file, including echo'd agent task results, and extracts information about decision cycles and retrievals for calculating simulated time.
It will generate a new file with a "_X" postfix in the name, which has the original agent task results plus the extracted information, in a tab-separated format.
