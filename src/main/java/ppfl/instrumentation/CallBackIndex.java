package ppfl.instrumentation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;

public class CallBackIndex {

	/**
	 *
	 */
	private static final String PRINT_CALLBACK_NAME = "printTopStack1";
	private static final int BUFFERSIZE = 1024;
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

	// logger
	private static Writer writer = null;

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

	public CallBackIndex(ConstPool constp, Writer writer) throws NotFoundException {
		// FileWriter file = null;
		// try {
		// file = new FileWriter("trace/logs/mytrace/all.log");
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		this.writer = writer;
		// writer = new BufferedWriter(file, BUFFERSIZE);
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
		try {
			writer.write(",stack=I:");
			writer.write(String.valueOf(i));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=int,pushvalue={}", i);
		return i;
	}

	public static double printTopStack1(double i) {
		try {
			writer.write(",stack=D:");
			writer.write(String.valueOf(i));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=double,pushvalue={}", i);
		return i;
	}

	public static short printTopStack1(short i) {
		try {
			writer.write(",stack=S:");
			writer.write(String.valueOf(i));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=short,pushvalue={}", i);
		return i;
	}

	public static char printTopStack1(char i) {
		try {
			writer.write(",stack=C:");
			// FIXME this is currently buggy if the string include \n or other special
			// character.
			// writer.write(String.valueOf(i));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=char,pushvalue={}", i);
		return i;
	}

	public static byte printTopStack1(byte i) {
		try {
			writer.write(",stack=B:");
			writer.write(String.valueOf(i));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=byte,pushvalue={}", i);
		return i;
	}

	public static boolean printTopStack1(boolean i) {
		try {
			writer.write(",stack=Z:");
			writer.write(String.valueOf(i));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=boolean,pushvalue={}", i);
		return i;
	}

	public static float printTopStack1(float i) {
		try {
			writer.write(",stack=F:");
			writer.write(String.valueOf(i));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=float,pushvalue={}", i);
		return i;
	}

	public static long printTopStack1(long i) {
		try {
			writer.write(",stack=J:");
			writer.write(String.valueOf(i));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=long,pushvalue={}", i);
		return i;
	}

	public static String printTopStack1(String i) {
		try {
			writer.write(",stack=Str:");
			// FIXME this is currently buggy if the string include \n.
			// writer.write(i);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=String,pushvalue=\"{}\"", i);
		return i;
	}

	public static Object printTopStack1(Object i) {
		// call system hashcode (jvm address)
		try {
			writer.write(",stack=Obj:");
			writer.write(String.valueOf(java.lang.System.identityHashCode(i)));
			writer.flush();
		} catch (IOException e) {
			// e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=object,pushvalue={}",
		// java.lang.System.identityHashCode(i));
		return i;
	}

	public static void logString(String s) {
		try {
			writer.write(s);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(s);
	}

	public static void flush() {
		try {
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
