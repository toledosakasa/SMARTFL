package ppfl.instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 

import java.lang.instrument.Instrumentation;

public class InstrumentationAgent {
    private static Logger LOGGER = LoggerFactory.getLogger(InstrumentationAgent.class);
    private static String logFile = null;
    
    
    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In premain method");
         
        String className = null;
        for(String s :agentArgs.split(",")) {
        	if(s.startsWith("class=")) {
        		className = s.split("=")[1];
        	}
        	if(s.startsWith("logfile=")) {
        		logFile = s.split("=")[1];
        	}
        }
        
        transformClass(className,inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In agentmain method");

        String className = agentArgs;
        transformClass(className,inst);
    }

    private static void transformClass(String className, Instrumentation instrumentation) {
    	LOGGER.info("[Agent] In transformClass method");
    	Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
        // see if we can get the class using forName
        try {
            LOGGER.info("className:" + className);
        	targetCls = Class.forName(className);
            targetClassLoader = targetCls.getClassLoader();

            transform(targetCls, targetClassLoader, instrumentation);
            return;
        } catch (Exception ex) {
            LOGGER.error("Class [{}] not found with Class.forName",className);
        }
        // otherwise iterate all loaded classes and find what we want
        for(Class<?> clazz: instrumentation.getAllLoadedClasses()) {
            if(clazz.getName().equals(className)) {
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                transform(targetCls, targetClassLoader, instrumentation);
                return;
            }
        }
        throw new RuntimeException("Failed to find class [" + className + "]");
    }

    private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation) {
        TraceTransformer dt = new TraceTransformer(clazz.getName(), classLoader);
        if(logFile != null) {
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