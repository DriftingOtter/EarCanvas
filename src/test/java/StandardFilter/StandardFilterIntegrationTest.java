package StandardFilter;

import AudioProcessingRangler.Complex;
import AudioProcessingRangler.FFT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the StandardFilter class.
 * These tests verify the filter's effect on a generated audio signal
 * by analyzing the frequency spectrum of the output using FFT.
 */
class StandardFilterIntegrationTest {

    private static final double SAMPLE_RATE = 44100.0;
    private static final int SIGNAL_LENGTH = 2048; // Use a power of 2 for FFT
    private static final int FILTER_ORDER = 8;

    @Test
    @DisplayName("Butterworth lowpass filter should attenuate high frequencies")
    void testLowpassFilterIntegration() throws InvalidFilterException {
        // 1. Setup: Create a lowpass filter
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty());
        filter.setLowpass(1000.0); // 1 kHz cutoff

        // 2. Action: Generate and process a signal
        double[] signal = generateTestSignal(SIGNAL_LENGTH, new double[]{500, 2000}); // Pass and attenuate frequencies
        double[] processedSignal = processSignal(signal, filter);

        // 3. Verification: Analyze with FFT
        Complex[] originalSpectrum = performFFTAnalysis(signal);
        Complex[] processedSpectrum = performFFTAnalysis(processedSignal);

        int passBin = findFrequencyBin(500, SIGNAL_LENGTH, SAMPLE_RATE);
        int attenuateBin = findFrequencyBin(2000, SIGNAL_LENGTH, SAMPLE_RATE);

        double originalPassMag = originalSpectrum[passBin].abs();
        double processedPassMag = processedSpectrum[passBin].abs();
        double originalAttenuateMag = originalSpectrum[attenuateBin].abs();
        double processedAttenuateMag = processedSpectrum[attenuateBin].abs();

        // Verify the 500 Hz signal passed through with minimal change
        assertTrue(processedPassMag > originalPassMag * 0.95, "Pass-band frequency (500 Hz) should be preserved.");

        // Verify the 2000 Hz signal was significantly attenuated
        assertTrue(processedAttenuateMag < originalAttenuateMag * 0.1, "Stop-band frequency (2000 Hz) should be attenuated.");
    }

    @Test
    @DisplayName("Butterworth highpass filter should attenuate low frequencies")
    void testHighpassFilterIntegration() throws InvalidFilterException {
        // 1. Setup: Create a highpass filter
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty());
        filter.setHighpass(1000.0); // 1 kHz cutoff

        // 2. Action: Generate and process a signal
        double[] signal = generateTestSignal(SIGNAL_LENGTH, new double[]{500, 2000}); // Attenuate and pass frequencies
        double[] processedSignal = processSignal(signal, filter);

        // 3. Verification: Analyze with FFT
        Complex[] originalSpectrum = performFFTAnalysis(signal);
        Complex[] processedSpectrum = performFFTAnalysis(processedSignal);

        int attenuateBin = findFrequencyBin(500, SIGNAL_LENGTH, SAMPLE_RATE);
        int passBin = findFrequencyBin(2000, SIGNAL_LENGTH, SAMPLE_RATE);

        double originalAttenuateMag = originalSpectrum[attenuateBin].abs();
        double processedAttenuateMag = processedSpectrum[attenuateBin].abs();
        double originalPassMag = originalSpectrum[passBin].abs();
        double processedPassMag = processedSpectrum[passBin].abs();

        // Verify the 500 Hz signal was significantly attenuated
        assertTrue(processedAttenuateMag < originalAttenuateMag * 0.1, "Stop-band frequency (500 Hz) should be attenuated.");

        // Verify the 2000 Hz signal passed through with minimal change
        assertTrue(processedPassMag > originalPassMag * 0.95, "Pass-band frequency (2000 Hz) should be preserved.");
    }

    @Test
    @DisplayName("Butterworth bandpass filter should pass a specific frequency band")
    void testBandpassFilterIntegration() throws InvalidFilterException {
        // 1. Setup: Create a bandpass filter
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty());
        filter.setBandpass(1000.0, 400.0); // Center 1kHz, width 400Hz (passband 800Hz-1200Hz)

        // 2. Action: Generate and process a signal
        double[] signal = generateTestSignal(SIGNAL_LENGTH, new double[]{500, 1000, 2000}); // Attenuate, pass, attenuate
        double[] processedSignal = processSignal(signal, filter);

        // 3. Verification: Analyze with FFT
        Complex[] originalSpectrum = performFFTAnalysis(signal);
        Complex[] processedSpectrum = performFFTAnalysis(processedSignal);

        int lowAttenuateBin = findFrequencyBin(500, SIGNAL_LENGTH, SAMPLE_RATE);
        int passBin = findFrequencyBin(1000, SIGNAL_LENGTH, SAMPLE_RATE);
        int highAttenuateBin = findFrequencyBin(2000, SIGNAL_LENGTH, SAMPLE_RATE);

        // Verify the 1000 Hz signal passed through
        assertTrue(processedSpectrum[passBin].abs() > originalSpectrum[passBin].abs() * 0.9, "Center frequency (1000 Hz) should pass.");

        // Verify the outer-band frequencies were attenuated
        assertTrue(processedSpectrum[lowAttenuateBin].abs() < originalSpectrum[lowAttenuateBin].abs() * 0.1, "Low frequency (500 Hz) should be attenuated.");
        assertTrue(processedSpectrum[highAttenuateBin].abs() < originalSpectrum[highAttenuateBin].abs() * 0.1, "High frequency (2000 Hz) should be attenuated.");
    }

    @Test
    @DisplayName("Butterworth bandstop filter should attenuate a specific frequency band")
    void testBandstopFilterIntegration() throws InvalidFilterException {
        // 1. Setup: Create a bandstop (notch) filter
        StandardFilter filter = new StandardFilter(StandardFilter.FilterType.Butterworth, FILTER_ORDER, SAMPLE_RATE, Optional.empty());
        filter.setBandstop(1000.0, 400.0); // Center 1kHz, width 400Hz (stopband 800Hz-1200Hz)

        // 2. Action: Generate and process a signal
        double[] signal = generateTestSignal(SIGNAL_LENGTH, new double[]{500, 1000, 2000}); // Pass, attenuate, pass
        double[] processedSignal = processSignal(signal, filter);

        // 3. Verification: Analyze with FFT
        Complex[] originalSpectrum = performFFTAnalysis(signal);
        Complex[] processedSpectrum = performFFTAnalysis(processedSignal);

        int lowPassBin = findFrequencyBin(500, SIGNAL_LENGTH, SAMPLE_RATE);
        int attenuateBin = findFrequencyBin(1000, SIGNAL_LENGTH, SAMPLE_RATE);
        int highPassBin = findFrequencyBin(2000, SIGNAL_LENGTH, SAMPLE_RATE);

        // Verify the 1000 Hz signal was attenuated
        assertTrue(processedSpectrum[attenuateBin].abs() < originalSpectrum[attenuateBin].abs() * 0.1, "Center frequency (1000 Hz) should be attenuated.");

        // Verify the outer-band frequencies passed through
        assertTrue(processedSpectrum[lowPassBin].abs() > originalSpectrum[lowPassBin].abs() * 0.9, "Low frequency (500 Hz) should pass.");
        assertTrue(processedSpectrum[highPassBin].abs() > originalSpectrum[highPassBin].abs() * 0.9, "High frequency (2000 Hz) should pass.");
    }


    // === Helper Methods ===

    /**
     * Generates a signal composed of multiple sine waves.
     */
    private double[] generateTestSignal(int length, double[] frequencies) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            for (double frequency : frequencies) {
                signal[i] += Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE);
            }
        }
        return signal;
    }

    /**
     * Processes a signal sample-by-sample through the given filter.
     */
    private double[] processSignal(double[] signal, StandardFilter filter) {
        double[] processed = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            processed[i] = filter.getSettings().filter(signal[i]);
        }
        return processed;
    }

    /**
     * Performs FFT on a signal.
     */
    private Complex[] performFFTAnalysis(double[] signal) {
        Complex[] complexSignal = new Complex[signal.length];
        for (int i = 0; i < signal.length; i++) {
            complexSignal[i] = new Complex(signal[i], 0);
        }
        return FFT.fft(complexSignal);
    }

    /**
     * Finds the corresponding index (bin) in the FFT output for a given frequency.
     */
    private int findFrequencyBin(double frequency, int fftSize, double sampleRate) {
        return (int) Math.round(frequency * fftSize / sampleRate);
    }
}
