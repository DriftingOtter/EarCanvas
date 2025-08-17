package NativeFilter;

public class InvalidFilterParametersException extends Exception {
	
	public InvalidFilterParametersException() {
		super();
	}
	
	public InvalidFilterParametersException(String msg) {
		super(msg);
	}
	
	public InvalidFilterParametersException(String msg, Exception e) {
		super(msg, e);
	}
}
