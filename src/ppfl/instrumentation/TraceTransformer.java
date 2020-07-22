package ppfl.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
import ppfl.instrumentation.opcode.OpcodeInst;

public class TraceTransformer implements ClassFileTransformer {

	private static Logger LOGGER = Logger.getLogger("TraceTransformer");
	// LoggerFactory.getLogger(TraceTransformer.class);

	// The logger name
	public static Logger TRACELOGGER = Logger.getLogger("PPFL_LOGGER");
	public static Logger SOURCELOGGER = Logger.getLogger("PPFL_LOGGER_SOURCE");
	
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
	
	public void setSourceLogFile(String s) {
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
			this.setLogger();
			try {
				ClassPool cp = ClassPool.getDefault();
				CtClass cc = cp.get(targetClassName);

				for (CtMethod m : cc.getDeclaredMethods()) {
					// hello in console
					LOGGER.log(Level.INFO, "[Agent] Transforming method " + m.getName());

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
						

						// debugging:opcode
						int op = tempci.byteAt(index);
						String opc = Mnemonic.OPCODE[op];
						System.out.println(opc);

						String getinst = getinst_map(tempci, index, constp);
						String linenumberinfo = ",lineinfo=" + cc.getName() + "#" + m.getName() + "#" + ln + "#"
								+ index;
						if (ln != lastln) {
							lastln = ln;
							System.out.println(ln);
						}
						instmap.put(i, getinst + linenumberinfo);
						tempci.next();
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
						this.SOURCELOGGER.log(Level.INFO,instinfo);
						oi.insertByteCodeBefore(ci, index, constp, instinfo, cbi);
						// move to the next inst. everything below this will be inserted after the inst.
						// ci.next();
						index = ci.next();
						// print advanced information(e.g. value pushed)
						oi.insertByteCodeAfter(ci, index, constp, cbi);
					}
					// not sure if this is necessary.
					ca.computeMaxStack();
				}
				byteCode = cc.toBytecode();
				cc.detach();

			} catch (NotFoundException | CannotCompileException | IOException | BadBytecode e) {
				LOGGER.log(Level.SEVERE, "Exception", e);
			}
		}
		return byteCode;

	}

	private void setLogger() {
		// disable console output
		Logger rootlogger = LOGGER.getParent();
		for (Handler h : rootlogger.getHandlers()) {
			rootlogger.removeHandler(h);
		}
		ConsoleHandler debugHandler = new ConsoleHandler();
		debugHandler.setLevel(Level.INFO);
		debugHandler.setFormatter(new Formatter() {

			@Override
			public String format(LogRecord record) {
				return record.getLevel() + ":" + record.getMessage() + "\n";
			}

		});
		LOGGER.addHandler(debugHandler);
		LOGGER.log(Level.INFO, "[Agent] Transforming class " + this.targetClassName);
		if (this.logFile != null) {
			FileHandler fileHandler,sourcefileHandler;
			ConsoleHandler consoleHandler;

			consoleHandler = new ConsoleHandler();
			consoleHandler.setLevel(Level.WARNING);
			TRACELOGGER.addHandler(consoleHandler);
			SOURCELOGGER.addHandler(consoleHandler);
			LOGGER.log(Level.INFO, "[Agent] Logfile: " + this.logFile);
			try {
				fileHandler = new FileHandler(this.logFile);
				TRACELOGGER.addHandler(fileHandler);
				fileHandler.setLevel(Level.INFO);

				fileHandler.setFormatter(new Formatter() {
					@Override
					public String format(LogRecord record) {
						return record.getMessage();
						// return record.getLevel() + ":" + record.getMessage() + "\n";
					}
				});
				
				sourcefileHandler = new FileHandler(this.logFile + ".source.log");
				SOURCELOGGER.addHandler(sourcefileHandler);
				sourcefileHandler.setLevel(Level.INFO);
				sourcefileHandler.setFormatter(new Formatter() {
					@Override
					public String format(LogRecord record) {
						return record.getMessage();
						// return record.getLevel() + ":" + record.getMessage() + "\n";
					}
				});

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String getinst_map(CodeIterator ci, int index, ConstPool constp) {
		int op = ci.byteAt(index);
		String inst = null;
		String opc = Mnemonic.OPCODE[op];
		OpcodeInst oi = Interpreter.map[op];
		if (oi == null) {
			LOGGER.log(Level.WARNING, "unsupported opcode: " + opc);
			return "";
		}
		inst = oi.getinst(ci, index, constp);
		return inst;
	}
}