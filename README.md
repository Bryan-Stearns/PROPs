# PROPs
This repository contains the code for the Soar Primitive Operators learning system. This system is under development. The latest version, as exists here, is PROP4.
This version goes beyond Bryan's thesis work, which culminated in PROP3. The thrust of PROP4 veers away from cognitive modeling and toward computational capability for AGI.

The main differences from PROP3 are as follows:

* **Elaboration contexts are now all retrieved into a single Soar state, the topstate.**
  The stack of contexts is maintained virtually in an i-supported graph structure in S1, each with their own workspace node in working memory.
  This allows interaction with I/O without terminating an active branch of elaboration contexts.
* **Multi-attributes are now supported using an attention mechanism.**
  Attention is here defined as taking a subset of working memory and making it available for procedure-context processing by placing into an elaboration context's workspace.
  Attention is programmed through a new sub-type of elaboration context: *attention contexts*.
  PROP3-style elaboration contexts are now given the sub-type name: *response contexts*.
* **Action lines are now instructed all in a single action context.**
* **Value contexts are removed.**
  Action line consts are kept in the parent elaboration context
* **The agent programming language is polished.**
  It is made more robust and user-friendly, based on a lisp syntax. The program parser is a single program written in Java.
* **Gradual chunking is not used.**
  In Soar 9.6, the transfer benefits from gradual chunking do not apply. 
  Future versions might re-introduce this feature if Soar is ever modified to include sequential operator compilation.


There are 2 folders:

* **InstructionParser**
  This contains a Java program for automatically converting Actransfer agent instruction programs into SMEM declarative instructions.
* **Interface**
  This contains the helper code that allows a domain environment program to easily interface with the PROP4 agent, simplifying much of the SML work.
* **PROPsEngine**
  This contains the main PROP4 Soar agent code.
