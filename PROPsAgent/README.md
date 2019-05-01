This is the actual agent code for PROPs. Here is a brief summary of the files:

* **\_firstload_props.soar** Source this to source the rest of the agent and configure Soar appropriately.
*	**props_apply_buffer.soar** This is the code that manages the apply buffer - the mechanism through which actions are returned.
*	**props_auto_addressing.soar** This is the code that manages automatic evaluation of primitive condition or action instantiation using Soar elaborations.
*	**props_epsets.soar** This is the code that manages epsets, or 'procedure contexts', the structures driving the fetch/execute process.
*	**props_primitives.soar** This is the code defining the primitive conditions and actions that are used to carry out task-general declarative instructions.
*	**props_prohibit_list.soar** This is a helper file for managing smem prohibits.
*	**props_rl.soar** Sourcing this enables RL for learning operator selection, using primitive-based numeric-indifferent preferences.
*	**props_learn_l1.soar** Sourcing this enables experimental Cognitive Phase learning of skill structure. (Note that L1 means something different in PROP3 compared to previous versions.)
*	**props_learn_l2.soar** Sourcing this code enables learning the hierarchical combinations of practiced primitive actions.
*	**props_learn_l3.soar** Sourcing this code enables learning complete operator application rules from instruction. (Note that L3 means something slightly different in PROP3 compared to previous versions. Instruction fetch is always required in PROP3 before chunks can execute actions.)

If you want to see how declarative instructions are used, it might be helpful to look at an example set of instructions from the **domains** directory back in the main folder.
