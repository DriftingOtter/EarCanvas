package AudioPipeline;

import AudioEqualizer.AudioEqualizer;
import AudioEqualizer.InvalidFilterException;

/**
 * Processing Properties:
 * 1. Track `processData` has been called (unit tests).
 * 2. Be configured to throw exception(s)  (unit tests).
 * 3. Siphon buffer data passed to it      (integration tests).
 */
public class FakeAudioEqualizer extends AudioEqualizer {
	
	public boolean processDataCalled = false;
    public boolean throwExceptionOnProcess = false;
    public double[] capturedData = null;

    public FakeAudioEqualizer() { 
        super(); 
    }

    @Override
    public double[] processData(double[] buffer) throws InvalidFilterException {
        this.processDataCalled = true;
        
        if (buffer != null) {
            this.capturedData = buffer.clone();
        }

        if (throwExceptionOnProcess) {
            throw new InvalidFilterException("Test exception from fake equalizer");
        }

        // Don't need to return processed data, we just need to know it is retrieving it.
        return new double[0];
    }
}