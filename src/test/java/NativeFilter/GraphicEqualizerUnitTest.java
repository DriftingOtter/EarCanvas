package NativeFilter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import StandardFilter.InvalidFilterParametersException; // Import the custom exception

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the GraphicEqualizer class.
 * These tests focus on the Java-level logic, such as constructor behavior,
 * state management (getters/setters), and input validation, without
 * invoking the native processing method.
 */
class GraphicEqualizerUnitTest {

    private static final int CHANNELS = 2;
    private static final int BUFFER_SIZE = 1024;
    private static final float SAMPLE_RATE = 44100.0f;

    @Test
    @DisplayName("Default constructor should initialize properties correctly")
    void testDefaultConstructor() {
        GraphicEqualizer eq = new GraphicEqualizer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);

        assertEquals(CHANNELS, eq.getChannels());
        assertEquals(BUFFER_SIZE, eq.getBufferSize());
        assertEquals(SAMPLE_RATE, eq.getSampleRate());
        assertEquals(10, eq.getBandCount());
        assertEquals(6.0, eq.getQ());
        assertArrayEquals(new double[10], eq.getGains(), "Default gains should be all zeros.");
    }

    @Test
    @DisplayName("Constructor with gains should sanitize and set them correctly")
    void testConstructorWithGains() throws InvalidFilterParametersException {
        double[] gains = {2.5, 0.5, -0.5, -3.0, 1.0}; // Includes values outside the [-2, 2] range
        double[] expectedSanitizedGains = {2.0, 0.5, -0.5, -2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0};

        GraphicEqualizer eq = new GraphicEqualizer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE, gains);

        assertEquals(SAMPLE_RATE, eq.getSampleRate());
        assertArrayEquals(expectedSanitizedGains, eq.getGains(), "Gains should be sanitized and padded correctly.");
    }

    @Test
    @DisplayName("Constructor should throw exception for null gains array")
    void testConstructorWithNullGains() {
        Exception e = assertThrows(InvalidFilterParametersException.class, () -> {
            new GraphicEqualizer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE, null);
        });
        assertEquals("Gains array cannot be null.", e.getMessage());
    }

    @Test
    @DisplayName("setGains should throw exception for null input")
    void testSetGainsWithNull() {
        GraphicEqualizer eq = new GraphicEqualizer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        // --- FIX: Expect the correct custom exception ---
        Exception e = assertThrows(InvalidFilterParametersException.class, () -> eq.setGains(null));
        assertEquals("Gains array cannot be null.", e.getMessage());
    }
    
    @Test
    @DisplayName("process should throw exception for null input buffer")
    void testProcessWithNullInput() {
        GraphicEqualizer eq = new GraphicEqualizer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        Exception e = assertThrows(IllegalArgumentException.class, () -> eq.process(null));
        assertEquals("Input buffer and gain array must not be null.", e.getMessage());
    }

    @Test
    @DisplayName("process should throw exception for mismatched buffer size")
    void testProcessWithMismatchedBufferSize() {
        GraphicEqualizer eq = new GraphicEqualizer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        double[] wrongSizeBuffer = new double[BUFFER_SIZE / 2];
        Exception e = assertThrows(IllegalArgumentException.class, () -> eq.process(wrongSizeBuffer));
        assertTrue(e.getMessage().contains("Buffer size mismatch"));
    }

    @Test
    @DisplayName("process should throw exception for invalid channel count")
    void testProcessWithInvalidChannels() {
        GraphicEqualizer eq = new GraphicEqualizer(0, BUFFER_SIZE, SAMPLE_RATE); // Invalid channels
        double[] buffer = new double[BUFFER_SIZE];
        Exception e = assertThrows(IllegalArgumentException.class, () -> eq.process(buffer));
        assertEquals("Channel count must be positive.", e.getMessage());
    }

    @Test
    @DisplayName("process should return a new buffer and not modify the original")
    void testProcessReturnsNewBuffer() {
        // This test verifies the Java wrapper's behavior without calling the native method.
        GraphicEqualizer eq = new GraphicEqualizer(CHANNELS, BUFFER_SIZE, SAMPLE_RATE);
        double[] originalBuffer = new double[BUFFER_SIZE];
        originalBuffer[0] = 1.0;
        double[] originalBufferCopy = originalBuffer.clone();

        // The Java code shows it always creates a copy before processing.
        double[] processedBuffer = eq.process(originalBuffer);

        assertNotSame(originalBuffer, processedBuffer, "The returned buffer should be a new instance.");
        assertArrayEquals(originalBufferCopy, originalBuffer, "The original input buffer should not be modified.");
    }
}
