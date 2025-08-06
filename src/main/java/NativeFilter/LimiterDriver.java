package NativeFilter;

public class LimiterDriver {

    public static void main(String[] args) {
        // --- 1. Setup Audio Parameters ---
        final int SAMPLE_RATE = 44100;
        final int CHANNELS = 1; // Mono for simplicity
        final int BUFFER_SIZE = 1024;

        System.out.println("--- Test Run: Limiter ---");
        System.out.println("Sample Rate: " + SAMPLE_RATE + " Hz, Channels: " + CHANNELS);
        System.out.println("==========================================================");

        // --- 2. Configure the Limiter ---
        Limiter limiter = new Limiter(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);

        // Use a very low threshold to make the effect obvious.
        double threshold_dB = -20.0;
        double release_ms = 100.0;
        double lookahead_ms = 1.0;
        double attack_ms = 0.0;

        limiter.setThreshold(threshold_dB);
        limiter.setReleaseTime(release_ms);
        limiter.setLookahead(lookahead_ms);
        limiter.setAttackTime(attack_ms);

        System.out.println("Limiter settings:");
        System.out.println("  - Threshold: " + threshold_dB + " dB");
        System.out.println("  - Attack: " + attack_ms + " ms (Instantaneous)");
        System.out.println("  - Release: " + release_ms + " ms");
        System.out.println("  - Lookahead: " + lookahead_ms + " ms");


        // --- 3. Create a Test Audio Buffer with Loud Clicks ---
        // This signal is mostly quiet, with loud peaks to test the limiter.
        double[] inputBuffer = new double[BUFFER_SIZE];
        // The first loud click will happen at this sample index.
        int firstClickIndex = 300; 

        // Fill with low-level noise
        for (int i = 0; i < BUFFER_SIZE; i++) {
            inputBuffer[i] = (Math.random() - 0.5) * 0.01; // Very quiet noise
        }
        // Add three loud clicks
        inputBuffer[firstClickIndex] = 1.0;  // 0 dBFS peak
        inputBuffer[firstClickIndex + 1] = -1.0; // 0 dBFS peak
        inputBuffer[500] = 1.0;
        inputBuffer[501] = -1.0;
        inputBuffer[700] = 1.0;
        inputBuffer[701] = -1.0;


        // --- 4. Process the Audio ---
        double[] outputBuffer = null;
        try {
            System.out.println("\nCalling native processData()...");
            outputBuffer = limiter.process(inputBuffer);
            System.out.println("Processing complete.\n");
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            return;
        }
        
        // --- 5. Analyze and Display Results ---
        double inputPeak = 0.0;
        for (double sample : inputBuffer) {
            inputPeak = Math.max(inputPeak, Math.abs(sample));
        }

        double outputPeak = 0.0;
        for (double sample : outputBuffer) {
            outputPeak = Math.max(outputPeak, Math.abs(sample));
        }
        
        double thresholdLinear = Math.pow(10.0, threshold_dB / 20.0);

        System.out.println("--- Analysis ---");
        System.out.printf("Input Peak Level:  %.6f (%.2f dBFS)%n", inputPeak, 20 * Math.log10(inputPeak));
        System.out.printf("Limiter Threshold: %.6f (%.2f dBFS)%n", thresholdLinear, threshold_dB);
        System.out.printf("Output Peak Level: %.6f (%.2f dBFS)%n", outputPeak, 20 * Math.log10(outputPeak));
        
        // Add a small tolerance for floating point inaccuracies
        System.out.println(outputPeak <= (thresholdLinear + 0.000001) ? "SUCCESS: Output peak is at or below the threshold." : "FAILURE: Output peak exceeds the threshold.");
        
        // --- 6. Display samples around the first loud click ---
        System.out.println("\n--- Comparing Samples Around the First Loud Click ---");
        System.out.printf("%-10s | %-20s | %-20s | %-15s%n", "Sample #", "Input Value", "Output Value", "Comment");
        System.out.println("--------------------------------------------------------------------------------");
        
        // Calculate the delay in samples to know where to expect the output
        int lookaheadSamples = (int) Math.floor(lookahead_ms * SAMPLE_RATE / 1000.0);
        
        // Start printing a few samples before the click appears in the output
        int printStartIndex = firstClickIndex + lookaheadSamples - 4;
        for (int i = printStartIndex; i < printStartIndex + 10; i++) {
            String comment = "";
            if (i == firstClickIndex + lookaheadSamples || i == firstClickIndex + lookaheadSamples + 1) {
                comment = "** PEAK LIMITED **";
            }
            System.out.printf("%-10d | %-20.15f | %-20.15f | %-15s%n", i, inputBuffer[i], outputBuffer[i], comment);
        }
    }
}
