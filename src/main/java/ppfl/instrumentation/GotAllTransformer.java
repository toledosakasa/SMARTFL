package ppfl.instrumentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import ppfl.WriterUtils;

public class GotAllTransformer extends Transformer {

    public GotAllTransformer(Map<String, ClassLoader> transformedclazz, String logfilename) {
        super(transformedclazz, logfilename);
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
