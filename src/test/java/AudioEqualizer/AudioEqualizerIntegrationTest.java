package AudioEqualizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.me.berndporr.iirj.Cascade;

import static org.junit.jupiter.api.Assertions.*;

public class AudioEqualizerIntegrationTest {

    private AudioEqualizer equalizer;
    private final double SAMPLE_RATE = 48000;
    // Use a power of 2 for the number of samples for the FFT
    private final int NUM_SAMPLES = 4096;

    @BeforeEach
    public void setUp() {
        equalizer = new AudioEqualizer();
    }

    // --- Helper Classes and Methods for FFT ---

    /**
     * A simple class to represent complex numbers for the FFT.
     */
    private static class Complex {
        public final double re;
        public final double im;

        public Complex(double re, double im) {
            this.re = re;
            this.im = im;
        }

        public Complex plus(Complex b) {
            return new Complex(this.re + b.re, this.im + b.im);
        }

        public Complex minus(Complex b) {
            return new Complex(this.re - b.re, this.im - b.im);
        }

        public Complex times(Complex b) {
            return new Complex(this.re * b.re - this.im * b.im, this.re * b.im + this.im * b.re);
        }

        public double abs() {
            return Math.hypot(re, im);
        }
    }

    /**
     * A simple FFT implementation.
     */
    private static class FFT {
        public static Complex[] fft(Complex[] x) {
            int n = x.length;
            if (n == 1) return new Complex[]{x[0]};

            if (n % 2 != 0) {
                throw new IllegalArgumentException("n is not a power of 2");
            }

            Complex[] even = new Complex[n / 2];
            for (int k = 0; k < n / 2; k++) {
                even[k] = x[2 * k];
            }
            Complex[] q = fft(even);

            Complex[] odd = even;
            for (int k = 0; k < n / 2; k++) {
                odd[k] = x[2 * k + 1];
            }
            Complex[] r = fft(odd);

            Complex[] y = new Complex[n];
            for (int k = 0; k < n / 2; k++) {
                double kth = -2 * k * Math.PI / n;
                Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
                y[k] = q[k].plus(wk.times(r[k]));
                y[k + n / 2] = q[k].minus(wk.times(r[k]));
            }
            return y;
        }
    }

    /**
     * Generates a test signal containing multiple sine waves.
     * @param frequencies An array of frequencies to include in the signal.
     * @return The generated signal.
     */
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

    /**
     * Finds the magnitude of a specific frequency in an FFT result.
     * @param fftResult The result from the FFT.
     * @param freq The frequency to find.
     * @return The magnitude of that frequency.
     */
    private double getFrequencyMagnitude(Complex[] fftResult, double freq) {
        int index = (int) (freq * NUM_SAMPLES / SAMPLE_RATE);
        return fftResult[index].abs();
    }


    @Test
    public void testLowPassFilterIntegration() {
        // 1. Configure a low-pass filter
        equalizer.addFilter("butterworth", 0);
        Cascade filter = assertDoesNotThrow(() -> equalizer.getFilter(0));
        double cutoff = 400; // Hz
        assertDoesNotThrow(() -> equalizer.setLowpass(filter, 4, SAMPLE_RATE, cutoff, 0));

        // 2. Create a signal with a component to be passed (200Hz) and one to be cut (8000Hz)
        double[] signal = generateTestSignal(200, 8000);
        double[] processedSignal = assertDoesNotThrow(() -> equalizer.processData(signal));

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
        equalizer.addFilter("butterworth", 0);
        Cascade filter = assertDoesNotThrow(() -> equalizer.getFilter(0));
        double cutoff = 4000; // Hz
        assertDoesNotThrow(() -> equalizer.setHighpass(filter, 4, SAMPLE_RATE, cutoff, 0));

        // 2. Create a signal with a component to be cut (200Hz) and one to be passed (8000Hz)
        double[] signal = generateTestSignal(200, 8000);
        double[] processedSignal = assertDoesNotThrow(() -> equalizer.processData(signal));

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
        equalizer.addFilter("butterworth", 0); // This will be the low-pass
        equalizer.addFilter("butterworth", 1); // This will be the high-pass

        Cascade lowPass = assertDoesNotThrow(() -> equalizer.getFilter(0));
        assertDoesNotThrow(() -> equalizer.setLowpass(lowPass, 4, SAMPLE_RATE, 400, 0));

        Cascade highPass = assertDoesNotThrow(() -> equalizer.getFilter(1));
        assertDoesNotThrow(() -> equalizer.setHighpass(highPass, 4, SAMPLE_RATE, 100, 0));

        // 2. Create a signal with components below, inside, and above the passband
        double[] signal = generateTestSignal(50, 250, 1000);
        double[] processedSignal = assertDoesNotThrow(() -> equalizer.processData(signal));

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
