package AudioEqualizer;

import java.util.ArrayList;
import uk.me.berndporr.iirj.*;

public class AudioEqualizer implements AudioEqualizerInterface {
	
	protected ArrayList<Cascade> filterRack;

	@Override
	public boolean addFilter(String filterName, int stackPos) {
		switch (filterName) {
			case "butterworth":
				return filterRack.add(new Butterworth());
			case "bessel":
				return filterRack.add(new Bessel());
			case "chebyshev I":
				return filterRack.add(new ChebyshevI());
			case "chebyshev II":
				return filterRack.add(new ChebyshevII());
		default:
				return false;
		}
	}
	
	@Override
	public boolean removeFilter(int filterPosition) throws EmptyFilterRackException, InvalidFilterRackPositionException {
		if (filterRack.isEmpty()){
			throw new EmptyFilterRackException("No filter at the location specificed.");
		} else {
			try {
				filterRack.remove(filterPosition);
				return true;	
			} catch (Exception e) {
				throw new InvalidFilterRackPositionException("Filter position specified does not exist in the rack.");
				
			}
		}
	}

	@Override
	public Cascade getFilter(int filterPosition) throws EmptyFilterRackException {
		if (filterRack.isEmpty()){
			throw new EmptyFilterRackException("No filter at the location specificed.");
		} else {
			return filterRack.get(filterPosition);
		}

	}
	
	
	@Override
	public boolean isFull() {return false;}
	@Override
	public boolean isEmpty() {return filterRack.isEmpty();}
	
	@Override
	public int size() {return filterRack.size();}
	
	@Override
	public String isFilter(Cascade filter) throws InvalidFilterException{
		try {
			if (filter instanceof Butterworth) {
				return "butterworth";
			} else if (filter instanceof Bessel) {
				return "bessel";
			} else if (filter instanceof ChebyshevI) {
				return "chebyshev I";
			} else if (filter instanceof ChebyshevII) {
				return "chebyshev II";
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			throw new InvalidFilterException("Filter provided is not of supported type");
		} 
    }

	@Override
	public Cascade setBandpass(Cascade filter, int order, double sampleRate, double centerFrequnecy, double widthFrequnecy, double rippleDb) throws InvalidFilterException {
		try {
			String filterType = isFilter(filter);
			
			switch (filterType) {
				case "butterworth":
					((Butterworth) filter).bandPass(order, sampleRate, centerFrequnecy, widthFrequnecy);
					break;
				case "bessel":
					((Bessel) filter).bandPass(order, sampleRate, centerFrequnecy, widthFrequnecy);
					break;
				case "chebyshev I":
					((ChebyshevI) filter).bandPass(order, sampleRate, centerFrequnecy, widthFrequnecy, rippleDb);
					break;
				case "chebyshev II":
					((ChebyshevII) filter).bandPass(order, sampleRate, centerFrequnecy, widthFrequnecy, rippleDb);
					break;
				default:
					throw new Exception();
			}
			return filter;
		} catch (Exception e) {
			throw new InvalidFilterException("Filter provided is not of supported type");
		}
	}

	@Override
	public Cascade setBandstop(Cascade filter, int order, double sampleRate, double centerFrequnecy, double widthFrequnecy, double rippleDb) throws InvalidFilterException{
		try {
			String filterType = isFilter(filter);
			
			switch (filterType) {
				case "butterworth":
					((Butterworth) filter).bandStop(order, sampleRate, centerFrequnecy, widthFrequnecy);
					break;
				case "bessel":
					((Bessel) filter).bandStop(order, sampleRate, centerFrequnecy, widthFrequnecy);
					break;
				case "chebyshev I":
					((ChebyshevI) filter).bandStop(order, sampleRate, centerFrequnecy, widthFrequnecy, rippleDb);
					break;
				case "chebyshev II":
					((ChebyshevII) filter).bandStop(order, sampleRate, centerFrequnecy, widthFrequnecy, rippleDb);
					break;
				default:
					throw new Exception();
			}
			return filter;
		} catch (Exception e) {
			throw new InvalidFilterException("Filter provided is not of supported type");
		}
	}

	@Override
	public Cascade setHighpass(Cascade filter, int order, double sampleRate, double cutoffFrequnecy, double rippleDb)  throws InvalidFilterException{
		try {
			String filterType = isFilter(filter);
			
			switch (filterType) {
				case "butterworth":
					((Butterworth) filter).highPass(order, sampleRate, cutoffFrequnecy);
					break;
				case "bessel":
					((Bessel) filter).highPass(order, sampleRate, cutoffFrequnecy);
					break;
				case "chebyshev I":
					((ChebyshevI) filter).highPass(order, sampleRate, cutoffFrequnecy, rippleDb);
					break;
				case "chebyshev II":
					((ChebyshevII) filter).highPass(order, sampleRate, cutoffFrequnecy, rippleDb);
					break;
				default:
					throw new Exception();
			}
			return filter;
		} catch (Exception e) {
			throw new InvalidFilterException("Filter provided is not of supported type");
		}
	}
	
	@Override
	public Cascade setLowpass(Cascade filter, int order, double sampleRate, double cutoffFrequnecy, double rippleDb) throws InvalidFilterException {
		try {
			String filterType = isFilter(filter);
			
			switch (filterType) {
				case "butterworth":
					((Butterworth) filter).lowPass(order, sampleRate, cutoffFrequnecy);
					break;
				case "bessel":
					((Bessel) filter).lowPass(order, sampleRate, cutoffFrequnecy);
					break;
				case "chebyshev I":
					((ChebyshevI) filter).lowPass(order, sampleRate, cutoffFrequnecy, rippleDb);
					break;
				case "chebyshev II":
					((ChebyshevII) filter).lowPass(order, sampleRate, cutoffFrequnecy, rippleDb);
					break;
				default:
					throw new Exception();
			}
			return filter;
		} catch (Exception e) {
			throw new InvalidFilterException("Filter provided is not of supported type");
		}
	}

	
	@Override
	public double[] processData(double[] buffer) throws InvalidFilterException {
		for (Cascade filter : filterRack) {
			String filterType;
			for (int i=0; i<buffer.length; ++i) {
				try {
					filterType = isFilter((Cascade) filter);

					switch (filterType) {
					case "butterworth":
						buffer[i] = ((Butterworth) filter).filter(buffer[i]);
						break;
					case "bessel":
						buffer[i] = ((Bessel) filter).filter(buffer[i]);
						break;
					case "chebyshev I":
						buffer[i] = ((ChebyshevI) filter).filter(buffer[i]);
						break;
					case "chebyshev II":
						buffer[i] = ((ChebyshevII) filter).filter(buffer[i]);
						break;
					default:
						throw new Exception();
				}
				} catch (Exception e) {
					throw new InvalidFilterException("A filter within the rack seems to not be of a supported type");
				}
			}
		}
		return buffer;
	}

}
