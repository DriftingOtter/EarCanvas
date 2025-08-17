package AudioProcessingRangler;

import NativeFilter.*;
import StandardFilter.StandardFilter;
import uk.me.berndporr.iirj.Cascade;
import java.util.ArrayList;

public class AudioProcessingRangler implements ProcessRanglerInterface {

    protected ArrayList<Object> filterRack;

    public AudioProcessingRangler() {
        this.filterRack = new ArrayList<>();
    }

    public void addFilter(Object filter, int rackPosition) {
        filterRack.add(rackPosition, filter);
    }

    public boolean removeFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException {
        if (filterRack.isEmpty()) {
            throw new EmptyFilterRackException("Filter position specified does not exist in the rack.");
        }
        if (filterPosition < 0 || filterPosition >= filterRack.size()) {
            throw new IndexOutOfBoundsException("Filter position specified does not exist in the rack.");
        }
        filterRack.remove(filterPosition);
        return true;
    }

    public Object getFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException {
        if (filterRack.isEmpty()) {
            throw new EmptyFilterRackException("Filter position specified does not exist in the rack.");
        }
        if (filterPosition < 0 || filterPosition >= filterRack.size()) {
            throw new IndexOutOfBoundsException("Filter position specified does not exist in the rack.");
        }
        return filterRack.get(filterPosition);
    }

    public double[] processData(double[] buffer) {
        for (Object filter : filterRack) {
            if (filter instanceof StandardFilter) {
                Cascade settings = ((StandardFilter)filter).getSettings();
                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = settings.filter(buffer[i]);
                }
            } else if (filter instanceof NativeFilterInterface) {
            	buffer = ((GraphicEqualizer)filter).process(buffer);
            }
        }
        return buffer;
    }

    public boolean isEmpty() { return filterRack.isEmpty(); }
    public int size() { return filterRack.size(); }
    public boolean isFull() { return false; }

}