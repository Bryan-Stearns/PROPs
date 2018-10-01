package edu.umich.eecs.soar.propsutil;

import sml.WMElement;

public class GhostWME {
	public static enum Type {
		STRING, INT, FLOAT, 
		CHAR, BOOLEAN, ID, VAR		// These 4 aren't implemented in Environment yet
	}
	public Type type;
	public String attribute;
	public String s_val;
	public int i_val;
	public double f_val;
	public boolean b_val;
	public boolean blink_new;
	
	public GhostWME(Type t, String attr, String val) {
		type = t;
		attribute = attr;
		
		switch (t) {
		case STRING: case CHAR: case ID: case VAR:
			s_val = val;
			break;
		case INT:
			i_val = Integer.parseInt(val);
			break;
		case FLOAT:
			f_val = Double.parseDouble(val);
			break;
		case BOOLEAN:
			b_val = Boolean.parseBoolean(val);
			break;
		}
		
		blink_new = true;
	}
	
	public boolean equals(WMElement wme) {
		if (wme == null)
			return false;
		
		String w_type = wme.GetValueType();
		String w_val = wme.GetValueAsString();
		switch (type) {
			case STRING:
				if (!w_type.equals("string") || !w_val.equals(s_val))
					return false;
				break;
			case INT:
				if (!w_type.equals("int") || !w_val.equals(Integer.toString(i_val)))
					return false;
				break;
			case FLOAT:
				if (!w_type.equals("double") || !w_val.equals(Double.toString(f_val)))
					return false;
				break;
			case CHAR:
				if (!w_type.equals("char") || !w_val.equals(s_val))
					return false;
				break;
			case BOOLEAN:
				if (!w_type.equals("boolean") || !w_val.equals(Boolean.toString(b_val)))
					return false;
				break;
			case ID:
				if (!w_type.equals("id") || !w_val.equals(s_val))
					return false;
				break;
			case VAR:
				if (!w_type.equals("variable") || !w_val.equals(s_val))
					return false;
				break;
		}
		return true;
	}
	
	public String toString() {
		switch (type) {
		case STRING: case CHAR:
			return attribute + "|" + s_val + "|";
		case INT:
			return attribute + Integer.toString(i_val);
		case FLOAT:
			return attribute + Double.toString(f_val);
		case BOOLEAN:
			return attribute + Boolean.toString(b_val);
		case ID:
			return attribute + s_val ;
		case VAR:
			return attribute + "<" + s_val + ">";
		default:
			return null;
		}
	}
}
