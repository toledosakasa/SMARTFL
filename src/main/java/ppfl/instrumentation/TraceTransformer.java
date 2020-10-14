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

	private static Logger LOGGER = LoggerFactory.getLogger("TraceTransformer");
	// LoggerFactory.getLogger(TraceTransformer.class);

	// The logger name
	public final static String traceLoggerName = "PPFL_LOGGER";
	public final static String sourceLoggerName = "PPFL_LOGGER_SOURCE";
	public static Logger TRACELOGGER = LoggerFactory.getLogger(traceLoggerName);
	public static Logger SOURCELOGGER = LoggerFactory.getLogger(sourceLoggerName);

	/** The internal form class name of the class to transform */
	private String targetClassName;
	/** The class loader of the class we want to transform */
	private ClassLoader targetClassLoader;

	/** filename for logging */
	private String logFile = null;
	public TraceTransformer(String targetClassName, ClassLoader targetClassLoader) {
		this.targetClassName = targetClassName;
		this.targetClassLoader = targetClassLoader;
		Interpreter.init();
	}

	public void setTargetClassName(String s) {
		this.targetClassName = s;
	}

	public void setLogFile(String s) {
		this.logFile = s.replace('\\','.').replace('/','.');
		MDC.put("logfile", this.logFile);
	}

	private byte[] transformBody(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		byte[] byteCode = classfileBuffer;
		this.setLogger(this.targetClassName);
		LOGGER.info("[Agent] Transforming class " + this.targetClassName);
		try {
			ClassPool cp = ClassPool.getDefault();
			CtClass cc = cp.get(targetClassName);

			for (CtBehavior m : cc.getDeclaredBehaviors()) {
				transformBehavior(m, cc);
			}
			byteCode = cc.toBytecode();
			cc.detach();

		} catch (NotFoundException | CannotCompileException | IOException | BadBytecode e) {
			LOGGER.error("[Bug]bytecode error", e);
		}
		return byteCode;
	}

	private void transformBehavior(CtBehavior m, CtClass cc) throws NotFoundException, BadBytecode {
		// hello in console
		LOGGER.info("[Agent] Transforming method " + m.getName());

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

		Map<Integer, String> instmap = new HashMap<Integer, String>();
		for (int i = 0; tempci.hasNext(); i++) {
			int index = tempci.lookAhead();
			int ln = mi.getLineNumber(index);

			// debugging:line number
			String getinst = getinst_map(tempci, index, constp);
			String linenumberinfo = ",lineinfo=" + cc.getName() + "#" + m.getName() + "#" + ln + "#" + index
					+ ",nextinst=";
			if (ln != lastln) {
				lastln = ln;
				LOGGER.info(String.valueOf(ln));
				// System.out.println(ln);
			}

			// debugging:opcode
			int op = tempci.byteAt(index);
			String opc = Mnemonic.OPCODE[op];
			LOGGER.info(opc);
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
			SOURCELOGGER.info(instinfo);
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
			String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/"); // replace . with /
			if (className == null || !className.equals(finalTargetClassName) || loader == null
					|| !loader.equals(targetClassLoader)) {
				return byteCode;
			}
			return transformBody(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
		} catch (Throwable e) {
			LOGGER.error("[Bug]Exception", e);
			e.printStackTrace();
			return null;
		}
	}

	private void setLogger(String clazzname) {
		MDC.put("sourcefile", clazzname);
	}

	private String getinst_map(CodeIterator ci, int index, ConstPool constp) {
		int op = ci.byteAt(index);
		String inst = null;
		String opc = Mnemonic.OPCODE[op];
		OpcodeInst oi = Interpreter.map[op];
		if (oi == null) {
			LOGGER.warn("unsupported opcode: " + opc);
			return "";
		}
		inst = oi.getinst(ci, index, constp);
		return inst;
	}
}