package ppfl.instrumentation;

import java.lang.instrument.Instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentationAgent {
	private static Logger LOGGER = LoggerFactory.getLogger(InstrumentationAgent.class);
	private static String logFile = null;
	private static String className = null;
	private static String prefix = null;

	public static void premain(String agentArgs, Instrumentation inst) {
		LOGGER.info("[Agent] In premain method");

		for (String s : agentArgs.split(",")) {
			if (s.startsWith("class=")) {
				className = s.split("=")[1];
			}
			if (s.startsWith("logfile=")) {
				logFile = s.split("=")[1];
			}
			if (s.startsWith("prefix=")) {
				prefix = s.split("=")[1];
			}
		}

		transformClass(inst);
	}

	public static void agentmain(String agentArgs, Instrumentation inst) {
		LOGGER.info("[Agent] In agentmain method");

		className = agentArgs;
		transformClass(inst);
	}

	private static void transformClass(Instrumentation instrumentation) {
		LOGGER.info("[Agent] In transformClass method");
		Class<?> targetCls = null;
		ClassLoader targetClassLoader = null;
		// if prefix available
		if (prefix != null && !prefix.contentEquals("")) {
			for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
				System.out.println(clazz.getName());
				if (clazz.getName().startsWith(prefix)) {

					targetCls = clazz;
					targetClassLoader = targetCls.getClassLoader();
					transform(targetCls, targetClassLoader, instrumentation);
				}
			}
			return;
		}

		// by default, transform all classes except neglected ones.
		if (className == null || className.contentEquals("")) {
			for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
				if (!isNeglect(clazz.getName())) {
					System.out.println(clazz.getName());
					targetCls = clazz;
					targetClassLoader = targetCls.getClassLoader();
					transform(targetCls, targetClassLoader, instrumentation);
				}
			}
			return;
		}

		// see if we can get the class using forName
		try {
			LOGGER.info("className:" + className);
			targetCls = Class.forName(className);
			targetClassLoader = targetCls.getClassLoader();

			transform(targetCls, targetClassLoader, instrumentation);
			return;
		} catch (Exception ex) {
			LOGGER.error("Class [{}] not found with Class.forName", className);
		}
		// otherwise iterate all loaded classes and find what we want
		for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
			if (clazz.getName().equals(className)) {
				targetCls = clazz;
				targetClassLoader = targetCls.getClassLoader();
				transform(targetCls, targetClassLoader, instrumentation);
				return;
			}
		}
		throw new RuntimeException("Failed to find class [" + className + "]");
	}

	private static boolean isNeglect(String name) {
		String negs[] = { "java", "org.slf4j" };
		for (String neg : negs) {
			if (name.startsWith(neg)) {
				return true;
			}
		}
		return false;
	}

	private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation) {
		TraceTransformer dt = new TraceTransformer(clazz.getName(), classLoader);
		if (logFile != null) {
			dt.setLogFile(logFile);
		}
		instrumentation.addTransformer(dt, true);
		try {
			instrumentation.retransformClasses(clazz);
		} catch (Exception ex) {
			throw new RuntimeException("Transform failed for class: [" + clazz.getName() + "]", ex);
		}
	}

}