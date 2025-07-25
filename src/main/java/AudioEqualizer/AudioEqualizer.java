package AudioEqualizer;

import Filter.Filter;
import Filter.InvalidFilterException;
import uk.me.berndporr.iirj.Cascade;
import java.util.ArrayList;
import java.util.Optional;

public class AudioEqualizer implements AudioEqualizerInterface {

    protected ArrayList<Filter> filterRack;

    public AudioEqualizer() {
        this.filterRack = new ArrayList<>();
    }

    public Filter addFilter(Filter.FilterType filterType, int order, double sampleRate, Optional<Double> rippleDb, int stackPos) throws InvalidFilterException {
        Filter newFilter = new Filter(filterType, order, sampleRate, rippleDb);
        filterRack.add(stackPos, newFilter);
        return newFilter;
    }

    public boolean removeFilter(int filterPosition) throws EmptyFilterRackException, InvalidFilterRackPositionException {
        if (filterRack.isEmpty()) {
            throw new EmptyFilterRackException("Filter rack is empty.");
        }
        if (filterPosition < 0 || filterPosition >= filterRack.size()) {
            throw new InvalidFilterRackPositionException("Filter position specified does not exist in the rack.");
        }
        filterRack.remove(filterPosition);
        return true;
    }

    public Filter getFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException {
        if (filterRack.isEmpty()) {
            throw new EmptyFilterRackException("Filter rack is empty.");
        }
        return filterRack.get(filterPosition);
    }
    
    public double[] processData(double[] buffer) {
        for (Filter filter : filterRack) {
            Cascade settings = filter.getSettings();
            for (int i = 0; i < buffer.length; ++i) {
                buffer[i] = settings.filter(buffer[i]);
            }
        }
        return buffer;
    }

    public boolean isEmpty() {return filterRack.isEmpty();}
    public int size() {return filterRack.size();}
    public boolean isFull() {return false;}
    
}
