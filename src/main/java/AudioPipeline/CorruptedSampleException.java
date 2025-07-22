package AudioPipeline;

public class CorruptedSampleException extends Exception {
	
	public CorruptedSampleException() {
		super();
	}
	
	public CorruptedSampleException(String msg) {
		super(msg);
	}
	
	public CorruptedSampleException(String msg, Throwable e) {
		super(msg, e);
	}
}
