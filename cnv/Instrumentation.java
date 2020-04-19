import BIT.highBIT.BasicBlock;
import BIT.highBIT.ClassInfo;
import BIT.highBIT.Routine;
import pt.ulisboa.tecnico.cnv.requestinfo.Measurement;
import pt.ulisboa.tecnico.cnv.requestinfo.Request;
import pt.ulisboa.tecnico.cnv.server.MeasurementsManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.List;

public class Instrumentation {
    //private static PrintStream out = null;
    //private static int i_count = 0, b_count = 0, m_count = 0;
    final private static MeasurementsManager manager = MeasurementsManager.getManager();

    /* main reads in all the files class files present in the input directory,
     * instruments them, and outputs them to the specified output directory.
     */
    public static void main(String argv[]) throws FileNotFoundException, UnsupportedEncodingException {
        File file_in = new File(argv[0]);
        String infilenames[] = file_in.list();

        //Request request = new Request("teste", "teste", 1, 2, 3);
        //Measurement measure = new Measurement(Thread.currentThread().getId(), request);
        //manager.insertMeasurement(Thread.currentThread().getId(), measure);

        for (int i = 0; i < infilenames.length; i++) {
            String infilename = infilenames[i];
            if (infilename.endsWith(".class")) {
				// create class info object
				ClassInfo ci = new ClassInfo(argv[0] + System.getProperty("file.separator") + infilename);

                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
					routine.addBefore("Instrumentation", "mcount", new Integer(1));

                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        bb.addBefore("Instrumentation", "count", new Integer(bb.size()));
                    }
                }
                // ci.addAfter("Intrumentalize", "printICount", ci.getClassName());
                ci.write(argv[1] + System.getProperty("file.separator") + infilename);

                }
            }
        manager.writeMeasurementsManager(Thread.currentThread().getId(),"Instrumentation measurements");
    }
    public static synchronized void count(int incr) {
        List<Measurement> thread_measures = manager.getMeasurement(Thread.currentThread().getId());
        Measurement current = thread_measures.get(thread_measures.size()-1);
        current.incBlocks();
        current.incInstructions(incr);
    }

    public static synchronized void mcount(int incr) {
        List<Measurement> thread_measures = manager.getMeasurement(Thread.currentThread().getId());
        Measurement current = thread_measures.get(thread_measures.size()-1);
        current.incMethods();
    }

    //public static synchronized void printICount(String foo) {
    //    System.out.println(i_count + " instructions in " + b_count + " basic blocks were executed in " + m_count + " methods.");
    //}

}



