#include "NativeFilter_ChannelBalancer.h"
#include <stdio.h>

void computeGains(double preference, double* leftGain, double* rightGain) {
    double x = preference - 0.5;

    if (x < 0) {
        *leftGain = 1.0;
        *rightGain = 1.0 + 2.0 * x;  // attenuate right
    } else {
        *leftGain = 1.0 - 2.0 * x;  // attenuate left
        *rightGain = 1.0;
    }
}

void channel_balancer_process(double* buffer, int numChannels, int numSamples, double sampleRate, double preference) {
    if (sampleRate <= 0 || numSamples <= 0) {
        return;
    }
    if (numChannels == 1) {
        return;
    }
    if (preference < 0.0 || preference > 1.0) {
        return;
    }

    double leftGain, rightGain;
    computeGains(preference, &leftGain, &rightGain);

    int frames = numSamples / numChannels;

    for (int frame = 0; frame < frames; frame++) {
        int idx = frame * numChannels;
        buffer[idx + 0] *= leftGain;   // Left channel
        buffer[idx + 1] *= rightGain;  // Right channel
    }
}

JNIEXPORT void JNICALL Java_NativeFilter_ChannelBalancer_processData
  (JNIEnv *env, jclass clazz,
   jdoubleArray bufferArray,
   jint numChannels,
   jint numSamples,
   jdouble sampleRate,
   jdouble preference) {

    jdouble *buffer = (*env)->GetDoubleArrayElements(env, bufferArray, NULL);

    channel_balancer_process(
        buffer,
        numChannels,
        numSamples,
        sampleRate,
        preference
    );

    //(*env)->ReleaseDoubleArrayElements(env, bufferArray, buffer, 0);
    (*env)->ReleaseDoubleArrayElements(env, bufferArray, buffer, JNI_COMMIT);

}

