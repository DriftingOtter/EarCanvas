#include <jni.h>
#include <stdio.h>
#include <math.h>

#ifndef PI
#define PI 3.14159265358979323846
#endif

void processGraphicEQ(double* samples, int numSamples, int numChannels, float sampleRate, double* bandGains, double qFactor) {
    // --- EQ Parameters ---
    const int    bands     = 10;
    const double Q         = qFactor;
    const double Fs        = (double)sampleRate;
    
    // Center frequencies for the 10 bands.
    const double f0[10] = {31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};

    // --- Intermediate Variables (per band) ---
    double A[bands], w0[bands], cos_w0[bands], sin_w0[bands], alpha[bands];

    // --- Biquad Coefficients (per band) ---
    double b0[bands], b1[bands], b2[bands], a0[bands], a1[bands], a2[bands];
    double norm_b0[bands], norm_b1[bands], norm_b2[bands], norm_a1[bands], norm_a2[bands];

    // --- State Variables (per band, per channel) | Stack allocated ---
    double x_n1[bands][numChannels], x_n2[bands][numChannels];
    double y_n1[bands][numChannels], y_n2[bands][numChannels];

    // Initialize state variables to zero.
    for (int i = 0; i < bands; i++) {
        for (int c = 0; c < numChannels; c++) {
            x_n1[i][c] = 0.0;
            x_n2[i][c] = 0.0;
            y_n1[i][c] = 0.0;
            y_n2[i][c] = 0.0;
        }
    }

    // --- 1. Calculate Intermediate variables and Coefficients for each band ---
    for (int i = 0; i < bands; i++) {
        // Calculate intermediate variables
        A[i]      = pow(10, 0.15 * bandGains[i]);
        w0[i]     = (2.0 * PI * f0[i]) / Fs;
        cos_w0[i] = cos(w0[i]);
        sin_w0[i] = sin(w0[i]);
        alpha[i]  = sin_w0[i] / (2.0 * Q);

        // --- Select Filter Type and Calculate Coefficients ---

        if (i == 0) { // First band: Low Shelf Filter
            double sqrt_A = sqrt(A[i]);
            b0[i] = A[i] * ((A[i] + 1) - (A[i] - 1) * cos_w0[i] + 2 * sqrt_A * alpha[i]);
            b1[i] = 2 * A[i] * ((A[i] - 1) - (A[i] + 1) * cos_w0[i]);
            b2[i] = A[i] * ((A[i] + 1) - (A[i] - 1) * cos_w0[i] - 2 * sqrt_A * alpha[i]);
            a0[i] = (A[i] + 1) + (A[i] - 1) * cos_w0[i] + 2 * sqrt_A * alpha[i];
            a1[i] = -2 * ((A[i] - 1) + (A[i] + 1) * cos_w0[i]);
            a2[i] = (A[i] + 1) + (A[i] - 1) * cos_w0[i] - 2 * sqrt_A * alpha[i];

        } else if (i == bands - 1) { // Last band: High Shelf Filter
            double sqrt_A = sqrt(A[i]);
            b0[i] = A[i] * ((A[i] + 1) + (A[i] - 1) * cos_w0[i] + 2 * sqrt_A * alpha[i]);
            b1[i] = -2 * A[i] * ((A[i] - 1) + (A[i] + 1) * cos_w0[i]);
            b2[i] = A[i] * ((A[i] + 1) + (A[i] - 1) * cos_w0[i] - 2 * sqrt_A * alpha[i]);
            a0[i] = (A[i] + 1) - (A[i] - 1) * cos_w0[i] + 2 * sqrt_A * alpha[i];
            a1[i] = 2 * ((A[i] - 1) - (A[i] + 1) * cos_w0[i]);
            a2[i] = (A[i] + 1) - (A[i] - 1) * cos_w0[i] - 2 * sqrt_A * alpha[i];

        } else { // Middle bands: Peaking EQ Filter
            b0[i] = 1 + alpha[i] * A[i];
            b1[i] = -2 * cos_w0[i];
            b2[i] = 1 - alpha[i] * A[i];
            a0[i] = 1 + alpha[i] / A[i];
            a1[i] = -2 * cos_w0[i];
            a2[i] = 1 - alpha[i] / A[i];
        }
    }

    // --- 2. Normalize coefficients for processing ---
    for (int i = 0; i < bands; i++) {
        norm_b0[i] = b0[i] / a0[i];
        norm_b1[i] = b1[i] / a0[i];
        norm_b2[i] = b2[i] / a0[i];
        norm_a1[i] = a1[i] / a0[i];
        norm_a2[i] = a2[i] / a0[i];
    }


    // --- 3. Process Audio Samples ---
    
    // Loop through each audio frame (e.g., a left/right pair in stereo).
    for (int s = 0; s < numSamples; s += numChannels) {
        for (int c = 0; c < numChannels; c++) {
            double sample_in = samples[s + c];

            // output of one band becomes input to the next.
            for (int i = 0; i < bands; i++) {
                double x_n = sample_in;

                // Apply the difference equation
                double y_n = norm_b0[i] * x_n + norm_b1[i] * x_n1[i][c] + norm_b2[i] * x_n2[i][c]
                           - norm_a1[i] * y_n1[i][c] - norm_a2[i] * y_n2[i][c];

                // Update state variables for this band and channel.
                x_n2[i][c] = x_n1[i][c];
                x_n1[i][c] = x_n;
                y_n2[i][c] = y_n1[i][c];
                y_n1[i][c] = y_n;

                // input for the next band.
                sample_in = y_n;
            }
            
            // write processed sample back to the buffer.
            samples[s + c] = sample_in;
        }
    }
}

/**
 * JNI bridge function that maps to the native method in GraphicEqualizer.java.
 *
 * @param env 		 JNI interface pointer.
 * @param clazz 	 The Java class object.
 * @param buffer 	 The audio buffer to be processed.
 * @param length 	 The total number of samples in the buffer.
 * @param channels   The number of audio channels.
 * @param sampleRate The sample rate of the audio.
 * @param bandGains  The array of 16 gain values (0.0 to 1.0).
 */
JNIEXPORT void JNICALL Java_NativeFilter_GraphicEqualizer_processData
  (JNIEnv *env, jclass clazz, jdoubleArray buffer, jint length, jint channels, jfloat sampleRate, jdoubleArray bandGains, jdouble qFactor) {

    jboolean isCopy;

    // Get direct access to the audio buffer elements from the Java array.
    jdouble* nativeBuffer = (*env)->GetDoubleArrayElements(env, buffer, &isCopy);
    if (nativeBuffer == NULL) {
        return;
    }

    // Get direct access to the gain array elements.
    jdouble* nativeBandGains = (*env)->GetDoubleArrayElements(env, bandGains, &isCopy);
    if (nativeBandGains == NULL) {
        (*env)->ReleaseDoubleArrayElements(env, buffer, nativeBuffer, 0);
        return;
    }

    // Call the C function to perform the actual audio processing.
    processGraphicEQ(nativeBuffer, length, channels, sampleRate, nativeBandGains, qFactor);

    // Release the native arrays, committing changes back to the Java heap.
    (*env)->ReleaseDoubleArrayElements(env, buffer, nativeBuffer, 0);
    (*env)->ReleaseDoubleArrayElements(env, bandGains, nativeBandGains, JNI_ABORT);
}
