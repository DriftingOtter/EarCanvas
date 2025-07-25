package ConfigParser;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import AudioEqualizer.AudioEqualizer;
import AudioEqualizer.EmptyFilterRackException;
import AudioEqualizer.InvalidFilterException;
import uk.me.berndporr.iirj.Cascade;

public class ConfigParser implements ParserInterface {
	
	private ObjectMapper objectMapper;
	private Path configDirectory;
	
	public ConfigParser() {
		this.objectMapper = new ObjectMapper();

		// Set the working directory /resources
		Path cwd 			= Paths.get(System.getProperty("user.dir"));
		Path resourcePath   = cwd.getParent().resolve("resources");
		this.configDirectory = resourcePath.toAbsolutePath();
	}

	@Override
	public boolean addConfig(ArrayList<Object> filterValues) throws IOException {
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		
		StringWriter stringFilterValues = new StringWriter();
		objectMapper.writeValue(stringFilterValues, filterValues);
		
		System.out.println(stringFilterValues.toString());
		return true;
	}

	@Override
	public boolean setConfig() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeConfig() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mkConfigDefaults() throws IOException {
		
		return false;
	}
	
}
