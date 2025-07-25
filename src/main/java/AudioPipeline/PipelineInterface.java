package AudioPipeline;

import AudioEqualizer.AudioEqualizer;
import javax.sound.sampled.AudioFormat;

public interface PipelineInterface extends Runnable {

    void setEqualizer(AudioEqualizer equalizer);

    AudioFormat getFormat();

    void start();
    void stop();
}
