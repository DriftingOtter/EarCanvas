package Filter;

public class InvalidFilterException extends Exception {
	
	public InvalidFilterException() {
		super();
	}
	
	public InvalidFilterException(String msg) {
		super(msg);
	}
	
	public InvalidFilterException(String msg, Exception e) {
		super(msg, e);
	}
}
