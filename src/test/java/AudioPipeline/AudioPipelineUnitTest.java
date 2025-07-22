package AudioPipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AudioPipelineUnitTest {

    private AudioPipeline pipeline;
    private FakeTargetDataLine fakeLine;
    private FakeAudioEqualizer fakeEqualizer;
    private AudioFormat testFormat;

    @BeforeEach
    void setUp() {
        // Define a standard audio format for all tests.
        testFormat = new AudioFormat(44100f, 64, 2, true, false);
        fakeEqualizer = new FakeAudioEqualizer();
    }

    @Test
    void testConstructorInitializesFormat() {
        // Arrange
        fakeLine = new FakeTargetDataLine(testFormat, new byte[0], false);
        
        // Act
        pipeline = new AudioPipeline(fakeLine);

        // Assert
        assertNotNull(pipeline.getFormat());
        assertEquals(testFormat, pipeline.getFormat());
        assertEquals(44100f, pipeline.sampleRate);
        assertEquals(64, pipeline.bitDepth);
        assertEquals(2, pipeline.channels);
    }

    @Test
    void testStartAndStop() throws InterruptedException {
        // Arrange: Create a fake line that will block when read() is called.
        fakeLine = new FakeTargetDataLine(testFormat, new byte[2048], true);
        pipeline = new AudioPipeline(fakeLine);
        pipeline.setEqualizer(fakeEqualizer);

        // Act: Start the pipeline.
        pipeline.start();

        // Wait until the pipeline's run() loop has called the read() method.
        // This guarantees the pipeline is in a running state.
        assertTrue(fakeLine.awaitRead(200, TimeUnit.MILLISECONDS), "Pipeline did not enter read() method in time.");
        
        // Assert that the line is now running and open.
        assertTrue(fakeLine.isRunning(), "Line should be running after pipeline.start() and entering read().");
        assertTrue(fakeLine.isOpen(), "Line should be open after pipeline.start().");

        // Act: Stop the pipeline.
        pipeline.stop();
        
        // Assert that the line is now stopped and closed.
        assertFalse(fakeLine.isRunning(), "Line should be stopped after pipeline.stop().");
        assertFalse(fakeLine.isOpen(), "Line should be closed after pipeline.stop().");
    }

    @Test
    void testRunMethodProcessesData() throws InterruptedException {
        // Arrange: Create a fake line that reads its data and then finishes.
        fakeLine = new FakeTargetDataLine(testFormat, new byte[2048], false);
        pipeline = new AudioPipeline(fakeLine);
        pipeline.setEqualizer(fakeEqualizer);

        // Act: Start the pipeline and let it run to completion.
        pipeline.start();
        TimeUnit.MILLISECONDS.sleep(200); // Give time for the thread to run and finish.

        // Assert: Check our fake equalizer to see if its processData method was called.
        assertTrue(fakeEqualizer.processDataCalled, "Equalizer's processData should have been called by the run loop.");
        
        pipeline.stop(); // Clean up the executor service.
    }

    @Test
    void testRunMethodStopsOnInvalidFilterException() throws InterruptedException {
        // Arrange: Configure the fake equalizer to throw an exception.
        fakeEqualizer.throwExceptionOnProcess = true;
        fakeLine = new FakeTargetDataLine(testFormat, new byte[2048], false);
        pipeline = new AudioPipeline(fakeLine);
        pipeline.setEqualizer(fakeEqualizer);

        // Act: Start the pipeline.
        pipeline.start();
        TimeUnit.MILLISECONDS.sleep(200);

        // Assert: The run loop should have exited after the exception.
        assertTrue(fakeEqualizer.processDataCalled, "processData should have been called, triggering the exception.");
        
        pipeline.stop();
    }

}
