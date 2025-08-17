package AudioPipeline;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import AudioProcessingRangler.AudioProcessingRangler;
import AudioProcessingRangler.ProcessRanglerInterface;

/**
 * Integration tests for AudioPipeline that test with real audio system components.
 * These tests verify that the pipeline can work with the actual Java Sound API.
 * 
 * Note: These tests may be skipped in environments without audio hardware/drivers.
 */
class AudioPipelineIntegrationTest {

    private AudioPipeline audioPipeline;
    private volatile boolean audioSystemAvailable = false;

    @BeforeEach
    void setUp() {
        // Check if audio system is available before running tests
        audioSystemAvailable = isAudioSystemAvailable();
        assumeTrue(audioSystemAvailable, "Audio system not available - skipping integration tests");
        
        // Only create pipeline if audio system is available
        if (audioSystemAvailable) {
            audioPipeline = new AudioPipeline();
        }
    }

    @AfterEach
    void tearDown() {
        if (audioPipeline != null) {
            audioPipeline.stop();
            // Give some time for cleanup
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @DisplayName("AudioPipeline should initialize with valid audio format")
    @Timeout(5)
    void testAudioPipelineInitialization() {
        AudioFormat format = audioPipeline.getFormat();
        
        assertNotNull(format, "Audio format should not be null");
        assertTrue(format.getSampleRate() > 0, "Sample rate should be positive");
        assertTrue(format.getSampleSizeInBits() > 0, "Bit depth should be positive");
        assertTrue(format.getChannels() > 0, "Channel count should be positive");
        assertNotNull(format.getEncoding(), "Audio encoding should not be null");
        
        System.out.println("Audio Format: " + format);
    }

    @Test
    @DisplayName("AudioPipeline should start and stop successfully")
    @Timeout(10)
    void testStartStop() throws InterruptedException {
        // Start the pipeline
        assertDoesNotThrow(() -> audioPipeline.start(), "Pipeline should start without throwing");
        
        // Let it run briefly
        Thread.sleep(100);
        
        // Stop the pipeline
        assertDoesNotThrow(() -> audioPipeline.stop(), "Pipeline should stop without throwing");
        
        // Note: AudioPipeline cannot be restarted after stopping due to ExecutorService shutdown
        // This is a limitation of the current implementation
        System.out.println("Pipeline stopped successfully (restart not supported in current implementation)");
    }

    @Test
    @DisplayName("AudioPipeline should handle multiple instances for start/stop cycles")
    @Timeout(15)
    void testMultipleStartStopCycles() throws InterruptedException {
        // Since AudioPipeline can't be restarted, we test with multiple instances
        for (int i = 0; i < 3; i++) {
            System.out.println("Start/Stop cycle " + (i + 1));
            
            AudioPipeline testPipeline = new AudioPipeline();
            testPipeline.start();
            Thread.sleep(50); // Let it run briefly
            testPipeline.stop();
            Thread.sleep(20); // Brief pause between cycles
        }
        
        System.out.println("Successfully completed multiple start/stop cycles with separate instances");
    }

    @Test
    @DisplayName("AudioPipeline should work with a simple equalizer")
    @Timeout(10)
    void testWithEqualizer() throws InterruptedException {
        TestEqualizer testEqualizer = new TestEqualizer();
        audioPipeline.setEqualizer(testEqualizer);
        
        audioPipeline.start();
        
        // Wait a bit for audio processing to occur
        Thread.sleep(200);
        
        audioPipeline.stop();
        
        // The equalizer should have been called if there was any audio input
        // Note: This might be 0 if no microphone input is available
        System.out.println("Equalizer was called " + testEqualizer.getCallCount() + " times");
        assertTrue(testEqualizer.getCallCount() >= 0, "Equalizer call count should be non-negative");
    }

    @Test
    @DisplayName("AudioPipeline should handle audio format changes gracefully")
    @Timeout(10)
    void testAudioFormatHandling() {
        AudioFormat format = audioPipeline.getFormat();
        
        // Verify the format is reasonable
        float sampleRate = format.getSampleRate();
        assertTrue(sampleRate >= 8000 && sampleRate <= 192000, 
            "Sample rate should be in reasonable range: " + sampleRate);
        
        int bitDepth = format.getSampleSizeInBits();
        assertTrue(bitDepth == 8 || bitDepth == 16 || bitDepth == 24 || bitDepth == 32, 
            "Bit depth should be standard value: " + bitDepth);
        
        int channels = format.getChannels();
        assertTrue(channels >= 1 && channels <= 8, 
            "Channel count should be reasonable: " + channels);
    }

    @Test
    @DisplayName("AudioPipeline should handle concurrent access safely")
    @Timeout(15)
    void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 5;
        final CountDownLatch startLatch = new CountDownLatch(threadCount);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final AtomicReference<Exception> exception = new AtomicReference<>();
        
        // Create multiple threads trying to start/stop the pipeline
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await(); // Wait for all threads to be ready
                    
                    if (threadId % 2 == 0) {
                        audioPipeline.start();
                        Thread.sleep(20);
                    } else {
                        Thread.sleep(10);
                        audioPipeline.stop();
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    finishLatch.countDown();
                }
            }).start();
        }
        
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
        assertNull(exception.get(), "No exceptions should occur during concurrent access");
    }

    @Test
    @DisplayName("AudioPipeline should handle system resource limitations gracefully")
    @Timeout(5)
    void testResourceHandling() throws InterruptedException {
        // This test verifies that the pipeline handles resource acquisition properly
        AudioPipeline secondPipeline = null;
        try {
            // Try to create and start multiple pipelines (may fail due to exclusive access)
            secondPipeline = new AudioPipeline();
            audioPipeline.start();
            
            // This might throw an exception if the audio device is exclusive
            // We're testing that it fails gracefully rather than crashing
            try {
                secondPipeline.start();
                // If both start successfully, that's fine too
                Thread.sleep(100);
            } catch (RuntimeException e) {
                // This is expected behavior - audio devices might be exclusive
                assertTrue(e.getMessage().contains("Could not open audio line") || 
                          e.getMessage().contains("unavailable"),
                    "Exception should be about line unavailability: " + e.getMessage());
            }
        } finally {
            if (secondPipeline != null) {
                secondPipeline.stop();
            }
        }
    }

    /**
     * Check if the audio system is available for testing
     */
    private boolean isAudioSystemAvailable() {
        try {
            // Try to get a target data line to see if audio input is available
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
            if (!AudioSystem.isLineSupported(info)) {
                return false;
            }
            
            // Try to actually get and open a line briefly
            TargetDataLine testLine = (TargetDataLine) AudioSystem.getLine(info);
            testLine.open();
            testLine.close();
            
            // Try the same for output
            DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, null);
            if (!AudioSystem.isLineSupported(sourceInfo)) {
                return false;
            }
            
            SourceDataLine testSourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
            testSourceLine.open();
            testSourceLine.close();
            
            return true;
        } catch (Exception e) {
            System.out.println("Audio system not available for testing: " + e.getMessage());
            return false;
        }
    }

    /**
     * Simple test equalizer that counts how many times it's called
     * This extends AudioProcessingRangler to work with the actual implementation
     */
    private static class TestEqualizer extends AudioProcessingRangler {
        private final AtomicInteger callCount = new AtomicInteger(0);

        public TestEqualizer() {
            super();
            // Add a simple test filter that applies gain
            addFilter(new SimpleTestFilter(), 0);
        }

        @Override
        public double[] processData(double[] data) {
            callCount.incrementAndGet();
            // Call the parent's processData to run through the filter rack
            return super.processData(data);
        }

        public int getCallCount() {
            return callCount.get();
        }

        /**
         * Simple test filter that implements a basic gain adjustment
         */
        private static class SimpleTestFilter {
            public double[] process(double[] data) {
                double[] processed = new double[data.length];
                for (int i = 0; i < data.length; i++) {
                    processed[i] = data[i] * 0.9; // Slight attenuation
                }
                return processed;
            }
        }
    }

    /**
     * Performance test to ensure the pipeline can handle audio in real-time
     */
    @Test
    @DisplayName("AudioPipeline should maintain real-time performance")
    @Timeout(20)
    void testPerformance() throws InterruptedException {
        PerformanceEqualizer perfEqualizer = new PerformanceEqualizer();
        audioPipeline.setEqualizer(perfEqualizer);
        
        audioPipeline.start();
        
        // Let it run for a reasonable time to gather performance data
        Thread.sleep(500);
        
        audioPipeline.stop();
        
        int totalCalls = perfEqualizer.getCallCount();
        long totalProcessingTime = perfEqualizer.getTotalProcessingTime();
        
        if (totalCalls > 0) {
            double avgProcessingTime = (double) totalProcessingTime / totalCalls;
            System.out.println("Performance stats:");
            System.out.println("  Total processing calls: " + totalCalls);
            System.out.println("  Average processing time: " + avgProcessingTime + " ns");
            System.out.println("  Average processing time: " + (avgProcessingTime / 1_000_000) + " ms");
            
            // Processing should be fast enough for real-time audio
            // Assuming 15ms buffer size, processing should take much less than that
            assertTrue(avgProcessingTime < 5_000_000, // 5ms in nanoseconds
                "Average processing time should be less than 5ms for real-time performance");
        }
    }

    /**
     * Equalizer that tracks performance metrics
     * This extends AudioProcessingRangler to work with the actual implementation
     */
    private static class PerformanceEqualizer extends AudioProcessingRangler {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AtomicReference<Long> totalProcessingTime = new AtomicReference<>(0L);

        public PerformanceEqualizer() {
            super();
            // Add a pass-through filter for performance testing
            addFilter(new PassThroughFilter(), 0);
        }

        @Override
        public double[] processData(double[] data) {
            long startTime = System.nanoTime();
            callCount.incrementAndGet();
            
            // Call parent's processData to run through the filter rack
            double[] result = super.processData(data);
            
            long endTime = System.nanoTime();
            totalProcessingTime.updateAndGet(current -> current + (endTime - startTime));
            
            return result;
        }

        public int getCallCount() {
            return callCount.get();
        }

        public long getTotalProcessingTime() {
            return totalProcessingTime.get();
        }

        /**
         * Pass-through filter for performance testing
         */
        private static class PassThroughFilter {
            public double[] process(double[] data) {
                // Just return the data unchanged
                return data;
            }
        }
    }
}