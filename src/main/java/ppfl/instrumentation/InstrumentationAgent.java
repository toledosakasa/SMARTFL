package ppfl.instrumentation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import ppfl.MyWriter;
import ppfl.WriterUtils;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class InstrumentationAgent {
	// private static Logger debugLogger =
	// LoggerFactory.getLogger(InstrumentationAgent.class);
	private static String logFile = null;
	private static String[] className = null;
	private static List<String> classNames = null;
	private static Set<TraceDomain> foldSet = null;
	private static String d4jdatafile = null;
	private static boolean simpleLog = false;
	private static String project = null;
	private static boolean instrumentNested = false; // evaluation switch
	private static MyWriter debugLogger;

	public enum CacheCond{
    	GenPool, GenClass, GotAll
    }
	private static CacheCond cacheCond = CacheCond.GotAll;

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
		agentArgs = agentArgs.replace('@', '$'); // deal with missing $ in d4j scripts
		WriterUtils.setPath("trace/debug/");
		debugLogger = WriterUtils.getWriter("Agent.log", true);
		if (agentArgs == null || agentArgs.equals(""))
			return;
		// debugLogger.info("[Agent] In main method");
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
			if (s.startsWith("simplelog=")) {
				simpleLog = Boolean.parseBoolean(s.split("=")[1]);
			}
			if (s.startsWith("project=")) {
				project = s.split("=")[1];
			}
			if(s.startsWith("fold=")){
				if(s.split("=")[1].equals("True")){
					foldSet = new HashSet<>();
					String foldpath = "trace/logs/mytrace/fold.log";
					try (BufferedReader reader = new BufferedReader(new FileReader(foldpath))) {
						String t = null;
						while ((t = reader.readLine()) != null) {
							if(t.startsWith("[count]"))
								continue;
							String[] tmps = t.split(":");
							TraceDomain	foldDomain = new TraceDomain(tmps[0], tmps[1], tmps[2]);
							foldSet.add(foldDomain);
						}
					} catch (Exception e) {
						String sStackTrace = WriterUtils.handleException(e);
						debugLogger.write(String.format("[Bug] IO Exception, %s\n", sStackTrace));
						debugLogger.flush();
					}
				}
			}
			if (s.startsWith("cache=")) {
				String cacheArg = s.split("=")[1];
				if(cacheArg.equals("GenPool"))
					cacheCond = CacheCond.GenPool;
				else if (cacheArg.equals("GenClass"))
					cacheCond = CacheCond.GenClass;
				else if (cacheArg.equals("GotAll"))
					cacheCond = CacheCond.GotAll;
			}
		}
		debugLogger.write(String.format("[Agent] Start Agent %s\n", logFile));
		debugLogger.flush();
		if (logFile == null && className == null && classNames == null && d4jdatafile == null)
			return;
		transformClass(inst);
	}

	private static void addNameToList(List<String> l, String names) {
		if (names != null) {
			for (String s : names.split(";")) {
				if (!s.isEmpty()) {
					l.add(s);
				}
			}
		}
	}

	private static void transformClass(Instrumentation instrumentation) {
		// debugLogger.info("[Agent] In transformClass method");
		boolean transformAllClasses = false;
		if (transformAllClasses) {
			transformAll(instrumentation);
			return;
		}
		if (d4jdatafile != null) {
			String relevantClasses = null;
			String allTestClasses = null;
			String relevantTests = null;
			// String configpath = String.format("d4j_resources/metadata_cached/%s%d.log",
			// project, id);
			debugLogger.writeln( "In agent, d4jdatafile = " + d4jdatafile);
			try (BufferedReader reader = new BufferedReader(new FileReader(d4jdatafile));) {
				String tmp;
				while ((tmp = reader.readLine()) != null) {
					debugLogger.writeln( "In agent, tmp = " + tmp);
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
			debugLogger.writeln( "In agent, relevantClasses = " + relevantClasses);
			ArrayList<String> classNameList = new ArrayList<>();
			addNameToList(classNameList, relevantClasses);
			if (simpleLog) {
				addNameToList(classNameList, allTestClasses);
			} else {
				addNameToList(classNameList, relevantTests);
			}

			className = classNameList.toArray(new String[classNameList.size()]);
			for(String name: className){
				debugLogger.writeln( "In agent, calssname = " + name);
			}
		}
		if (className == null) {
			return;
		}
		for (String classname : className) {
			transform(classname, instrumentation);
		}

		String logfilename = "all.log";
		if (logFile != null) {
			logfilename = logFile;
		}
		if(simpleLog){
			SimpleTransformer dt = new SimpleTransformer(transformedclazz, logfilename);
			// d4jdatafile only used in simpleLog, and should got d4jdatafile here
			if (d4jdatafile != null) {
				dt.setD4jDataFile(d4jdatafile);
			}
			instrumentation.addTransformer(dt, true);
			// for(String classname : transformedclazz){
			// 	debugLogger.write(String.format("[Profile] Transform %s\n", classname));
			// 	try {
			// 		Class.forName(classname, false, InstrumentationAgent.class.getClassLoader());
			// 	} catch (Exception ex) {
			// 		debugLogger.write(String.format("[ProfileBug] Class %s not found with Class.forName\n", classname));
			// 	}
			// }
			return;
		}
		boolean usenewTransformer = true;
		if(usenewTransformer){
			Transformer dt;
			if(cacheCond == CacheCond.GenPool)
				dt = new GenPoolTransformer(transformedclazz, logfilename, foldSet);
			else if (cacheCond == CacheCond.GenClass)
				dt = new GenClassTransformer(transformedclazz, logfilename, foldSet);
			else
				dt = new GotAllTransformer(transformedclazz, logfilename, foldSet);

			instrumentation.addTransformer(dt, true);
			long startTime = System.currentTimeMillis();
			for(String classname : transformedclazz){
				// targetCls = Class.forName(classname);
				debugLogger.write(String.format("[Agent] Transform %s\n", classname));
				try {
					Class.forName(classname, false, InstrumentationAgent.class.getClassLoader());
				} catch (Exception ex) {
					debugLogger.write(String.format("[Bug] Class %s not found with Class.forName\n", classname));
				}
			}
			long endTime = System.currentTimeMillis();
			double time = (endTime - startTime) / 1000.0;
			debugLogger.write(String.format("[Agent] Transform time %f, %s\n", time, logfilename));
			debugLogger.flush();
			// the first test has no cache now, need to write
			if(cacheCond == CacheCond.GenPool){
				dt.output();
				System.exit(0); // this time, don't run the test
			}
			if(cacheCond == CacheCond.GenClass || cacheCond == CacheCond.GotAll)
				dt.addhook();
		}
		else{
			// TODO: 这种情况下，或许可以考虑一个优化是GenPool和GenClass在同一次启动时处理，
			// TODO: 第一次GenPool之后把对应的Transformer关掉，然后再加上GenClass的Transformer来处理，可能可以节省加载类的耗时
			TraceTransformer dt = new TraceTransformer(transformedclazz, logfilename, cacheCond);
			// for(String clazz : transformedclazz.keySet())
			// 	debugLogger.writeln("clazz: "+clazz);

			// if (d4jdatafile != null) {
			// 	dt.setD4jDataFile(d4jdatafile);
			// }
			// if (logSourceToScreen) {
			// 	dt.setLogSourceToScreen(true);
			// }
			// if (simpleLog) {
			// 	dt.setSimpleLog(true);
			// }
			instrumentation.addTransformer(dt, true);
			long startTime = System.currentTimeMillis();
			// for (Class<?> clazz : clazzset) {
			// 	try {
			// 		instrumentation.retransformClasses(clazz);
			// 	} catch (Exception ex) {
			// 		throw new RuntimeException("Transform failed for class: [" + clazz.getName() + "]", ex);
			// 	}
			// }
			long endTime = System.currentTimeMillis();
			double time = (endTime - startTime) / 1000.0;
			debugLogger.write(String.format("[Agent] Transform time %f, %s\n", time, logfilename));
			// the first test has no cache now, need to write
			if(dt.cacheCond == CacheCond.GenPool){
				dt.writeTracePool();
				dt.staticAnalysis();
				dt.writeStaticAnalyzer();
				System.exit(0); // this time, don't run the test
			}
		}
	}

	private static void transformAll(Instrumentation inst) {
		// String logfilename = "all.log";
		// if (logFile != null) {
		// 	logfilename = logFile;
		// }
		// AllClassTransformer dt = new AllClassTransformer(logfilename, project);
		// if (d4jdatafile != null) {
		// 	dt.setD4jDataFile(d4jdatafile);
		// }
		// if (simpleLog) {
		// 	dt.setSimpleLog(true);
		// }
		// inst.addTransformer(dt, true);
	}

	static Set<String> transformedclazz = new HashSet<>();

	private static void transform(String classname, Instrumentation instrumentation) {
		// classname = classname.replace(".", "/"); // replace . with /
		if (transformedclazz.contains(classname))
			return;
		// transformedclazz.put(classname, classLoader);
		transformedclazz.add(classname);
		// clazzset.add(clazz);
		// if (transformedclazz.contains(clazz.getName())) {
		// return;
		// }
		// transformedclazz.add(clazz.getName());

		// if (instrumentNested) {
		// 	// retransform nested classes.
		// 	Class<?> children[] = clazz.getDeclaredClasses();
		// 	if (children != null && children.length > 0) {
		// 		for (Class<?> chdclazz : children)
		// 			transform(chdclazz, classLoader, instrumentation);
		// 	}
		// }

		// boolean retransformSuper = false;// evaluation switch
		// // retransform super class.
		// if (retransformSuper) {
		// 	Class<?> superCl = clazz.getSuperclass();
		// 	if (superCl != null)
		// 		transform(superCl, classLoader, instrumentation);
		// }
	}

}