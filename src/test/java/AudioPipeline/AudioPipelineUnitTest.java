package AudioPipeline;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import AudioEqualizer.AudioEqualizer;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AudioPipelineUnitTest {

    private AudioPipeline pipeline;

    @Mock
    private TargetDataLine mockLine;

    @Mock
    private AudioEqualizer mockEqualizer;

    private AudioFormat testFormat;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Default format for basic unit tests
        testFormat = new AudioFormat(44100f, 16, 2, true, false);
        when(mockLine.getFormat()).thenReturn(testFormat);
        pipeline = new AudioPipeline(mockLine);
        pipeline.setEqualizer(mockEqualizer);
    }

    @Test
    void testConstructorInitializesFormat() {
        assertNotNull(pipeline.getFormat());
        assertEquals(testFormat, pipeline.getFormat());
        assertEquals(44100f, pipeline.sampleRate);
        assertEquals(16, pipeline.bitDepth);
        assertEquals(2, pipeline.channels);
    }

    @Test
    void testStartAndStopLifecycle() {
        pipeline.start();
        verify(mockLine, timeout(100)).start();
        pipeline.stop();
        verify(mockLine, timeout(100)).stop();
        verify(mockLine, timeout(100)).close();
    }

    @Test
    void testRunMethodProcessesDataWhenEqualizerIsActive() {
        when(mockEqualizer.size()).thenReturn(1);
        when(mockLine.read(any(byte[].class), anyInt(), anyInt())).thenReturn(1024).thenReturn(-1);

        pipeline.start();

        verify(mockEqualizer, timeout(500).times(1)).processData(any(double[].class));
        pipeline.stop();
    }

    @Test
    void testRunMethodSkipsProcessingWhenEqualizerIsEmpty() throws InterruptedException {
        when(mockEqualizer.size()).thenReturn(0);
        when(mockLine.read(any(byte[].class), anyInt(), anyInt())).thenReturn(1024).thenReturn(-1);

        pipeline.start();
        TimeUnit.MILLISECONDS.sleep(200);

        verify(mockEqualizer, never()).processData(any(double[].class));
        pipeline.stop();
    }
}