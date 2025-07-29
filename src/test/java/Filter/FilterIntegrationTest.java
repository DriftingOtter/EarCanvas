package Filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.me.berndporr.iirj.Cascade;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FilterIntegrationTest {

    private final int order = 4;
    private final double sampleRate = 44100.0;
    private final double rippleDb = 1.0;
    private final double sampleInput = 1.0;

    @Test
    @DisplayName("Butterworth Low-pass filter processes a sample")
    void testButterworthLowPassIntegration() {
        assertDoesNotThrow(() -> {
            Filter filter = new Filter(Filter.FilterType.Butterworth, order, sampleRate, Optional.empty());
            filter.setLowpass(1000.0);
            Cascade settings = filter.getSettings();
            double output = settings.filter(sampleInput);
            assertTrue(Double.isFinite(output), "Output should be a finite number");
        });
    }

    @Test
    @DisplayName("Bessel High-pass filter processes a sample")
    void testBesselHighPassIntegration() {
        assertDoesNotThrow(() -> {
            Filter filter = new Filter(Filter.FilterType.Bessel, order, sampleRate, Optional.empty());
            filter.setHighpass(1000.0);
            Cascade settings = filter.getSettings();
            double output = settings.filter(sampleInput);
            assertTrue(Double.isFinite(output), "Output should be a finite number");
        });
    }

    @Test
    @DisplayName("ChebyshevI Band-pass filter processes a sample")
    void testChebyshevIBandPassIntegration() {
        assertDoesNotThrow(() -> {
            Filter filter = new Filter(Filter.FilterType.ChebyshevI, order, sampleRate, Optional.of(rippleDb));
            filter.setBandpass(2000.0, 500.0);
            Cascade settings = filter.getSettings();
            double output = settings.filter(sampleInput);
            assertTrue(Double.isFinite(output), "Output should be a finite number");
        });
    }

    @Test
    @DisplayName("ChebyshevII Band-stop filter processes a sample")
    void testChebyshevIIBandStopIntegration() {
        assertDoesNotThrow(() -> {
            Filter filter = new Filter(Filter.FilterType.ChebyshevII, order, sampleRate, Optional.of(rippleDb));
            filter.setBandstop(2000.0, 500.0);
            Cascade settings = filter.getSettings();
            double output = settings.filter(sampleInput);
            assertTrue(Double.isFinite(output), "Output should be a finite number");
        });
    }
}
