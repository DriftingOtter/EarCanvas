package AudioPipeline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;

import AudioEqualizer.AudioEqualizer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AudioPipelineIntegrationTest {

    private AudioPipeline pipeline;

    @Mock
    private TargetDataLine mockLine;

    @Mock
    private AudioEqualizer mockEqualizer;

    // Constants from AudioPipeline for accurate test data generation
    private static final double NORM_8_BIT = 127.0;
    private static final double NORM_16_BIT = 32767.0;
    private static final double NORM_32_BIT_INT = 2147483647.0;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest(name = "should correctly convert {0}-bit data")
    @ValueSource(ints = {8, 16, 32, 64})
    void testPipelineCorrectlyConvertsAndPassesData(int bitDepth) {
        // --- Arrange ---
        AudioFormat.Encoding encoding;
        boolean isFloat; // 1. Declare isFloat here, outside the if/else block.

        if (bitDepth == 8) {
            encoding = AudioFormat.Encoding.PCM_UNSIGNED;
            isFloat = false; // 2. Assign its value inside the if/else blocks.
        } else {
            isFloat = (bitDepth == 32 || bitDepth == 64);
            encoding = isFloat ? AudioFormat.Encoding.PCM_FLOAT : AudioFormat.Encoding.PCM_SIGNED;
        }
        
        AudioFormat testFormat = new AudioFormat(encoding, 44100f, bitDepth, 1, bitDepth / 8, 44100f, false);

        when(mockLine.getFormat()).thenReturn(testFormat);
        pipeline = new AudioPipeline(mockLine);
        pipeline.setEqualizer(mockEqualizer);

        double[] originalSignal = {0.0, 1.0, -1.0, 0.5, -0.25, 0.125};
        // The call to toByteArray will now compile correctly.
        byte[] sourceData = toByteArray(originalSignal, bitDepth, pipeline.bigEndian, isFloat);

        when(mockEqualizer.size()).thenReturn(1);
        when(mockLine.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            byte[] buffer = invocation.getArgument(0);
            System.arraycopy(sourceData, 0, buffer, 0, sourceData.length);
            return sourceData.length;
        }).thenReturn(-1);

        // --- Act ---
        pipeline.start();

        // --- Assert ---
        ArgumentCaptor<double[]> captor = ArgumentCaptor.forClass(double[].class);
        verify(mockEqualizer, timeout(500)).processData(captor.capture());

     // Determine an appropriate tolerance based on the bit depth
        double delta = (bitDepth == 8) ? 0.01 : 0.0001;

        assertArrayEquals(originalSignal, captor.getValue(), delta, "Data for " + bitDepth + "-bit format must match original signal after conversion.");
        
        pipeline.stop();
    }

    /**
     * Helper method to convert a double array to a byte array for any bit depth,
     * mirroring the logic inside AudioPipeline.
     */
    private byte[] toByteArray(double[] doubleArray, int bitDepth, boolean bigEndian, boolean isFloat) {
        ByteBuffer buffer = ByteBuffer.allocate(doubleArray.length * (bitDepth / 8));
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (double sample : doubleArray) {
            double clampedSample = Math.max(-1.0, Math.min(1.0, sample));
            switch (bitDepth) {
                case 8:
                    buffer.put((byte) ((clampedSample * NORM_8_BIT) + (NORM_8_BIT+1)));
                    break;
                case 16:
                    buffer.putShort((short) (clampedSample * NORM_16_BIT));
                    break;
                case 32:
                    if (isFloat) {
                        buffer.putFloat((float) clampedSample);
                    } else {
                        buffer.putInt((int) (clampedSample * NORM_32_BIT_INT));
                    }
                    break;
                case 64:
                     if (isFloat) {
                        buffer.putDouble(clampedSample);
                     } else {
                        throw new UnsupportedOperationException("64-bit integer PCM is not supported.");
                     }
                    break;
            }
        }
        return buffer.array();
    }
}