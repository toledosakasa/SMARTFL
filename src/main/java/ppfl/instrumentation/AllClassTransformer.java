// package ppfl.instrumentation;

// import java.lang.instrument.IllegalClassFormatException;
// import java.security.ProtectionDomain;
// import java.util.HashMap;
// import java.util.Map;

// public class AllClassTransformer extends TraceTransformer {
//   static Map<String, String> projectPrefix = new HashMap<>();
//   static {
//     projectPrefix.put("Lang", "org/apache/commons/lang3");
//     projectPrefix.put("Math", "org/apache/commons/math3");
//     projectPrefix.put("Chart", "org/jfree");
//     projectPrefix.put("Time", "org/joda/time");
//     projectPrefix.put("Closure", "com/google/javascript");
//   }
//   String project;

//   public AllClassTransformer(String logfilename, String project) {
//     super(null, null, logfilename);
//   }

//   @Override
//   public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
//       ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//     String prefix = projectPrefix.get(project);
//     if (!className.startsWith(prefix)) {
//       return null;
//     }
//     try {
//       return transformBody(className);
//       // return transformBody(loader, className, classBeingRedefined,
//       // protectionDomain, classfileBuffer);
//     } catch (Exception e) {
//       // debugLogger.error("[Bug]Exception", e);
//       e.printStackTrace();
//       return null;
//     }
//   }
// }
