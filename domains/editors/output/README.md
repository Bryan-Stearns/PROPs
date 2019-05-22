Running the experiments set up in the Java domain environments will create verbose output files of agent decision making. (The output that would be seen in the Soar debugger.)  
Verbose output files are then run through the parse_props_verbose.py file found in the analysis domain. This script analyzes the decisions and actions made by the agent and creates the _X1 or _C files shown here in this folder.

The naming convention of these files:  
* `l123` - This refers to the levels of learning used. L1 is the primitive operator instantiation learning introduced in PROP1. L2 is the generic Associative Phase learning, common to all these models (learn hierarchical compilation of rules). L3 is the optional Autonomous Phase learning (learn fully automatic rules for operator proposals and actions).  
* `sc_lc` - This indicates that spreading activation from conditions ("sc") and learning chunks for that condition spread ("lc") were both enabled. Together this corresponds to Cognitive Phase learning in the model (learn what instructions to fetch).
* `X1prN` - The value of N is the number of msec simulated per decision. If no N is given, 50 msec / decision is assumed.  
* `tN` - This indicates the chunk threshold used, where N is the threshold number.

The individual files are tab-separated records of stats per agent task action. 

The X1 file columns are as follows:

| task condition | day | editor | trial | edit type | ll | mt | real time | action latency | decision cycles (simulated) | simulated time (decisions + retrievals) | #LTM retrievals | #failed instruction fetches |  
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |  

The C files are similar, but simpler, and include chunking data:

| task condition | day | editor | trial | edit type | decision cycles | total chunks | simulated time (total) | gen proposal chunks | gen apply chunks | condition chunks | auto chunks |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
