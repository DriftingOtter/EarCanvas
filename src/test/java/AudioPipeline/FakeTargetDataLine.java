package AudioPipeline;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.TargetDataLine;

/**
 * Processing properties:
 * 1. Provide a predefined byte array to simulated audio stream.
 * 2. Be configured to block on read, to test the 'running' state.
 * 3. Use a CountDownLatch to signal when its read() method has been entered by another thread.
 */
public class FakeTargetDataLine implements TargetDataLine {
    private final AudioFormat format;
    private final byte[] dataToRead;
    private final boolean blockOnRead;
    private final CountDownLatch readEnteredLatch = new CountDownLatch(1);

    private int readPosition = 0;
    private boolean isOpen = false;
    private boolean isRunning = false;

    public FakeTargetDataLine(AudioFormat format, byte[] dataToRead, boolean blockOnRead) {
        this.format = format;
        this.dataToRead = dataToRead;
        this.blockOnRead = blockOnRead;
    }

    // Allows a test thread to wait until the pipeline thread has called the read() method.
    public boolean awaitRead(long timeout, TimeUnit unit) throws InterruptedException {
        return readEnteredLatch.await(timeout, unit);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        readEnteredLatch.countDown(); // Signal that this method was entered.
        
        if (blockOnRead) {
            try {
                // Block indefinitely to let the test control execution.
                // The pipeline's stop() method will interrupt this thread.
                Thread.sleep(5000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve interrupted status
                return -1;
            }
        }
        
        if (readPosition >= dataToRead.length) {
            return -1; // Simulate end of stream
        }
        
        int bytesToRead = Math.min(len, dataToRead.length - readPosition);
        System.arraycopy(dataToRead, readPosition, b, off, bytesToRead);
        readPosition += bytesToRead;
        
        return bytesToRead;
    }

    @Override public void open(AudioFormat format, int bufferSize) { this.isOpen = true; }
    @Override public void close() { this.isOpen = false; this.isRunning = false; }
    @Override public void start() { this.isRunning = true; }
    @Override public void stop() { this.isRunning = false; }
    @Override public boolean isRunning() { return this.isRunning; }
    @Override public boolean isOpen() { return this.isOpen; }
    @Override public AudioFormat getFormat() { return this.format; }
    @Override public int getBufferSize() { return 2048; }

    // --- Unused Methods (Required by Interface) ---
    @Override public void open(AudioFormat format) { /* Unused by AudioPipeline */ this.isOpen = true; }
    @Override public void open() { /* Unused by AudioPipeline */ this.isOpen = true; }
    @Override public boolean isActive() { /* Unused by AudioPipeline */ return this.isRunning; }
    @Override public int available() { /* Unused by AudioPipeline */ return 0; }
    @Override public void drain() { /* Unused by AudioPipeline */ }
    @Override public void flush() { /* Unused by AudioPipeline */ }
    @Override public int getFramePosition() { /* Unused by AudioPipeline */ return 0; }
    @Override public long getLongFramePosition() { /* Unused by AudioPipeline */ return 0; }
    @Override public long getMicrosecondPosition() { /* Unused by AudioPipeline */ return 0; }
    @Override public float getLevel() { /* Unused by AudioPipeline */ return 0; }
    @Override public Line.Info getLineInfo() { /* Unused by AudioPipeline */ return null; }
    @Override public void addLineListener(LineListener listener) { /* Unused by AudioPipeline */ }
    @Override public void removeLineListener(LineListener listener) { /* Unused by AudioPipeline */ }
    @Override public Control[] getControls() { /* Unused by AudioPipeline */ return new Control[0]; }
    @Override public boolean isControlSupported(Control.Type control) { /* Unused by AudioPipeline */ return false; }
    @Override public Control getControl(Control.Type control) { /* Unused by AudioPipeline */ return null; }
}
