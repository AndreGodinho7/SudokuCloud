import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class RequestsMeasurements{
    public ConcurrentHashMap<Long, List<Measurement>> requests =
        new ConcurrentHashMap<Long, List<Measurement>>();

    RequestsMeasurements(){}

    public void record(long key, Measurement measure) {
        List<Measurement> measurements = requests.get(key);
        if (measurements == null){
            requests.putIfAbsent(key, Collections.synchronizedList(new ArrayList<Measurement>()));
            measurements = requests.get(key);
        }
        measurements.add(measure);
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "RequestsMeasurements{" +
                "requests= " + requests +
                '}';
    }
}