package ppfl.instrumentation;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Mnemonic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TraceTransformer implements ClassFileTransformer {

	private static Logger LOGGER = LoggerFactory.getLogger(TraceTransformer.class);

	// The logger name
	public static String LOGGERNAME = "PPFL_LOGGER";

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

	public void setLogFile(String s) {
		this.logFile = s;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		byte[] byteCode = classfileBuffer;
		String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/"); // replace . with /
		if (!className.equals(finalTargetClassName)) {
			return byteCode;
		}

		if (className.equals(finalTargetClassName) && loader.equals(targetClassLoader)) {
			LOGGER.info("[Agent] Transforming class {}", finalTargetClassName);
			if (this.logFile != null) {
				LOGGER.info("[Agent] Logfile: {}", this.logFile);
				java.util.logging.Logger logger = java.util.logging.Logger.getLogger(LOGGERNAME);

				java.util.logging.FileHandler fileHandler;
				java.util.logging.ConsoleHandler consoleHandler;
				try {
					// disable console output
					java.util.logging.Logger rootlogger = logger.getParent();
					for (java.util.logging.Handler h : rootlogger.getHandlers()) {
						rootlogger.removeHandler(h);
					}
					consoleHandler = new java.util.logging.ConsoleHandler();
					consoleHandler.setLevel(java.util.logging.Level.WARNING);
					logger.addHandler(consoleHandler);
					fileHandler = new java.util.logging.FileHandler(this.logFile);
					logger.addHandler(fileHandler);
					fileHandler.setLevel(java.util.logging.Level.INFO);

					fileHandler.setFormatter(new Formatter() {
						@Override
						public String format(LogRecord record) {
							return record.getLevel() + ":" + record.getMessage() + "\n";
						}
					});

				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				ClassPool cp = ClassPool.getDefault();
				CtClass cc = cp.get(targetClassName);

				for (CtMethod m : cc.getDeclaredMethods()) {
					// hello in console
					LOGGER.info("[Agent] Transforming method {}", m.getName());

					// get iterator
					MethodInfo mi = m.getMethodInfo();
					CodeAttribute ca = mi.getCodeAttribute();
					CodeIterator ci = ca.iterator();

					// add constants to constpool.
					// index will be used during instrumentation.
					ConstPool constp = mi.getConstPool();
					CallBackIndex cbi = new CallBackIndex(constp);

					// iterate every instruction
					int lastln = -1;
					while (ci.hasNext()) {
						// lookahead the next instruction.
						int index = ci.lookAhead();
						int op = ci.byteAt(index);
						OpcodeInst oi = Interpreter.map[op];

						// debugging:opcode
						String opc = Mnemonic.OPCODE[op];
						System.out.println(opc);
						// linenumber information.
						int ln = mi.getLineNumber(index);
						String linenumberinfo = cc.getName() + ":" + m.getName() + ":" + ln + ":";
						if (ln != lastln) {
							lastln = ln;
							System.out.println(ln);
						}

						// insert bytecode right before this inst.
						// print basic information of this instruction
						oi.insertByteCodeBefore(ci, index, constp, linenumberinfo, cbi);
						// move to the next inst. everything below this will be inserted after the inst.
						ci.next();
						// print advanced information(e.g. value pushed)
						oi.insertByteCodeAfter(ci, index, constp, cbi);
					}
					// not sure if this is necessary.
					ca.computeMaxStack();
				}
				byteCode = cc.toBytecode();
				cc.detach();

			} catch (NotFoundException | CannotCompileException | IOException | BadBytecode e) {
				LOGGER.error("Exception", e);
			}
		}
		return byteCode;

	}

	// callbacks.
	// will be called by bytecode instrumentation
	public static int printTopStack1(int i) {
		java.util.logging.Logger ppfl_logger = java.util.logging.Logger.getLogger(LOGGERNAME);
		ppfl_logger.log(java.util.logging.Level.INFO, "Topstack:int, value=" + String.valueOf(i));
		return i;
	}

	public static double printTopStack1(double i) {
		java.util.logging.Logger ppfl_logger = java.util.logging.Logger.getLogger(LOGGERNAME);
		ppfl_logger.log(java.util.logging.Level.INFO, "Topstack:double, value=" + String.valueOf(i));
		return i;
	}

	public static void logString(String s) {
		java.util.logging.Logger ppfl_logger = java.util.logging.Logger.getLogger(LOGGERNAME);
		ppfl_logger.log(java.util.logging.Level.INFO, s);
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
		// return this.getLogStmt(inst);
	}
}