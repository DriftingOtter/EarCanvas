package NativeFilter;

import java.util.Arrays;

public class GraphicEqualizerDriver {
	
	public static void main(String[] args) {
        // --- 1. Setup Audio Parameters ---
        final int SAMPLE_RATE = 44100;
        final int CHANNELS = 1; // Mono for simplicity
        final int BUFFER_SIZE = 1024;

        System.out.println("--- Test Run: Graphic Equalizer as Low-Pass Filter ---");
        System.out.println("Sample Rate: " + SAMPLE_RATE + " Hz, Channels: " + CHANNELS);
        System.out.println("==========================================================");

        // --- 2. Configure the Equalizer ---
        // Instantiate the GraphicEqualizer
        GraphicEqualizer eq = new GraphicEqualizer(CHANNELS, BUFFER_SIZE, (float) SAMPLE_RATE);

        // Set up band gains to simulate a low-pass filter
        // We cut all frequencies above 2000 Hz
        // The f0 bands are: {31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000}
        double[] bandGains = new double[] {
            0.0,    // 31 Hz
            0.0,    // 63 Hz
            0.0,    // 125 Hz
            0.0,    // 250 Hz
            0.0,    // 500 Hz
            0.0,    // 1000 Hz
            0.0,    // 2000 Hz
            -2.0,   // 4000 Hz  <- CUT
            -2.0,   // 8000 Hz  <- CUT
            -2.0    // 16000 Hz <- CUT
        };
        eq.setGains(bandGains);
        System.out.println("EQ Gains set to: " + Arrays.toString(eq.getGains()));


        // --- 3. Create a Fake Audio Buffer ---
        // A simple ramp signal from -1.0 to 1.0
        double[] inputBuffer = new double[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            inputBuffer[i] = (2.0 * i / (BUFFER_SIZE - 1)) - 1.0;
        }

        // --- 4. Process the Audio ---
        // IMPORTANT: The process() method calls the native C code
        System.out.println("\nCalling native processData()...");
        double[] outputBuffer = eq.process(inputBuffer);
        System.out.println("Processing complete.\n");

        // --- 5. Display Results ---
        System.out.println("--- Comparing Input and Output Buffers (First 15 Samples) ---");
        System.out.printf("%-10s | %-20s | %-20s%n", "Sample #", "Input Value", "Output Value");
        System.out.println("---------------------------------------------------------------");
        for (int i = 0; i < 16; i++) {
            System.out.printf("%-10d | %-20.15f | %-20.15f%n", i, inputBuffer[i], outputBuffer[i]);
        }
    }
}
