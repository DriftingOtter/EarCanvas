package AudioEqualizer;

public class InvalidFilterRackPositionException extends Exception {
	
	public InvalidFilterRackPositionException() {
		super();
	}
	
	public InvalidFilterRackPositionException(String msg) {
		super(msg);
	}
	
	public InvalidFilterRackPositionException(String msg, Exception e) {
		super(msg, e);
	}

}
