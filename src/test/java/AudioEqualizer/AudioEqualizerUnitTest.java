package AudioEqualizer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import IIRFilter.IIRFilter;
import IIRFilter.InvalidFilterException;
import uk.me.berndporr.iirj.Butterworth;


public class AudioEqualizerUnitTest {

    private AudioEqualizer equalizer;
    private final double sampleRate = 44100;
    private final int order = 4;

    // This method runs before each test, ensuring a clean equalizer instance for each test.
    @BeforeEach
    public void setUp() {
        equalizer = new AudioEqualizer();
    }

    @Test
    public void testInitialState() {
        assertTrue(equalizer.isEmpty(), "A new equalizer should be empty.");
        assertEquals(0, equalizer.size(), "A new equalizer should have a size of 0.");
        assertFalse(equalizer.isFull(), "The equalizer should never report as full.");
    }

    @Test
    public void testAddAndGetFilter() throws InvalidFilterException, EmptyFilterRackException, IndexOutOfBoundsException {
        // 1. Create a Butterworth filter instance
        IIRFilter butterworthFilter = new IIRFilter(IIRFilter.FilterType.Butterworth, order, sampleRate, Optional.empty());
        
        // 2. Add the filter to the equalizer
        equalizer.addFilter(butterworthFilter, 0);

        // 3. Verify the state of the equalizer
        assertEquals(1, equalizer.size());
        assertFalse(equalizer.isEmpty());
        
        // 4. Retrieve the filter and verify it's the correct one
        IIRFilter retrievedFilter = equalizer.getFilter(0);
        assertSame(butterworthFilter, retrievedFilter, "The retrieved filter should be the same instance that was added.");
        assertInstanceOf(Butterworth.class, retrievedFilter.getSettings(), "The filter's settings should be of type Butterworth.");
    }

    @Test
    public void testAddMultipleFilters() throws InvalidFilterException, EmptyFilterRackException, IndexOutOfBoundsException {
        // Create multiple filter instances
        IIRFilter filter1 = new IIRFilter(IIRFilter.FilterType.Butterworth, order, sampleRate, Optional.empty());
        IIRFilter filter2 = new IIRFilter(IIRFilter.FilterType.ChebyshevI, order, sampleRate, Optional.of(0.5));

        // Add filters to the rack
        equalizer.addFilter(filter1, 0);
        equalizer.addFilter(filter2, 1);
        assertEquals(2, equalizer.size(), "Equalizer should contain two filters.");

        // Verify the correct filters are in the correct positions
        assertSame(filter1, equalizer.getFilter(0));
        assertSame(filter2, equalizer.getFilter(1));
        assertEquals(0.5, equalizer.getFilter(1).getRippleDb());
    }


    @Test
    public void testRemoveFilter() throws InvalidFilterException, EmptyFilterRackException, IndexOutOfBoundsException {
        // Add two filters
        IIRFilter butterworthFilter = new IIRFilter(IIRFilter.FilterType.Butterworth, order, sampleRate, Optional.empty());
        IIRFilter besselFilter = new IIRFilter(IIRFilter.FilterType.Bessel, order, sampleRate, Optional.empty());
        equalizer.addFilter(butterworthFilter, 0);
        equalizer.addFilter(besselFilter, 1);
        assertEquals(2, equalizer.size());

        // Remove the second filter (Bessel)
        assertTrue(equalizer.removeFilter(1), "removeFilter should return true on success.");
        assertEquals(1, equalizer.size(), "Size should be 1 after removing a filter.");

        // Verify the correct filter (Butterworth) remains
        IIRFilter remainingFilter = equalizer.getFilter(0);
        assertInstanceOf(Butterworth.class, remainingFilter.getSettings());

        // Remove the last filter
        equalizer.removeFilter(0);
        assertTrue(equalizer.isEmpty(), "Equalizer should be empty after removing the last filter.");
    }

    @Test
    public void testModifyFilterAfterAdding() throws InvalidFilterException, EmptyFilterRackException, IndexOutOfBoundsException {
        // Create and add a filter
        IIRFilter filter = new IIRFilter(IIRFilter.FilterType.ChebyshevI, order, sampleRate, Optional.of(1.0));
        equalizer.addFilter(filter, 0);

        // Retrieve the filter from the rack and modify it
        IIRFilter filterToModify = equalizer.getFilter(0);
        assertDoesNotThrow(() -> filterToModify.setHighpass(1000), "Should be able to modify a filter after retrieval.");
        
        // This test primarily ensures that operations can be performed on a retrieved filter.
        // A more complex test could process data to verify the modification was applied.
    }

    @Test
    public void testExceptionHandling() throws InvalidFilterException {
        // Test operations on an empty rack
        assertThrows(EmptyFilterRackException.class, () -> equalizer.removeFilter(0), "Should throw when removing from an empty rack.");
        assertThrows(EmptyFilterRackException.class, () -> equalizer.getFilter(0), "Should throw when getting from an empty rack.");

        // Add a filter to test out-of-bounds exceptions
        equalizer.addFilter(new IIRFilter(IIRFilter.FilterType.Bessel, 2, sampleRate, Optional.empty()), 0);

        // Test out-of-bounds access
        assertThrows(IndexOutOfBoundsException.class, () -> equalizer.removeFilter(1), "Should throw for index >= size.");
        assertThrows(IndexOutOfBoundsException.class, () -> equalizer.removeFilter(-1), "Should throw for a negative index.");
        assertThrows(IndexOutOfBoundsException.class, () -> equalizer.getFilter(1), "Should throw for index >= size.");
        assertThrows(IndexOutOfBoundsException.class, () -> equalizer.getFilter(-1), "Should throw for a negative index.");
    }

    @Test
    public void testProcessDataWithEmptyRack() {
        double[] input = {0.1, 0.2, 0.3, 0.4, 0.5};
        double[] originalInput = input.clone();

        // Processing with an empty rack should not alter the data
        double[] output = equalizer.processData(input);
        assertArrayEquals(originalInput, output, "Processing with an empty rack should not change the data.");
    }

    @Test
    public void testProcessDataWithActiveFilter() throws InvalidFilterException {
        double[] input = {0.1, -0.2, 0.3, -0.4, 0.5};
        double[] originalInput = input.clone();

        // Create a strong low-pass filter
        IIRFilter filter = new IIRFilter(IIRFilter.FilterType.Butterworth, 4, sampleRate, Optional.empty());
        filter.setLowpass(100); // Set a low cutoff frequency
        equalizer.addFilter(filter, 0);

        // Process the data
        double[] output = equalizer.processData(input);

        // Verify that the filter has modified the data
        assertEquals(originalInput.length, output.length, "Output array length must match input array length.");
        assertFalse(Arrays.equals(originalInput, output), "Processing with an active filter should modify the data.");
    }
}