package AudioPipeline;

import javax.sound.sampled.AudioFormat;

import AudioProcessingRangler.AudioProcessingRangler;

public interface PipelineInterface extends Runnable {

    void setEqualizer(AudioProcessingRangler equalizer);

    AudioFormat getFormat();

    void start();
    void stop();
}
