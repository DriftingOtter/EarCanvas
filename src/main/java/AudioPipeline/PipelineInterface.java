package AudioPipeline;

import javax.sound.sampled.AudioFormat;

public interface PipelineInterface extends Runnable {

    AudioFormat getFormat();

    void start();
    void stop();
    
}