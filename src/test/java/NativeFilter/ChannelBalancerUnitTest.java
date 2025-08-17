package NativeFilter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ChannelBalancer class.
 * These tests focus on the Java-level logic, such as constructor behavior,
 * state management (getters/setters), and input validation, without
 * invoking the native processing method.
 */
class ChannelBalancerUnitTest {

    private static final int CHANNELS = 2;
    private static final int BUFFER_SIZE = 1024;
    private static final double SAMPLE_RATE = 44100.0;

    @Test
    @DisplayName("Default constructor should initialize properties correctly")
    void testDefaultConstructor() {
        ChannelBalancer balancer = new ChannelBalancer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);

        assertEquals(CHANNELS, balancer.getChannels());
        assertEquals(BUFFER_SIZE, balancer.getBufferSize());
        assertEquals(SAMPLE_RATE, balancer.getSampleRate());
        assertEquals(0.0, balancer.getPreference(), "Default preference should be 0.0");
    }

    @Test
    @DisplayName("Parameterized constructor should set preference correctly")
    void testParameterizedConstructor() {
        double preference = 0.75;
        ChannelBalancer balancer = new ChannelBalancer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE, preference);
        assertEquals(preference, balancer.getPreference());
    }
    
    @Test
    @DisplayName("process should throw exception for null input")
    void testProcessWithNullInput() {
        ChannelBalancer balancer = new ChannelBalancer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        Exception e = assertThrows(IllegalArgumentException.class, () -> balancer.process(null));
        assertEquals("Input buffer must not be null.", e.getMessage());
    }

    @Test
    @DisplayName("process should throw exception for mismatched buffer size")
    void testProcessWithMismatchedBufferSize() {
        ChannelBalancer balancer = new ChannelBalancer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        double[] wrongSizeBuffer = new double[1];
        Exception e = assertThrows(IllegalArgumentException.class, () -> balancer.process(wrongSizeBuffer));
        assertTrue(e.getMessage().contains("Buffer size mismatch"));
    }

    @Test
    @DisplayName("process should throw exception for invalid channel count")
    void testProcessWithInvalidChannels() {
        ChannelBalancer balancer = new ChannelBalancer(0, BUFFER_SIZE, SAMPLE_RATE);
        double[] buffer = new double[BUFFER_SIZE];
        Exception e = assertThrows(IllegalArgumentException.class, () -> balancer.process(buffer));
        assertEquals("Channel count must be positive.", e.getMessage());
    }

    @Test
    @DisplayName("process should return a new buffer and not modify the original")
    void testProcessReturnsNewBuffer() {
        ChannelBalancer balancer = new ChannelBalancer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        double[] originalBuffer = new double[BUFFER_SIZE];
        originalBuffer[0] = 1.0;
        double[] originalBufferCopy = originalBuffer.clone();

        double[] processedBuffer = balancer.process(originalBuffer);

        assertNotSame(originalBuffer, processedBuffer, "The returned buffer should be a new instance.");
        assertArrayEquals(originalBufferCopy, originalBuffer, "The original input buffer should not be modified.");
    }
}
