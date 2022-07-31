package ppfl.instrumentation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.slf4j.MDC;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Mnemonic;
import ppfl.MyWriter;
import ppfl.ProfileUtils;
import ppfl.WriterUtils;
import ppfl.instrumentation.opcode.GetFieldInst;
import ppfl.instrumentation.opcode.GetStaticInst;
import ppfl.instrumentation.opcode.InvokeInst;
import ppfl.instrumentation.opcode.LookupSwitchInst;
import ppfl.instrumentation.opcode.OpcodeInst;
import ppfl.instrumentation.opcode.PutFieldInst;
import ppfl.instrumentation.opcode.PutStaticInst;
import ppfl.instrumentation.opcode.TableSwitchInst;

// import java.lang.management.ManagementFactory;

public class TraceTransformer implements ClassFileTransformer {

	private boolean useCachedClass = false;
	public static boolean useNewTrace = true;

	private static MyWriter debugLogger = null;
	// LoggerFactory.getLogger(TraceTransformer.class);

	// The logger name
	public static final String TRACELOGGERNAME = "PPFL_LOGGER";
	public static final String SOURCELOGGERNAME = "PPFL_LOGGER_SOURCE";
	// public static final Logger traceLogger =
	// LoggerFactory.getLogger(TRACELOGGERNAME);
	// public static final Logger sourceLogger =
	// LoggerFactory.getLogger(SOURCELOGGERNAME);
	private static final int BUFFERSIZE = 1 << 20;
	private static BufferedWriter sourceWriter = null;
	private static BufferedWriter staticInitWriter = null;
	private static Writer traceWriter = null;
	private static BufferedWriter whatIsTracedWriter = null;
	/** The internal form class name of the class to transform */
	private String targetClassName;
	/** The class loader of the class we want to transform */
	private ClassLoader targetClassLoader;

	private boolean useD4jTest = false;
	private Set<String> d4jMethodNames = new HashSet<>();
	private boolean logSourceToScreen = false;
	private boolean simpleLog = false;

	private Set<String> transformedMethods = new HashSet<>();

	private static int howmanytimes = 0;
	private int copytimes;

	private static boolean first_addShutdownHook = true;

	private static List<String> stackwriter;

	/** filename for logging */
	public TraceTransformer(String targetClassName, ClassLoader targetClassLoader, String logfilename) {
		this.targetClassName = targetClassName;
		this.targetClassLoader = targetClassLoader;

		File debugdir = new File("trace/debug/");
		debugdir.mkdirs();
		WriterUtils.setPath("trace/debug/");
		debugLogger = WriterUtils.getWriter(logfilename, true);

		File logdir = new File("trace/logs/run/");
		logdir.mkdirs();
		Interpreter.init();
		setWhatIsTracedWriterFile();
		FileWriter file = null;
		try {
			file = new FileWriter("trace/logs/run/" + logfilename, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// TODO: 可以尝试把多个transformer改成一个来处理的格式
		CallBackIndex.tracewriter = new TraceSequence(logfilename);

		traceWriter = file;
		// traceWriter = new BufferedWriter(file, BUFFERSIZE);
		CallBackIndex.setWriter(traceWriter);

		stackwriter = new ArrayList<>();
		CallBackIndex.stackwriter = stackwriter;

		// traceWriter = new BufferedWriter(file, BUFFERSIZE);
		howmanytimes+=1;
		copytimes = howmanytimes;
	}

	private static void setStaticInitFile(String clazzname) {
		setStaticInitWriterFile(String.format("trace/logs/mytrace/%s.init.log", clazzname));
	}

	private static void setStaticInitWriterFile(String filename) {
		FileWriter file = null;
		try {
			file = new FileWriter(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		staticInitWriter = new BufferedWriter(file, BUFFERSIZE);
	}

	private static void setSourceFile(String clazzname) {
		setWriterFile(String.format("trace/logs/mytrace/%s.source.log", clazzname));
	}

	private static void setWriterFile(String filename) {
		FileWriter file = null;
		try {
			file = new FileWriter(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		sourceWriter = new BufferedWriter(file, BUFFERSIZE);
	}

	private static void setWhatIsTracedWriterFile() {
		FileWriter file = null;
		String filename = "trace/logs/mytrace/traced.source.log";
		try {
			File logdir = new File("trace/logs/mytrace/");
			logdir.mkdirs();
			file = new FileWriter(filename, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		whatIsTracedWriter = new BufferedWriter(file, BUFFERSIZE);
	}

	public void setLogSourceToScreen(boolean b) {
		this.logSourceToScreen = b;
	}

	private static void setSimpleLogFile() {
		FileWriter file = null;
		try {
			File logdir = new File("trace/logs/mytrace/");
			logdir.mkdirs();
			file = new FileWriter("trace/logs/mytrace/profile.log", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		traceWriter = file;
		// traceWriter = new BufferedWriter(file, BUFFERSIZE);
	}

	public void setSimpleLog(boolean b) {
		this.simpleLog = b;
		setSimpleLogFile();
	}

	public void setLogFile(String s) {
		// String logFile = null;
		// logFile = s.replace('\\', '.').replace('/', '.');
		// MDC.put("logfile", logFile);
	}

	public void setD4jDataFile(String filepath) {
		// System.out.println(filepath);
		useD4jTest = true;
		String methodstring = "methods.test.all=";
		try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
			String s = null;
			while ((s = reader.readLine()) != null) {
				if (!s.startsWith(methodstring))
					continue;
				String[] classandmethods = s.substring(methodstring.length()).split(";");
				// Collections.addAll(d4jMethodNames, methodnames);
				for (String tmp : classandmethods) {
					if (!tmp.isEmpty()) {
						String[] splt = tmp.split("::");
						if (splt.length < 2)
							continue;
						String[] methodsname = splt[1].split(",");
						for (String methodname : methodsname) {
							d4jMethodNames.add(splt[0] + "::" + methodname);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// System.out.println(d4jMethodNames.size());
		ProfileUtils.setD4jMethods(d4jMethodNames);
	}

	private boolean isD4jTestMethod(CtClass cc, CtBehavior m) {
		if (!useD4jTest) {
			return false;
		}
		String longname = cc.getName() + "::" + m.getName();
		return d4jMethodNames.contains(longname);
	}

	private void writeWhatIsTraced(String str) {
		if (this.simpleLog)
			return;
		try {
			whatIsTracedWriter.write(str);
			whatIsTracedWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected byte[] transformBody(String classname) {
		byte[] byteCode = null;
		classname = classname.replace("/", ".");
		// debugLogger.write(String.format("[Agent] Transforming class %s\n",
		// this.targetClassName));

		if (useCachedClass) {
			String classcachefolder = "trace/classcache/";
			File file = new File(classcachefolder);
			if (!file.exists()) {
				file.mkdirs();
			}
			File classcache = new File(classcachefolder, classname + ".log");
			if (!this.simpleLog && classcache.exists()) {
				// debugLogger.writeln("Cache loaded:" + this.targetClassName);
				try {
					return java.nio.file.Files.readAllBytes(classcache.toPath());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if (!this.simpleLog) {
			this.setLogger(classname);
			setSourceFile(classname);
			setStaticInitFile(classname);
		}

		try {
			ClassPool cp = ClassPool.getDefault();
			CtClass cc = cp.get(classname);
			ConstPool constp = cc.getClassFile().getConstPool();
			CallBackIndex cbi = new CallBackIndex(constp, traceWriter);
			// if (cc.getGenericSignature() != null) {
			// return cc.toBytecode();
			// }
			List<MethodInfo> methods = new ArrayList<>();
			// methods.addAll(cc.getClassFile().getMethods());

			if (!this.simpleLog) {
				MethodInfo staticInit = cc.getClassFile().getStaticInitializer();
				if (staticInit != null) {
					getStaticInitializerInfo(staticInit, cc, constp);
				}
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

			// for (CtMethod cm : cc.getDeclaredMethods()) {
			// methods.add(cm.getMethodInfo());
			// }
			// for (CtConstructor ccon : cc.getDeclaredConstructors()) {
			// methods.add(ccon.getMethodInfo());
			// }
			// for (MethodInfo m : methods) {
			// // if (!this.simpleLog && m.isStaticInitializer()) {
			// // continue;
			// // }
			// String longname = m.getName() + "#" + m.getDescriptor();
			// if (!transformedMethods.contains(longname)) {
			// transformedMethods.add(longname);
			// writeWhatIsTraced(longname + ",");
			// transformBehavior(m, cc);
			// }
			// }

			byteCode = cc.toBytecode();
			cc.detach();

		} catch (Exception e) {
			System.out.println(e);
			// debugLogger.error("[Bug]bytecode error", e);
		}
		if (!this.simpleLog && useCachedClass) {
			try {
				String classcachefolder = "trace/classcache/";
				java.nio.file.Files.write(Paths.get(classcachefolder, classname + ".log"), byteCode);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return byteCode;
	}

	private void getStaticInitializerInfo(MethodInfo m, CtClass cc, ConstPool constp) throws BadBytecode {
		MethodInfo mi = m;
		CodeAttribute ca = mi.getCodeAttribute();

		// ConstPool constp = mi.getConstPool();
		CodeIterator tempci = ca.iterator();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; tempci.hasNext(); i++) {
			int index = tempci.lookAhead();
			int ln = mi.getLineNumber(index);
			String getinst = getInstMap(tempci, index, constp);
			String sig = mi.getDescriptor();
			// ExceptionTable eTable =
			// m.getMethodInfo().getCodeAttribute().getExceptionTable();
			String linenumberinfo = ",lineinfo=" + cc.getName() + "#" + mi.getName() + "#" + sig + "#" + ln + "#" + index
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
			e.printStackTrace();
		}

	}

	private void transformBehavior(MethodInfo m, CtClass cc, ConstPool constp, CallBackIndex cbi) throws NotFoundException, BadBytecode {
		// hello in console
		// debugLogger.writeln("%s::%s", cc.getName(), m.getName());

		// if (!(m instanceof CtMethod)) {
		// return;
		// }

		// get iterator
		// MethodInfo mi = m.getMethodInfo();
		MethodInfo mi = m;
		CodeAttribute ca = mi.getCodeAttribute();

		// add constants to constpool.
		// index will be used during instrumentation.
		// ConstPool constp = mi.getConstPool();
		// CallBackIndex cbi = new CallBackIndex(constp, traceWriter);

		if (!this.simpleLog)
			instrumentByteCode(cc, mi, ca, constp, cbi);
		// log method name at the beginning of this method.
		if (!this.simpleLog) {
			CodeIterator ci = ca.iterator();
			if(useNewTrace){
				Trace longname = new Trace(cc.getName(), m.getName());
				int poolindex = TracePool.indexAt();
				TracePool.add(longname);

				int instpos = ci.insertGap(6);
				int instindex = constp.addIntegerInfo(poolindex);
				ci.writeByte(19, instpos);// ldc_w
				ci.write16bit(instindex, instpos + 1);
				ci.writeByte(184, instpos + 3);// invokestatic
				ci.write16bit(cbi.logtraceindex, instpos + 4);
			}
			else{
				String longname = String.format("%n###%s::%s", cc.getName(), m.getName());
				int instpos = ci.insertGap(6);
				int instindex = constp.addStringInfo(longname);
	
				ci.writeByte(19, instpos);// ldc_w
				ci.write16bit(instindex, instpos + 1);
				ci.writeByte(184, instpos + 3);// invokestatic
				ci.write16bit(cbi.logstringindex, instpos + 4);
			}
		}
		if (this.simpleLog) {
			ProfileUtils.init(constp, traceWriter);
			CodeIterator ci = ca.iterator();
			String longname = String.format("%s::%s", cc.getName(), m.getName());
			ProfileUtils.logMethodName(ci, longname, constp);
		}

		// not sure if this is necessary.
		ca.computeMaxStack();
		// flushing buffer
		if (!this.simpleLog) {
			try {
				sourceWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void instrumentByteCode(CtClass cc, MethodInfo mi, CodeAttribute ca, ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		// record line info and instructions, since instrumentation will change
		// branchbyte and byte index.
		CodeIterator tempci = ca.iterator();
		Map<Integer, String> instmap = new HashMap<>();
		Map<Integer, Integer> tracemap = new HashMap<>();
		for (int i = 0; tempci.hasNext(); i++) {
			int index = tempci.lookAhead();
			int ln = mi.getLineNumber(index);
			String getinst = getInstMap(tempci, index, constp);

			int opcode = tempci.byteAt(index);
			Integer load = null, store = null, popnum = null, pushnum = null;
			// Integer _default = null;
			// String _switch = null;
			String[] split = getinst.split(",");
			// now "branchbyte" and "switch" is not used in running trace
			// still need to handle "field"
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
				// if (infotype.equals("default")) {
				// 	_default = Integer.valueOf(infovalue);
				// }
				// if (infotype.equals("switch")) {
				// 	_switch = infovalue;
				// }
			}

			String calltype = null, callclass = null, callname = null;
			if(Interpreter.map[opcode] instanceof InvokeInst){
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

			// if(Interpreter.map[opcode] instanceof LookupSwitchInst ||
			// Interpreter.map[opcode] instanceof TableSwitchInst){
			// int num = tempci.u16bitAt(index + 1);
			// field = constp.getFieldrefName(num);
			// }

			String classname = cc.getName();
			String methodname = mi.getName();
			String signature = mi.getDescriptor();
			int nextinst = -1;
			tempci.next();
			if(tempci.hasNext())
				nextinst = tempci.lookAhead();
			Trace instruction = new Trace(opcode, ln, index, nextinst, load, store, popnum, pushnum, classname, methodname, signature);

			if(Interpreter.map[opcode] instanceof InvokeInst){
				instruction = new InvokeTrace(instruction, calltype, callclass, callname);
			}

			if (field != null) {
				instruction = new FieldTrace(instruction, field);
			}

			// if (_default != null) {
			// 	instruction = new SwitchTrace(instruction, _default, _switch);
			// }

			int poolindex = TracePool.indexAt();
			TracePool.add(instruction);
			tracemap.put(i, poolindex);

			// ExceptionTable eTable =
			// m.getMethodInfo().getCodeAttribute().getExceptionTable();
			String linenumberinfo = ",lineinfo=" + classname + "#" + methodname + "#" + signature + "#" + ln + "#" + index
					+ ",nextinst=";
			
			if (!tempci.hasNext()) {
				linenumberinfo = linenumberinfo + "-1";
			} else {
				linenumberinfo = linenumberinfo + String.valueOf(tempci.lookAhead());
			}
			instmap.put(i, getinst + linenumberinfo);
		}
		// iterate every instruction
		CodeIterator ci = ca.iterator();
		for (int i = 0; ci.hasNext(); i++) {
			// lookahead the next instruction.
			int index = ci.lookAhead();
			int op = ci.byteAt(index);
			OpcodeInst oi = Interpreter.map[op];
			// linenumber information.
			String instinfo = instmap.get(i);

			// insert bytecode right before this inst.
			// print basic information of this instruction
			// if (logSourceToScreen)
			// sourceLogger.info(instinfo);

			try {
				sourceWriter.write(instinfo);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if(tracemap.get(i) == null)
			{
				debugLogger.write("Empty Trace!\n");
				continue;
			}

			int poolindex = tracemap.get(i);

			if (oi != null) {
				if (!mi.isStaticInitializer()){
					if(useNewTrace)
					{
						// debugLogger.write(TracePool.get(poolindex).toString());
						// debugLogger.write("		insert, " + "poolindex = " + poolindex);
						oi.insertBefore(ci, constp, poolindex, cbi);
					}
					else
						oi.insertByteCodeBefore(ci, index, constp, instinfo, cbi);
				}
			}
			int previndex = index;
			// move to the next inst. everything below this will be inserted after the inst.
			index = ci.next();
			// print advanced information(e.g. value pushed)
			if (oi != null) {
				// if (oi.form > 42)
				// getstatic should be treated like invocation,
				// in the case that static-initializer may be called.
				// FIXME: what about the loaded obj of getstatic and new
				if (oi instanceof InvokeInst || oi.form == 178 || oi.form == 187) {// getstatic and new
					if (!mi.isStaticInitializer()){
						if(useNewTrace)
						{
							oi.insertReturn(ci, constp, poolindex, cbi);
						}
						else
							oi.insertReturnSite(ci, index, constp, instinfo, cbi);
					}
				} else {
					if (!mi.isStaticInitializer()){
						if(useNewTrace)
						{
							oi.insertAfter(ci, index, constp, cbi);
						}
						else
							oi.insertByteCodeAfter(ci, index, constp, cbi);
					}
				}
			}
		}
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if(first_addShutdownHook){
			first_addShutdownHook = false;
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						if(!useNewTrace){
							// String pid = ManagementFactory.getRuntimeMXBean().getName();
							// TraceTransformer.traceWriter.write(",Over
							// Now!"+"@"+copytimes+"@"+targetClassName+"\n");
							for (String s : stackwriter)
								TraceTransformer.traceWriter.write(s);
							TraceTransformer.traceWriter.close();
							// closing the stream may trigger double-close bug.
							// TraceTransformer.traceWriter.close();
						}
						else{
							// int size = CallBackIndex.tracewriter.size();
							// for(int i=0;i<size;i++){
							// 	DynamicTrace trace = CallBackIndex.tracewriter.get(i);
							// 	traceWriter.write(trace.toString());
							// }

							FileOutputStream outStream = new FileOutputStream("trace/logs/run/" + CallBackIndex.tracewriter.getName()+".ser");
							ObjectOutputStream fileObjectOut = new ObjectOutputStream(outStream);
							fileObjectOut.writeObject(CallBackIndex.tracewriter);
							fileObjectOut.close();
							outStream.close();
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
		try {
			byte[] byteCode = classfileBuffer;
			// TODO modify here to transform all classes.
			String finalTargetClassName = this.targetClassName.replace(".", "/"); // replace . with /
			if (className == null || !className.equals(finalTargetClassName) || loader == null
					|| !loader.equals(targetClassLoader)) {
				return byteCode;
			}
			byte[] ret = transformBody(className);
			// debugLogger.write(String.format("\n%s\n", className));
			// for(byte b: ret)
			// debugLogger.write(String.format("%x ", b));
			return ret;
			// return transformBody(className);

			// return transformBody(loader, className, classBeingRedefined,
			// protectionDomain, classfileBuffer);
		} catch (Exception e) {
			// debugLogger.error("[Bug]Exception", e);
			e.printStackTrace();
			return null;
		}
	}

	private void setLogger(String clazzname) {
		// MDC.put("sourcefile", clazzname);
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