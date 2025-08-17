package AudioProcessingRangler;

public class FFT {
    public static Complex[] fft(Complex[] x) {
        int n = x.length;
        if (n == 1) return new Complex[]{x[0]};

        if (n % 2 != 0) {
            // This implementation of FFT requires the length of the array to be a power of 2.
            throw new IllegalArgumentException("FFT input length must be a power of 2");
        }

        // --- FIX: Create separate arrays for even and odd parts ---
        Complex[] even = new Complex[n / 2];
        Complex[] odd = new Complex[n / 2];
        for (int k = 0; k < n / 2; k++) {
            even[k] = x[2 * k];
            odd[k] = x[2 * k + 1];
        }

        // Recursive FFT calls
        Complex[] q = fft(even);
        Complex[] r = fft(odd);

        // Combine results
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
