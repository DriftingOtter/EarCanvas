package NativeFilter;

public class ChannelBalancerDriver {

    public static void main(String[] args) {
        // --- 1. Setup Audio Parameters ---
        final int SAMPLE_RATE = 44100;
        final int CHANNELS = 2; // Stereo
        final int BUFFER_SIZE = 1024;

        System.out.println("--- Test Run: ChannelBalancer ---");
        System.out.println("Sample Rate: " + SAMPLE_RATE + " Hz, Channels: " + CHANNELS);
        System.out.println("==========================================================");

        // --- 2. Create Test Stereo Audio Buffer ---
        // Simple test: left channel ramp, right channel constant tone
        double[] inputBuffer = new double[BUFFER_SIZE];

        for (int i = 0; i < BUFFER_SIZE / CHANNELS; i++) {
            // Left channel: ramp from 0.0 to 1.0
            inputBuffer[i * CHANNELS + 0] = (double) i / (BUFFER_SIZE / CHANNELS - 1);
            // Right channel: constant 0.5
            inputBuffer[i * CHANNELS + 1] = 0.5;
        }

        System.out.println("Input buffer first 10 stereo frames:");
        for (int i = 0; i < 10 * CHANNELS; i += CHANNELS) {
            System.out.printf("L: %.3f, R: %.3f%n", inputBuffer[i], inputBuffer[i + 1]);
        }

        // --- 3. Create and configure ChannelBalancer ---
        ChannelBalancer balancer = new ChannelBalancer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);

        // Test different preferences: full left, center, full right
        double[] testPreferences = {0.0, 0.5, 1.0};

        for (double pref : testPreferences) {
            balancer.setPreference(pref);
            System.out.printf("%nProcessing with preference = %.2f%n", pref);

            double[] processedBuffer = balancer.process(inputBuffer);

            System.out.println("Output buffer first 10 stereo frames:");
            for (int i = 0; i < 10 * CHANNELS; i += CHANNELS) {
                System.out.printf("L: %.3f, R: %.3f%n", processedBuffer[i], processedBuffer[i + 1]);
            }
        }
    }
}
