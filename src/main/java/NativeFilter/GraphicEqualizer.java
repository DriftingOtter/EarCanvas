package NativeFilter;

import java.util.Arrays;

public class GraphicEqualizer { 
    static {
        try {
        	NativeLibLoader.loadLibrary("graphic_equalizer");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("GraphicEqualizer: Native code library failed to load.\n" + e);
            throw e;
        }
    }

    private int channels;
    private int bufferSize;
    private float sampleRate;
    private final int bandCount = 10;
    private double qFactor = 6;
    private double[] bandGains = new double[] {
            0.0,    // 31 Hz
            0.0,    // 63 Hz
            0.0,    // 125 Hz
            0.0,    // 250 Hz
            0.0,    // 500 Hz
            0.0,    // 1000 Hz
            0.0,    // 2000 Hz
            0.0,    // 4000 Hz
            0.0,    // 8000 Hz
            0.0     // 16000 Hz
        };


    public int getChannels() { return this.channels; }
    public int getBufferSize() { return this.bufferSize; }
    public int getBandCount() { return this.bandCount; }
    public double[] getGains() { return this.bandGains; }
    public double getQ() { return this.qFactor; }

    public void resetGains() { this.bandGains = new double[bandCount]; }
    public void setChannels(int channelCount) { this.channels = channelCount; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
    public void setGains(double[] bandGains) { this.bandGains = sanitizeGains(bandGains, this.bandCount); }
    public void setSampleRate(float sampleRate) {this.sampleRate = sampleRate;}
    public void setQ(double q) {this.qFactor = q;}
    
    private static double[] sanitizeGains(double[] gains, int bandCount) {
        double[] sanitized = new double[bandCount];
        for (int i = 0; i < bandCount; i++) {
            double g = (i < gains.length) ? gains[i] : 0.0;
            sanitized[i] = Math.max(-2.0, Math.min(2.0, g));
        }
        return sanitized;
    }
    
    // --- Constructors ---
    public GraphicEqualizer(int channels, int bufferSize, float sampleRate) {
        this.channels = channels;
        this.bufferSize = bufferSize;
        this.sampleRate = sampleRate;
        this.qFactor = 6.0;
        this.bandGains = new double[bandCount];
    }

    public GraphicEqualizer(int channels, int bufferSize, float sampleRate, double[] bandGains) {
        this.channels = channels;
        this.bufferSize = bufferSize;
        this.sampleRate = sampleRate;
        this.qFactor = 6.0;
        this.bandGains = sanitizeGains(bandGains, this.bandCount);
    }
    
    // --- Native Method ---
    private static native void processData(double[] buffer, int length, int channels, float sampleRate, double[] bandGains, double qFactor);

    public double[] process(double[] inputBuffer) {
        if (inputBuffer == null || bandGains == null) {
            throw new IllegalArgumentException("Input buffer and gain array must not be null.");
        }
        if (inputBuffer.length != this.bufferSize) {
            throw new IllegalArgumentException("Buffer size mismatch: expected " + this.bufferSize + ", got " + inputBuffer.length);
        }
        if (this.channels <= 0) {
            throw new IllegalArgumentException("Channel count must be positive.");
        }

        double[] processedBuffer = Arrays.copyOf(inputBuffer, inputBuffer.length);
        
        processData(processedBuffer, processedBuffer.length, this.channels, this.sampleRate, this.bandGains, this.qFactor);
        
        return processedBuffer;
    }
    
}