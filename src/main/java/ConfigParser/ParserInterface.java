package ConfigParser;

import java.io.IOException;
import java.util.ArrayList;

import AudioEqualizer.AudioEqualizer;
import uk.me.berndporr.iirj.Cascade;

public interface ParserInterface {

	boolean addConfig(ArrayList<Object> filterValues) throws IOException;
	public boolean setConfig() throws IOException;
	public boolean removeConfig() throws IOException;
	
	
	boolean mkConfigDefaults() throws IOException;
	
	
}
