package ppfl.instrumentation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.lang.System;

import ppfl.MyWriter;
import ppfl.WriterUtils;

// import java.lang.management.ManagementFactory;

public class Transformer implements ClassFileTransformer {

    protected static MyWriter debugLogger = null;
    protected static Writer traceWriter = null;
    protected static final int maxMethodSize = 10000;
    // Map of transformed clazz, key: classname, value: classloader
    protected Set<String> transformedclazz;
    protected Set<TraceDomain> foldSet;

    /** filename for logging */
    public Transformer(Set<String> transformedclazz, String logfilename, Set<TraceDomain> foldSet) {
        this.transformedclazz = new HashSet<>();
        for(String classname : transformedclazz){
            // replace . with /
            this.transformedclazz.add(classname.replace(".", "/"));
        }
        
        this.foldSet = new HashSet<>();
        if(foldSet != null){
            for(TraceDomain foldMethod : foldSet){
                this.foldSet.add(foldMethod);
            }
        }

        File debugdir = new File("trace/debug/");
        debugdir.mkdirs();
        WriterUtils.setPath("trace/debug/");
        debugLogger = WriterUtils.getWriter(logfilename, true);

        File logdir = new File("trace/logs/run/");
        logdir.mkdirs();
        Interpreter.init();
        FileWriter file = null;
        try {
            file = new FileWriter("trace/logs/run/" + logfilename, false);
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }

        traceWriter = file;
        CallBackIndex.setWriter(traceWriter);
    }

    // should be overrided by GenPoolTransformer
    public void output() {

    }

    public void addhook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    long startTime = System.currentTimeMillis();
                    FileOutputStream outStream = new FileOutputStream(
                            "trace/logs/run/" + CallBackIndex.tracewriter.getName() + ".ser");
                    BufferedOutputStream bufferStream = new BufferedOutputStream(outStream);
                    ObjectOutputStream fileObjectOut = new ObjectOutputStream(bufferStream);
                    fileObjectOut.writeObject(CallBackIndex.tracewriter);
                    fileObjectOut.close();
                    outStream.close();
                    long endTime = System.currentTimeMillis();
                    double time = (endTime - startTime) / 1000.0;
                    traceWriter.write(String.format("[Agent] write trace done\n"));
                    traceWriter.write(String.format("[Agent] write trace time %f\n", time));
                    traceWriter.flush();

                } catch (IOException e) {
                    String sStackTrace = WriterUtils.handleException(e);
                    debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
                }
            }
        });

    }

    // should be Override
    protected byte[] transformBody(String classname) {
        byte[] byteCode = null;
        return byteCode;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            long startTime = System.currentTimeMillis();
            byte[] byteCode = classfileBuffer;
            // TODO modify here to transform all classes.
            if (className == null || !transformedclazz.contains(className))
                return byteCode;
            transformedclazz.remove(className);
            debugLogger.write(String.format("[Agent] class = %s\n", className));
            byte[] ret = transformBody(className);
            long endTime = System.currentTimeMillis();
            double time = (endTime - startTime) / 1000.0;
            debugLogger.write(String.format("[Agent] Transform time %f\n", time));
            return ret;
        } catch (Exception e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
            return null;
        }
    }
}