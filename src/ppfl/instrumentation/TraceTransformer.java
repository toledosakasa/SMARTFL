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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		Interpreter.init();
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

				for (CtMethod m : cc.getDeclaredMethods()) {
					// hello in console
					LOGGER.info("[Agent] Transforming method {}", m.getName());

					// preprocessing of linenumbers and specific bytecode instructions
					MethodInfo mi = m.getMethodInfo();
					CodeIterator ci = mi.getCodeAttribute().iterator();
					List<Integer> li = new ArrayList<Integer>();
					Map<Integer, StringBuilder> insertmap = new HashMap<Integer, StringBuilder>();
					int lastln = -1;
					try {
						while (ci.hasNext()) {
							int index = ci.next();
							int ln = mi.getLineNumber(index);
							if (ln != lastln) {
								li.add(ln);
								lastln = ln;
								System.out.println(ln);
								String stmt = this.getLogStmt(cc.getName() + ":" + m.getName() + ":" + ln);
								insertmap.put(ln, new StringBuilder(stmt));
							}

							// deal with specific bytecode instructions
							String inst = getinst_map(ci, index);
							if (inst != null)
								insertmap.get(ln).append(inst);
						}
					} catch (BadBytecode e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// add logger as local var
					m.addLocalVariable("ppfl_logger", loggerclass);
					StringBuilder startBlock = new StringBuilder();
					startBlock.append("ppfl_logger = java.util.logging.Logger.getLogger(\"" + targetClassName + "\");");

					// instrumentation
					m.insertBefore(startBlock.toString());
					for (Integer k : insertmap.keySet()) {
						System.out.println("inst at line " + k + ":");
						System.out.println(insertmap.get(k));
						m.insertAt(k, insertmap.get(k).toString());
					}
				}
				byteCode = cc.toBytecode();
				cc.detach();

			} catch (NotFoundException | CannotCompileException | IOException e) {
				LOGGER.error("Exception", e);
			}
		}
		return byteCode;

	}
	private String getinst_map(CodeIterator ci,int index) {
		int op = ci.byteAt(index);
		String inst = null;
		String opc = Mnemonic.OPCODE[op];
		OpcodeInst oi = Interpreter.map[op];
		if(oi == null) {
			LOGGER.warn("unsupported opcode: " + opc);
			return "";
		}
		inst = oi.getinst(ci, index);
		return this.getLogStmt(inst);
	}
	
	private String getinst(CodeIterator ci, int index) {
		int op = ci.byteAt(index);
		String inst = null;
		String opc = Mnemonic.OPCODE[op];
		// loads
		if (op <= 29 && op >= 26) {
			assert (opc.startsWith("iload"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=iload,pushnum=1,pushvar=" + (op - 26));
		} else if (op == 21) {
			assert (opc.equals("iload"));
			System.out.println(opc);
			int varindex = ci.byteAt(index + 1);
			inst = this.getLogStmt("opcode=iload,pushnum=1,pushvar=" + varindex);
		} else if (op <= 45 && op >= 42) {
			assert (opc.startsWith("aload"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=aload,pushnum=1,pushvar=" + (op - 42));
		} else if (op == 25) {
			assert (opc.equals("aload"));
			System.out.println(opc);
			int varindex = ci.byteAt(index + 1);
			inst = this.getLogStmt("opcode=iload,pushnum=1,pushvar=" + varindex);
		} else if (op >= 2 && op <= 8) {
			assert (opc.startsWith("iconst"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=iconst,pushnum=1,pushconst=" + (op - 3));
		} else if (op == 16) {
			assert (opc.startsWith("bipush"));
			System.out.println(opc);
			int varindex = ci.byteAt(index + 1);
			inst = this.getLogStmt("opcode=bipush,pushnum=1,pushconst=" + varindex);
		}
		// stores
		else if (op >= 59 && op <= 62) {
			assert (opc.equals("istore"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=istore,popnum=1,popvar=" + (op - 59));
		} else if (op == 54) {
			assert (opc.startsWith("istore"));
			System.out.println(opc);
			int varindex = ci.byteAt(index + 1);
			inst = this.getLogStmt("opcode=istore,popnum=1,popvar=" + varindex);
		} else if (op == 88 || op == 87) {
			assert (opc.startsWith("pop"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=pop,popnum=" + (op - 86));
		} 
		// branching
		else if (op >= 153 && op <= 158) {
			assert (opc.startsWith("if"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=if,popnum=1");
		} else if (op >= 159 && op <= 164) {
			assert (opc.startsWith("if_icmp"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=icmp,popnum=2");
		} else if (op == 167) {// controls
			assert (opc.startsWith("goto"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=goto");
		} else if (op == 172) {
			assert (opc.startsWith("ireturn"));
			System.out.println(opc);
			// clear the stack,return to caller's frame
			inst = this.getLogStmt("opcode=ireturn,popnum=-1,pushnum=1");
		} else if (op == 177) {
			assert (opc.startsWith("return"));
			System.out.println(opc);
			// clear the stack,return to caller's frame
			inst = this.getLogStmt("opcode=return,popnum=-1");
		}
		// invokes
		else if (op == 182) {
			assert (opc.startsWith("invokevirtual"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=invokevirtual");
		} else if (op == 184) {
			assert (opc.startsWith("invokestatic"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=invokestatic");
		} 
		// arith
		else if (op == 132) {
			assert (opc.startsWith("iinc"));
			System.out.println(opc);
			int varindex = ci.byteAt(index + 1);
			int vconst = ci.byteAt(index + 2);
			inst = this.getLogStmt("opcode=iinc,var=" + varindex + ",const=" + vconst);
		} else if (op == 96) {
			assert (opc.startsWith("iadd"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=iadd,popnum=2,pushnum=1");
		} else if (op == 104) {
			assert (opc.startsWith("imul"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=imul,popnum=2,pushnum=1");
		} else if (op == 100) {
			assert (opc.startsWith("isub"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=isub,popnum=2,pushnum=1");
		} else if (op == 108) {
			assert (opc.startsWith("idiv"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=idiv,popnum=2,pushnum=1");
		} else if (op == 116) {
			assert (opc.startsWith("ineg"));
			System.out.println(opc);
			inst = this.getLogStmt("opcode=ineg,popnum=1,pushnum=1");
		}

		else {
			LOGGER.warn("unsupport_opcode: " + opc);
		}
		return inst;
	}
}