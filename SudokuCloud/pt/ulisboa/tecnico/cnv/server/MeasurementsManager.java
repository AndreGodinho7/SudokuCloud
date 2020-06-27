package pt.ulisboa.tecnico.cnv.server;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.cnv.requestinfo.Measurement;

public class MeasurementsManager {

    private static MeasurementsManager manager;

//    private static ConcurrentHashMap<Long, List<Measurement>> measurements =
//            new ConcurrentHashMap<Long,  List<Measurement>>();

    private static ConcurrentHashMap<Long, Measurement> measurements =
            new ConcurrentHashMap<Long,  Measurement>();

    public static MeasurementsManager getManager() {
        if (manager == null) {
            manager = new MeasurementsManager();
        }
        return manager;
    }

    public Measurement getMeasurement(long threadID){
        return measurements.get(threadID);
    }

    public synchronized void insertMeasurement(Measurement measure){
        //System.out.println("Thread ID: "+Thread.currentThread().getId());
        measurements.put(measure.getThreadID(), measure);

    }

    public synchronized static void incMethodsManager(){
//        System.out.println("Increment metrics - Thread ID: "+Thread.currentThread().getId());
        Measurement current = measurements.get(Thread.currentThread().getId());
        current.incMethods();
        measurements.replace(Thread.currentThread().getId(), current);
    }


    public void calcRequestCost(long threadID){
        Measurement current = measurements.get(threadID);;
        current.calculateCost();
    }

    @Override
    public String toString() {
        return "MeasurementsManager\n{" +
                "\n     All Measurements:\n         " + measurements +
                "\n}\n";
    }

//    public synchronized void writeMeasurementsManager(long threadID, String filename) throws FileNotFoundException, UnsupportedEncodingException {
//        // write a .txt output file
//        try {
//            FileWriter file = new FileWriter(filename);
//            PrintWriter writer = new PrintWriter(file);
//            writer.print(this.measurements);
//            writer.close();
//            file.close();
//        } catch (FileNotFoundException e) {
//            System.out.println("FileNotFoundException: writing output Measurements Manager file.");
//            e.printStackTrace();
//            throw e;
//        } catch (IOException e) {
//            System.err.print("IOException: writing output Measurements Manager file.");
//        }
//    }
    public synchronized void removeMeasurement(long threadID){
        measurements.remove(threadID);
    }
}

