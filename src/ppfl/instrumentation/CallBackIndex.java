package ppfl.instrumentation;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;

public class CallBackIndex {

	// use the logger set by TraceTransformer
	int logstringindex;
	int tsindex_int;
	int tsindex_short;
	int tsindex_byte;
	int tsindex_char;
	int tsindex_boolean;
	int tsindex_long;
	int tsindex_float;
	int tsindex_double;
	int tsindex_ldc;// use constp.getLdcValue to get type.
	int tsindex_string;
	int tsindex_object;

	public CallBackIndex(ConstPool constp) throws NotFoundException {
		ClassPool cp = ClassPool.getDefault();
		CtClass THISCLASS = cp.get("ppfl.instrumentation.CallBackIndex");
		int classindex = constp.addClassInfo(THISCLASS);
		logstringindex = constp.addMethodrefInfo(classindex, "logString", "(Ljava/lang/String;)V");
		tsindex_int = constp.addMethodrefInfo(classindex, "printTopStack1", "(I)I");
	}

	// callbacks.
	// will be called by bytecode instrumentation
	public static int printTopStack1(int i) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:int, value=" + String.valueOf(i));
		return i;
	}

	public static double printTopStack1(double i) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:double, value=" + String.valueOf(i));
		return i;
	}
	
	public static short printTopStack1(short i) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:short, value=" + String.valueOf(i));
		return i;
	}
	
	public static char printTopStack1(char i) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:char, value=" + String.valueOf(i));
		return i;
	}
	
	public static byte printTopStack1(byte i) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:byte, value=" + String.valueOf(i));
		return i;
	}
	
	public static boolean printTopStack1(boolean i) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:boolean, value=" + String.valueOf(i));
		return i;
	}
	
	public static float printTopStack1(float i) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:float, value=" + String.valueOf(i));
		return i;
	}
	
	public static long printTopStack1(long i) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:long, value=" + String.valueOf(i));
		return i;
	}

	public static String printTopStack1(String i) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:double, value=" + i);
		return i;
	}
	
	public static Object printTopStack1(Object i) {
		//call system hashcode (jvm address)
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, "Topstack:object, value=" + java.lang.System.identityHashCode(i));
		return i;
	}
	
	public static void logString(String s) {
		TraceTransformer.TRACELOGGER.log(java.util.logging.Level.INFO, s);
	}

}
