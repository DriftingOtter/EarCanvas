package ConfigParser;

public class ConfigIOAccessException extends RuntimeException {
	
	public ConfigIOAccessException() {
		super();
	}
	
	public ConfigIOAccessException(String msg) {
		super(msg);
	}
	
	public ConfigIOAccessException(String msg, Throwable e) {
		super(msg, e);
	}
}
