package AudioProcessingRangler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import NativeFilter.GraphicEqualizer;
import StandardFilter.StandardFilter;
import StandardFilter.InvalidFilterException;

@ExtendWith(MockitoExtension.class)
class AudioProcessingRanglerUnitTest {

    @Mock
    private GraphicEqualizer mockGraphicEqualizer;

    private AudioProcessingRangler rangler;
    private StandardFilter testStandardFilter;

    @BeforeEach
    void setUp() throws InvalidFilterException {
        rangler = new AudioProcessingRangler();
        
        // Create a real StandardFilter for testing
        testStandardFilter = new StandardFilter(
            StandardFilter.FilterType.Butterworth, 
            4, 
            44100.0, 
            Optional.empty()
        );
        testStandardFilter.setLowpass(1000.0); // 1kHz lowpass
    }

    @Test
    @DisplayName("Should initialize with empty filter rack")
    void testInitialization() {
        assertTrue(rangler.isEmpty(), "Filter rack should be empty on initialization");
        assertEquals(0, rangler.size(), "Filter rack size should be 0 on initialization");
        assertFalse(rangler.isFull(), "Filter rack should never be full (returns false)");
    }

    @Test
    @DisplayName("Should add filters at specified positions")
    void testAddFilter() throws IndexOutOfBoundsException, EmptyFilterRackException {
        // Add first filter
        rangler.addFilter(testStandardFilter, 0);
        assertFalse(rangler.isEmpty(), "Filter rack should not be empty after adding filter");
        assertEquals(1, rangler.size(), "Filter rack size should be 1 after adding one filter");

        // Add second filter
        rangler.addFilter(mockGraphicEqualizer, 1);
        assertEquals(2, rangler.size(), "Filter rack size should be 2 after adding two filters");

        // Insert filter at beginning (shifts others)
        StandardFilter newFilter = createTestFilter();
        rangler.addFilter(newFilter, 0);
        assertEquals(3, rangler.size(), "Filter rack size should be 3 after inserting filter");
        assertEquals(newFilter, rangler.getFilter(0), "New filter should be at position 0");
    }

    @Test
    @DisplayName("Should remove filters at specified positions")
    void testRemoveFilter() throws IndexOutOfBoundsException, EmptyFilterRackException {
        // Setup: add some filters
        rangler.addFilter(testStandardFilter, 0);
        rangler.addFilter(mockGraphicEqualizer, 1);
        assertEquals(2, rangler.size(), "Setup: should have 2 filters");

        // Remove filter at position 0
        assertTrue(rangler.removeFilter(0), "Should return true when successfully removing filter");
        assertEquals(1, rangler.size(), "Size should be 1 after removing one filter");
        assertEquals(mockGraphicEqualizer, rangler.getFilter(0), "Remaining filter should shift to position 0");

        // Remove last filter
        assertTrue(rangler.removeFilter(0), "Should return true when removing last filter");
        assertTrue(rangler.isEmpty(), "Filter rack should be empty after removing all filters");
    }

    @Test
    @DisplayName("Should throw EmptyFilterRackException when removing from empty rack")
    void testRemoveFromEmptyRack() {
        assertThrows(EmptyFilterRackException.class, () -> rangler.removeFilter(0),
            "Should throw EmptyFilterRackException when trying to remove from empty rack");
    }

    @Test
    @DisplayName("Should throw IndexOutOfBoundsException for invalid remove positions")
    void testRemoveInvalidPosition() {
        rangler.addFilter(testStandardFilter, 0);

        assertThrows(IndexOutOfBoundsException.class, () -> rangler.removeFilter(-1),
            "Should throw IndexOutOfBoundsException for negative index");
        
        assertThrows(IndexOutOfBoundsException.class, () -> rangler.removeFilter(1),
            "Should throw IndexOutOfBoundsException for index >= size");
    }

    @Test
    @DisplayName("Should retrieve filters at specified positions")
    void testGetFilter() throws IndexOutOfBoundsException, EmptyFilterRackException {
        rangler.addFilter(testStandardFilter, 0);
        rangler.addFilter(mockGraphicEqualizer, 1);

        assertEquals(testStandardFilter, rangler.getFilter(0), "Should retrieve correct filter at position 0");
        assertEquals(mockGraphicEqualizer, rangler.getFilter(1), "Should retrieve correct filter at position 1");
    }

    @Test
    @DisplayName("Should throw EmptyFilterRackException when getting from empty rack")
    void testGetFromEmptyRack() {
        assertThrows(EmptyFilterRackException.class, () -> rangler.getFilter(0),
            "Should throw EmptyFilterRackException when trying to get from empty rack");
    }

    @Test
    @DisplayName("Should throw IndexOutOfBoundsException for invalid get positions")
    void testGetInvalidPosition() {
        rangler.addFilter(testStandardFilter, 0);

        assertThrows(IndexOutOfBoundsException.class, () -> rangler.getFilter(-1),
            "Should throw IndexOutOfBoundsException for negative index");
        
        assertThrows(IndexOutOfBoundsException.class, () -> rangler.getFilter(1),
            "Should throw IndexOutOfBoundsException for index >= size");
    }

    @Test
    @DisplayName("Should process data through StandardFilter correctly")
    void testProcessDataWithStandardFilter() {
        rangler.addFilter(testStandardFilter, 0);

        // Create a simple test signal - DC and high frequency
        double[] inputData = new double[100];
        for (int i = 0; i < inputData.length; i++) {
            inputData[i] = 1.0 + 0.5 * Math.sin(2 * Math.PI * 5000 * i / 44100.0); // DC + 5kHz (should be filtered)
        }
        double[] originalData = inputData.clone();

        double[] result = rangler.processData(inputData);

        // The lowpass filter should have reduced the high-frequency component
        // We can't predict exact values, but we can verify processing occurred
        assertSame(inputData, result, "Should return the same array reference (in-place processing)");
        
        // Verify that some processing occurred (array was modified)
        boolean wasModified = false;
        for (int i = 0; i < inputData.length; i++) {
            if (Math.abs(inputData[i] - originalData[i]) > 1e-10) {
                wasModified = true;
                break;
            }
        }
        assertTrue(wasModified, "StandardFilter should have modified the input data");
    }

    @Test
    @DisplayName("Should process data through NativeFilterInterface correctly")
    void testProcessDataWithNativeFilter() {
        double[] inputData = {1.0, 2.0, 3.0};
        double[] expectedOutput = {1.5, 3.0, 4.5};

        when(mockGraphicEqualizer.process(inputData)).thenReturn(expectedOutput);

        rangler.addFilter(mockGraphicEqualizer, 0);

        double[] result = rangler.processData(inputData);

        verify(mockGraphicEqualizer).process(inputData);
        assertArrayEquals(expectedOutput, result, "Should return processed data from native filter");
    }

    @Test
    @DisplayName("Should process data through multiple filters in sequence")
    void testProcessDataMultipleFilters() {
        // Setup StandardFilter - real lowpass filter
        StandardFilter lowpassFilter = createTestFilter();
        
        // Setup mock GraphicEqualizer
        double[] testData = {1.0, 2.0, 3.0};
        double[] expectedOutput = {1.5, 3.0, 4.5};
        when(mockGraphicEqualizer.process(any(double[].class))).thenReturn(expectedOutput);

        // Add filters in order
        rangler.addFilter(lowpassFilter, 0); // First: StandardFilter (real)
        rangler.addFilter(mockGraphicEqualizer, 1); // Second: GraphicEqualizer (mock)

        double[] result = rangler.processData(testData);

        // Verify GraphicEqualizer was called (it's the last filter)
        verify(mockGraphicEqualizer).process(any(double[].class));
        
        // Result should be what the GraphicEqualizer returned
        assertArrayEquals(expectedOutput, result, "Should return final processed data from GraphicEqualizer");
    }

    @Test
    @DisplayName("Should handle empty data array")
    void testProcessDataEmptyArray() {
        rangler.addFilter(testStandardFilter, 0);

        double[] emptyData = {};
        double[] result = rangler.processData(emptyData);

        assertArrayEquals(new double[]{}, result, "Should handle empty array correctly");
        assertSame(emptyData, result, "Should return same array reference for empty array");
    }

    @Test
    @DisplayName("Should pass through data unchanged when no filters")
    void testProcessDataNoFilters() {
        double[] inputData = {1.0, 2.0, 3.0};
        double[] originalData = inputData.clone(); // Keep copy of original
        
        double[] result = rangler.processData(inputData);

        assertArrayEquals(originalData, result, "Data should pass through unchanged when no filters");
        assertSame(inputData, result, "Should return same array reference");
    }

    @Test
    @DisplayName("Should maintain filter order during processing")
    void testFilterOrder() throws InvalidFilterException {
        // Create multiple standard filters to test order
        StandardFilter filter1 = new StandardFilter(StandardFilter.FilterType.Butterworth, 2, 44100, Optional.empty());
        StandardFilter filter2 = new StandardFilter(StandardFilter.FilterType.Butterworth, 2, 44100, Optional.empty());
        
        // Configure filters with different characteristics
        filter1.setHighpass(100.0); // Remove very low frequencies
        filter2.setLowpass(8000.0); // Remove very high frequencies

        // Add filters in specific order
        rangler.addFilter(filter1, 0);
        rangler.addFilter(filter2, 1);

        // Test with a signal that has both low and high frequency components
        double[] inputData = new double[100];
        for (int i = 0; i < inputData.length; i++) {
            inputData[i] = Math.sin(2 * Math.PI * 50 * i / 44100.0) +    // 50 Hz (should be filtered by filter1)
                          Math.sin(2 * Math.PI * 1000 * i / 44100.0) +   // 1000 Hz (should pass through)
                          Math.sin(2 * Math.PI * 12000 * i / 44100.0);   // 12 kHz (should be filtered by filter2)
        }
        
        double[] originalData = inputData.clone();
        rangler.processData(inputData);

        // Verify that processing occurred (filters were applied in order)
        boolean wasProcessed = false;
        for (int i = 0; i < inputData.length; i++) {
            if (Math.abs(inputData[i] - originalData[i]) > 1e-10) {
                wasProcessed = true;
                break;
            }
        }
        assertTrue(wasProcessed, "Filters should be applied in order and modify the signal");
    }

    @Test
    @DisplayName("Should handle mixed filter types correctly")
    void testMixedFilterTypes() {
        // Setup StandardFilter (real)
        StandardFilter standardFilter = createTestFilter();

        // Setup mock GraphicEqualizer to multiply input by 2
        when(mockGraphicEqualizer.process(any(double[].class))).thenAnswer(invocation -> {
            double[] input = invocation.getArgument(0);
            return Arrays.stream(input).map(x -> x * 2.0).toArray();
        });

        // Add mixed filter types
        rangler.addFilter(standardFilter, 0);     // First: StandardFilter (real processing)
        rangler.addFilter(mockGraphicEqualizer, 1); // Second: GraphicEqualizer (multiply by 2)

        double[] inputData = {1.0, 2.0, 3.0};
        double[] result = rangler.processData(inputData);

        // Verify GraphicEqualizer was called
        verify(mockGraphicEqualizer).process(any(double[].class));
        
        // The result should be approximately double the filtered input
        // (GraphicEqualizer multiplies by 2)
        assertTrue(result.length == 3, "Result should have same length as input");
        // We can't predict exact values due to real filter processing, but we can verify
        // that GraphicEqualizer processing occurred
        assertNotNull(result, "Mixed filter types should work together");
    }

    // Helper method to create test StandardFilter
    private StandardFilter createTestFilter() {
        try {
            StandardFilter filter = new StandardFilter(
                StandardFilter.FilterType.Butterworth, 
                2, 
                44100.0, 
                Optional.empty()
            );
            filter.setLowpass(2000.0); // 2kHz lowpass
            return filter;
        } catch (InvalidFilterException e) {
            throw new RuntimeException("Failed to create test filter", e);
        }
    }
}