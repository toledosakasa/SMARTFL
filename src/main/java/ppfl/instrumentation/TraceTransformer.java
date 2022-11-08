package ppfl.instrumentation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
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
import java.lang.System;

import javafx.util.Callback;

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
import ppfl.instrumentation.InstrumentationAgent.CacheCond;

// import java.lang.management.ManagementFactory;

public class TraceTransformer implements ClassFileTransformer {

	// TODO: now the ser log is based on TracePool cache, try to find a better way
	public boolean useCachedClass = true; // has to be true, otherwise writeWhatIsTraced is wrong
	// public boolean foundCache = false;
	// public boolean foundSA = false;
	public CacheCond cacheCond = CacheCond.GotAll;
	public static boolean useNewTrace = true;
	public static boolean useIndexTrace = true;


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
	// Map of transformed clazz, key: classname, value: classloader
	private Map<String,ClassLoader> transformedclazz;
	private StaticAnalyzer staticAnalyzer = null;

	private boolean useD4jTest = false;
	private Set<String> d4jMethodNames = new HashSet<>();
	private boolean logSourceToScreen = false;
	private boolean simpleLog = false;

	private Set<String> transformedMethods = new HashSet<>();

	private boolean first_addShutdownHook = true;

	private static List<String> stackwriter;

	/** filename for logging */
	public TraceTransformer(Map<String,ClassLoader> transformedclazz, String logfilename, CacheCond cacheCond){
		this.transformedclazz = transformedclazz;
		this.cacheCond = cacheCond;
		File debugdir = new File("trace/debug/");
		debugdir.mkdirs();
		WriterUtils.setPath("trace/debug/");
		debugLogger = WriterUtils.getWriter(logfilename, true);

		File logdir = new File("trace/logs/run/");
		logdir.mkdirs();
		Interpreter.init();
		if(this.cacheCond == CacheCond.GenPool)
			setWhatIsTracedWriterFile();
		FileWriter file = null;
		try {
			file = new FileWriter("trace/logs/run/" + logfilename, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		CallBackIndex.tracewriter = new TraceSequence(logfilename);
		CallBackIndex.tracepool = new TracePool();
		CallBackIndex.loopset = new ArrayList<>();

		// long startTime = System.currentTimeMillis();
		if(this.cacheCond == CacheCond.GotAll || this.cacheCond == CacheCond.GenClass)
			this.initCache();
		// else if (this.cacheCond == CacheCond.GenClass){
		// 	this.initPool();
		// 	this.staticAnalysis();
		// 	this.writeStaticAnalyzer();
		// }
		// else{
		// 	// this.FillTracePool();
		// 	// this.writeTracePool();
		// 	// this.staticAnalysis();
		// 	// this.writeStaticAnalyzer();
		// }
		// long endTime = System.currentTimeMillis();
		// double time = (endTime - startTime) / 1000.0;
		// debugLogger.write(String.format("[Agent] SA init time %f\n", time));

		traceWriter = file;
		CallBackIndex.setWriter(traceWriter);

		stackwriter = new ArrayList<>();
		CallBackIndex.stackwriter = stackwriter;
	}

	private void initCache(){
		// String SAfolder = "trace/logs/mytrace/";
		// File SAcache = new File(SAfolder, "StaticAnalyzer.ser");
		// if (SAcache.exists()) {
		// 	try {
		// 		FileInputStream fileIn = new FileInputStream(SAcache);
		// 		ObjectInputStream in = new ObjectInputStream(fileIn);
		// 		CallBackIndex.staticAnalyzer = (StaticAnalyzer) in.readObject();
		// 		in.close();
		// 		fileIn.close();
		// 		foundSA = true;
		// 		debugLogger.write(String.format("[Agent] found SA\n"));
		// 	} catch (IOException e) {
		// 		e.printStackTrace();
		// 	} catch (ClassNotFoundException c) {
		// 		c.printStackTrace();
		// 	}
		// }
		// else{
		// 	CallBackIndex.staticAnalyzer = new StaticAnalyzer();
		// }

		String SAfolder = "trace/logs/mytrace/";
		File loopset = new File(SAfolder, "loopset.log");
		try (BufferedReader reader = new BufferedReader(new FileReader(loopset))){
			String t = null;
			while ((t = reader.readLine()) != null) {
				int start = Integer.valueOf(t.split(",")[0]);
				int end = Integer.valueOf(t.split(",")[1]);
				CallBackIndex.loopset.add(new BackEdge(start, end));
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		CallBackIndex.initCompressInfos();
		// foundSA = true;

		// if use index Trace, we don't need tracepool
		if(!TraceTransformer.useIndexTrace){
			try {
				String poolfolder = "trace/logs/mytrace/";
				File poolcache = new File(poolfolder, "TracePool" + ".ser");
				FileInputStream fileIn = new FileInputStream(poolcache);
				BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
				ObjectInputStream in = new ObjectInputStream(bufferedIn);
				CallBackIndex.tracepool = (TracePool) in.readObject();
				// debugLogger.writeln("Pool loaded:" + classname);
				// for(int i=0;i<tracepool.indexAt();i++)
				// 	debugLogger.writeln(tracepool.get(i).toString());
				in.close();
				fileIn.close();
			} catch (IOException e) {
				// debugLogger.writeln("IOException:" + classname);
				// debugLogger.writeln(e.toString());
				e.printStackTrace();
			} catch (ClassNotFoundException c) {
				// debugLogger.writeln("ClassNotFoundException:" + classname);
				// debugLogger.writeln(c.toString());
				c.printStackTrace();
			}
		}
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

	private void setSimpleLogFile() {
		FileWriter file = null;
		try {
			File logdir = new File("trace/logs/profile/");
			logdir.mkdirs();
			//TODO maybe change into multi files
			file = new FileWriter("trace/logs/profile/profile.log", true);
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
		// should be done in GenPool
		if(this.cacheCond != CacheCond.GenPool)
			return;
		if (this.simpleLog)
			return;
		try {
			whatIsTracedWriter.write(str);
			whatIsTracedWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeTracePool(){
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
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		double time = (endTime - startTime) / 1000.0;
		debugLogger.write(String.format("[Agent] Pool write done\n"));
		debugLogger.write(String.format("[Agent] Pool write time %f\n", time));
	}

	public void writeStaticAnalyzer(){
		// long startTime = System.currentTimeMillis();
		try {
			FileWriter SALogger = new FileWriter("trace/logs/mytrace/" + "loopset.log", true);
			for(BackEdge loopedge: CallBackIndex.loopset){
				SALogger.write(String.format("%d,%d\n", loopedge.start, loopedge.end));
			}
			SALogger.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// long endTime = System.currentTimeMillis();
		// double time = (endTime - startTime) / 1000.0;
		// debugLogger.write(String.format("[Agent] Loop write\n"));
		// debugLogger.write(String.format("[Agent] Loop write time %f\n", time));

	}

	public void staticAnalysis(){
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

	protected byte[] transformBody(String classname) {
		byte[] byteCode = null;
		classname = classname.replace("/", ".");
		// debugLogger.write(String.format("[Agent] Transforming class %s\n", classname));

		if (useCachedClass) {
			String classcachefolder = "trace/classcache/";
			File file = new File(classcachefolder);
			if (!file.exists()) {
				file.mkdirs();
			}
			// File poolcache = new File(classcachefolder, "TracePool" + ".ser");
			// File classcache = new File(classcachefolder, classname + ".log");
			// debugLogger.writeln("PoolCache " + poolcache.exists());
			// debugLogger.writeln("ClassCache " + classcache.exists());
			if (!this.simpleLog && this.cacheCond == CacheCond.GotAll) {
				// debugLogger.writeln("Cache loaded:" + classname);
				try {
					// // if use index Trace, we don't need tracepool
					// if(!TraceTransformer.useIndexTrace && !CallBackIndex.tracepool.hsinit()){
					// 	long startTime = System.currentTimeMillis();
					// 	FileInputStream fileIn = new FileInputStream(poolcache);
					// 	ObjectInputStream in = new ObjectInputStream(fileIn);
					// 	CallBackIndex.tracepool = (TracePool) in.readObject();
					// 	// debugLogger.writeln("Pool loaded:" + classname);
					// 	// for(int i=0;i<tracepool.indexAt();i++)
					// 	// 	debugLogger.writeln(tracepool.get(i).toString());
					// 	CallBackIndex.tracepool.init();
					// 	in.close();
					// 	fileIn.close();
					// 	long endTime = System.currentTimeMillis();
					// 	double time = (endTime - startTime) / 1000.0;
					// 	debugLogger.write(String.format("[Agent] Pool read done\n"));
					// 	debugLogger.write(String.format("[Agent] Pool read time %f\n", time));
					// }
					// foundCache = true; // now just for one class, but is same for all classes
					// // debugLogger.writeln("Cache loaded:" + classname);
					File classcache = new File(classcachefolder, classname + ".log");
					return java.nio.file.Files.readAllBytes(classcache.toPath());
				} catch (IOException e) {
					// debugLogger.writeln("IOException:" + classname);
					// debugLogger.writeln(e.toString());
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if (!this.simpleLog) {
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
					// TODO, 似乎对static final 且右侧是常数的话，就不会出现在这里,例如 Math 104
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
		if (!this.simpleLog && useCachedClass && (cacheCond == CacheCond.GenClass)) {
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
		// if(this.cacheCond != CacheCond.GenPool)
		// 	return;
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
			debugLogger.write(String.format("[Agent] init done. m = %s\n", mi.getName()));
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
		// for abstract method, the ca is null
		if(ca == null)
			return;

		// add constants to constpool.
		// index will be used during instrumentation.
		// ConstPool constp = mi.getConstPool();
		// CallBackIndex cbi = new CallBackIndex(constp, traceWriter);

		if (!this.simpleLog)
			instrumentByteCode(cc, mi, ca, constp, cbi);
		// add the outpoint of this method into tracepool (for static analysis)
		Trace outpoint = new Trace(cc.getName(), m.getName(), m.getDescriptor());
		outpoint.setTypeOutPoint();
		CallBackIndex.tracepool.add(outpoint);
		// log method name at the beginning of this method.
		if (!this.simpleLog) {
			CodeIterator ci = ca.iterator();
			if(useNewTrace){
				Trace longname = new Trace(cc.getName(), m.getName(), m.getDescriptor());
				longname.setTypeMethodLog();
				int poolindex = CallBackIndex.tracepool.indexAt();
				CallBackIndex.tracepool.add(longname);

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
		// instrument the bytecodes for method-level profiling
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
			Integer branchbyte = null;
			Integer _default = null;
			String _switch = null;
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

			if (_default != null) {
				instruction = new SwitchTrace(instruction, _default, _switch);
			}

			if(branchbyte != null){
				instruction = new BranchTrace(instruction, branchbyte);
			}

			int poolindex = CallBackIndex.tracepool.indexAt();
			CallBackIndex.tracepool.add(instruction);
			// TODO: 之后在GenPool时，把这个一起输出。第二次GenClass时，就直接用了
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
			//FIXME: to false
			boolean needCompress = false;
			for(BackEdge loopedge: CallBackIndex.loopset){
				if(loopedge.end == poolindex)
					needCompress = true;
			}

			if (oi != null) {
				if (!mi.isStaticInitializer()){
					if(useNewTrace)
					{
						// debugLogger.write(TracePool.get(poolindex).toString());
						// debugLogger.write("		insert, " + "poolindex = " + poolindex);
						if(needCompress){
							oi.insertBeforeCompress(ci, constp, poolindex, cbi);
							// debugLogger.writeln("need Compress at index" + poolindex);
						}
						else
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

							long startTime = System.currentTimeMillis();
							//TODO: 加buffer Output
							FileOutputStream outStream = new FileOutputStream("trace/logs/run/" + CallBackIndex.tracewriter.getName()+".ser");
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
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
		try {
			long startTime = System.currentTimeMillis();
			byte[] byteCode = classfileBuffer;
			// TODO modify here to transform all classes.
			if(className == null || !transformedclazz.containsKey(className))
				return byteCode;
			if(loader == null || !loader.equals(transformedclazz.get(className)))
				return byteCode;
			transformedclazz.remove(className);
			
			byte[] ret = transformBody(className);
			debugLogger.write(String.format("[Agent] class = %s\n", className));
			long endTime = System.currentTimeMillis();
			double time = (endTime - startTime) / 1000.0;
			debugLogger.write(String.format("[Agent] Transform time %f\n", time));
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