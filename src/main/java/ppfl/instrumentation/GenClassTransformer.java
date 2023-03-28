package ppfl.instrumentation;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ExceptionTable;

import ppfl.instrumentation.opcode.InvokeInst;
import ppfl.instrumentation.opcode.OpcodeInst;

import ppfl.WriterUtils;

public class GenClassTransformer extends Transformer {

    private int traceMapIndex;
    private List<Integer> traceMap;

    private int getTraceMap() {
        int poolindex = traceMap.get(traceMapIndex);
        traceMapIndex++;
        return poolindex;
    }

    public GenClassTransformer(Set<String> transformedclazz, String logfilename, Set<TraceDomain> foldSet) {
        super(transformedclazz, logfilename, foldSet);
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
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
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
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] IOException, %s\n", sStackTrace));
        }

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

        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(classname);
            ConstPool constp = cc.getClassFile().getConstPool();
            CallBackIndex cbi = new CallBackIndex(constp, traceWriter);

            boolean instrumentJunit = true;// evaluation switch
            for (MethodInfo m : cc.getClassFile().getMethods()) {
                // handle fold method
                String methodname = m.getName();
                String signature = m.getDescriptor();
                TraceDomain thisDomain = new TraceDomain(classname, methodname, signature);
                if(foldSet.contains(thisDomain)){
                    debugLogger.writeln("[Agent] fold method %s, class = %s \n", methodname, classname);
                    continue;
                }
                // handle big method, it seems insertgap will be slow
                CodeAttribute ca = m.getCodeAttribute();
                if(ca != null && ca.length() > maxMethodSize){
                    debugLogger.writeln("[Agent] ignore method %s, class = %s, size = %d\n", methodname, classname, ca.length());
                    continue;
                }

                if (instrumentJunit && cc.getName().startsWith("junit") && !m.getName().startsWith("assert")) {
                    continue;
                }
                transformBehavior(m, cc, constp, cbi);
            }

            byteCode = cc.toBytecode();
            cc.detach();
        } catch (Exception e) {
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] notfound of badbytecode, %s\n", sStackTrace));
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
            String sStackTrace = WriterUtils.handleException(e);
            debugLogger.write(String.format("[Bug] write class error, %s\n", sStackTrace));
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

		// int instpos_ = ci.insertGap(3);
		// ci.writeByte(184, instpos_);// invokestatic
		// ci.write16bit(cbi.stackindex, instpos_ + 1);

        // ci = ca.iterator();
        int poolindex = getTraceMap();
        int instpos = ci.insertExGap(6);
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

        ExceptionTable exceptionTB = ca.getExceptionTable();
        Set<Integer> handlerStart = new HashSet<>();
        int tbSize = exceptionTB.size();
        for(int i = 0; i < tbSize; i++){
            handlerStart.add(exceptionTB.handlerPc(i));
            //debugLogger.writeln("find hanlder, method = " + mi.getName() + ", index = " + exceptionTB.handlerPc(i));
        }

        debugLogger.writeln("handle %s::%s", cc.getName(), mi.getName());

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
            if(handlerStart.contains(CallBackIndex.tracepool.get(poolindex).index)){
                oi.insertStack(ci, cbi);
            }

            // getstatic, putstatic, new
            boolean do_before = !(op == 178 || op == 179 || op == 187);
            if (oi != null && do_before) {
                if (needCompress) {
                    oi.insertBeforeCompress(ci, constp, poolindex, cbi, false);
                } else
                    oi.insertBefore(ci, constp, poolindex, cbi, false);
            }
            // move to the next inst. everything below this will be inserted after the inst.
            index = ci.next();

            if(oi != null && !do_before){
                if (needCompress) {
                    oi.insertBeforeCompress(ci, constp, poolindex, cbi, true);
                } else
                    oi.insertBefore(ci, constp, poolindex, cbi, true);
            }

            // print advanced information(e.g. value pushed)
            if (oi != null) {
                if (oi instanceof InvokeInst) {
                    oi.insertReturn(ci, constp, poolindex, cbi);
                } else {
                    oi.insertAfter(ci, index, constp, cbi);
                }
            }
            // if(!ci.hasNext()){
            //     // // for clinit, add an end trace
            //     if(mi.getName().equals("<clinit>")){
            //         ci = ca.iterator();
            //         poolindex = getTraceMap();
            //         debugLogger.writeln("instrument clinit, index = " + poolindex);
            //         int instpos = ci.insertGap(6);
            //         int instindex = constp.addIntegerInfo(poolindex);
            //         ci.writeByte(19, instpos);// ldc_w
            //         ci.write16bit(instindex, instpos + 1);
            //         ci.writeByte(184, instpos + 3);// invokestatic
            //         ci.write16bit(cbi.rettraceindex, instpos + 4);
            //     }

            //     break;
            // }
        }
    }

}
