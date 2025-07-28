package Filter;

import uk.me.berndporr.iirj.Bessel;
import uk.me.berndporr.iirj.Butterworth;
import uk.me.berndporr.iirj.Cascade;
import uk.me.berndporr.iirj.ChebyshevI;
import uk.me.berndporr.iirj.ChebyshevII;
import Filter.InvalidFilterException;
import java.util.Optional;

public class Filter implements FilterInterface {
    int order;
	double sampleRate;
	double centerFrequnecy;
	double frequnecyWidth;
    double cutoffFrequnecy;
	double rippleDb;

    FilterType filterType;
	Cascade settings;

    public enum FilterType {
		Butterworth, Bessel, ChebyshevI, ChebyshevII
	}
	
	public int getOrder() {return this.order;}
	public double getSamplerate() {return this.centerFrequnecy;}
	public double getCenterFrequnecy() {return this.centerFrequnecy;}
	public double getFrequencyWidth() {return this.frequnecyWidth;}
	public double getRippleDb() {return this.rippleDb;}
	public double getCutoffFrequnecy() {return this.cutoffFrequnecy;}
    public Cascade getSettings() {return this.settings;}

	private void setCenterFrequnecy(double centerFrequnecy) {this.centerFrequnecy = centerFrequnecy;}
	private void setFrequencyWidth(double frequnecyWidth) {this.frequnecyWidth = frequnecyWidth;}
	private void setCutoffFrequnecy(double cutoffFrequnecy) {this.cutoffFrequnecy = cutoffFrequnecy;}

    public Filter(FilterType filterType, int order, double sampleRate, Optional<Double> rippleDb) throws InvalidFilterException {
        try {
            switch (filterType) {
                case Butterworth:
                    settings = new Butterworth();
                    break;
                case Bessel:
                    settings = new Bessel();
                    break;
                case ChebyshevI:
                    settings = new ChebyshevI();
                    break;
                case ChebyshevII:
                    settings = new ChebyshevII();
                    break;
                default:
                    throw new RuntimeException();
            }
            
            this.filterType = filterType;
            this.order = order;
            this.sampleRate = sampleRate;
            this.rippleDb = rippleDb.orElse(0.0);
            
            } catch (Exception e) {
                    throw new InvalidFilterException("Invalid filter type provided. Please enter (i.e. Butterworth, Bessel, ChebyshevI, ChebyshevII)", e);
            }
    }
	
	public void setBandpass(double centerFrequnecy, double frequnecyWidth) throws UnsupportedOperationException {
        setCenterFrequnecy(centerFrequnecy); 
        setFrequencyWidth(frequnecyWidth);

        switch (filterType) {
            case Butterworth:
                ((Butterworth)settings).bandPass(order, sampleRate, centerFrequnecy, frequnecyWidth);
                break;
			case Bessel:
                ((Bessel)settings).bandPass(order, sampleRate, centerFrequnecy, frequnecyWidth);
                break;
			case ChebyshevI:
                ((ChebyshevI)settings).bandPass(order, sampleRate, centerFrequnecy, frequnecyWidth, rippleDb);
                break;
			case ChebyshevII:
                 ((ChebyshevII)settings).bandPass(order, sampleRate, centerFrequnecy, frequnecyWidth, rippleDb);
                 break;
			default:
				throw new UnsupportedOperationException("This filter does not support setting a bandpass.");
	    }
	}

	public void setBandstop(double centerFrequnecy, double frequnecyWidth) throws UnsupportedOperationException {
        setCenterFrequnecy(centerFrequnecy); 
        setFrequencyWidth(frequnecyWidth);

        switch (filterType) {
            case Butterworth:
                ((Butterworth)settings).bandStop(order, sampleRate, centerFrequnecy, frequnecyWidth);
				break;
			case Bessel:
                ((Bessel)settings).bandStop(order, sampleRate, centerFrequnecy, frequnecyWidth);
				break;
			case ChebyshevI:
                ((ChebyshevI)settings).bandStop(order, sampleRate, centerFrequnecy, frequnecyWidth, rippleDb);
				break;
			case ChebyshevII:
                 ((ChebyshevII)settings).bandStop(order, sampleRate, centerFrequnecy, frequnecyWidth, rippleDb);
				break;
			default:
			throw new UnsupportedOperationException("This filter does not support setting a bandstop.");
		}
	}

	public void setHighpass(double cutoffFrequnecy) throws UnsupportedOperationException {
        setCutoffFrequnecy(cutoffFrequnecy);

        switch (filterType) {
            case Butterworth:
                ((Butterworth)settings).highPass(order, sampleRate, cutoffFrequnecy);
				break;
			case Bessel:
                ((Bessel)settings).highPass(order, sampleRate, cutoffFrequnecy);
				break;
			case ChebyshevI:
                ((ChebyshevI)settings).highPass(order, sampleRate, cutoffFrequnecy, rippleDb);
				break;
			case ChebyshevII:
                 ((ChebyshevII)settings).highPass(order, sampleRate, cutoffFrequnecy, rippleDb);
				break;
			default:
					throw new UnsupportedOperationException("This filter does not support setting a highpass.");
		}
	}
	
	public void setLowpass(double cutoffFrequnecy) throws UnsupportedOperationException {
        setCutoffFrequnecy(cutoffFrequnecy);

        switch (filterType) {
            case Butterworth:
                ((Butterworth)settings).lowPass(order, sampleRate, cutoffFrequnecy);
				break;
			case Bessel:
                ((Bessel)settings).lowPass(order, sampleRate, cutoffFrequnecy);
				break;
			case ChebyshevI:
                ((ChebyshevI)settings).lowPass(order, sampleRate, cutoffFrequnecy, rippleDb);
				break;
			case ChebyshevII:
                 ((ChebyshevII)settings).lowPass(order, sampleRate, cutoffFrequnecy, rippleDb);
				break;
			default:
				throw new UnsupportedOperationException("This filter does not support setting a lowpass.");
		}
	}

}
