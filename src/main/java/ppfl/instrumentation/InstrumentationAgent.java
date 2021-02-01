package ppfl.instrumentation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentationAgent {
	private static Logger debugLogger = LoggerFactory.getLogger(InstrumentationAgent.class);
	private static String logFile = null;
	private static String[] className = null;
	private static List<String> classNames = null;
	private static String d4jdatafile = null;

	private InstrumentationAgent() {
		throw new IllegalStateException("Agent class");
	}

	public static void premain(String agentArgs, Instrumentation inst) {
		main(agentArgs, inst);
	}

	public static void agentmain(String agentArgs, Instrumentation inst) {
		// LOGGER.info("[Agent] In agentmain method");
		main(agentArgs, inst);
	}

	private static synchronized void main(String agentArgs, Instrumentation inst) {
		if (agentArgs == null || agentArgs.equals(""))
			return;
		debugLogger.info("[Agent] In main method");
		for (String s : agentArgs.split(",")) {
			if (s.startsWith("instrumentingclass=")) {
				className = s.split("=")[1].split(":");
			}
			if (s.startsWith("logfile=")) {
				logFile = s.split("=")[1];
			}
			if (s.startsWith("classnamefile=")) {
				String cf = s.split("=")[1];
				Path cpath = FileSystems.getDefault().getPath(cf);
				classNames = new ArrayList<>();
				try {
					classNames = java.nio.file.Files.readAllLines(cpath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (s.startsWith("d4jdatafile=")) {
				d4jdatafile = s.split("=")[1];
			}
		}
		if (logFile == null && className == null && classNames == null && d4jdatafile == null)
			return;
		transformClass(inst);
	}

	private static void transformClass(Instrumentation instrumentation) {
		debugLogger.info("[Agent] In transformClass method");
		Class<?> targetCls = null;
		ClassLoader targetClassLoader = null;
		if (d4jdatafile != null) {
			String relevantClasses = null;
			String allTestClasses = null;
			String relevantTests = null;
			// String configpath = String.format("d4j_resources/metadata_cached/%s%d.log",
			// project, id);
			try (BufferedReader reader = new BufferedReader(new FileReader(d4jdatafile));) {
				String tmp;
				while ((tmp = reader.readLine()) != null) {
					String[] splt = tmp.split("=");
					if (splt[0].equals("classes.relevant")) {
						relevantClasses = splt[1];
					}
					if (splt[0].equals("tests.all")) {
						allTestClasses = splt[1];
					}
					if (splt[0].equals("tests.relevant")) {
						relevantTests = splt[1];
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			ArrayList<String> classNameList = new ArrayList<>();
			if (relevantClasses != null) {
				for (String s : relevantClasses.split(";")) {
					if (!s.isEmpty()) {
						classNameList.add(s);
					}
				}
			}
			if (relevantTests != null) {// TODO change to all test classes(filtering)
				for (String s : relevantTests.split(";")) {
					if (!s.isEmpty()) {
						classNameList.add(s);
					}
				}
			}
			className = classNameList.toArray(new String[classNameList.size()]);
		}
		if (className == null) {
			return;
		}
		for (String classname : className) {
			// see if we can get the class using forName
			try {
				debugLogger.info("className:{}", classname);
				targetCls = Class.forName(classname);
				targetClassLoader = targetCls.getClassLoader();
				transform(targetCls, targetClassLoader, instrumentation);
				continue;
			} catch (Exception ex) {
				debugLogger.error("Class [{}] not found with Class.forName", classname);
			}
			// otherwise iterate all loaded classes and find what we want
			for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
				if (clazz.getName().equals(classname)) {
					targetCls = clazz;
					targetClassLoader = targetCls.getClassLoader();
					transform(targetCls, targetClassLoader, instrumentation);
				}
			}
			throw new RuntimeException("Failed to find class [" + classname + "]");
		}

	}

	private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation) {
		TraceTransformer dt = new TraceTransformer(clazz.getName(), classLoader);
		if (logFile != null) {
			dt.setLogFile(logFile);
		}
		if (d4jdatafile != null) {
			dt.setD4jDataFile(d4jdatafile);
		}
		instrumentation.addTransformer(dt, true);
		try {
			instrumentation.retransformClasses(clazz);
		} catch (Exception ex) {
			throw new RuntimeException("Transform failed for class: [" + clazz.getName() + "]", ex);
		}
	}

}