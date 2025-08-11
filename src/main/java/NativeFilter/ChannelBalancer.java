package NativeFilter;

import java.util.Arrays;

public class ChannelBalancer implements NativeFilterInterface{

    static {
        try {
            NativeLibLoader.loadLibrary("channelbalencer");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Limiter: Native code library failed to load.\n" + e);
            throw e;
        }
    }

    private int channels;
    private int bufferSize;
    private double sampleRate;

    private double preference  = 0.0;

    public int getChannels() 		   { return this.channels; }
    public int getBufferSize() 		   { return this.bufferSize; }
    public double getSampleRate() 	   { return this.sampleRate; }
    public double getPreference()      { return this.preference; }
    
    public void setBufferSize(int bufferSize) 			   { this.bufferSize = bufferSize; }
    public void setChannels(int channelCount) 			   { this.channels = channelCount; }
    public void setSampleRate(double sampleRate) 		   { this.sampleRate = sampleRate; }
    public void setPreference(double preference) 		   { this.preference = preference; }

    public ChannelBalancer(int channels, int bufferSize, double sampleRate) {
        this.channels   = channels;
        this.bufferSize  = bufferSize;
        this.sampleRate = sampleRate;
    }
    
    public ChannelBalancer(int channels, int bufferSize, double sampleRate, double preference) {
    	this.channels   = channels;
        this.bufferSize  = bufferSize;
        this.sampleRate = sampleRate;
        this.preference = preference;
    }

    private static native void processData(double[] buffer, int channels, int length, double sampleRate, double preference);

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

        processData(processedBuffer, this.channels, processedBuffer.length, this.sampleRate, this.preference);

        return processedBuffer;
    }
}
