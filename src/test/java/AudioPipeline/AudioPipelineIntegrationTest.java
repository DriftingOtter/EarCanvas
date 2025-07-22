package AudioPipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AudioPipelineIntegrationTest {

    private AudioPipeline pipeline;
    private FakeTargetDataLine fakeLine;
    private FakeAudioEqualizer fakeEqualizer;
    private AudioFormat testFormat;

    @BeforeEach
    void setUp() {
        // Use a format that works with 8-byte doubles for easy conversion.
        testFormat = new AudioFormat(44100f, 64, 1, true, false);
        // Instantiate the external fake equalizer for each test.
        fakeEqualizer = new FakeAudioEqualizer();
    }

    @Test
    void testPipelinePassesCorrectDataToEqualizer() throws InterruptedException {
        // --- Arrange ---

        // 1. Create a known audio signal as an array of doubles.
        double[] originalSignal = {0.0, 1.0, -1.0, 0.5, -0.25, 0.125};
        
        // 2. Convert this known signal into a byte array for the fake line to "read".
        byte[] sourceData = toByteArray(originalSignal, false);
        
        // 3. Set up the fake line (non-blocking) and the pipeline.
        // We use the external FakeTargetDataLine class here.
        fakeLine = new FakeTargetDataLine(testFormat, sourceData, false);
        pipeline = new AudioPipeline(fakeLine);
        pipeline.setEqualizer(fakeEqualizer);

        // --- Act ---

        // 4. Start the pipeline and let it process the single buffer from the fake line.
        pipeline.start();
        TimeUnit.MILLISECONDS.sleep(200); // Give the thread time to run and finish.

        // --- Assert ---

        // 5. Verify that our fake equalizer's processData method was called.
        assertTrue(fakeEqualizer.processDataCalled, "The equalizer's processData method should have been called.");
        
        // 6. Assert that the data captured by the fake equalizer is identical to our original signal.
        // This proves the pipeline read, converted, and passed the data correctly.
        assertNotNull(fakeEqualizer.capturedData, "The equalizer should have captured some data.");
        assertArrayEquals(originalSignal, fakeEqualizer.capturedData, 0.00001, 
            "Data received by the equalizer must match the original signal.");

        pipeline.stop();
    }

    private byte[] toByteArray(double[] doubleArray, boolean bigEndian) {
        ByteBuffer buffer = ByteBuffer.allocate(doubleArray.length * 8);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        
        for (double d : doubleArray) {
            buffer.putDouble(d);
        }
        
        return buffer.array();
    }
}
