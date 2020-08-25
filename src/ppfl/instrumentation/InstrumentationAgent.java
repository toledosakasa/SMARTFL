package ppfl.instrumentation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentationAgent {
	private static Logger LOGGER = LoggerFactory.getLogger(InstrumentationAgent.class);
	private static String logFile = null;
	private static String[] className = null;
	private static List<String> classNames = null;

	public static void premain(String agentArgs, Instrumentation inst) {
		main(agentArgs, inst);
	}

	public static void agentmain(String agentArgs, Instrumentation inst) {
		// LOGGER.info("[Agent] In agentmain method");
		main(agentArgs, inst);
	}

	private static synchronized void main(String agentArgs, Instrumentation inst) {
		System.out.println(agentArgs);
		if (agentArgs == null || agentArgs == "")
			return;
		// LOGGER.info("[Agent] In premain method");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("/home/akarin/1.txt"));
			writer.write("[Agent] In premain method");
			writer.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
				classNames = new ArrayList<String>();
				try {
					classNames = java.nio.file.Files.readAllLines(cpath);
//					BufferedReader reader = new BufferedReader(new FileReader(cf));
//					String tmp;
//					while ((tmp = reader.readLine()) != null) {
//						classNames.add(tmp);
//					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (logFile == null && className == null && classNames == null)
			return;
		transformClass(inst);
	}

	private static void transformClass(Instrumentation instrumentation) {
		LOGGER.info("[Agent] In transformClass method");
		Class<?> targetCls = null;
		ClassLoader targetClassLoader = null;

		// by default, transform all classes except neglected ones.
//		if (className == null || className.contentEquals("")) {
//			for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
//				if (!isNeglect(clazz.getName())) {
//					System.out.println(clazz.getName());
//					targetCls = clazz;
//					targetClassLoader = targetCls.getClassLoader();
//					transform(targetCls, targetClassLoader, instrumentation);
//				}
//			}
//			return;
//		}
		if (className == null) {
			return;
		}
		for (String classname : className) {
			// see if we can get the class using forName
			try {
				LOGGER.info("className:" + classname);
				targetCls = Class.forName(classname);
				targetClassLoader = targetCls.getClassLoader();
				transform(targetCls, targetClassLoader, instrumentation);
				continue;
			} catch (Exception ex) {
				LOGGER.error("Class [{}] not found with Class.forName", classname);
			}
			// otherwise iterate all loaded classes and find what we want
			for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
				if (clazz.getName().equals(classname)) {
					targetCls = clazz;
					targetClassLoader = targetCls.getClassLoader();
					transform(targetCls, targetClassLoader, instrumentation);
					continue;
				}
			}
			throw new RuntimeException("Failed to find class [" + classname + "]");
		}
		if (classNames != null) {
			for (String classname : classNames) {
				// see if we can get the class using forName
				try {
					LOGGER.info("className:" + classname);
					targetCls = Class.forName(classname);
					targetClassLoader = targetCls.getClassLoader();
					transform(targetCls, targetClassLoader, instrumentation);
					continue;
				} catch (Exception ex) {
					LOGGER.error("Class [{}] not found with Class.forName", classname);
				}
				// otherwise iterate all loaded classes and find what we want
				for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
					if (clazz.getName().equals(classname)) {
						targetCls = clazz;
						targetClassLoader = targetCls.getClassLoader();
						transform(targetCls, targetClassLoader, instrumentation);
						continue;
					}
				}
				throw new RuntimeException("Failed to find class [" + classname + "]");
			}
		}

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