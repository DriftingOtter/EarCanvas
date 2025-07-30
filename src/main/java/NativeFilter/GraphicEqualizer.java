package NativeFilter;

import java.io.*;
import java.util.Arrays;

public class GraphicEqualizer implements NativeFilterInterface {

    static {
        try {
            System.loadLibrary("graphic_equalizer");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("GraphicEqualizer: Native code library failed to load.\n" + e);
            throw e;
        }
    }

    private int channels;
    private int bufferSize;
    private float sampleRate;
    private final int bandCount = 16;
    private double[] gains16;

    private static native void processData(double[] buffer, int length, int channels, float sampleRate, double[] gains16);

    public GraphicEqualizer(int channels, int bufferSize, float sampleRate) {
        this.channels = channels;
        this.bufferSize = bufferSize;
        this.sampleRate = sampleRate;
        this.gains16 = new double[bandCount];
    }

    public GraphicEqualizer(int channels, int bufferSize, float sampleRate, double[] gains16) {
        this.channels = channels;
        this.bufferSize = bufferSize;
        this.sampleRate = sampleRate;
        this.gains16 = sanitizeGains(gains16);
    }
    
    public int getChannels() { return this.channels; }
    public int getBufferSize() { return this.bufferSize; }
    public int getBandCount() { return this.bandCount; }
    public double[] getGains16() { return this.gains16; }

    public void resetGains() { this.gains16 = new double[bandCount]; }
   
    public void setChannels(int channelCount) { this.channels = channelCount; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
    public void setGains16(double[] gains16) { this.gains16 = sanitizeGains(gains16); }
    public void setSampleRate(float sampleRate) {this.sampleRate = sampleRate;}
    public void setFrequency(float frequency) {throw new UnsupportedOperationException("GraphicEQ has fixed band frequencies. Use setGains16() instead.");}
    public void setQ(float q) {throw new UnsupportedOperationException("GraphicEQ has fixed Q values for its bands.");}
    public void setGain(float gainDb) {throw new UnsupportedOperationException("Use setGains16() to set gains for all bands.");}

    @Override
    public double[] process(double[] inputBuffer) {
        if (inputBuffer == null || gains16 == null) {
            throw new IllegalArgumentException("Input buffer and gain array must not be null.");
        }
        if (inputBuffer.length != this.bufferSize) {
            throw new IllegalArgumentException("Buffer size mismatch: expected " + this.bufferSize + ", got " + inputBuffer.length);
        }
        if (this.channels <= 0) {
            throw new IllegalArgumentException("Channel count must be positive.");
        }

        double[] processedBuffer = Arrays.copyOf(inputBuffer, inputBuffer.length);
        
        processData(processedBuffer, processedBuffer.length, this.channels, this.sampleRate, this.gains16);
        
        return processedBuffer;
    }

    private static double[] sanitizeGains(double[] gains) {
        double[] sanitized = new double[16];
        for (int i = 0; i < 16; i++) {
            double g = (i < gains.length) ? gains[i] : 0.0;
            sanitized[i] = Math.max(0.0, Math.min(1.0, g));
        }
        return sanitized;
    }
}