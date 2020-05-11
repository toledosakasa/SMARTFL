package ppfl.instrumentation;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

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
			StringBuilder loggerDefineBlock = new StringBuilder();
			if (this.logFile != null) {
				LOGGER.info("[Agent] Logfile: {}", this.logFile);
				java.util.logging.Logger logger = java.util.logging.Logger.getLogger(targetClassName);
				java.util.logging.FileHandler fileHandler;
				try {
					fileHandler = new java.util.logging.FileHandler(this.logFile);
					logger.addHandler(fileHandler);
					//logger.log(java.util.logging.Level.INFO, "");
					java.util.logging.SimpleFormatter sf = new java.util.logging.SimpleFormatter();
					fileHandler.setFormatter(sf);
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
				if (this.logFile != null) {
					String loggeri = "private static java.util.logging.Logger ppfl_logger = java.util.logging.Logger.getLogger(\"" + targetClassName + "\");";
					System.out.println(loggeri);
					CtField ppfl_logger = CtField.make(loggeri, cc);
					cc.addField(ppfl_logger);
				}

				for (CtMethod m : cc.getDeclaredMethods()) {
					LOGGER.info("[Agent] Transforming method {}", m.getName());
					m.addLocalVariable("startTime", CtClass.longType);

					StringBuilder startBlock = new StringBuilder();
					startBlock.append("startTime = System.currentTimeMillis();");
					startBlock.append("ppfl_logger.log(java.util.logging.Level.INFO,\"hello in " + m.getName() + "\");");
					m.insertBefore(startBlock.toString());

					StringBuilder endBlock = new StringBuilder();

					m.addLocalVariable("endTime", CtClass.longType);
					m.addLocalVariable("opTime", CtClass.longType);
					endBlock.append("endTime = System.currentTimeMillis();");
					endBlock.append("opTime = (endTime-startTime)/1000;");

					endBlock.append(
							"ppfl_logger.log(java.util.logging.Level.INFO,\"[Application] Withdrawal operation completed in:\" + opTime + \" seconds!\");");

					m.insertAfter(endBlock.toString());
					
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