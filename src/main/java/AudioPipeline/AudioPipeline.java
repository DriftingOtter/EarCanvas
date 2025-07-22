package AudioPipeline;

import javax.sound.sampled.*;

import AudioEqualizer.AudioEqualizer;
import AudioEqualizer.InvalidFilterException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioPipeline implements PipelineInterface {

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
                throw new LineSupportException("Target data line does not support line access.");
            }
            
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open();
            
            this.format = line.getFormat();
            this.sampleRate 	  = format.getSampleRate();
            this.bitDepth		  = format.getSampleSizeInBits();
            this.channels 		  = format.getChannels();
            this.signed 		  = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
            this.bigEndian 		  = format.isBigEndian();
            
            line.close();
        } catch (LineUnavailableException e) {
            throw new LineSupportException("System line is currently unavailable...", e);
        }
    }

    public void setEqualizer(AudioEqualizer equalizer) {
        this.equalizer = equalizer;
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }
    
    private byte[] returnFilteredBuffer(byte[] buffer) { return buffer; }

    
    @Override
    public void start() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new LineSupportException("Target data line does not support " + format.toString() + ".");
            }

            line = (TargetDataLine) AudioSystem.getLine(info);
            
            line.open(format, line.getBufferSize());
            line.start();

            running.set(true);
            executorService.submit(this);

            System.out.println("AudioPipelineCtl: AudioPipeline service started successfully...");

        } catch (LineUnavailableException e) {
            throw new LineSupportException("Could not open audio line due to pipes busy...", e);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("AudioPipelineCtl: Stopping AudioPipeline...");
            
            executorService.shutdown();

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
        double[] outputBuffer = new double[bufferSize];
        
        while (running.get()) {
            if (line != null) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                	try {
                    	outputBuffer = toDoubleArray(buffer, bufferSize);
                		outputBuffer = equalizer.processData(outputBuffer);
                		buffer 		= toByteArray(outputBuffer, bufferSize);
					} catch (InvalidFilterException e) {
						break;
					}
                }
                
            }
        }
        
        returnFilteredBuffer(buffer);
        return;
    }
    
    private double[] toDoubleArray(byte[] byteArray, int bufferSize) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);

        if (this.bigEndian) {
            buffer.order(ByteOrder.BIG_ENDIAN);
        } else {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        double[] doubleArray = new double[Math.min(bufferSize, byteArray.length) / 8];

        for (int i = 0; i < doubleArray.length; i++) {
            doubleArray[i] = buffer.getDouble();
        }

        return doubleArray;
    }
    
    private byte[] toByteArray(double[] doubleArray, int bufferSize) {
        int bytesToProcess = Math.min(bufferSize, doubleArray.length * 8);
        int doublesToProcess = bytesToProcess / 8;
        
        ByteBuffer buffer = ByteBuffer.allocate(bytesToProcess);
        
        if (this.bigEndian) {
            buffer.order(ByteOrder.BIG_ENDIAN);
        } else {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        
        for (int i = 0; i < doublesToProcess; i++) {
            buffer.putDouble(doubleArray[i]);
        }
        
        return buffer.array();
    }

	@Override
    public String toString() {
        return "AudioPipeline[format=" + format + ", running=" + running.get() + "]";
    }
    
}