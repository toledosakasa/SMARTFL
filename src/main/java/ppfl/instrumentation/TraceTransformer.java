package ppfl.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Mnemonic;
import ppfl.instrumentation.opcode.OpcodeInst;

public class TraceTransformer implements ClassFileTransformer {

	private static Logger debugLogger = LoggerFactory.getLogger("Debugger");
	// LoggerFactory.getLogger(TraceTransformer.class);

	// The logger name
	public static final  String TRACELOGGERNAME = "PPFL_LOGGER";
	public static final  String SOURCELOGGERNAME = "PPFL_LOGGER_SOURCE";
	public static final Logger traceLogger = LoggerFactory.getLogger(TRACELOGGERNAME);
	public static final Logger sourceLogger = LoggerFactory.getLogger(SOURCELOGGERNAME);

	/** The internal form class name of the class to transform */
	private String targetClassName;
	/** The class loader of the class we want to transform */
	private ClassLoader targetClassLoader;

	/** filename for logging */
	public TraceTransformer(String targetClassName, ClassLoader targetClassLoader) {
		this.targetClassName = targetClassName;
		this.targetClassLoader = targetClassLoader;
		Interpreter.init();
	}

	public void setTargetClassName(String s) {
		this.targetClassName = s;
	}

	public void setLogFile(String s) {
		String logFile= null;
		logFile = s.replace('\\','.').replace('/','.');
		MDC.put("logfile", logFile);
	}

	private byte[] transformBody(byte[] classfileBuffer) {
		byte[] byteCode = classfileBuffer;
		this.setLogger(this.targetClassName);
		debugLogger.info("[Agent] Transforming class " + this.targetClassName);
		try {
			ClassPool cp = ClassPool.getDefault();
			CtClass cc = cp.get(targetClassName);

			for (CtBehavior m : cc.getDeclaredBehaviors()) {
				transformBehavior(m, cc);
			}
			byteCode = cc.toBytecode();
			cc.detach();

		} catch (NotFoundException | CannotCompileException | IOException | BadBytecode e) {
			debugLogger.error("[Bug]bytecode error", e);
		}
		return byteCode;
	}

	private void transformBehavior(CtBehavior m, CtClass cc) throws NotFoundException, BadBytecode {
		// hello in console
		debugLogger.info("[Agent] Transforming method " + m.getName());

		if (!(m instanceof CtMethod)) {
			return;
		}

		// get iterator
		MethodInfo mi = m.getMethodInfo();
		CodeAttribute ca = mi.getCodeAttribute();

		// add constants to constpool.
		// index will be used during instrumentation.
		ConstPool constp = mi.getConstPool();
		CallBackIndex cbi = new CallBackIndex(constp);

		// record line info and instructions, since instrumentation will change
		// branchbyte and byte index.
		CodeIterator tempci = ca.iterator();
		int lastln = -1;

		Map<Integer, String> instmap = new HashMap<>();
		for (int i = 0; tempci.hasNext(); i++) {
			int index = tempci.lookAhead();
			int ln = mi.getLineNumber(index);

			// debugging:line number
			String getinst = getInstMap(tempci, index, constp);
			String linenumberinfo = ",lineinfo=" + cc.getName() + "#" + m.getName() + "#" + ln + "#" + index
					+ ",nextinst=";
			if (ln != lastln) {
				lastln = ln;
				debugLogger.info(String.valueOf(ln));
				// System.out.println(ln);
			}

			// debugging:opcode
			int op = tempci.byteAt(index);
			String opc = Mnemonic.OPCODE[op];
			debugLogger.info(opc);
			// System.out.println(opc);

			tempci.next();
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
			// LOGGER.log(Level.SEVERE, instinfo);
			sourceLogger.info(instinfo);
			if (oi != null)
				oi.insertByteCodeBefore(ci, index, constp, instinfo, cbi);
			// move to the next inst. everything below this will be inserted after the inst.
			// ci.next();
			index = ci.next();
			// print advanced information(e.g. value pushed)
			if (oi != null)
				oi.insertByteCodeAfter(ci, index, constp, cbi);
		}
		// not sure if this is necessary.
		ca.computeMaxStack();
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		try {
			byte[] byteCode = classfileBuffer;
			String finalTargetClassName = this.targetClassName.replace(".", "/"); // replace . with /
			if (className == null || !className.equals(finalTargetClassName) || loader == null
					|| !loader.equals(targetClassLoader)) {
				return byteCode;
			}
			return transformBody(classfileBuffer);
			//return transformBody(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
		} catch (Exception e) {
			debugLogger.error("[Bug]Exception", e);
			e.printStackTrace();
			return null;
		}
	}

	private void setLogger(String clazzname) {
		MDC.put("sourcefile", clazzname);
	}

	private String getInstMap(CodeIterator ci, int index, ConstPool constp) {
		int op = ci.byteAt(index);
		String inst = null;
		String opc = Mnemonic.OPCODE[op];
		OpcodeInst oi = Interpreter.map[op];
		if (oi == null) {
			debugLogger.warn("unsupported opcode: " + opc);
			return "";
		}
		inst = oi.getinst(ci, index, constp);
		return inst;
	}
}