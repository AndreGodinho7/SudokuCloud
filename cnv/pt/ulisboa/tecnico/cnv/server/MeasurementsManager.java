package pt.ulisboa.tecnico.cnv.server;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.cnv.requestinfo.Measurement;

public class MeasurementsManager {

    private static MeasurementsManager manager;

    private static ConcurrentHashMap<Long, List<Measurement>> measurements =
            new ConcurrentHashMap<Long,  List<Measurement>>();

    public static MeasurementsManager getManager() {
        if (manager == null) {
            manager = new MeasurementsManager();
        }
        return manager;
    }

    public static List<Measurement> getMeasurement(long threadID) {
        return measurements.get(threadID);
    }

    public synchronized void insertMeasurement(Measurement measure){
        List<Measurement> thread_measures = measurements.get(measure.getThreadID());
        if (thread_measures == null) {
            measurements.putIfAbsent(measure.getThreadID(), Collections.synchronizedList(new ArrayList<Measurement>()));
            thread_measures = measurements.get(measure.getThreadID());
        }
        thread_measures.add(measure);
    }

    public static void incMethodsManager(){
        List<Measurement> thread_measures = measurements.get(Thread.currentThread().getId());
        Measurement current = thread_measures.get(thread_measures.size() - 1);
        current.incMethods();
    }

    public void calcRequestCost(long threadID){
        List<Measurement> thread_measures = measurements.get(threadID);
        Measurement current = thread_measures.get(thread_measures.size() - 1);
        current.calculateCost();
    }

    @Override
    public String toString() {
        return "MeasurementsManager\n{" +
                "\n     All Measurements:\n         " + measurements +
                "\n}\n";
    }

    public synchronized void writeMeasurementsManager(long threadID, String filename) throws FileNotFoundException, UnsupportedEncodingException {
        // write a .txt output file
        try {
            FileWriter file = new FileWriter(filename);
            PrintWriter writer = new PrintWriter(file);
            writer.print(this.measurements);
            writer.close();
            file.close();
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException: writing output Measurements Manager file.");
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            System.err.print("IOException: writing output Measurements Manager file.");
        }
    }

}

