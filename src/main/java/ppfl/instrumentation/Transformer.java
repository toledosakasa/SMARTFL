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
import java.util.Map;
import java.lang.System;

import ppfl.MyWriter;
import ppfl.WriterUtils;

// import java.lang.management.ManagementFactory;

public class Transformer implements ClassFileTransformer {

    protected static MyWriter debugLogger = null;
    protected static Writer traceWriter = null;
    // Map of transformed clazz, key: classname, value: classloader
    protected Map<String, ClassLoader> transformedclazz;

    /** filename for logging */
    public Transformer(Map<String, ClassLoader> transformedclazz, String logfilename) {
        this.transformedclazz = transformedclazz;
        File debugdir = new File("trace/debug/");
        debugdir.mkdirs();
        WriterUtils.setPath("trace/debug/");
        debugLogger = WriterUtils.getWriter(logfilename, true);

        File logdir = new File("trace/logs/run/");
        logdir.mkdirs();
        Interpreter.init();
        FileWriter file = null;
        try {
            file = new FileWriter("trace/logs/run/" + logfilename, true);
        } catch (IOException e) {
            e.printStackTrace();
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
                    e.printStackTrace();
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
            if (className == null || !transformedclazz.containsKey(className))
                return byteCode;
            if (loader == null || !loader.equals(transformedclazz.get(className)))
                return byteCode;
            transformedclazz.remove(className);

            byte[] ret = transformBody(className);
            debugLogger.write(String.format("[Agent] class = %s\n", className));
            long endTime = System.currentTimeMillis();
            double time = (endTime - startTime) / 1000.0;
            debugLogger.write(String.format("[Agent] Transform time %f\n", time));
            return ret;
        } catch (Exception e) {
            // debugLogger.error("[Bug]Exception", e);
            e.printStackTrace();
            return null;
        }
    }
}