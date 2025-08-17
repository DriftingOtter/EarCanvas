package StandardFilter;

import uk.me.berndporr.iirj.Cascade;

public interface StandardFilterInterface {
	
	public int 	  getOrder();
	public double getSamplerate();
	public double getCenterFrequnecy();
	public double getFrequencyWidth();
    public double getCutoffFrequnecy();
	public double getRippleDb();
    public Cascade getSettings();
    
}
