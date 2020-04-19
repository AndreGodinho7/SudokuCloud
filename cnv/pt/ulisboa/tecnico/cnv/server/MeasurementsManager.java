package pt.ulisboa.tecnico.cnv.server;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import pt.ulisboa. tecnico.cnv.requestinfo.Measurement;


public class MeasurementsManager {

    private static MeasurementsManager manager;

    private ConcurrentHashMap<Long, List<Measurement>> measurements =
            new ConcurrentHashMap<Long,  List<Measurement>>();

    public static MeasurementsManager getManager() {
        if (manager == null) {
            manager = new MeasurementsManager();
            System.out.println("--- Created a new Measurement Manager. ---\n");
        }
        System.out.println("--- Returned Measurement Manager ---");
        return manager;
    }

    public  List<Measurement> getMeasurement(long threadID) {
        return measurements.get(threadID);
    }

    public void setMeasurements(ConcurrentHashMap<Long, List<Measurement>> measurements) {
        this.measurements = measurements;
    }

    public synchronized void insertMeasurement(long threadID, Measurement measure){
        List<Measurement> thread_measures = measurements.get(threadID);
        if (thread_measures == null) {
            measurements.putIfAbsent(threadID, Collections.synchronizedList(new ArrayList<Measurement>()));
            thread_measures = measurements.get(threadID);
        }
        thread_measures.add(measure);
    }

    @Override
    public String toString() {
        return "MeasurementsManager\n{" +
                "\n     All Measurements:\n         " + measurements +
                "\n}\n";
    }

    public void removeMeasurement(long threadID){
        this.measurements.remove(threadID);
    }

    public synchronized void writeMeasurementsManager(long threadID, String filename) throws FileNotFoundException, UnsupportedEncodingException {
        System.out.println("--- Will save the following measurements to output file ---\n");
        System.out.println(this.measurements+"\n");

        // write a .txt output file
        try {
            FileWriter file = new FileWriter(filename+".txt");
            PrintWriter writer = new PrintWriter(file);
            System.out.println("--- Saving measurements to output file ---");
            writer.print(this.measurements);
            System.out.println("--- Successfully written to output file "+filename+" ---");
            writer.close();
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException: writing output Measurements Manager file.");
            throw e;
        } catch (IOException e) {
            System.err.print("IOException: writing output Measurements Manager file.");
        }
    }

}

