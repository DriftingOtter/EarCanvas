package AudioEqualizer;

import Filter.Filter;
import Filter.InvalidFilterException;
import java.util.Optional;

public interface AudioEqualizerInterface {

    Filter addFilter(Filter.FilterType filterType, int order, double sampleRate, Optional<Double> rippleDb, int stackPos) throws InvalidFilterException;
    boolean removeFilter(int filterPosition) throws EmptyFilterRackException, InvalidFilterRackPositionException;
    Filter getFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException;

    boolean isFull();
    boolean isEmpty();
    int size();

    double[] processData(double[] buffer);
}
