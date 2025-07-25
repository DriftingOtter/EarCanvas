package AudioEqualizer;

import Filter.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.me.berndporr.iirj.Bessel;
import uk.me.berndporr.iirj.Butterworth;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AudioEqualizerUnitTest {
    private AudioEqualizer equalizer;
    private final double sampleRate = 44100;
    private final int order = 4;

    // This method runs before each test, ensuring a clean equalizer instance.
    @BeforeEach
    public void setUp() {
        equalizer = new AudioEqualizer();
    }

    @Test
    public void testInitialState() {
        assertTrue(equalizer.isEmpty(), "New equalizer should be empty.");
        assertEquals(0, equalizer.size(), "New equalizer should have size 0.");
        assertFalse(equalizer.isFull(), "Equalizer should never be full.");
    }

    @Test
    public void testAddFilter() {
        // Add a Butterworth filter and verify state
        assertDoesNotThrow(() -> {
            Filter filter1 = equalizer.addFilter(Filter.FilterType.Butterworth, order, sampleRate, Optional.empty(), 0);
            assertNotNull(filter1);
            assertEquals(order, filter1.getOrder());
        });
        assertEquals(1, equalizer.size());
        assertFalse(equalizer.isEmpty());

        // Add a Chebyshev filter with ripple and verify state
        assertDoesNotThrow(() -> {
            Filter filter2 = equalizer.addFilter(Filter.FilterType.ChebyshevI, order, sampleRate, Optional.of(0.5), 1);
            assertNotNull(filter2);
            assertEquals(0.5, filter2.getRippleDb());
        });
        assertEquals(2, equalizer.size());
    }

    @Test
    public void testGetFilter() {
        // Add two different filters
        assertDoesNotThrow(() -> {
            equalizer.addFilter(Filter.FilterType.Butterworth, order, sampleRate, Optional.empty(), 0);
            equalizer.addFilter(Filter.FilterType.Bessel, order, sampleRate, Optional.empty(), 1);
        });

        // Assert we get the correct type of filter settings from the specified position
        assertDoesNotThrow(() -> {
            Filter f1 = equalizer.getFilter(0);
            Filter f2 = equalizer.getFilter(1);
            assertInstanceOf(Butterworth.class, f1.getSettings());
            assertInstanceOf(Bessel.class, f2.getSettings());
        });
    }

    @Test
    public void testRemoveFilter() {
        // Add filters
        assertDoesNotThrow(() -> {
            equalizer.addFilter(Filter.FilterType.Butterworth, order, sampleRate, Optional.empty(), 0);
            equalizer.addFilter(Filter.FilterType.Bessel, order, sampleRate, Optional.empty(), 1);
        });
        assertEquals(2, equalizer.size());

        // Test removing a filter successfully
        assertDoesNotThrow(() -> equalizer.removeFilter(1)); // Remove Bessel
        assertEquals(1, equalizer.size());

        // Check that the correct filter remains
        assertDoesNotThrow(() -> {
            Filter filter = equalizer.getFilter(0);
            assertInstanceOf(Butterworth.class, filter.getSettings());
        });

        // Remove the last filter
        assertDoesNotThrow(() -> equalizer.removeFilter(0));
        assertTrue(equalizer.isEmpty());
    }

    @Test
    public void testModifyFilter() {
        // Add a filter
        Filter chebyFilter = assertDoesNotThrow(() -> 
            equalizer.addFilter(Filter.FilterType.ChebyshevI, order, sampleRate, Optional.of(1.0), 0)
        );
        
        // Retrieve the filter and modify it. We can't inspect the state easily,
        // but we can ensure no exceptions are thrown for valid operations.
        assertDoesNotThrow(() -> {
            Filter filterToModify = equalizer.getFilter(0);
            filterToModify.setHighpass(1000);
        });

        assertDoesNotThrow(() -> {
            Filter filterToModify = equalizer.getFilter(0);
            filterToModify.setLowpass(500);
        });
        
        assertDoesNotThrow(() -> {
            Filter filterToModify = equalizer.getFilter(0);
            filterToModify.setBandpass(1000, 400);
        });

        assertDoesNotThrow(() -> {
            Filter filterToModify = equalizer.getFilter(0);
            filterToModify.setBandstop(2000, 200);
        });
    }

    @Test
    public void testExceptionHandling() {
        // Test removing from an empty rack
        assertThrows(EmptyFilterRackException.class, () -> equalizer.removeFilter(0));

        // Test getting from an empty rack
        assertThrows(EmptyFilterRackException.class, () -> equalizer.getFilter(0));

        // Add a filter for further tests
        assertDoesNotThrow(() -> equalizer.addFilter(Filter.FilterType.Bessel, 2, sampleRate, Optional.empty(), 0));

        // Test out-of-bounds removal
        assertThrows(InvalidFilterRackPositionException.class, () -> equalizer.removeFilter(1), "Should throw for index >= size");
        assertThrows(InvalidFilterRackPositionException.class, () -> equalizer.removeFilter(-1), "Should throw for negative index");

        // Test out-of-bounds get
        assertThrows(IndexOutOfBoundsException.class, () -> equalizer.getFilter(1));
        assertThrows(IndexOutOfBoundsException.class, () -> equalizer.getFilter(-1));

        // Test adding a filter with an invalid parameter (e.g., ripple for Butterworth)
        // Note: The current Filter class constructor handles this gracefully, but this tests the principle.
        assertDoesNotThrow(() -> equalizer.addFilter(Filter.FilterType.Butterworth, order, sampleRate, Optional.of(1.0), 0));
    }
    
    @Test
    public void processDataEmptyRackTest() {
        double[] input = {0.1, 0.2, 0.3, 0.4, 0.5};
        double[] inputClone = input.clone();

        // Processing with an empty rack should not change the data
        double[] output = equalizer.processData(input);
        assertArrayEquals(inputClone, output);
    }

    @Test
    public void processDataWithFilterTest() {
        double[] input = {0.1, -0.2, 0.3, -0.4, 0.5};
        double[] inputClone = input.clone();

        // Add a filter and set it up (e.g., a low-pass filter)
        Filter filter = assertDoesNotThrow(() -> 
            equalizer.addFilter(Filter.FilterType.Butterworth, 4, sampleRate, Optional.empty(), 0)
        );
        filter.setLowpass(100); // A strong low-pass filter

        // Process the data
        double[] output = equalizer.processData(input);
        
        // The output should be different from the input
        assertFalse(Arrays.equals(inputClone, output), "Processing with a filter should modify the data.");
        assertEquals(inputClone.length, output.length, "Output array length should match input array length.");
    }
}
