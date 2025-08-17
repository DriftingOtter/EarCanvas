package AudioProcessingRangler;

public class EmptyFilterRackException extends Exception {
	
	public EmptyFilterRackException() {
		super();
	}
	
	public EmptyFilterRackException(String msg) {
		super(msg);
	}
	
	public EmptyFilterRackException(String msg, Exception e) {
		super(msg, e);
	}
}
