package AudioPipeline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import AudioProcessingRangler.AudioProcessingRangler;

@ExtendWith(MockitoExtension.class)
class AudioPipelineUnitTest {

    // Mocks for Java Sound API classes
    @Mock
    private TargetDataLine mockTargetLine;
    @Mock
    private SourceDataLine mockSourceLine;
    @Mock
    private AudioProcessingRangler mockEqualizer;

    // A static mock for the AudioSystem class, which is used to get audio lines
    private MockedStatic<AudioSystem> mockAudioSystem;

    private AudioPipeline audioPipeline;
    private AudioFormat standardFormat;

    @BeforeEach
    void setUp() throws LineUnavailableException {
        // Define a standard audio format for all tests.
        // 16-bit, stereo, signed, little-endian.
        standardFormat = new AudioFormat(44100, 16, 2, true, false);

        // Start the static mock of AudioSystem before each test.
        mockAudioSystem = mockStatic(AudioSystem.class);

        // --- FIX 1: Mocking the Constructor's Behavior ---
        // The AudioPipeline constructor immediately calls AudioSystem.getLine() to
        // determine the default format. We must mock this call BEFORE instantiating the class.
        TargetDataLine mockTempLineForConstructor = mock(TargetDataLine.class);
        mockAudioSystem.when(() -> AudioSystem.isLineSupported(any(DataLine.Info.class))).thenReturn(true);
        mockAudioSystem.when(() -> AudioSystem.getLine(any(DataLine.Info.class))).thenReturn(mockTempLineForConstructor);
        when(mockTempLineForConstructor.getFormat()).thenReturn(standardFormat);

        // Now it's safe to instantiate the class under test.
        audioPipeline = new AudioPipeline();

        // After the constructor is finished, we re-configure the mock to return the
        // specific mock lines needed for the start() method. We use argThat to
        // differentiate between requests for a TargetDataLine and a SourceDataLine.
        mockAudioSystem.when(() -> AudioSystem.getLine(argThat(info -> info.getLineClass() == TargetDataLine.class)))
                .thenReturn(mockTargetLine);
        mockAudioSystem.when(() -> AudioSystem.getLine(argThat(info -> info.getLineClass() == SourceDataLine.class)))
                .thenReturn(mockSourceLine);
    }

    @AfterEach
    void tearDown() {
        // Ensure the pipeline is stopped to terminate the background thread.
        audioPipeline.stop();
        // It's crucial to close the static mock after each test to avoid test pollution.
        mockAudioSystem.close();
    }

    @Test
    void testStart_OpensAndStartsLines() throws LineUnavailableException {
        // Action
        audioPipeline.start();

        // Verification
        // Verify that the pipeline opened and started the target and source lines exactly once.
        verify(mockTargetLine, times(1)).open(any(AudioFormat.class), anyInt());
        verify(mockTargetLine, times(1)).start();
        verify(mockSourceLine, times(1)).open(any(AudioFormat.class), anyInt());
        verify(mockSourceLine, times(1)).start();
    }

    @Test
    void testStop_StopsAndClosesLines() {
        // Setup: Start the pipeline first to ensure lines are open.
        audioPipeline.start();

        // Action
        audioPipeline.stop();

        // Verification
        // Verify that all lines are properly stopped, drained, and closed.
        verify(mockTargetLine, times(1)).stop();
        verify(mockTargetLine, times(1)).close();
        verify(mockSourceLine, times(1)).drain();
        verify(mockSourceLine, times(1)).stop();
        verify(mockSourceLine, times(1)).close();
    }

    @Test
    void testRun_ReadsAndWritesAudioData() throws InterruptedException {
        // Setup
        byte[] fakeAudioData = new byte[]{10, 20, 30, 40, 50, 60, 70, 80};
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger callCount = new AtomicInteger(0);

        // When targetLine.read() is called, return fake data once, then return 0
        when(mockTargetLine.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            if (callCount.incrementAndGet() == 1) {
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(fakeAudioData, 0, buffer, 0, fakeAudioData.length);
                latch.countDown(); // Signal that the read operation has completed.
                return fakeAudioData.length;
            } else {
                // Return 0 on subsequent calls to prevent infinite loop in tests
                return 0;
            }
        });

        // Action
        audioPipeline.start(); // This starts the run() method in a new thread.

        // Wait for the run loop to process our fake data.
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "The audio processing loop did not read data in time.");

        // Verification
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(mockSourceLine, timeout(1000).times(1)).write(captor.capture(), eq(0), eq(fakeAudioData.length));

        // Assert that the data written is the same as the data read.
        // We copy the captured array to ensure we only compare the relevant bytes.
        byte[] writtenData = Arrays.copyOf(captor.getValue(), fakeAudioData.length);
        assertArrayEquals(fakeAudioData, writtenData);
    }

    @Test
    void testRun_UsesEqualizerWhenSet() throws InterruptedException {
        // Setup
        audioPipeline.setEqualizer(mockEqualizer);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger callCount = new AtomicInteger(0);

        // Define consistent test data. 8 bytes for 16-bit stereo = 4 samples.
        byte[] originalData = new byte[]{10, 0, 20, 0, 30, 0, 40, 0};
        double[] processedDoubles = new double[]{0.1, 0.2, 0.3, 0.4};

        // Pre-calculate the expected byte array using the same method as AudioPipeline
        byte[] expectedProcessedBytes = toByteArrayMatchingAudioPipeline(processedDoubles, standardFormat);

        when(mockTargetLine.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            if (callCount.incrementAndGet() == 1) {
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(originalData, 0, buffer, 0, originalData.length);
                latch.countDown();
                return originalData.length;
            } else {
                return 0; // Prevent infinite loop
            }
        });

        // Mock the equalizer to return our pre-defined processed data.
        when(mockEqualizer.isEmpty()).thenReturn(false);
        when(mockEqualizer.processData(any(double[].class))).thenReturn(processedDoubles);

        // Action
        audioPipeline.start();
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "The audio processing loop did not read data in time.");

        // Verification
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(mockEqualizer, timeout(1000).times(1)).processData(any(double[].class));
        verify(mockSourceLine, timeout(1000).times(1)).write(captor.capture(), eq(0), eq(originalData.length));

        byte[] actualWrittenBytes = Arrays.copyOf(captor.getValue(), expectedProcessedBytes.length);
        assertArrayEquals(expectedProcessedBytes, actualWrittenBytes, "Data written to source line was not the processed data.");
    }

    /**
     * Helper method that matches the toByteArray method in AudioPipeline exactly
     */
    private byte[] toByteArrayMatchingAudioPipeline(double[] doubleArray, AudioFormat format) {
        int byteLength = doubleArray.length * (format.getSampleSizeInBits() / 8);
        ByteBuffer buffer = ByteBuffer.allocate(byteLength);
        buffer.order(format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (double sample : doubleArray) {
            double clampedSample = Math.max(-1.0, Math.min(1.0, sample));

            switch (format.getSampleSizeInBits()) {
                case 8:
                    buffer.put((byte) ((clampedSample * 127.0) + (127.0 + 1)));
                    break;
                case 16:
                    buffer.putShort((short) (clampedSample * 32767.0));
                    break;
                case 32:
                    if (format.getEncoding() == AudioFormat.Encoding.PCM_FLOAT) {
                        buffer.putFloat((float) clampedSample);
                    } else {
                        buffer.putInt((int) (clampedSample * 2147483647.0));
                    }
                    break;
                case 64:
                    if (format.getEncoding() == AudioFormat.Encoding.PCM_FLOAT) {
                        buffer.putDouble(clampedSample);
                    } else {
                        throw new UnsupportedOperationException("64-bit integer PCM is not supported.");
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported bit depth: " + format.getSampleSizeInBits());
            }
        }
        return buffer.array();
    }
} 