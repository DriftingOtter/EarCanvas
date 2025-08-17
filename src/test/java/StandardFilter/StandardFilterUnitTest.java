package StandardFilter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.me.berndporr.iirj.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StandardFilterUnitTest {

    private static final double SAMPLE_RATE = 44100.0;
    private static final int ORDER = 4;

    @Test
    @DisplayName("Should create Butterworth filter successfully")
    void testButterworthCreation() throws InvalidFilterException {
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.Butterworth, ORDER, SAMPLE_RATE, Optional.empty());
        assertNotNull(filter.getSettings(), "Settings should not be null");
        assertTrue(filter.getSettings() instanceof Butterworth, "Should create an instance of Butterworth");
        assertEquals(ORDER, filter.getOrder());
        assertEquals(SAMPLE_RATE, filter.getSamplerate());
    }

    @Test
    @DisplayName("Should create Bessel filter successfully")
    void testBesselCreation() throws InvalidFilterException {
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.Bessel, ORDER, SAMPLE_RATE, Optional.empty());
        assertNotNull(filter.getSettings(), "Settings should not be null");
        assertTrue(filter.getSettings() instanceof Bessel, "Should create an instance of Bessel");
    }

    @Test
    @DisplayName("Should create ChebyshevI filter successfully with ripple")
    void testChebyshevICreation() throws InvalidFilterException {
        double rippleDb = 1.0;
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.ChebyshevI, ORDER, SAMPLE_RATE, Optional.of(rippleDb));
        assertNotNull(filter.getSettings(), "Settings should not be null");
        assertTrue(filter.getSettings() instanceof ChebyshevI, "Should create an instance of ChebyshevI");
        assertEquals(rippleDb, filter.getRippleDb());
    }

    @Test
    @DisplayName("Should create ChebyshevII filter successfully with stopband attenuation")
    void testChebyshevIICreation() throws InvalidFilterException {
        double stopbandDb = 40.0;
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.ChebyshevII, ORDER, SAMPLE_RATE, Optional.of(stopbandDb));
        assertNotNull(filter.getSettings(), "Settings should not be null");
        assertTrue(filter.getSettings() instanceof ChebyshevII, "Should create an instance of ChebyshevII");
        assertEquals(stopbandDb, filter.getRippleDb()); // Note: internal variable is reused for stopband
    }

    @Test
    @DisplayName("Should throw InvalidFilterException for a null filter type (though enum prevents this)")
    void testNullFilterType() {
        // This test is more of a logical check, as the enum type prevents passing a true null.
        // We test the constructor's robustness.
        assertThrows(InvalidFilterException.class, () -> {
            new StandardFilter(null, ORDER, SAMPLE_RATE, Optional.empty());
        });
    }

    @Test
    @DisplayName("Should correctly set lowpass filter parameters")
    void testSetLowpass() throws InvalidFilterException {
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.Butterworth, ORDER, SAMPLE_RATE, Optional.empty());
        double cutoff = 1000.0;
        filter.setLowpass(cutoff);
        assertEquals(cutoff, filter.getCutoffFrequnecy(), "Cutoff frequency should be set correctly for lowpass");
    }

    @Test
    @DisplayName("Should correctly set highpass filter parameters")
    void testSetHighpass() throws InvalidFilterException {
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.Butterworth, ORDER, SAMPLE_RATE, Optional.empty());
        double cutoff = 500.0;
        filter.setHighpass(cutoff);
        assertEquals(cutoff, filter.getCutoffFrequnecy(), "Cutoff frequency should be set correctly for highpass");
    }

    @Test
    @DisplayName("Should correctly set bandpass filter parameters")
    void testSetBandpass() throws InvalidFilterException {
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.Butterworth, ORDER, SAMPLE_RATE, Optional.empty());
        double centerFreq = 1000.0;
        double width = 400.0;
        filter.setBandpass(centerFreq, width);
        assertEquals(centerFreq, filter.getCenterFrequnecy(), "Center frequency should be set correctly for bandpass");
        assertEquals(width, filter.getFrequencyWidth(), "Frequency width should be set correctly for bandpass");
    }

    @Test
    @DisplayName("Should correctly set bandstop filter parameters")
    void testSetBandstop() throws InvalidFilterException {
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.ChebyshevII, ORDER, SAMPLE_RATE, Optional.of(40.0));
        double centerFreq = 1000.0;
        double width = 200.0;
        filter.setBandstop(centerFreq, width);
        assertEquals(centerFreq, filter.getCenterFrequnecy(), "Center frequency should be set correctly for bandstop");
        assertEquals(width, filter.getFrequencyWidth(), "Frequency width should be set correctly for bandstop");
    }

    @Test
    @DisplayName("Should use rippleDb for ChebyshevI filter setups")
    void testRippleDbUsageForChebyshevI() throws InvalidFilterException {
        double ripple = 1.5;
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.ChebyshevI, ORDER, SAMPLE_RATE, Optional.of(ripple));
        
        // The test is implicitly that this does not throw an error and the ripple is stored.
        filter.setLowpass(1000.0);
        assertEquals(ripple, filter.getRippleDb());

        filter.setHighpass(1000.0);
        assertEquals(ripple, filter.getRippleDb());

        filter.setBandpass(1000.0, 200.0);
        assertEquals(ripple, filter.getRippleDb());

        filter.setBandstop(1000.0, 200.0);
        assertEquals(ripple, filter.getRippleDb());
    }
}
