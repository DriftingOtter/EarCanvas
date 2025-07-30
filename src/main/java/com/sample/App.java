package com.sample;

public class App {
	
	static {
        // Detect OS and load native library dynamically
        String os     = System.getProperty("os.name").toLowerCase();
        String arch   = System.getProperty("os.arch").toLowerCase();
        String libDir = "libs"; // relative path to your native libraries
        String libName;

        if (os.contains("win")) {
            libName = "native_k_band_equalizer.dll";
        } else if (os.contains("mac")) {
            libName = "native_k_band_equalizer.dylib";
        } else if (os.contains("nux") || os.contains("nix")) {
            libName = "native_k_band_equalizer.so";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + os);
        }

        try {
            String fullPath = new java.io.File(libDir + "/" + libName).getAbsolutePath();
            System.load(fullPath);
            System.out.println("Loaded native library: " + fullPath);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load native EQ library", e);
        }
    }

    public static void main(String[] args) {
        // Launch your audio pipeline
        AudioPipeline.AudioPipeline pipeline = new AudioPipeline.AudioPipeline();
        pipeline.start();

        // Example: let it run for 10 seconds then stop
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {}

        pipeline.stop();
    }
    
}
