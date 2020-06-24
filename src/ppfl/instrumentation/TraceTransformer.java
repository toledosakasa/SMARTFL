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

	// will be called by bytecode instrumentation
	public static int printTopStack1(int i) {
		java.util.logging.Logger ppfl_logger = java.util.logging.Logger.getLogger(LOGGERNAME);
		ppfl_logger.log(java.util.logging.Level.INFO, "Topstack:int, value=" + String.valueOf(i));
		return i;
	}

	public static void printTopStack1() {
		java.util.logging.Logger ppfl_logger = java.util.logging.Logger.getLogger(LOGGERNAME);
		ppfl_logger.log(java.util.logging.Level.INFO, "Nothing");
	}

	public static void logString(String s) {
		java.util.logging.Logger ppfl_logger = java.util.logging.Logger.getLogger(LOGGERNAME);
		ppfl_logger.log(java.util.logging.Level.INFO, s);
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

					// preprocessing of linenumbers and specific bytecode instructions
					MethodInfo mi = m.getMethodInfo();
					ConstPool constp = mi.getConstPool();
					CodeAttribute ca = mi.getCodeAttribute();
					CodeIterator ci = ca.iterator();
					int lastln = -1;
					// index used for instrumentation
					CtClass THISCLASS = cp.get("ppfl.instrumentation.TraceTransformer");
					int classindex = constp.addClassInfo(THISCLASS);
					// iterate every instruction
					try {
						while (ci.hasNext()) {
							// lookahead the next instruction.
							int index = ci.lookAhead();
							int ln = mi.getLineNumber(index);
							String linenumberinfo = cc.getName() + ":" + m.getName() + ":" + ln + ":";
							if (ln != lastln) {
								lastln = ln;
								System.out.println(ln);
							}

							// insert bytecode right before this inst.
							int op = ci.byteAt(index);
							String opc = Mnemonic.OPCODE[op];
							OpcodeInst oi = Interpreter.map[op];
							System.out.println(opc);
							// get print information
							String inst = getinst_map(ci, index, constp);
							inst = linenumberinfo + inst;
							if (inst != null) {
								// insertmap.get(ln).append(inst);
								int instpos = ci.insertGap(8);
								int instindex = constp.addStringInfo(inst);
								System.out.println(constp.getStringInfo(instindex));
								ci.writeByte(19, instpos);// ldc_w
								ci.write16bit(instindex, instpos + 1);
								int methodindex = constp.addMethodrefInfo(classindex, "logString",
										"(Ljava/lang/String;)V");
								ci.writeByte(184, instpos + 3);
								ci.write16bit(methodindex, instpos + 4);

							}
							// move to the next inst. instrumentation below this will be after this inst.
							ci.next();
							// print stack value pushed by this instruction.
							// this should be inserted after the instruction is executed
							// (after ci.next() is called)
							if (oi.pushnum == 1 
									&& (opc.startsWith("i"))// temporary solution for integer insts.should extend opcodeinst class.
									){
								System.out.println("dealing with: " + opc);

								int methodindex = constp.addMethodrefInfo(classindex, "printTopStack1", "(I)I");
								int instpos = ci.insertGap(8);
								// ci.writeByte(93, instpos + 1);// dup(buggy. I can't explain why. use (I)I
								// instead.)
								ci.writeByte(184, instpos + 2);// invokestatic
								ci.write16bit(methodindex, instpos + 3);
//								Bytecode bc = new Bytecode(mi.getConstPool());
//								bc.add(93);//dup
//								byte[] code = bc.toCodeAttribute().getCode();

							}
							// deal with specific bytecode instructions

						}
					} catch (BadBytecode e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

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