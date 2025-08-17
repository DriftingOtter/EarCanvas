package NativeFilter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Limiter class.
 * These tests focus on the Java-level logic, such as constructor behavior,
 * state management (getters/setters), and input validation, without
 * invoking the native processing method.
 */
class LimiterUnitTest {

    private static final int CHANNELS = 2;
    private static final int BUFFER_SIZE = 1024;
    private static final double SAMPLE_RATE = 44100.0;

    @Test
    @DisplayName("Default constructor should initialize properties with default values")
    void testDefaultConstructor() {
        Limiter limiter = new Limiter(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);

        assertEquals(CHANNELS, limiter.getChannels());
        assertEquals(BUFFER_SIZE, limiter.getBufferSize());
        assertEquals(SAMPLE_RATE, limiter.getSampleRate());
        assertEquals(-0.1, limiter.getThreshold());
        assertEquals(0.1, limiter.getAttackTime());
        assertEquals(2.0, limiter.getReleaseTime());
        assertEquals(100.0, limiter.getLookahead());
    }

    @Test
    @DisplayName("Parameterized constructor should set all properties correctly")
    void testParameterizedConstructor() {
        Limiter limiter = new Limiter(CHANNELS, BUFFER_SIZE, SAMPLE_RATE, -6.0, 5.0, 50.0, 10.0);

        assertEquals(-6.0, limiter.getThreshold());
        assertEquals(5.0, limiter.getAttackTime());
        assertEquals(50.0, limiter.getReleaseTime());
        assertEquals(10.0, limiter.getLookahead());
    }

    @Test
    @DisplayName("process should throw exception for null input")
    void testProcessWithNullInput() {
        Limiter limiter = new Limiter(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        Exception e = assertThrows(IllegalArgumentException.class, () -> limiter.process(null));
        assertEquals("Input buffer must not be null.", e.getMessage());
    }

    @Test
    @DisplayName("process should throw exception for mismatched buffer size")
    void testProcessWithMismatchedBufferSize() {
        Limiter limiter = new Limiter(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        double[] wrongSizeBuffer = new double[BUFFER_SIZE + 4];
        Exception e = assertThrows(IllegalArgumentException.class, () -> limiter.process(wrongSizeBuffer));
        assertTrue(e.getMessage().contains("Buffer size mismatch"));
    }

    @Test
    @DisplayName("process should throw exception for invalid channel count")
    void testProcessWithInvalidChannels() {
        Limiter limiter = new Limiter(0, BUFFER_SIZE, SAMPLE_RATE); // Invalid channels
        double[] buffer = new double[BUFFER_SIZE];
        Exception e = assertThrows(IllegalArgumentException.class, () -> limiter.process(buffer));
        assertEquals("Channel count must be positive.", e.getMessage());
    }

    @Test
    @DisplayName("process should return a new buffer and not modify the original")
    void testProcessReturnsNewBuffer() {
        Limiter limiter = new Limiter(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        double[] originalBuffer = new double[BUFFER_SIZE];
        originalBuffer[0] = 1.0;
        double[] originalBufferCopy = originalBuffer.clone();

        double[] processedBuffer = limiter.process(originalBuffer);

        assertNotSame(originalBuffer, processedBuffer, "The returned buffer should be a new instance.");
        assertArrayEquals(originalBufferCopy, originalBuffer, "The original input buffer should not be modified.");
    }
}
