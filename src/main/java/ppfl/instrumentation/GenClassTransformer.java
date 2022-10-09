package ppfl.instrumentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.lang.System;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import ppfl.instrumentation.opcode.InvokeInst;
import ppfl.instrumentation.opcode.OpcodeInst;

public class GenClassTransformer extends Transformer {

    private int traceMapIndex;
    private List<Integer> traceMap;

    private int getTraceMap() {
        int poolindex = traceMap.get(traceMapIndex);
        traceMapIndex++;
        return poolindex;
    }

    public GenClassTransformer(Map<String, ClassLoader> transformedclazz, String logfilename) {
        super(transformedclazz, logfilename);
        traceMapIndex = 0;
        traceMap = new ArrayList<>();
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

        }
        CallBackIndex.initCompressInfos();
        // init traceMap
        File tracemapfile = new File(folder, "tracemap.log");
        try (BufferedReader reader = new BufferedReader(new FileReader(tracemapfile))) {
            String t = reader.readLine();
            String[] indexs = t.split(",");
            for (String index : indexs) {
                traceMap.add(Integer.valueOf(index));
            }
        } catch (Exception e) {

        }

    }

    protected byte[] transformBody(String classname) {
        byte[] byteCode = null;
        classname = classname.replace("/", ".");

        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(classname);
            ConstPool constp = cc.getClassFile().getConstPool();
            CallBackIndex cbi = new CallBackIndex(constp, traceWriter);

            boolean instrumentJunit = true;// evaluation switch
            for (MethodInfo m : cc.getClassFile().getMethods()) {
                if (instrumentJunit && cc.getName().startsWith("junit") && !m.getName().startsWith("assert")) {
                    continue;
                }
                if (!m.isStaticInitializer()) {
                    transformBehavior(m, cc, constp, cbi);
                }
            }

            byteCode = cc.toBytecode();
            cc.detach();
        } catch (Exception e) {
            System.out.println(e);
            // debugLogger.error("[Bug]bytecode error", e);
        }
        // write the classfile cache
        String classcachefolder = "trace/classcache/";
        File file = new File(classcachefolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        try {
            java.nio.file.Files.write(Paths.get(classcachefolder, classname + ".log"), byteCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteCode;
    }

    private void transformBehavior(MethodInfo m, CtClass cc, ConstPool constp, CallBackIndex cbi)
            throws NotFoundException, BadBytecode {

        MethodInfo mi = m;
        CodeAttribute ca = mi.getCodeAttribute();
        // for abstract method, the ca is null
        if (ca == null)
            return;

        // add constants to constpool.
        // index will be used during instrumentation.
        instrumentByteCode(cc, mi, ca, constp, cbi);

        // log method name at the beginning of this method.
        CodeIterator ci = ca.iterator();
        int poolindex = getTraceMap();
        int instpos = ci.insertGap(6);
        int instindex = constp.addIntegerInfo(poolindex);
        ci.writeByte(19, instpos);// ldc_w
        ci.write16bit(instindex, instpos + 1);
        ci.writeByte(184, instpos + 3);// invokestatic
        ci.write16bit(cbi.logtraceindex, instpos + 4);

        // not sure if this is necessary.
        ca.computeMaxStack();

    }

    private void instrumentByteCode(CtClass cc, MethodInfo mi, CodeAttribute ca, ConstPool constp, CallBackIndex cbi)
            throws BadBytecode {
        // iterate every instruction
        CodeIterator ci = ca.iterator();
        while (ci.hasNext()) {
            // lookahead the next instruction.
            int index = ci.lookAhead();
            int op = ci.byteAt(index);
            OpcodeInst oi = Interpreter.map[op];
            int poolindex = getTraceMap();
            boolean needCompress = false;
            for (BackEdge loopedge : CallBackIndex.loopset) {
                if (loopedge.end == poolindex)
                    needCompress = true;
            }
            if (oi != null) {
                if (!mi.isStaticInitializer()) {
                    if (needCompress) {
                        oi.insertBeforeCompress(ci, constp, poolindex, cbi);
                    } else
                        oi.insertBefore(ci, constp, poolindex, cbi);
                }
            }
            // move to the next inst. everything below this will be inserted after the inst.
            index = ci.next();
            // print advanced information(e.g. value pushed)
            if (oi != null) {
                // if (oi.form > 42)
                // getstatic should be treated like invocation,
                // in the case that static-initializer may be called.
                // FIXME: what about the loaded obj of getstatic and new
                if (oi instanceof InvokeInst || oi.form == 178 || oi.form == 187) {// getstatic and new
                    if (!mi.isStaticInitializer()) {
                        oi.insertReturn(ci, constp, poolindex, cbi);
                    }
                } else {
                    if (!mi.isStaticInitializer()) {
                        oi.insertAfter(ci, index, constp, cbi);
                    }
                }
            }
        }
    }

}
