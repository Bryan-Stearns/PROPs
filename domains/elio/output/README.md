Running the experiments set up in the Java domain environments will create verbose output files of agent decision making. (The output that would be seen in the Soar debugger.)  
Verbose output files are then run through the parse_props_verbose.py file found in the analysis domain. This script analyzes the decisions and actions made by the agent and creates the _X1 files shown here in this folder.

The naming convention of these files:  
* `l123` - This refers to the levels of learning used. L1 is the primitive operator instantiation learning introduced in PROP1. L2 is the generic Associative Phase learning, common to all these models (learn hierarchical compilation of rules). L3 is the optional Autonomous Phase learning (learn fully automatic rules for operator proposals and actions).  
* `sc_lc` - This indicates that spreading activation from conditions ("sc") and learning chunks for that condition spread ("lc") were both enabled. Together this corresponds to Cognitive Phase learning in the model (learn what instructions to fetch).  
* `X1prN` - The value of N is the number of msec simulated per decision. If no N is given, 50 msec / decision is assumed.

The individual files are tab-separated records of stats per agent task action. The columns are as follows:

| task name | trial | task line | real-time latency (sec) | answer | decision cycles (total) | chunks total | sample (buggy) | decision cycles (simulated) | simulated time including cycles and retrievals | simulated time including cycles only | #failed instruction fetches | action latencies |  
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |  
