This is the actual agent code for PROPs. Here is a brief summary of the files:

* **\_firstload_props.soar** Source this to source the rest of the agent and configure Soar appropriately.
*	**props_fetch.soar** This is the code that manages declarative instruction fetching.
*	**props_static.soar** This is the code that manages instruction evaluation memories kept statically in SMEM across different instruction evaluation substates, such as the declarative sequence of things to fetch.
*	**props_instruction_init.soar** This is the code that initializes instruction execution after instructions are fetched and selected.
*	**props_instruction_eval.soar** This is the code that manages declarative instruction evaluation
*	**props_primitives.soar** This is the code defining the Soar PRimitive OPerators (PROPs) that are used to perform task-general declarative instructions.
*	**props_primitive_addressing.soar** This is the code that traces Soar working memory to find described memory elements, as needed to perform the instructed memory operations.
*	**props_build_proposals.soar** This is the code for the near-architectural process of composing pairs of executed operators together and returns them for chunking.
*	**props_shared.soar** This is an eccentric set of generic elaboration rules shared across substates in the PROPs evaluation process.
*	**lib_smem_clean.soar** This is generic library code to clear SMEM commands when they are completed.
*	**lib_smem_prohibit_store.soar** This is generic library code to swap SMEM prohibits in and out of the SMEM command structure when appropriate, so that they don't interfere with commands like ^store.
*	**props_learn_conds.soar** Sourcing this code enables learning instruction condition associations, so that instruction can be fetched according to contextual spreading activation.
*	**props_learn_l1.soar** Sourcing this code enables learning the memory-addressing chunks apart from operator composition. (Don't source this if you are sourcing props_learn_l2.soar, since this l1 learning is subsumed by l2, and could actually cause errors if they're both enabled.)
*	**props_learn_l2.soar** Sourcing this code enables learning the operator combinations performed in props_build_proposals.soar. (Sourcing this automatically sources props_build_proposals.soar)
*	**props_learn_l3.soar** Sourcing this code enables learning autonomous task rules from declarative instructions, after l2 compositions are completely learned. These rules will fire in the task state without the need for the fetch/execute cycle.
*	**props_learn_l3only.soar** Sourcing this code enables learning autonomous task rules, but assumes that l1 and l2 are not enabled. It will thus learn task rules immediately without first composing l2 compositions.

If you want to see how declarative instructions are used, it might be helpful to look at an example set of instructions from the **domains** directory back in the main folder.
