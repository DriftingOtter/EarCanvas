package AudioPipeline;

import AudioEqualizer.AudioEqualizer;

public class FakeAudioEqualizer extends AudioEqualizer {
	
	public boolean processDataCalled = false;
    public double[] capturedData = null;

    public FakeAudioEqualizer() { 
        super(); 
    }

    @Override
    public double[] processData(double[] buffer) {
        this.processDataCalled = true;
        
        if (buffer != null) {
            this.capturedData = buffer.clone();
        }

        // The logic for throwing an exception is removed as the superclass method
        // no longer throws InvalidFilterException.

        // For testing, we don't need to return real data.
        return buffer;
    }
}
