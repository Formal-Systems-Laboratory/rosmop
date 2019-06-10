package rosmop;

/**
 * @author Cansu Erdogan
 *
 * Exception thrown after something related to ROSMOP went wrong
 */
public class ROSMOPException extends Exception {
	
	private static final long serialVersionUID = -6641997342670912718L;
	
	public ROSMOPException(String str){
		super(str);
	}
}
