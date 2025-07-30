package NativeFilter;

public interface NativeFilterInterface {

	public int getChannels();
	public int getBufferSize();

	public void setSampleRate(float sampleRate);
	public void setFrequency(float frequency);
	public void setQ(float q);
	public void setGain(float gainDb);

	public double[] process(double[] inputBuffer);

}