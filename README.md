# PROPs
This repository contains the code for the Soar Primitive Operators learning system. This system is under development. The latest version, as exists here, is PROP2, as described in (Stearns & Laird, 2018).

Some architectural modifications were made to Soar to achieve the functionality described in that paper. You can find the altered architecture code in the "cbc-experimental" branch of the Soar repository: https://github.com/SoarGroup/Soar/tree/cbc-experimental
The main change was that chunking was made gradual. Without this modification, learning will be 1-shot and super-human, but with slightly less transferable chunks.
Another minor change is that the (dont-learn) RHS function was made to be the inverse of (force-learn), so that the agent could "change its mind" and decide to disable chunking for a state. If you use the PROPs agent without this modification, comment out the rules that invoke the (dont-learn) function. You will simply get additional super-human learning.


There are 4 folders:

* **PROPsAgent**
  This contains the Soar agent code for running the agent. It will take effect whenever there is a state no-change (SNC) impasse, and otherwise will be dormant. (There are no guarantees that the code won't interfere with other agent code if PROPs was sourced as a library into another agent. Modularity was not a focus of PROPs development.)
* **analysis**
  This contains general scripts and functions for parsing the behavior of a PROPs agent.
* **code_converter**
  This contains a C++ program for automatically converting Actransfer (.lisp) or Soar (.soar) agent files into PROP directives (.prop) or SMEM declarative instructions (PROP.soar).
* **domains**
  This contains various files related to running human models for specific domains. Primary are the Java (Eclipse) environments for running Soar PROPs agents and collect agent output, as well as additional Soar agent files specific to that domain (e.g. additional SMEM memories the agent recalls, or Actransfer-simulating topstate working memory usage. Additionally there are scripts for parsing these outputs and generating graphs.


References:
* Bryan Stearns, Mazin Assanie, John E. Laird (2017). Applying Primitive Elements Theory For Procedural Transfer in Soar. _Proceedings of the 15th International Conference on Cognitive Modelling (ICCM). Warwick, UK._
* Bryan Stearns, John E. Laird (2018). Modeling Instruction Fetch in Procedural Learning. _Proceedings of the 16th International Conference on Cognitive Modelling (ICCM). Madison, WI._
