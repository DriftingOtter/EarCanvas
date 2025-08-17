package AudioPipeline;

import javax.sound.sampled.*;

import AudioProcessingRangler.AudioProcessingRangler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioPipeline implements Runnable {

    // --- Normalization Constants ---
    private static final double NORM_8_BIT = 127.0;
    private static final double NORM_16_BIT = 32767.0;
    private static final double NORM_32_BIT_INT = 2147483647.0;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService; // Changed from final to allow recreation
    
    private TargetDataLine targetLine;
    private SourceDataLine sourceLine;
    private AudioFormat format;

    protected float sampleRate;
    protected int bitDepth;
    protected int channels;
    protected boolean bigEndian;
    protected AudioFormat.Encoding encoding;

    private AudioProcessingRangler equalizer;

    public AudioPipeline() {
        try {
            // Get default audio format from the system
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
            if (!AudioSystem.isLineSupported(info)) {
                throw new RuntimeException("System data line does not support line access.");
            }
            
            TargetDataLine tempLine = (TargetDataLine) AudioSystem.getLine(info);
            tempLine.open();
            
            this.format = tempLine.getFormat();
            this.sampleRate = format.getSampleRate();
            this.bitDepth = format.getSampleSizeInBits();
            this.channels = format.getChannels();
            this.bigEndian = format.isBigEndian();
            this.encoding = format.getEncoding();
            
            tempLine.close();
        } catch (LineUnavailableException e) {
            throw new RuntimeException("System line is currently unavailable...", e);
        }
    }
    
    public void setEqualizer(AudioProcessingRangler equalizer) {
        this.equalizer = equalizer;
    }

    public AudioFormat getFormat() {
        return this.format;
    }

    public void start() {
        // Check if already running
        if (running.get()) {
            System.out.println("AudioPipeline: Already running, ignoring start request.");
            return;
        }

        try {
            // Create a new executor service if needed (for restart capability)
            if (executorService == null || executorService.isShutdown()) {
                executorService = Executors.newSingleThreadExecutor();
            }

            // --- Setup Input Line (Microphone) ---
            DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(targetInfo)) {
                throw new RuntimeException("Target data line does not support " + format.toString() + ".");
            }
            this.targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
            this.targetLine.open(format, targetLine.getBufferSize());
            this.targetLine.start();

            // --- Setup Output Line (Speakers) ---
            DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(sourceInfo)) {
                throw new RuntimeException("Source data line does not support " + format.toString() + ".");
            }
            this.sourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
            this.sourceLine.open(format, sourceLine.getBufferSize());
            this.sourceLine.start();

            running.set(true);
            executorService.submit(this);
            System.out.println("AudioPipeline: Service started successfully...");
        } catch (LineUnavailableException e) {
            // Clean up on failure
            cleanup();
            throw new RuntimeException("Could not open audio line.", e);
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("AudioPipeline: Stopping...");
            
            // Shutdown executor service
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            cleanup();
            System.out.println("AudioPipeline: Stopped.");
        }
    }

    // Helper method to clean up audio lines
    private void cleanup() {
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
            targetLine = null;
        }
        if (sourceLine != null) {
            sourceLine.drain();
            sourceLine.stop();
            sourceLine.close();
            sourceLine = null;
        }
    }

    public void run() {
        int bufferSize = (int)(sampleRate * channels * (bitDepth / 8) * 0.015);
        byte[] buffer = new byte[bufferSize];

        while (running.get()) {
            if (targetLine != null && sourceLine != null) {
                int bytesRead = targetLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    byte[] processedBytes;
                    if (equalizer != null && !equalizer.isEmpty()) {
                        double[] doubleBuffer = toDoubleArray(buffer, bytesRead);
                        doubleBuffer = equalizer.processData(doubleBuffer);
                        processedBytes = toByteArray(doubleBuffer, bytesRead);
                    } else {
                        // If no equalizer, just pass the original audio through
                        processedBytes = buffer;
                    }
                    // Write the final audio (processed or not) to the speakers
                    sourceLine.write(processedBytes, 0, bytesRead);
                }
            }
        }
        System.out.println("AudioPipeline: Processing loop finished.");
    }

    private double[] toDoubleArray(byte[] byteArray, int bytesRead) {
        int bytesPerSample = bitDepth / 8;
        int samples = bytesRead / bytesPerSample;
        double[] doubleArray = new double[samples];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray, 0, bytesRead);
        buffer.order(this.bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < samples; i++) {
            switch (bitDepth) {
                case 8:
                	doubleArray[i] = ((buffer.get() & 0xFF) - NORM_8_BIT) / NORM_8_BIT;
                    break;
                case 16:
                    doubleArray[i] = buffer.getShort() / NORM_16_BIT;
                    break;
                case 32:
                    if (encoding == AudioFormat.Encoding.PCM_FLOAT) {
                        doubleArray[i] = buffer.getFloat();
                    } else {
                        doubleArray[i] = buffer.getInt() / NORM_32_BIT_INT;
                    }
                    break;
                case 64:
                     if (encoding == AudioFormat.Encoding.PCM_FLOAT) {
                        doubleArray[i] = buffer.getDouble();
                    } else {
                         throw new UnsupportedOperationException("64-bit integer PCM is not supported.");
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported bit depth: " + bitDepth);
            }
        }
        return doubleArray;
    }

    private byte[] toByteArray(double[] doubleArray, int byteLength) {
        ByteBuffer buffer = ByteBuffer.allocate(byteLength);
        buffer.order(this.bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (double sample : doubleArray) {
            double clampedSample = Math.max(-1.0, Math.min(1.0, sample));

            switch (bitDepth) {
                case 8:
                    buffer.put((byte) ((clampedSample * NORM_8_BIT) + (NORM_8_BIT+1)));
                    break;
                case 16:
                    buffer.putShort((short) (clampedSample * NORM_16_BIT));
                    break;
                case 32:
                    if (encoding == AudioFormat.Encoding.PCM_FLOAT) {
                        buffer.putFloat((float) clampedSample);
                    } else {
                        buffer.putInt((int) (clampedSample * NORM_32_BIT_INT));
                    }
                    break;
                case 64:
                    if (encoding == AudioFormat.Encoding.PCM_FLOAT) {
                        buffer.putDouble(clampedSample);
                    } else {
                         throw new UnsupportedOperationException("64-bit integer PCM is not supported.");
                    }
                    break;
                default:
                     throw new UnsupportedOperationException("Unsupported bit depth: " + bitDepth);
            }
        }
        return buffer.array();
    }
}