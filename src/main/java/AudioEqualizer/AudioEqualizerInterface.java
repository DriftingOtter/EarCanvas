package AudioEqualizer;

public interface AudioEqualizerInterface {

    void addFilter(Object filter, int rackPosition);
    boolean removeFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException;
    Object getFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException;

    boolean isFull();
    boolean isEmpty();
    int size();

    double[] processData(double[] buffer);
}