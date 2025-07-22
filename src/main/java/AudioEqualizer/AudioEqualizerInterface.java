package AudioEqualizer;

import uk.me.berndporr.iirj.Cascade;

public interface AudioEqualizerInterface {
	
	public boolean addFilter(String filterName, int filterPosition);
	public boolean removeFilter(int filterPosition) throws EmptyFilterRackException, InvalidFilterRackPositionException;
	
	public Cascade getFilter(int filterPosition) throws EmptyFilterRackException;

	
	public boolean isFull();
	public boolean isEmpty();
	public int size();

	public String isFilter(Cascade filter) throws InvalidFilterException;

	
	public Cascade setBandpass(Cascade filter, int order, double sampleRate, double centerFrequnecy, double widthFrequnecy, double rippleDb) throws InvalidFilterException;
	public Cascade setBandstop(Cascade filter, int order, double sampleRate, double centerFrequnecy, double widthFrequnecy, double rippleDb) throws InvalidFilterException;
	public Cascade setHighpass(Cascade filter, int order, double sampleRate, double cutoffFrequnecy, double rippleDb) throws InvalidFilterException;
	public Cascade setLowpass(Cascade filter, int order, double sampleRate, double cutoffFrequnecy, double rippleDb) throws InvalidFilterException;

	
	public double[] processData(double[] buffer) throws InvalidFilterException;
	
}
