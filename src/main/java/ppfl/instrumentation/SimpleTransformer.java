package ppfl.instrumentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.lang.System;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import ppfl.MyWriter;
import ppfl.ProfileUtils;
import ppfl.WriterUtils;

public class SimpleTransformer implements ClassFileTransformer {

	private static MyWriter debugLogger = null;
	private static Writer traceWriter = null;
	private Set<String> d4jMethodNames = new HashSet<>();
	// Map of transformed clazz, key: classname, value: classloader
	private Set<String> transformedclazz;
	private boolean first_addShutdownHook = true;

	/** filename for logging */
	public SimpleTransformer(Set<String> transformedclazz, String logfilename) {
		this.transformedclazz = new HashSet<>();
		for(String classname : transformedclazz){
            // replace . with /
            this.transformedclazz.add(classname.replace(".", "/"));
        }
		File debugdir = new File("trace/debug/");
		debugdir.mkdirs();
		WriterUtils.setPath("trace/debug/");
		debugLogger = WriterUtils.getWriter(logfilename, true);

		File logdir = new File("trace/logs/profile/");
		logdir.mkdirs();
		Interpreter.init();
		FileWriter file = null;
		try {
			file = new FileWriter("trace/logs/profile/profile.log", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		traceWriter = file;
		CallBackIndex.setWriter(traceWriter);
	}

	public void setD4jDataFile(String filepath) {
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
		ProfileUtils.setD4jMethods(d4jMethodNames);
	}

	protected byte[] transformBody(String classname) {
		byte[] byteCode = null;
		classname = classname.replace("/", ".");
		// debugLogger.write(String.format("[Agent] Transforming class %s\n",
		// classname));

		try {
			ClassPool cp = ClassPool.getDefault();
			CtClass cc = cp.get(classname);
			ConstPool constp = cc.getClassFile().getConstPool();
			CallBackIndex cbi = new CallBackIndex(constp, traceWriter);

			boolean instrumentJunit = true;// evaluation switch
			for (MethodInfo m : cc.getClassFile().getMethods()) {
				debugLogger.write("[Debug] handle %s :: %s\n", cc.getName(), m.getName());
				if (instrumentJunit && cc.getName().startsWith("junit") && !m.getName().startsWith("assert")) {
					continue;
				}
				transformBehavior(m, cc, constp, cbi);
			}

			byteCode = cc.toBytecode();
			cc.detach();

		} catch (Exception e) {
			System.out.println(e);
			// debugLogger.error("[Bug]bytecode error", e);
		}
		return byteCode;
	}

	private void transformBehavior(MethodInfo m, CtClass cc, ConstPool constp, CallBackIndex cbi)
			throws NotFoundException, BadBytecode {

		// get iterator
		// MethodInfo mi = m.getMethodInfo();
		MethodInfo mi = m;
		CodeAttribute ca = mi.getCodeAttribute();
		// for abstract method, the ca is null
		if (ca == null)
			return;

		// instrument the bytecodes for method-level profiling
		ProfileUtils.init(constp, traceWriter);
		CodeIterator ci = ca.iterator();
		String longname = String.format("%s::%s", cc.getName(), m.getName());
		ProfileUtils.logMethodName(ci, longname, constp);

		debugLogger.write("[Debug] transform %s :: %s\n", cc.getName(), m.getName());

		// not sure if this is necessary.
		ca.computeMaxStack();
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (first_addShutdownHook) {
			first_addShutdownHook = false;
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						SimpleTransformer.traceWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
		try {
			debugLogger.write(String.format("[Agent] class = %s\n", className));
			long startTime = System.currentTimeMillis();
			byte[] byteCode = classfileBuffer;
			// TODO modify here to transform all classes.
			if (className == null || !transformedclazz.contains(className))
				return byteCode;
			transformedclazz.remove(className);

			byte[] ret = transformBody(className);
			long endTime = System.currentTimeMillis();
			double time = (endTime - startTime) / 1000.0;
			debugLogger.write(String.format("[Agent] Transform time %f\n", time));
			return ret;
		} catch (Exception e) {
			// debugLogger.error("[Bug]Exception", e);
			e.printStackTrace();
			return null;
		}
	}

}