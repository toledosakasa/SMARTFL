package ppfl.instrumentation;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Mnemonic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TraceTransformer implements ClassFileTransformer {

	private static Logger LOGGER = LoggerFactory.getLogger(TraceTransformer.class);

	/** The internal form class name of the class to transform */
	private String targetClassName;
	/** The class loader of the class we want to transform */
	private ClassLoader targetClassLoader;

	/** filename for logging */
	private String logFile = null;

	public TraceTransformer(String targetClassName, ClassLoader targetClassLoader) {
		this.targetClassName = targetClassName;
		this.targetClassLoader = targetClassLoader;
	}

	public void setLogFile(String s) {
		this.logFile = s;
	}

	private String getLogStmt(String s) {
		return "ppfl_logger.log(java.util.logging.Level.INFO,\"" + s + "\");";
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
			// StringBuilder loggerDefineBlock = new StringBuilder();
			if (this.logFile != null) {
				LOGGER.info("[Agent] Logfile: {}", this.logFile);
				java.util.logging.Logger logger = java.util.logging.Logger.getLogger(targetClassName);
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
					
					//java.util.logging.SimpleFormatter sf = new java.util.logging.SimpleFormatter();
					//fileHandler.setFormatter(sf);
					
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
				CtClass loggerclass = cp.get("java.util.logging.Logger");
				if (this.logFile != null) {
					// String loggeri = "private static java.util.logging.Logger ppfl_logger =
					// java.util.logging.Logger.getLogger(\"" + targetClassName + "\");";
					// System.out.println(loggeri);
				}

				for (CtMethod m : cc.getDeclaredMethods()) {

					// hello in console
					LOGGER.info("[Agent] Transforming method {}", m.getName());

					//print bytecode and collect linenumbers
					MethodInfo mi = m.getMethodInfo();
					CodeIterator ci = mi.getCodeAttribute().iterator();
					List<Integer> li = new ArrayList<Integer>();
					int lastln = -1;
					try {
						while (ci.hasNext()) {
							int index = ci.next();
							int ln = mi.getLineNumber(index);
							if (ln != lastln) {
								li.add(ln);
								lastln = ln;
								System.out.println(ln);
							}
							int op = ci.byteAt(index);
							System.out.println(Mnemonic.OPCODE[op]);
						}
					} catch (BadBytecode e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					//add logger as local var
					m.addLocalVariable("ppfl_logger", loggerclass);
					StringBuilder startBlock = new StringBuilder();
					startBlock.append("ppfl_logger = java.util.logging.Logger.getLogger(\"" + targetClassName + "\");");
					m.insertBefore(startBlock.toString());

					//insert linenumber logging
					for (Integer i : li) {
						String stmt = this.getLogStmt(cc.getName() + ":" + m.getName() + ":"+i);
						m.insertAt(i, stmt);
					}

//					
//					m.addLocalVariable("startTime", CtClass.longType);
//
//					
//					startBlock.append("startTime = System.currentTimeMillis();");
//					
//					startBlock
//							.append("ppfl_logger.log(java.util.logging.Level.INFO,\"hello in " + m.getName() + "\");");
//					
//
//					StringBuilder endBlock = new StringBuilder();
//
//					m.addLocalVariable("endTime", CtClass.longType);
//					m.addLocalVariable("opTime", CtClass.longType);
//					endBlock.append("endTime = System.currentTimeMillis();");
//					endBlock.append("opTime = (endTime-startTime)/1000;");
//
//					endBlock.append(
//							"ppfl_logger.log(java.util.logging.Level.INFO,\"[Application] Withdrawal operation completed in:\" + opTime + \" seconds!\");");
//
//					m.insertAfter(endBlock.toString());

				}
				byteCode = cc.toBytecode();
				cc.detach();

			} catch (NotFoundException | CannotCompileException | IOException e) {
				LOGGER.error("Exception", e);
			}
		}
		return byteCode;

	}
}