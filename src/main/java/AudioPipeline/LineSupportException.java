package AudioPipeline;

public class LineSupportException extends RuntimeException {
	
	public LineSupportException() {
		super();
	}
	
	public LineSupportException(String msg) {
		super(msg);
	}
	
	public LineSupportException(String msg, Throwable e) {
		super(msg, e);
	}
}
