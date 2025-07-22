package AudioEqualizer;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AudioEqualizerTest {
	
	AudioEqualizer equalizer = new AudioEqualizer();
	
	@Test
	public void addFilterTest() {
		String[] filterNames = {"fakeFilter I", "fakeFilter II", "fakeFilter III"};
	
		int stackPos = 0;
		for (String filter : filterNames) {
			if (equalizer.addFilter(filter, stackPos) == true) {
				assertTrue(false);
			} else {
				assertTrue(true);
			}
		}
	}
	
	@Test
	public void removeFilterInRangeTest() {
		String[] filterNames = {"butterworth", "chebyshev II", "bessel"};
		
		int stackPos = 0;
		for (String filter: filterNames) {
			equalizer.addFilter(filter, stackPos);
		}
		
		int priorRackSize = equalizer.size();
		for (int i=priorRackSize; i>=0; --i) {
			try {
				equalizer.removeFilter(i);
			} catch (EmptyFilterRackException e) {
				assertTrue(false);
			} catch (InvalidFilterRackPositionException e) {
				assertTrue(false);
			}
		}
		
		if (equalizer.size() == 0 && priorRackSize != 0) {
			assertTrue(true);
		} else {
			assertTrue(false);
		}
		
	}
	
	@Test
	public void removeFilterOutRangeTest() {
		if (equalizer.isEmpty()) {
			try {
				equalizer.removeFilter(-999);
			} catch (Exception e) {
				assertTrue(true);
			}
		}
	}

	
	@Test
	public void getFilterTest() {
		// TODO
	}
	
	@Test
	public void isFilterTest() {
		// TODO
		
	}

	
	@Test
	public void setBandpassTest() {
		// TODO
	}
	
	@Test
	public void setBandstopTest() {
		// TODO
	}
	
	@Test
	public void setHighpassTest() {
		// TODO
	}
	
	@Test
	public void setLowpassTest() {
		// TODO
	}
	
	
	@Test
	public void processDataTest() {
		// TODO
	}
	
}
