package ppfl.instrumentation;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import ppfl.WriterUtils;

public class GotAllTransformer extends Transformer {

    public GotAllTransformer(Set<String> transformedclazz, String logfilename, Set<TraceDomain> foldSet) {
        super(transformedclazz, logfilename, foldSet);
        CallBackIndex.tracewriter = new TraceSequence(logfilename);
        CallBackIndex.loopset = new ArrayList<>();
        this.initCache();
    }

    private void initCache() {
        // init loopset
        String folder = "trace/logs/mytrace/";
        File loopsetfile = new File(folder, "loopset.log");
        try (BufferedReader reader = new BufferedReader(new FileReader(loopsetfile))) {
            String t = null;
            while ((t = reader.readLine()) != null) {
                int start = Integer.valueOf(t.split(",")[0]);
                int end = Integer.valueOf(t.split(",")[1]);
                CallBackIndex.loopset.add(new BackEdge(start, end));
            }
        } catch (Exception e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
        CallBackIndex.initCompressInfos();

        String poolname = folder + "TracePool.ser";
        try{
            long startTime = System.currentTimeMillis();
            FileInputStream fileIn = new FileInputStream(poolname);
            BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
            ObjectInputStream in = new ObjectInputStream(bufferedIn);
            CallBackIndex.tracepool = (TracePool) in.readObject();
            in.close();
            fileIn.close();
            long endTime = System.currentTimeMillis();
            double time = (endTime - startTime) / 1000.0;
            System.out.println("[Agent] Pool read done");
            System.out.println("[Agent] Pool read time " + time);
        }
        catch(IOException i)
        {
            i.printStackTrace();
            return;
        }catch(ClassNotFoundException c)
        {
            System.out.println("TracePool class not found");
            c.printStackTrace();
            return;
        }
    }

    protected byte[] transformBody(String classname) {
        byte[] byteCode = null;
        classname = classname.replace("/", ".");
        String classcachefolder = "trace/classcache/";
        try {
            File classcache = new File(classcachefolder, classname + ".log");
            return java.nio.file.Files.readAllBytes(classcache.toPath());
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
        return byteCode;
    }

}
