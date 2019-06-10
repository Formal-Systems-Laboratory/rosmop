package rosmop.parser.ast;

/**
 * @author Cansu Erdogan
 *
 * AST class to keep variables, fields, parameters
 * 
 */
public class Variable {
	private String type;
	private String declaredName;
	private String initValue; //might be null
	
	/**
	 * Variables are parsed as plain strings, then parameterized
	 * They may not be initialized, but they need to have a type and a declared name
	 * @param wholeString Parameter as plain string
	 */
	public Variable(String wholeString) {
		if(wholeString.indexOf("=") != -1){
			initValue = wholeString.substring(wholeString.lastIndexOf("=")+1, 
					wholeString.length()).trim();
			wholeString = wholeString.substring(0, wholeString.lastIndexOf("=")-1).trim();
		} else initValue = null;
		
		declaredName = wholeString.substring(wholeString.lastIndexOf(" "), 
				wholeString.length()).trim();
		wholeString = wholeString.substring(0, wholeString.lastIndexOf(" ")).trim();
		type = wholeString.trim();
//		toString();
	}
	
	public Variable(String type, String declaredName, String initValue) {
		this.type = type;
		this.declaredName = declaredName;
		this.initValue = initValue;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDeclaredName() {
		return declaredName;
	}

	public void setDeclaredName(String declaredName) {
		this.declaredName = declaredName;
	}

	public String getInitValue() {
		return initValue;
	}

	public void setInitValue(String initValue) {
		this.initValue = initValue;
	}
	
	public String toString(){
		String s = type + " " + declaredName;
		if(initValue != null) s += " = " + initValue;
		
		return s;
	}
	
	public boolean equals(Variable var){
		return type.equals(var.getType()) && declaredName.equals(var.getDeclaredName());
	}
	
//	public boolean compareTo(Variable var2) {
//		if(this.declaredName.equals(var2.declaredName))
//			return true;
//		return false;
//	}
	
}
