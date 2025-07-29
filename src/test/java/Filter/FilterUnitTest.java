package Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.me.berndporr.iirj.Bessel;
import uk.me.berndporr.iirj.Butterworth;
import uk.me.berndporr.iirj.ChebyshevI;
import uk.me.berndporr.iirj.ChebyshevII;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FilterUnitTest {

    private final int order = 4;
    private final double sampleRate = 44100.0;
    private final double rippleDb = 1.0;

    @Test
    @DisplayName("Test Butterworth filter creation")
    void testButterworthConstructor() {
        assertDoesNotThrow(() -> {
            Filter filter = new Filter(Filter.FilterType.Butterworth, order, sampleRate, Optional.empty());
            assertEquals(order, filter.getOrder());
            assertEquals(sampleRate, filter.sampleRate);
            assertEquals(Filter.FilterType.Butterworth, filter.filterType);
            assertInstanceOf(Butterworth.class, filter.getSettings(), "Settings should be an instance of Butterworth");
        });
    }

    @Test
    @DisplayName("Test Bessel filter creation")
    void testBesselConstructor() {
        assertDoesNotThrow(() -> {
            Filter filter = new Filter(Filter.FilterType.Bessel, order, sampleRate, Optional.empty());
            assertEquals(order, filter.getOrder());
            assertEquals(sampleRate, filter.sampleRate);
            assertEquals(Filter.FilterType.Bessel, filter.filterType);
            assertInstanceOf(Bessel.class, filter.getSettings(), "Settings should be an instance of Bessel");
        });
    }

    @Test
    @DisplayName("Test ChebyshevI filter creation with ripple")
    void testChebyshevIConstructor() {
        assertDoesNotThrow(() -> {
            Filter filter = new Filter(Filter.FilterType.ChebyshevI, order, sampleRate, Optional.of(rippleDb));
            assertEquals(order, filter.getOrder());
            assertEquals(sampleRate, filter.sampleRate);
            assertEquals(rippleDb, filter.getRippleDb());
            assertEquals(Filter.FilterType.ChebyshevI, filter.filterType);
            assertInstanceOf(ChebyshevI.class, filter.getSettings(), "Settings should be an instance of ChebyshevI");
        });
    }

    @Test
    @DisplayName("Test ChebyshevII filter creation with ripple")
    void testChebyshevIIConstructor() {
        assertDoesNotThrow(() -> {
            Filter filter = new Filter(Filter.FilterType.ChebyshevII, order, sampleRate, Optional.of(rippleDb));
            assertEquals(order, filter.getOrder());
            assertEquals(sampleRate, filter.sampleRate);
            assertEquals(rippleDb, filter.getRippleDb());
            assertEquals(Filter.FilterType.ChebyshevII, filter.filterType);
            assertInstanceOf(ChebyshevII.class, filter.getSettings(), "Settings should be an instance of ChebyshevII");
        });
    }

    @Test
    @DisplayName("Test setBandpass sets parameters correctly")
    void testSetBandpass() throws InvalidFilterException {
        Filter filter = new Filter(Filter.FilterType.Butterworth, order, sampleRate, Optional.empty());
        double centerFreq = 1000.0;
        double freqWidth = 500.0;
        assertDoesNotThrow(() -> filter.setBandpass(centerFreq, freqWidth));
        assertEquals(centerFreq, filter.getCenterFrequnecy());
        assertEquals(freqWidth, filter.getFrequencyWidth());
    }

    @Test
    @DisplayName("Test setBandstop sets parameters correctly")
    void testSetBandstop() throws InvalidFilterException {
        Filter filter = new Filter(Filter.FilterType.Butterworth, order, sampleRate, Optional.empty());
        double centerFreq = 1000.0;
        double freqWidth = 500.0;
        assertDoesNotThrow(() -> filter.setBandstop(centerFreq, freqWidth));
        assertEquals(centerFreq, filter.getCenterFrequnecy());
        assertEquals(freqWidth, filter.getFrequencyWidth());
    }

    @Test
    @DisplayName("Test setHighpass sets parameters correctly")
    void testSetHighpass() throws InvalidFilterException {
        Filter filter = new Filter(Filter.FilterType.Butterworth, order, sampleRate, Optional.empty());
        double cutoff = 1500.0;
        assertDoesNotThrow(() -> filter.setHighpass(cutoff));
        assertEquals(cutoff, filter.getCutoffFrequnecy());
    }

    @Test
    @DisplayName("Test setLowpass sets parameters correctly")
    void testSetLowpass() throws InvalidFilterException {
        Filter filter = new Filter(Filter.FilterType.Butterworth, order, sampleRate, Optional.empty());
        double cutoff = 500.0;
        assertDoesNotThrow(() -> filter.setLowpass(cutoff));
        assertEquals(cutoff, filter.getCutoffFrequnecy());
    }
}
