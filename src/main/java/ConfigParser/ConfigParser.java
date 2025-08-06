package ConfigParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ConfigParser implements ParserInterface {
	
	private ObjectMapper objectMapper;
	private Path configDirectory;
	private DateTimeFormatter dateTimeFormatter;
	
	public ConfigParser() {
		this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        this.dateTimeFormatter = DateTimeFormatter.ofPattern("dd_MM_yy__hh_mm_a");

        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path resourcePath = cwd.resolve("src/main/configs");
        this.configDirectory = resourcePath.toAbsolutePath();

        System.out.println(configDirectory.toString());
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create config directory: " + configDirectory, e);
        }	}
	
	@Override
    public boolean addConfig(ArrayList<Object> filterValues, Optional<String> configName) throws IOException {
        String defaultName = String.format("tuning_configuration__%s", LocalDateTime.now().format(dateTimeFormatter));
        String fileName = sanitizeFileName(configName.orElse(defaultName));
        
        Path filePath = configDirectory.resolve(fileName);
        objectMapper.writeValue(filePath.toFile(), filterValues);
        return true;
    }

    private String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }


    @Override
    public boolean removeConfig(String configName) throws IOException {
        Path filePath = configDirectory.resolve(configName);
        File file = filePath.toFile();

        if (file.exists() && file.isFile()) {
            return file.delete();
        }

        return false;
    }

    @Override
    public ArrayList<Object> getConfig(String configName) {
        Path filePath = configDirectory.resolve(configName);
        File file = filePath.toFile();

        if (!file.exists() || !file.isFile()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(
                file,
                objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Object.class)
            );
        } catch (IOException e) {
        	throw new ConfigIOAccessException("Failed to read config: " + configName);
        }
    }

    @Override
    public Path findConfig(Path configPath) {
        Path filePath = configDirectory.resolve(configPath.getFileName());
        return Files.exists(filePath) ? filePath : null;
    }

}
