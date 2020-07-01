package ppfl.instrumentation;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;

public class CallBackIndex {

	// use the logger set by TraceTransformer
	public int logstringindex;
	public int tsindex_int;
	public int tsindex_short;
	public int tsindex_byte;
	public int tsindex_char;
	public int tsindex_boolean;
	public int tsindex_long;
	public int tsindex_float;
	public int tsindex_double;
	public int tsindex_ldc;// use constp.getLdcValue to get type.
	public int tsindex_string;
	public int tsindex_object;

	public CallBackIndex(ConstPool constp) throws NotFoundException {
		ClassPool cp = ClassPool.getDefault();
		CtClass THISCLASS = cp.get("ppfl.instrumentation.CallBackIndex");
		int classindex = constp.addClassInfo(THISCLASS);
		logstringindex = constp.addMethodrefInfo(classindex, "logString", "(Ljava/lang/String;)V");
		tsindex_int = constp.addMethodrefInfo(classindex, "printTopStack1", "(I)I");
		tsindex_long = constp.addMethodrefInfo(classindex, "printTopStack1", "(J)J");
		tsindex_double = constp.addMethodrefInfo(classindex, "printTopStack1", "(D)D");
		tsindex_short = constp.addMethodrefInfo(classindex, "printTopStack1", "(S)S");
		tsindex_byte = constp.addMethodrefInfo(classindex, "printTopStack1", "(B)B");
		tsindex_char = constp.addMethodrefInfo(classindex, "printTopStack1", "(C)C");
		tsindex_boolean = constp.addMethodrefInfo(classindex, "printTopStack1", "(Z)Z");
		tsindex_float = constp.addMethodrefInfo(classindex, "printTopStack1", "(F)F");
		tsindex_string = constp.addMethodrefInfo(classindex, "printTopStack1", "(Ljava/lang/String;)Ljava/lang/String;");
		tsindex_object = constp.addMethodrefInfo(classindex, "printTopStack1", "(Ljava/lang/Object;)Ljava/lang/Object;");
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