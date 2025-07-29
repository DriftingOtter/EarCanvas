package ConfigParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

public interface ParserInterface {

	public boolean addConfig(ArrayList<Object> filterValues, Optional<String> configName) throws IOException;
	public boolean removeConfig(String configName) throws IOException;
	
	public ArrayList<Object> getConfig(String configName);
	
	public Path findConfig(Path configPath);
	
	
}
