package AudioPipeline;

import javax.sound.sampled.*;

import AudioEqualizer.AudioEqualizer;
import AudioEqualizer.InvalidFilterException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioPipeline implements PipelineInterface, Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private TargetDataLine line;
    private AudioFormat format;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected float sampleRate;
    protected int sampleSizeInBits;
    protected int channels;
    protected int bitDepth;
    protected boolean signed;
    protected boolean bigEndian;

    private AudioEqualizer equalizer;

    public AudioPipeline() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
            if (!AudioSystem.isLineSupported(info)) {
                throw new RuntimeException("System data line does not support line access.");
            }
            
            TargetDataLine tempLine = (TargetDataLine) AudioSystem.getLine(info);
            tempLine.open();
            
            this.format = tempLine.getFormat();
            this.sampleRate 	  = format.getSampleRate();
            this.bitDepth		  = format.getSampleSizeInBits();
            this.channels 		  = format.getChannels();
            this.signed 		  = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
            this.bigEndian 		  = format.isBigEndian();
            
            tempLine.close();
        } catch (LineUnavailableException e) {
            throw new RuntimeException("System line is currently unavailable...", e);
        }
    }

    /**
     * A new, protected constructor designed specifically for testing.
     * It allows us to "inject" a fake TargetDataLine to control its behavior during a test.
     * @param lineForTest A pre-configured TargetDataLine (which will be a fake one in our tests).
     */
    protected AudioPipeline(TargetDataLine lineForTest) {
        this.line = lineForTest;
        if (this.line != null) {
            this.format = line.getFormat();
            this.sampleRate = format.getSampleRate();
            this.bitDepth = format.getSampleSizeInBits();
            this.channels = format.getChannels();
            this.signed = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
            this.bigEndian = format.isBigEndian();
        }
    }

    public void setEqualizer(AudioEqualizer equalizer) {
        this.equalizer = equalizer;
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }
    
    private byte[] returnFilteredBuffer(byte[] buffer) {
    	// TODO
    	return buffer;
    }

    
    @Override
    public void start() {
        try {
            /* If a line was already injected by test constructors, use it. 
             * else, we fetch a real one from the AudioSystem.
            */
            if (this.line == null) {
                 DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    throw new RuntimeException("Target data line does not support " + format.toString() + ".");
                }
                this.line = (TargetDataLine) AudioSystem.getLine(info);
            }

            if (!line.isOpen()) {
                 line.open(format, line.getBufferSize());
            }
            line.start();

            running.set(true);
            executorService.submit(this);

            System.out.println("AudioPipelineCtl: AudioPipeline service started successfully...");

        } catch (LineUnavailableException e) {
            throw new RuntimeException("Could not open audio line due to pipes busy...", e);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("AudioPipelineCtl: Stopping AudioPipeline...");
            
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }

            if (line != null) {
                line.stop();
                line.close();
            }
            System.out.println("AudioPipelineCtl: AudioPipeline stopped.");
        }
    }
    
    @Override
    public void run() {
    	int bufferSize = (int)(sampleRate * channels * (bitDepth/8) * 0.05);
        byte[] buffer = new byte[bufferSize];
        
        while (running.get()) {
            if (line != null) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                	try {
                        if (equalizer != null) {
                            double[] outputBuffer = toDoubleArray(buffer, bytesRead);
                            outputBuffer = equalizer.processData(outputBuffer);
                            byte[] processedBytes = toByteArray(outputBuffer, outputBuffer.length * 8);
                        }
					} catch (InvalidFilterException e) {
                        System.err.println("AudioPipelineCtl: Invalid filter in equalizer rack. Stopping pipeline...");
						running.set(false);
					}
                } else if (bytesRead == -1) {
                    running.set(false);
                }
                
            }
        }
        System.out.println("AudioPipelineCtl: Processing loop finished.");
    }
    
    private double[] toDoubleArray(byte[] byteArray, int bytesRead) {
        int doublesToRead = bytesRead / 8;
        double[] doubleArray = new double[doublesToRead];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray, 0, bytesRead);
        
        buffer.order(this.bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < doubleArray.length; i++) {
            doubleArray[i] = buffer.getDouble();
        }
        
        return doubleArray;
    }
    
    private byte[] toByteArray(double[] doubleArray, int bufferSize) {
        int bytesToProcess = Math.min(bufferSize, doubleArray.length * 8);
        ByteBuffer buffer = ByteBuffer.allocate(bytesToProcess);
        
        buffer.order(this.bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        
        for (int i = 0; i < doubleArray.length; i++) {
            buffer.putDouble(doubleArray[i]);
        }
        
        return buffer.array();
    }

	@Override
    public String toString() {
        return "AudioPipeline[format=" + format + ", running=" + running.get() + "]";
    }
}
