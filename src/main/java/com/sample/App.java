package com.sample;

import AudioPipeline.AudioPipeline;
import AudioPipeline.PipelineInterface;

public class App {
    public static void main(String[] args) {
    	
    	PipelineInterface ap = new AudioPipeline();
    	ap.start();
    	ap.run();
    	ap.stop();
    }
}
