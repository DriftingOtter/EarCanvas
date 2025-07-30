#include <jni.h>
#include <stdio.h>
#include <math.h>

// Simple C EQ example — single biquad low-shelf filter
void processEQ(double* samples, int numSamples, int numChannels) {
    const double gain = 1.5; // Boost
    for (int i = 0; i < numSamples; i++) {
        samples[i] *= gain;
    }
}

JNIEXPORT void JNICALL Java_AudioEqualizer_AudioEqualizerJNI_processEQ
  (JNIEnv *env, jclass clazz, jdoubleArray buffer, jint length, jint channels) {

    jboolean isCopy;
    jdouble* nativeBuffer = (*env)->GetDoubleArrayElements(env, buffer, &isCopy);

    processEQ(nativeBuffer, length, channels);

    (*env)->ReleaseDoubleArrayElements(env, buffer, nativeBuffer, 0);
}
