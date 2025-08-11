package NativeFilter;

import java.util.Arrays;

public class Limiter implements NativeFilterInterface {

    static {
        try {
            NativeLibLoader.loadLibrary("limiter");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Limiter: Native code library failed to load.\n" + e);
            throw e;
        }
    }

    private int channels;
    private int bufferSize;
    private double sampleRate;

    private double attack_ms = 0.1;
    private double release_ms = 2.0;
    private double threshold_dB = -0.1;
    private double lookahead_ms = 100.0;

    public int getChannels() { return this.channels; }
    public int getBufferSize() { return this.bufferSize; }
    public double getLookahead() { return this.lookahead_ms; } 
    public double getSampleRate() { return this.sampleRate; }
    public double getThreshold() { return this.threshold_dB; }
    public double getAttackTime() { return this.attack_ms; }
    public double getReleaseTime() { return this.release_ms; }

    public void setChannels(int channelCount) { this.channels = channelCount; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
    public void setSampleRate(double sampleRate) { this.sampleRate = sampleRate; }
    public void setThreshold(double threshold_dB) { this.threshold_dB = threshold_dB; }
    public void setAttackTime(double attack_ms) { this.attack_ms = attack_ms; }
    public void setReleaseTime(double release_ms) { this.release_ms = release_ms; }
    public void setLookahead(double lookahead_ms) { this.lookahead_ms = lookahead_ms; }

    public Limiter(int channels, int bufferSize, double sampleRate) {
        this.channels = channels;
        this.bufferSize = bufferSize;
        this.sampleRate = sampleRate;
    }

    public Limiter(int channels, int bufferSize, double sampleRate, double threshold_dB, double attack_ms, double release_ms, double lookahead_ms) {
        this.channels = channels;
        this.bufferSize = bufferSize;
        this.sampleRate = sampleRate;
        this.threshold_dB = threshold_dB;
        this.attack_ms = attack_ms;
        this.release_ms = release_ms;
        this.lookahead_ms = lookahead_ms;
    }

    private static native void processData(double[] buffer, int channels, int length, double sampleRate, double attack_ms, double release_ms, double threshold_db, double lookahead_ms);

    public double[] process(double[] inputBuffer) {
        if (inputBuffer == null) {
            throw new IllegalArgumentException("Input buffer must not be null.");
        }
        if (inputBuffer.length != this.bufferSize) {
            throw new IllegalArgumentException("Buffer size mismatch: expected " + this.bufferSize + ", got " + inputBuffer.length);
        }
        if (this.channels <= 0) {
            throw new IllegalArgumentException("Channel count must be positive.");
        }

        double[] processedBuffer = Arrays.copyOf(inputBuffer, inputBuffer.length);

        processData(processedBuffer, this.channels, processedBuffer.length, this.sampleRate, this.attack_ms, this.release_ms, this.threshold_dB, this.lookahead_ms);

        return processedBuffer;
    }
}
