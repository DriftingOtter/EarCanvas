package AudioEqualizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.me.berndporr.iirj.*;

import static org.junit.jupiter.api.Assertions.*;

public class AudioEqualizerUnitTest {
	private AudioEqualizer equalizer;

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
    public void addFilterTest() {
        // Test adding valid filters
        assertTrue(equalizer.addFilter("butterworth", 0));
        assertEquals(1, equalizer.size());
        assertTrue(equalizer.addFilter("bessel", 0));
        assertEquals(2, equalizer.size());
        assertTrue(equalizer.addFilter("chebyshev I", 0));
        assertEquals(3, equalizer.size());
        assertTrue(equalizer.addFilter("chebyshev II", 0));
        assertEquals(4, equalizer.size());

        // Test adding an invalid filter
        assertFalse(equalizer.addFilter("fakeFilter", 0));
        assertEquals(4, equalizer.size(), "Adding a fake filter should not change the rack size.");
    }

    @Test
    public void removeFilterTest() {
        equalizer.addFilter("butterworth", 0);
        equalizer.addFilter("bessel", 0);
        assertEquals(2, equalizer.size());

        // Test removing a filter successfully
        assertDoesNotThrow(() -> equalizer.removeFilter(1)); // Remove bessel
        assertEquals(1, equalizer.size());

        // Check that the correct filter remains
        assertDoesNotThrow(() -> {
            Cascade filter = equalizer.getFilter(0);
            assertInstanceOf(Butterworth.class, filter);
        });

        // Remove the last filter
        assertDoesNotThrow(() -> equalizer.removeFilter(0));
        assertTrue(equalizer.isEmpty());
    }

    @Test
    public void removeFilterExceptionsTest() {
        // Test removing from an empty rack
        assertThrows(EmptyFilterRackException.class, () -> equalizer.removeFilter(0));

        // Add a filter, then test out-of-bounds removal
        equalizer.addFilter("butterworth", 0);
        assertThrows(InvalidFilterRackPositionException.class, () -> equalizer.removeFilter(1), "Should throw for index >= size");
        assertThrows(InvalidFilterRackPositionException.class, () -> equalizer.removeFilter(-1), "Should throw for negative index");
    }

    @Test
    public void getFilterTest() {
        equalizer.addFilter("butterworth", 0);
        equalizer.addFilter("bessel", 0);

        // Assert we get the correct type of filter from the specified position
        assertDoesNotThrow(() -> {
            assertInstanceOf(Butterworth.class, equalizer.getFilter(0));
            assertInstanceOf(Bessel.class, equalizer.getFilter(1));
        });
    }

    @Test
    public void getFilterExceptionsTest() {
        // Test getting from an empty rack
        assertThrows(EmptyFilterRackException.class, () -> equalizer.getFilter(0));

        // Add a filter, then test out-of-bounds
        equalizer.addFilter("bessel", 0);
        assertThrows(IndexOutOfBoundsException.class, () -> equalizer.getFilter(1));
        assertThrows(IndexOutOfBoundsException.class, () -> equalizer.getFilter(-1));
    }

    @Test
    public void isFilterTest() {
        // Test that the method correctly identifies filter types
        assertDoesNotThrow(() -> {
            assertEquals("butterworth", equalizer.isFilter(new Butterworth()));
            assertEquals("bessel", equalizer.isFilter(new Bessel()));
            assertEquals("chebyshev I", equalizer.isFilter(new ChebyshevI()));
            assertEquals("chebyshev II", equalizer.isFilter(new ChebyshevII()));
        });

        // Test that it throws an exception for an unsupported type
        // We can simulate an unsupported type with a generic Cascade object
        class UnsupportedFilter extends Cascade {}
        assertThrows(InvalidFilterException.class, () -> equalizer.isFilter(new UnsupportedFilter()));
    }

    // The following tests for set methods primarily check that no exceptions are thrown
    // for valid filter types, as we cannot easily inspect the filter's internal state.
    @Test
    public void setBandpassTest() {
        Cascade butterworth = new Butterworth();
        assertDoesNotThrow(() -> equalizer.setBandpass(butterworth, 4, 44100, 1000, 500, 0));
    }

    @Test
    public void setBandstopTest() {
        Cascade chebyshevI = new ChebyshevI();
        assertDoesNotThrow(() -> equalizer.setBandstop(chebyshevI, 4, 44100, 1000, 500, 0.5));
    }

    @Test
    public void setHighpassTest() {
        Cascade bessel = new Bessel();
        assertDoesNotThrow(() -> equalizer.setHighpass(bessel, 4, 44100, 1000, 0));
    }

    @Test
    public void setLowpassTest() {
        Cascade chebyshevII = new ChebyshevII();
        assertDoesNotThrow(() -> equalizer.setLowpass(chebyshevII, 4, 44100, 1000, 0.5));
    }

    @Test
    public void setFilterInvalidTypeTest() {
        class UnsupportedFilter extends Cascade {}
        Cascade unsupported = new UnsupportedFilter();
        // Assert that all 'set' methods throw an exception for an unsupported filter type
        assertThrows(InvalidFilterException.class, () -> equalizer.setLowpass(unsupported, 4, 44100, 1000, 0.5));
        assertThrows(InvalidFilterException.class, () -> equalizer.setHighpass(unsupported, 4, 44100, 1000, 0.5));
        assertThrows(InvalidFilterException.class, () -> equalizer.setBandpass(unsupported, 4, 44100, 1000, 500, 0.5));
        assertThrows(InvalidFilterException.class, () -> equalizer.setBandstop(unsupported, 4, 44100, 1000, 500, 0.5));
    }

    @Test
    public void processDataEmptyRackTest() {
        double[] input = {0.1, 0.2, 0.3, 0.4, 0.5};
        double[] inputClone = input.clone();

        // Processing with an empty rack should not change the data
        double[] output = assertDoesNotThrow(() -> equalizer.processData(input));
        assertArrayEquals(inputClone, output);
    }
}
