package AudioEqualizer;

import Filter.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AudioEqualizerIntegrationTest {

    private AudioEqualizer equalizer;

    private final double SAMPLE_RATE = 48000;
    private final int NUM_SAMPLES    = 4096;
    private final int FILTER_ORDER   = 4;

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
            signal[i] = sample / frequencies.length;
        }
        return signal;
    }

    private double getFrequencyMagnitude(Complex[] fftResult, double freq) {
        int index = (int) (freq * NUM_SAMPLES / SAMPLE_RATE);
        return fftResult[index].abs();
    }


    @Test
    public void testLowPassFilterIntegration() {
        // 1. Configure a low-pass filter
        double cutoff = 400; // Hz
        Filter filter = assertDoesNotThrow(() -> 
            equalizer.addFilter(Filter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty(), 0)
        );
        filter.setLowpass(cutoff);

        // 2. Create a signal with a component to be passed (200Hz) and one to be cut (8000Hz)
        double[] signal = generateTestSignal(200, 8000);
        double[] processedSignal = equalizer.processData(signal);

        // 3. Perform FFT on the processed signal
        Complex[] complexSignal = new Complex[NUM_SAMPLES];
        for (int i = 0; i < NUM_SAMPLES; i++) {
            complexSignal[i] = new Complex(processedSignal[i], 0);
        }
        Complex[] fftResult = FFT.fft(complexSignal);

        // 4. Verify the frequency magnitudes
        double lowFreqMag = getFrequencyMagnitude(fftResult, 200);
        double highFreqMag = getFrequencyMagnitude(fftResult, 8000);

        // The high frequency component should be significantly attenuated compared to the low one.
        assertTrue(lowFreqMag > highFreqMag * 10,
            "Low-pass filter should attenuate high frequencies. Low Freq Mag: " + lowFreqMag + ", High Freq Mag: " + highFreqMag);
    }

    @Test
    public void testHighPassFilterIntegration() {
        // 1. Configure a high-pass filter
        double cutoff = 4000; // Hz
        Filter filter = assertDoesNotThrow(() -> 
            equalizer.addFilter(Filter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty(), 0)
        );
        filter.setHighpass(cutoff);

        // 2. Create a signal with a component to be cut (200Hz) and one to be passed (8000Hz)
        double[] signal = generateTestSignal(200, 8000);
        double[] processedSignal = equalizer.processData(signal);

        // 3. Perform FFT on the processed signal
        Complex[] complexSignal = new Complex[NUM_SAMPLES];
        for (int i = 0; i < NUM_SAMPLES; i++) {
            complexSignal[i] = new Complex(processedSignal[i], 0);
        }
        Complex[] fftResult = FFT.fft(complexSignal);

        // 4. Verify the frequency magnitudes
        double lowFreqMag = getFrequencyMagnitude(fftResult, 200);
        double highFreqMag = getFrequencyMagnitude(fftResult, 8000);

        // The low frequency component should be significantly attenuated compared to the high one.
        assertTrue(highFreqMag > lowFreqMag * 10,
            "High-pass filter should attenuate low frequencies. High Freq Mag: " + highFreqMag + ", Low Freq Mag: " + lowFreqMag);
    }

    @Test
    public void testBandPassBehaviorWithMultipleFilters() {
        // 1. Configure a low-pass and a high-pass to create a band-pass filter (100Hz - 400Hz)
        Filter lowPassFilter = assertDoesNotThrow(() -> 
            equalizer.addFilter(Filter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty(), 0)
        );
        lowPassFilter.setLowpass(400);

        Filter highPassFilter = assertDoesNotThrow(() -> 
            equalizer.addFilter(Filter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty(), 1)
        );
        highPassFilter.setHighpass(100);

        // 2. Create a signal with components below, inside, and above the passband
        double[] signal = generateTestSignal(50, 250, 1000);
        double[] processedSignal = equalizer.processData(signal);

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

        // The frequency inside the passband should have a much larger magnitude than those outside.
        assertTrue(insideMag > belowMag * 10, "Component below passband should be attenuated.");
        assertTrue(insideMag > aboveMag * 10, "Component above passband should be attenuated.");
    }

}
