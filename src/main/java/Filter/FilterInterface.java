package Filter;

import uk.me.berndporr.iirj.Cascade;

public interface FilterInterface {
	
	public int 	  getOrder();
	public double getSamplerate();
	public double getCenterFrequnecy();
	public double getFrequencyWidth();
    public double getCutoffFrequnecy();
	public double getRippleDb();
    public Cascade getSettings();
    
}
