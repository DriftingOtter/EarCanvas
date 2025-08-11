#include <jni.h>
#include "NativeFilter_Limiter.h" 
#include <math.h>
#include <string.h>
#include <stdbool.h>

#define MAX_CHANNELS 8
#define MAX_LOOKAHEAD_SAMPLES 4096

static double current_gain = 1.0;
static double delay_buffer[MAX_CHANNELS][MAX_LOOKAHEAD_SAMPLES];
static int write_pos = 0;
static int current_lookahead_samples = 0;
static bool state_initialized = false;

void limiter_processData(double* data, int numChannels, int numSamples, double samplerate, double attack_ms, double release_ms, double threshold_db, double lookahead_ms) {

    // --- Parameter & State Validation ---
    if (samplerate <= 0 || release_ms <= 0) {
        return;
    }
    if (numChannels > MAX_CHANNELS) {
        return;
    }

    int new_lookahead_samples = (int)floor(lookahead_ms * samplerate / 1000.0);
    if (new_lookahead_samples < 0) new_lookahead_samples = 0;
    if (new_lookahead_samples >= MAX_LOOKAHEAD_SAMPLES) new_lookahead_samples = MAX_LOOKAHEAD_SAMPLES - 1;

    if (!state_initialized || new_lookahead_samples != current_lookahead_samples) {
        current_gain = 1.0;
        write_pos = 0;
        current_lookahead_samples = new_lookahead_samples;
        for (int ch = 0; ch < MAX_CHANNELS; ++ch) {
            memset(delay_buffer[ch], 0, MAX_LOOKAHEAD_SAMPLES * sizeof(double));
        }
        state_initialized = true;
    }
    
    // --- Coefficient Calculation ---
    double threshold_linear = pow(10.0, threshold_db / 20.0);
    double attack_coeff = 0.0;
    
    if (attack_ms > 0.0) {
        attack_coeff = exp(-1.0 / (attack_ms * samplerate / 1000.0));
    }
    
    double release_coeff = exp(-1.0 / (release_ms * samplerate / 1000.0));

    // --- Main Processing Loop ---
    for (int i = 0; i < numSamples; i += numChannels) {
        double peak_level = 0.0;
        for (int ch = 0; ch < numChannels; ++ch) {
            double current_sample_abs = fabs(data[i + ch]);
            if (current_sample_abs > peak_level) {
                peak_level = current_sample_abs;
            }
        }

        double target_gain = 1.0;
        if (peak_level > threshold_linear) {
            target_gain = threshold_linear / peak_level;
        }

        if (target_gain < current_gain) {
            current_gain = (1.0 - attack_coeff) * target_gain + attack_coeff * current_gain;
        } else {
            current_gain = (1.0 - release_coeff) * target_gain + release_coeff * current_gain;
        }

        int read_pos = (write_pos - current_lookahead_samples + MAX_LOOKAHEAD_SAMPLES) % MAX_LOOKAHEAD_SAMPLES;
        for (int ch = 0; ch < numChannels; ++ch) {
            double delayed_sample = delay_buffer[ch][read_pos];
            delay_buffer[ch][write_pos] = data[i + ch];
            double output_sample = delayed_sample * current_gain;
            data[i + ch] = output_sample;
        }
        write_pos = (write_pos + 1) % MAX_LOOKAHEAD_SAMPLES;
    }
}

JNIEXPORT void JNICALL Java_NativeFilter_Limiter_processData(
    JNIEnv *env, jclass clazz, jdoubleArray data, jint numChannels, jint numSamples,
    jdouble samplerate, jdouble attack, jdouble release, jdouble threshold, jdouble lookahead)
{
    jdouble* buffer = (*env)->GetDoubleArrayElements(env, data, NULL);
    if (buffer == NULL) return;

    limiter_processData(buffer, (int)numChannels, (int)numSamples, (double)samplerate,
                        (double)attack, (double)release, (double)threshold, (double)lookahead);

    (*env)->ReleaseDoubleArrayElements(env, data, buffer, 0);
}
