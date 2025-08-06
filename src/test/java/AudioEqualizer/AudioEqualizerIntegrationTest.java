package AudioEqualizer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import IIRFilter.IIRFilter;
import IIRFilter.InvalidFilterException;

public class AudioEqualizerIntegrationTest {

    private AudioEqualizer equalizer;
    private final double SAMPLE_RATE = 48000.0;
    private final int NUM_SAMPLES = 4096;
    private final int FILTER_ORDER = 4;

    @BeforeEach
    public void setUp() {
        equalizer = new AudioEqualizer();
    }

    private double[] generateTestSignal(double... frequencies) {
        double[] signal = new double[NUM_SAMPLES];
        for (int i = 0; i < NUM_SAMPLES; i++) {
            double sample = 0;
            for (double freq : frequencies) {
                sample += Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE);
            }
            signal[i] = sample / frequencies.length; // Normalize to prevent clipping
        }
        return signal;
    }

    private double getFrequencyMagnitude(Complex[] fftResult, double freq) {
        int index = (int) (freq * NUM_SAMPLES / SAMPLE_RATE);
        if (index >= fftResult.length) {
            // Handle potential out-of-bounds access if freq is too high
            return 0.0;
        }
        return fftResult[index].abs();
    }

    @Test
    public void testLowPassFilterIntegration() throws InvalidFilterException {
        // 1. Create and configure a low-pass filter
        double cutoff = 400; // Hz
        IIRFilter filter = new IIRFilter(IIRFilter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty());
        filter.setLowpass(cutoff);
        equalizer.addFilter(filter, 0); // Add the configured filter to the rack

        // 2. Create a signal with components to be passed (200Hz) and cut (8000Hz)
        double[] signal = generateTestSignal(200, 8000);
        double[] processedSignal = equalizer.processData(signal.clone());

        // 3. Perform FFT on the processed signal
        Complex[] complexSignal = new Complex[NUM_SAMPLES];
        for (int i = 0; i < NUM_SAMPLES; i++) {
            complexSignal[i] = new Complex(processedSignal[i], 0);
        }
        Complex[] fftResult = FFT.fft(complexSignal);

        // 4. Verify the frequency magnitudes
        double lowFreqMag = getFrequencyMagnitude(fftResult, 200);
        double highFreqMag = getFrequencyMagnitude(fftResult, 8000);

        // The high frequency component should be significantly attenuated
        assertTrue(lowFreqMag > highFreqMag * 10,
            "Low-pass filter should attenuate high frequencies. Low Freq Mag: " + lowFreqMag + ", High Freq Mag: " + highFreqMag);
    }

    @Test
    public void testHighPassFilterIntegration() throws InvalidFilterException {
        // 1. Create and configure a high-pass filter
        double cutoff = 4000; // Hz
        IIRFilter filter = new IIRFilter(IIRFilter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty());
        filter.setHighpass(cutoff);
        equalizer.addFilter(filter, 0);

        // 2. Create a signal with components to be cut (200Hz) and passed (8000Hz)
        double[] signal = generateTestSignal(200, 8000);
        double[] processedSignal = equalizer.processData(signal.clone());

        // 3. Perform FFT
        Complex[] complexSignal = new Complex[NUM_SAMPLES];
        for (int i = 0; i < NUM_SAMPLES; i++) {
            complexSignal[i] = new Complex(processedSignal[i], 0);
        }
        Complex[] fftResult = FFT.fft(complexSignal);

        // 4. Verify the frequency magnitudes
        double lowFreqMag = getFrequencyMagnitude(fftResult, 200);
        double highFreqMag = getFrequencyMagnitude(fftResult, 8000);

        // The low frequency component should be significantly attenuated
        assertTrue(highFreqMag > lowFreqMag * 10,
            "High-pass filter should attenuate low frequencies. High Freq Mag: " + highFreqMag + ", Low Freq Mag: " + lowFreqMag);
    }

    @Test
    public void testBandPassBehaviorWithMultipleFilters() throws InvalidFilterException {
        // 1. Configure a low-pass and a high-pass to create a band-pass effect
        IIRFilter lowPassFilter = new IIRFilter(IIRFilter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty());
        lowPassFilter.setLowpass(400); // Pass frequencies below 400Hz

        IIRFilter highPassFilter = new IIRFilter(IIRFilter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty());
        highPassFilter.setHighpass(100); // Pass frequencies above 100Hz

        // Add both filters to the rack
        equalizer.addFilter(lowPassFilter, 0);
        equalizer.addFilter(highPassFilter, 1);

        // 2. Create a signal with components below, inside, and above the passband
        double[] signal = generateTestSignal(50, 250, 1000);
        double[] processedSignal = equalizer.processData(signal.clone());

        // 3. Perform FFT
        Complex[] complexSignal = new Complex[NUM_SAMPLES];
        for (int i = 0; i < NUM_SAMPLES; i++) {
            complexSignal[i] = new Complex(processedSignal[i], 0);
        }
        Complex[] fftResult = FFT.fft(complexSignal);

        // 4. Verify magnitudes
        double belowMag = getFrequencyMagnitude(fftResult, 50);
        double insideMag = getFrequencyMagnitude(fftResult, 250);
        double aboveMag = getFrequencyMagnitude(fftResult, 1000);

        // The frequency inside the passband (250Hz) should have a much larger magnitude
        assertTrue(insideMag > belowMag * 10, "Component below passband (50Hz) should be attenuated.");
        assertTrue(insideMag > aboveMag * 10, "Component above passband (1000Hz) should be attenuated.");
    }
}