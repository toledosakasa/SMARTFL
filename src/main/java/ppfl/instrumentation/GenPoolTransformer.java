package ppfl.instrumentation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
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
import javassist.bytecode.Mnemonic;

import ppfl.instrumentation.opcode.GetFieldInst;
import ppfl.instrumentation.opcode.GetStaticInst;
import ppfl.instrumentation.opcode.InvokeInst;
import ppfl.instrumentation.opcode.OpcodeInst;
import ppfl.instrumentation.opcode.PutFieldInst;
import ppfl.instrumentation.opcode.PutStaticInst;

import ppfl.WriterUtils;

public class GenPoolTransformer extends Transformer {

    private BufferedWriter sourceWriter = null;
    private BufferedWriter staticInitWriter = null;
    private BufferedWriter whatIsTracedWriter = null;
    private final int BUFFERSIZE = 1 << 20;
    private List<Integer> traceMap;
    private StaticAnalyzer staticAnalyzer = null;

    public GenPoolTransformer(Map<String, ClassLoader> transformedclazz, String logfilename) {
        super(transformedclazz, logfilename);
        File logdir = new File("trace/logs/mytrace/");
        logdir.mkdirs();
        setWhatIsTracedWriterFile();
        traceMap = new ArrayList<>();
        CallBackIndex.tracepool = new TracePool();
        CallBackIndex.loopset = new ArrayList<>();
    }

    @Override
    public void output() {
        this.writeTracePool();
        this.staticAnalysis();
        this.writeStaticAnalyzer();
        this.writeTraceMap();
    }

    private void writeTracePool() {
        long startTime = System.currentTimeMillis();
        try {
            String poolfolder = "trace/logs/mytrace/";
            FileOutputStream outStream = new FileOutputStream(poolfolder + "TracePool" + ".ser");
            BufferedOutputStream bufferStream = new BufferedOutputStream(outStream);
            ObjectOutputStream fileObjectOut = new ObjectOutputStream(bufferStream);
            fileObjectOut.writeObject(CallBackIndex.tracepool);
            // debugLogger.writeln("Write Tracepool");
            fileObjectOut.close();
            outStream.close();
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
        long endTime = System.currentTimeMillis();
        double time = (endTime - startTime) / 1000.0;
        debugLogger.write(String.format("[Agent] Pool write done\n"));
        debugLogger.write(String.format("[Agent] Pool write time %f\n", time));
    }

    private void staticAnalysis() {
        // long startTime = System.currentTimeMillis();
        staticAnalyzer = new StaticAnalyzer();
        staticAnalyzer.setTracePool(CallBackIndex.tracepool);
        staticAnalyzer.setLoopSet(CallBackIndex.loopset);
        staticAnalyzer.parse(debugLogger);
        staticAnalyzer.get_pre_idom();
        staticAnalyzer.find_loop();
        CallBackIndex.initCompressInfos();
        // this.staticAnalyzer.get_post_idom();
        // staticAnalyzer.clear();
        // CallBackIndex.staticAnalyzer.setTracePool(null);
        // long endTime = System.currentTimeMillis();
        // double time = (endTime - startTime) / 1000.0;
        // debugLogger.write(String.format("[Agent] SA done\n"));
        // debugLogger.write(String.format("[Agent] SA time %f\n", time));
    }

    private void writeStaticAnalyzer() {
        // long startTime = System.currentTimeMillis();
        try {
            FileWriter SALogger = new FileWriter("trace/logs/mytrace/" + "loopset.log");
            for (BackEdge loopedge : CallBackIndex.loopset) {
                SALogger.write(String.format("%d,%d\n", loopedge.start, loopedge.end));
            }
            SALogger.close();
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
        // long endTime = System.currentTimeMillis();
        // double time = (endTime - startTime) / 1000.0;
        // debugLogger.write(String.format("[Agent] Loop write\n"));
        // debugLogger.write(String.format("[Agent] Loop write time %f\n", time));

    }

    private void writeTraceMap() {
        try {
            FileWriter file = new FileWriter("trace/logs/mytrace/" + "tracemap.log");
            BufferedWriter traceMapLogger = new BufferedWriter(file, BUFFERSIZE);
            for (Integer index : traceMap) {
                traceMapLogger.write(String.format("%d,", index));
            }
            traceMapLogger.close();
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
    }

    private void setWhatIsTracedWriterFile() {
        FileWriter file = null;
        String filename = "trace/logs/mytrace/traced.source.log";
        try {
            file = new FileWriter(filename);
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
        whatIsTracedWriter = new BufferedWriter(file, BUFFERSIZE);
    }

    private void writeWhatIsTraced(String str) {
        try {
            whatIsTracedWriter.write(str);
            whatIsTracedWriter.flush();
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
    }

    private void setSourceFile(String clazzname) {
        String filename = String.format("trace/logs/mytrace/%s.source.log", clazzname);
        FileWriter file = null;
        try {
            file = new FileWriter(filename);
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
        sourceWriter = new BufferedWriter(file, BUFFERSIZE);
    }

    private void setStaticInitFile(String clazzname) {
        String filename = String.format("trace/logs/mytrace/%s.init.log", clazzname);
        FileWriter file = null;
        try {
            file = new FileWriter(filename);
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
        staticInitWriter = new BufferedWriter(file, BUFFERSIZE);
    }

    private void getStaticInitializerInfo(MethodInfo m, CtClass cc, ConstPool constp) throws BadBytecode {
        MethodInfo mi = m;
        CodeAttribute ca = mi.getCodeAttribute();

        // ConstPool constp = mi.getConstPool();
        CodeIterator tempci = ca.iterator();
        StringBuilder sb = new StringBuilder();
        while (tempci.hasNext()) {
            int index = tempci.lookAhead();
            int ln = mi.getLineNumber(index);
            String getinst = getInstMap(tempci, index, constp);
            String sig = mi.getDescriptor();
            // ExceptionTable eTable =
            // m.getMethodInfo().getCodeAttribute().getExceptionTable();
            String linenumberinfo = ",lineinfo=" + cc.getName() + "#" + mi.getName() + "#" + sig + "#" + ln + "#"
                    + index
                    + ",nextinst=";

            tempci.next();
            if (!tempci.hasNext()) {
                linenumberinfo = linenumberinfo + "-1";
            } else {
                linenumberinfo = linenumberinfo + String.valueOf(tempci.lookAhead());
            }
            String instinfo = getinst + linenumberinfo;
            sb.append(instinfo);
        }
        try {
            staticInitWriter.write(sb.toString());
            staticInitWriter.flush();
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }

    }

    @Override
    protected byte[] transformBody(String classname) {
        byte[] byteCode = null;
        classname = classname.replace("/", ".");
        setSourceFile(classname);
        setStaticInitFile(classname);

        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(classname);
            ConstPool constp = cc.getClassFile().getConstPool();
            CallBackIndex cbi = new CallBackIndex(constp, traceWriter);
            MethodInfo staticInit = cc.getClassFile().getStaticInitializer();
            if (staticInit != null) {
                // TODO, 似乎对static final 且右侧是常数的话，就不会出现在这里,例如 Math 104
                getStaticInitializerInfo(staticInit, cc, constp);
            }
            if (!cc.getClassFile().getMethods().isEmpty() || cc.getClassFile().getSuperclass() != null) {
                writeWhatIsTraced("\n" + classname + "::");
            }

            boolean instrumentJunit = true;// evaluation switch
            for (MethodInfo m : cc.getClassFile().getMethods()) {
                if (instrumentJunit && cc.getName().startsWith("junit") && !m.getName().startsWith("assert")) {
                    continue;
                }
                if (!m.isStaticInitializer()) {
                    writeWhatIsTraced(m.getName() + "#" + m.getDescriptor() + ",");
                    transformBehavior(m, cc, constp, cbi);
                }
            }
            // dump class inheritance
            String superClassName = cc.getClassFile().getSuperclass();
            if (superClassName != null)
                writeWhatIsTraced(superClassName + "#" + "SuperClass");

            byteCode = cc.toBytecode();
            cc.detach();

        } catch (Exception e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] notfound of badbytecode, %s\n", sStackTrace));
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
        // ConstPool constp = mi.getConstPool();
        // CallBackIndex cbi = new CallBackIndex(constp, traceWriter);
        instrumentByteCode(cc, mi, ca, constp, cbi);
        // add the outpoint of this method into tracepool (for static analysis)
        Trace outpoint = new Trace(cc.getName(), m.getName(), m.getDescriptor());
        outpoint.setTypeOutPoint();
        CallBackIndex.tracepool.add(outpoint);

        // log method name at the beginning of this method.
        Trace longname = new Trace(cc.getName(), m.getName(), m.getDescriptor());
        longname.setTypeMethodLog();
        int poolindex = CallBackIndex.tracepool.indexAt();
        CallBackIndex.tracepool.add(longname);
        traceMap.add(poolindex);

        // not sure if this is necessary.
        ca.computeMaxStack();
        // flushing buffer
        try {
            sourceWriter.flush();
        } catch (IOException e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }
    }

    private void instrumentByteCode(CtClass cc, MethodInfo mi, CodeAttribute ca, ConstPool constp, CallBackIndex cbi)
            throws BadBytecode {
        // record line info and instructions, since instrumentation will change
        // branchbyte and byte index.
        CodeIterator tempci = ca.iterator();
        while (tempci.hasNext()) {
            int index = tempci.lookAhead();
            int ln = mi.getLineNumber(index);
            String getinst = getInstMap(tempci, index, constp);

            int opcode = tempci.byteAt(index);
            Integer load = null, store = null, popnum = null, pushnum = null;
            Integer branchbyte = null;
            Integer _default = null;
            String _switch = null;
            String[] split = getinst.split(",");
            for (String instinfo : split) {
                String[] splitinstinfo = instinfo.split("=");
                String infotype = splitinstinfo[0];
                String infovalue = splitinstinfo[1];
                if (infotype.equals("load")) {
                    load = Integer.valueOf(infovalue);
                }
                if (infotype.equals("store")) {
                    store = Integer.valueOf(infovalue);
                }
                if (infotype.equals("popnum")) {
                    popnum = Integer.valueOf(infovalue);
                }
                if (infotype.equals("pushnum")) {
                    pushnum = Integer.valueOf(infovalue);
                }
                if (infotype.equals("branchbyte")) {
                    branchbyte = Integer.valueOf(infovalue);
                }
                if (infotype.equals("default")) {
                    _default = Integer.valueOf(infovalue);
                }
                if (infotype.equals("switch")) {
                    _switch = infovalue;
                }
            }

            String calltype = null, callclass = null, callname = null;
            if (Interpreter.map[opcode] instanceof InvokeInst) {
                int callindex = tempci.u16bitAt(index + 1);
                calltype = constp.getMethodrefType(callindex);
                callclass = constp.getMethodrefClassName(callindex);
                callname = constp.getMethodrefName(callindex);
            }

            String field = null;
            if (Interpreter.map[opcode] instanceof PutFieldInst ||
                    Interpreter.map[opcode] instanceof GetFieldInst) {
                int num = tempci.u16bitAt(index + 1);
                field = constp.getFieldrefName(num);
            }

            if (Interpreter.map[opcode] instanceof PutStaticInst ||
                    Interpreter.map[opcode] instanceof GetStaticInst) {
                int num = tempci.u16bitAt(index + 1);
                field = constp.getFieldrefClassName(num) + "#" + constp.getFieldrefName(num);
            }

            String classname = cc.getName();
            String methodname = mi.getName();
            String signature = mi.getDescriptor();
            int nextinst = -1;
            tempci.next();
            if (tempci.hasNext())
                nextinst = tempci.lookAhead();
            Trace instruction = new Trace(opcode, ln, index, nextinst, load, store, popnum, pushnum, classname,
                    methodname, signature);

            if (Interpreter.map[opcode] instanceof InvokeInst) {
                instruction = new InvokeTrace(instruction, calltype, callclass, callname);
            }

            if (field != null) {
                instruction = new FieldTrace(instruction, field);
            }

            if (_default != null) {
                instruction = new SwitchTrace(instruction, _default, _switch);
            }

            if (branchbyte != null) {
                instruction = new BranchTrace(instruction, branchbyte);
            }

            int poolindex = CallBackIndex.tracepool.indexAt();
            CallBackIndex.tracepool.add(instruction);
            // TODO: 之后在GenPool时，把这个一起输出。第二次GenClass时，就直接用了
            traceMap.add(poolindex);

            String linenumberinfo = ",lineinfo=" + classname + "#" + methodname + "#" + signature + "#" + ln + "#"
                    + index
                    + ",nextinst=";

            if (!tempci.hasNext()) {
                linenumberinfo = linenumberinfo + "-1";
            } else {
                linenumberinfo = linenumberinfo + String.valueOf(tempci.lookAhead());
            }
            try {
                sourceWriter.write(getinst + linenumberinfo);
            } catch (IOException e) {
                String sStackTrace = WriterUtils.handleException(e);
                debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
            }

        }
    }

    private String getInstMap(CodeIterator ci, int index, ConstPool constp) {
        int op = ci.byteAt(index);
        String inst = null;
        String opc = Mnemonic.OPCODE[op];
        OpcodeInst oi = Interpreter.map[op];
        if (oi == null) {
            debugLogger.write("unsupported opcode: ");
            debugLogger.write(opc + "\n");
            return "";
        }
        inst = oi.getinst(ci, index, constp);
        return inst;
    }

}
