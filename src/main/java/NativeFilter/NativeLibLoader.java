package NativeFilter;

import java.io.*;
import java.nio.file.*;

public class NativeLibLoader {

    public static void loadLibrary(String libName) {
        String os = detectOS();
        String arch = detectArch();
        String mappedName = mapLibraryName(libName, os);

        String resourcePath = "/native/" + os + "-" + arch + "/" + mappedName;

        try (InputStream in = NativeLibLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Native library not found at: " + resourcePath);
            }

            Path tempDir = Files.createTempDirectory("nativeLibs");
            tempDir.toFile().deleteOnExit();

            Path tempLib = tempDir.resolve(mappedName);
            Files.copy(in, tempLib, StandardCopyOption.REPLACE_EXISTING);
            tempLib.toFile().deleteOnExit();

            System.load(tempLib.toAbsolutePath().toString());

        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library: " + mappedName, e);
        }
    }

    private static String detectOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "macos";
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) return "linux";
        throw new UnsupportedOperationException("Unsupported OS: " + os);
    }

    private static String detectArch() {
        String arch = System.getProperty("os.arch");
        if (arch.equals("x86_64") || arch.equals("amd64")) return "x86_64";
        if (arch.equals("aarch64")) return "aarch64";
        throw new UnsupportedOperationException("Unsupported architecture: " + arch);
    }

    private static String mapLibraryName(String baseName, String os) {
        if (os.equals("windows")) return baseName + ".dll";
        if (os.equals("macos")) return "lib" + baseName + ".dylib";
        return "lib" + baseName + ".so"; // linux
    }

}

