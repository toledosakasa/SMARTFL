package ppfl.instrumentation;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;

public class CallBackIndex {

	/**
	 *
	 */
	private static final String PRINT_CALLBACK_NAME = "printTopStack1";
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
	// private int tsindex_ldc;// use constp.getLdcValue to get type.
	public int tsindex_string;
	public int tsindex_object;

	public int getLdcCallBack(Object o) {
		// decide v's type using instanceof
		if (o instanceof String)
			return tsindex_string;
		else if (o instanceof Short)
			return tsindex_short;
		else if (o instanceof Long)
			return tsindex_long;
		else if (o instanceof Integer)
			return tsindex_int;
		else if (o instanceof Byte)
			return tsindex_byte;
		else if (o instanceof Character)
			return tsindex_char;
		else if (o instanceof Boolean)
			return tsindex_boolean;
		else if (o instanceof Float)
			return tsindex_float;
		else if (o instanceof Double) {
			return tsindex_double;
		} else
			return tsindex_object;
	}

	public CallBackIndex(ConstPool constp) throws NotFoundException {
		ClassPool cp = ClassPool.getDefault();
		CtClass thisKlass = cp.get("ppfl.instrumentation.CallBackIndex");
		int classindex = constp.addClassInfo(thisKlass);

		logstringindex = constp.addMethodrefInfo(classindex, "logString", "(Ljava/lang/String;)V");
		tsindex_int = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(I)I");
		tsindex_long = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(J)J");
		tsindex_double = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(D)D");
		tsindex_short = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(S)S");
		tsindex_byte = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(B)B");
		tsindex_char = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(C)C");
		tsindex_boolean = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(Z)Z");
		tsindex_float = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(F)F");
		tsindex_string = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(Ljava/lang/String;)Ljava/lang/String;");
		tsindex_object = constp.addMethodrefInfo(classindex, PRINT_CALLBACK_NAME, "(Ljava/lang/Object;)Ljava/lang/Object;");
	}

	// callbacks.
	// will be called by bytecode instrumentation
	public static int printTopStack1(int i) {
		TraceTransformer.traceLogger.info(String.format(",pushtype=int,pushvalue=%d", i));
		return i;
	}

	public static double printTopStack1(double i) {
		TraceTransformer.traceLogger.info(String.format(",pushtype=double,pushvalue=%lf", i));
		return i;
	}

	public static short printTopStack1(short i) {
		TraceTransformer.traceLogger.info(String.format(",pushtype=short,pushvalue=%hd", i));
		return i;
	}

	public static char printTopStack1(char i) {
		TraceTransformer.traceLogger.info(String.format(",pushtype=char,pushvalue=%c", i));
		return i;
	}

	public static byte printTopStack1(byte i) {
		TraceTransformer.traceLogger.info(String.format(",pushtype=byte,pushvalue=%d", i));
		return i;
	}

	public static boolean printTopStack1(boolean i) {
		TraceTransformer.traceLogger.info(String.format(",pushtype=boolean,pushvalue=%d" + i));
		return i;
	}

	public static float printTopStack1(float i) {
		TraceTransformer.traceLogger.info(String.format(",pushtype=float,pushvalue=%f", i));
		return i;
	}

	public static long printTopStack1(long i) {
		TraceTransformer.traceLogger.info(String.format(",pushtype=long,pushvalue=%ld", i));
		return i;
	}

	public static String printTopStack1(String i) {
		TraceTransformer.traceLogger.info(",pushtype=String,pushvalue=" + i);
		return i;
	}

	public static Object printTopStack1(Object i) {
		// call system hashcode (jvm address)
		TraceTransformer.traceLogger.info(",pushtype=object,pushvalue=" + java.lang.System.identityHashCode(i));
		return i;
	}

	public static void logString(String s) {
		TraceTransformer.traceLogger.info(s);
	}

}
