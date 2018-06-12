package edu.umich.eecs.soar.props.editors;

public class SaCondition {
	public String name;
	public int[] trials;
	public String[] conditions;
	
	SaCondition(String name, String[] conds, int[] trls) {
		this.name = name;
		this.conditions = conds;
		this.trials = trls;
	}
}
