package AudioEqualizer;

import Filter.Filter;

public interface AudioEqualizerInterface {

    void addFilter(Filter filter, int rackPosition);
    boolean removeFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException;
    Filter getFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException;

    boolean isFull();
    boolean isEmpty();
    int size();

    double[] processData(double[] buffer);
}
