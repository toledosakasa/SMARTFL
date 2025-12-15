package ppfl.instrumentation;

import java.io.IOException;
import java.io.Writer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;

import java.util.List;
import java.util.ArrayList;;

public class CallBackIndex {

	/**
	 *
	 */
	private static final String PRINT_CALLBACK_NAME = "printTopStack1";
	private static final String TRACE_CALLBACK_NAME = "traceTopStack1";
	private static final int BUFFERSIZE = 1024;
	// use the logger set by TraceTransformer
	public int stackindex;
	public int logtraceindex;
	public int logcompressindex;
	public int rettraceindex;
	public int traceindex_int;
	public int traceindex_short;
	public int traceindex_byte;
	public int traceindex_char;
	public int traceindex_boolean;
	public int traceindex_long;
	public int traceindex_float;
	public int traceindex_double;
	public int traceindex_string;
	public int traceindex_object;

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

	// threshold
	public static int loglimit = 1200000;
	public static int logcount = 0;

	// logger
	public static Writer writer = null;

	public static List<String> stackwriter = null;

	public static TraceSequence tracewriter = null;
	public static TracePool tracepool = null;
	public static List<BackEdge> loopset = null;
	// public static StaticAnalyzer staticAnalyzer = null;
	public static List<LoopCompress> compressInfos = null;

	public static class LoopCompress{
		public int compressFirst;
		public int compressLast;
		int loopFirst;
		int loopLast;
		LoopCompress(){
			compressFirst = -1;
			compressLast = -1;
			loopFirst = -1;
			loopLast = -1;
		}
	}

	public static void initCompressInfos(){
		int size = loopset.size();
		compressInfos = new ArrayList<>();
		for(int i = 0; i < size; i++){
			compressInfos.add(new LoopCompress()); 
		}
	}

	public int getLdcCallBack(Object o) {
		// decide v's type using instanceof
		if(TraceTransformer.useNewTrace){
			if (o instanceof String)
				return traceindex_string;
			else if (o instanceof Short)
				return traceindex_short;
			else if (o instanceof Long)
				return traceindex_long;
			else if (o instanceof Integer)
				return traceindex_int;
			else if (o instanceof Byte)
				return traceindex_byte;
			else if (o instanceof Character)
				return traceindex_char;
			else if (o instanceof Boolean)
				return traceindex_boolean;
			else if (o instanceof Float)
				return traceindex_float;
			else if (o instanceof Double)
				return traceindex_double;
			else
				return traceindex_object;
		}
		else{
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
			else if (o instanceof Double)
				return tsindex_double;
			else
				return tsindex_object;
		}
	}

	public static void setWriter(Writer w) {
		writer = w;
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

		stackindex = constp.addMethodrefInfo(classindex, "logStack", "()V");
		logtraceindex = constp.addMethodrefInfo(classindex, "logTrace", "(I)V");
		logcompressindex = constp.addMethodrefInfo(classindex, "logCompress", "(I)V");
		rettraceindex = constp.addMethodrefInfo(classindex, "retTrace", "(I)V");
		// traceindex_int = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(I)I");
		// traceindex_long = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(J)J");
		// traceindex_double = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(D)D");
		// traceindex_short = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(S)S");
		// traceindex_byte = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(B)B");
		// traceindex_char = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(C)C");
		// traceindex_boolean = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(Z)Z");
		// traceindex_float = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(F)F");
		// traceindex_string = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(Ljava/lang/String;)Ljava/lang/String;");
		// traceindex_object = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(Ljava/lang/Object;)Ljava/lang/Object;");
		traceindex_string = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(Ljava/lang/String;)V");
		traceindex_object = constp.addMethodrefInfo(classindex, TRACE_CALLBACK_NAME, "(Ljava/lang/Object;)V");

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
		// try {
		// 	writer.write(",stack=I:");
		// 	writer.write(String.valueOf(i));
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// stackwriter.add(",stack=I:"+String.valueOf(i)+"\n");
		// TraceTransformer.traceLogger.info(",pushtype=int,pushvalue={}", i);
		return i;
	}

	public static double printTopStack1(double i) {
		// stackwriter.add(",stack=D:"+String.valueOf(i)+"\n");
		// try {
		// 	writer.write(",stack=D:");
		// 	writer.write(String.valueOf(i));
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// TraceTransformer.traceLogger.info(",pushtype=double,pushvalue={}", i);
		return i;
	}

	public static short printTopStack1(short i) {
		// try {
		// 	writer.write(",stack=S:");
		// 	writer.write(String.valueOf(i));
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// TraceTransformer.traceLogger.info(",pushtype=short,pushvalue={}", i);
		return i;
	}

	public static char printTopStack1(char i) {
		// try {
		// 	writer.write(",stack=C:");
		// 	// FIXME this is currently buggy if the string include \n or other special
		// 	// character.
		// 	// writer.write(String.valueOf(i));
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// TraceTransformer.traceLogger.info(",pushtype=char,pushvalue={}", i);
		return i;
	}

	public static byte printTopStack1(byte i) {
		// try {
		// 	writer.write(",stack=B:");
		// 	writer.write(String.valueOf(i));
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// TraceTransformer.traceLogger.info(",pushtype=byte,pushvalue={}", i);
		return i;
	}

	public static boolean printTopStack1(boolean i) {
		// try {
		// 	writer.write(",stack=Z:");
		// 	writer.write(String.valueOf(i));
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// TraceTransformer.traceLogger.info(",pushtype=boolean,pushvalue={}", i);
		return i;
	}

	public static float printTopStack1(float i) {
		// try {
		// 	writer.write(",stack=F:");
		// 	writer.write(String.valueOf(i));
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// TraceTransformer.traceLogger.info(",pushtype=float,pushvalue={}", i);
		return i;
	}

	public static long printTopStack1(long i) {
		// try {
		// 	writer.write(",stack=J:");
		// 	writer.write(String.valueOf(i));
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// TraceTransformer.traceLogger.info(",pushtype=long,pushvalue={}", i);
		return i;
	}

	public static String printTopStack1(String i) {
		// try {
		// 	writer.write(",stack=Str:");
		// 	// FIXME this is currently buggy if the string include \n.
		// 	// writer.write(i);
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// TraceTransformer.traceLogger.info(",pushtype=String,pushvalue=\"{}\"", i);
		return i;
	}

	public static Object printTopStack1(Object i) {
		// call system hashcode (jvm address)
		try {
			writer.write(",stack=Obj:");
			writer.write(String.valueOf(java.lang.System.identityHashCode(i)));
			// writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(",pushtype=object,pushvalue={}",
		// java.lang.System.identityHashCode(i));
		return i;
	}


	// callbacks.
	// will be called by bytecode instrumentation
	public static int traceTopStack1(int i) {
		// String stackType = "I";
		// tracewriter.top().addDynamicInfo(stackType, i);
		return i;
	}

	public static double traceTopStack1(double i) {
		// String stackType = "D";
		// tracewriter.top().addDynamicInfo(stackType, i);
		return i;
	}

	public static short traceTopStack1(short i) {
		// TraceTransformer.traceLogger.info(",pushtype=short,pushvalue={}", i);
		// String stackType = "S";
		// tracewriter.top().addDynamicInfo(stackType, i);
		return i;
	}

	public static char traceTopStack1(char i) {
		// String stackType = "C";
		// tracewriter.top().addDynamicInfo(stackType, i);
		return i;
	}

	public static byte traceTopStack1(byte i) {
		// String stackType = "B";
		// tracewriter.top().addDynamicInfo(stackType, i);
		return i;
	}

	public static boolean traceTopStack1(boolean i) {
		// String stackType = "Z";
		// int value = i? 1 : 0;
		// tracewriter.top().addDynamicInfo(stackType, value);
		return i;
	}

	public static float traceTopStack1(float i) {
		// String stackType = "F";
		// tracewriter.top().addDynamicInfo(stackType, i);
		return i;
	}

	public static long traceTopStack1(long i) {
		// String stackType = "J";
		// tracewriter.top().addDynamicInfo(stackType, i);
		return i;
	}

	public static void traceTopStack1(String i) {
		// call system hashcode (jvm address)
		String stackType = "Str";
		int value = java.lang.System.identityHashCode(i);
		tracewriter.top().addDynamicInfo(stackType, value);
		return;
	}


	public static void traceTopStack1(Object i) {
		// call system hashcode (jvm address)
		String stackType = "Obj";
		int value = java.lang.System.identityHashCode(i);
		tracewriter.top().addDynamicInfo(stackType, value);
		return;
	}

	public static void logString(String s) {
		logcount++;
		if (logcount > loglimit) {
			System.exit(0);
		}
		try {
			writer.write(s);
			// writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TraceTransformer.traceLogger.info(s);
	}

	public static void logStack(){
		logcount++;
		if (tracewriter.size() > loglimit) {
			try {
				writer.write(String.format("Oversize Exit\n"));
				writer.flush();
			} catch (Exception e) {
				// TODO: handle exception
			}
			System.exit(0);
		}

		DynamicTrace st = new DynamicTrace();
		StackTraceElement[] stackTraces = new Throwable().getStackTrace();
		for(StackTraceElement stackTrace : stackTraces){
			st.stackTrace.push(stackTrace.getClassName() + "#" + stackTrace.getMethodName());
		}
		tracewriter.add(st);
		// try {
		// 	writer.write(String.format("handler donw\n"));
		// 	writer.write(st.toString());
		// 	writer.flush();
		// } catch (Exception e) {
		// 	// TODO: handle exception
		// }

	}

	public static void logTrace(int poolindex) {
		// try {
		// 	writer.write(String.format("%d\n", poolindex));
		// 	writer.flush();
		// 	// String log = tracepool.get(poolindex).toString() + "\n";
		// 	// writer.write(log);
		// 	// writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		logcount++;
		// if(logcount%10000 == 0)
		// 	System.out.println(logcount);
		// if (logcount > loglimit) {
		// 	System.exit(0);
		// }
		if (tracewriter.size() > loglimit) {
			try {
				writer.write(String.format("Oversize Exit\n"));
				writer.flush();
			} catch (Exception e) {
				// TODO: handle exception
			}
			System.exit(0);
		}
		DynamicTrace inst;
		if(TraceTransformer.useIndexTrace)
		 	inst = new DynamicTrace(poolindex);
		else
			inst = new DynamicTrace(tracepool.get(poolindex));
		tracewriter.add(inst);

		// if(logcount%100000 == 0){
		// 	try {
		// 		writer.write(String.format("logcount = %d, trace size %d\n", logcount, tracewriter.size()));
		// 		writer.flush();
		// 	} catch (Exception e) {
	
		// 	}
		// }
		// if(logcount%100000 == 0){
		// 	try {
		// 		writer.write(String.format("logcount = %d, trace at %d, tracesize = %d\n", logcount , tracewriter.size()));
		// 		writer.flush();
		// 	} catch (Exception f) {
		// 	}
		// }

		// try {
		// 	writer.write(inst.toString());
		// 	writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }

		// if(logcount > 160075 &&  tracewriter.get(160070) == null)
		// 	System.exit(0);

		// String s = TracePool.get(poolindex).toString();
		// try {
		// 	writer.write(String.format("%d\n", poolindex));
		// 	writer.flush();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }

	}

	public static void logCompress(int poolindex) {
		logcount++;
		if (tracewriter.size() > loglimit) {
			try {
				writer.write(String.format("Oversize Exit\n"));
				writer.flush();
			} catch (Exception e) {
				// TODO: handle exception
			}
			System.exit(0);
		}
		DynamicTrace inst;
		if(TraceTransformer.useIndexTrace)
		 	inst = new DynamicTrace(poolindex);
		else
			inst = new DynamicTrace(tracepool.get(poolindex));
		// loop_compress
		if(TraceTransformer.useIndexTrace){
			int loopID = 0;
			for(BackEdge loopedge: loopset){
				int lastindex = -1;
				if(tracewriter.size() > 0)
					lastindex = tracewriter.top().traceindex;
				int lasttraceindex = tracewriter.size() - 1;
				int thistraceindex = tracewriter.size();
				if(loopedge.start == lastindex && loopedge.end == poolindex){
					LoopCompress compressInfo = compressInfos.get(loopID);
					if (compressInfo.compressFirst == -1) // now start the compress
						compressInfo.compressFirst = thistraceindex;
					else{
						if(compressInfo.compressLast == -1){ // the first compress ends
							compressInfo.compressLast = lasttraceindex;
						}
						else{ // already exits first loop
							compressInfo.loopLast = lasttraceindex; // determine the end of this loop (the start is determined in last round)
							boolean canPress = true;
							int compressSize = compressInfo.compressLast - compressInfo.compressFirst;
							if (compressInfo.loopLast - compressInfo.loopFirst != compressSize) {
								canPress = false;
							} 
							else {
								for (int j = 0; j <= compressSize; j++) {
									DynamicTrace trace1 = tracewriter.getRaw(j + compressInfo.compressFirst);
									DynamicTrace trace2 = tracewriter.getRaw(j + compressInfo.loopFirst);
									if(trace1.isStackTrace() || trace2.isStackTrace()){
										canPress = false;
										break;
									}
									int index1 = trace1.traceindex;
									int index2 = trace1.traceindex;
									// TODO 目前没管dynamictrace后面跟着的地址，在考虑如果地址不一样的话，是不是不适合压缩？
									if(index1 != index2){
										canPress = false;
										break;
									}
								}

								int invokecount = 0;
								int retcount = 0;
								for(int j = 0; j <= compressSize; j++){
									DynamicTrace trace = tracewriter.getRaw(j + compressInfo.compressFirst);
									if(trace.isret)
										retcount++;
									else{
										int opcode = tracepool.get(trace.traceindex).opcode;
										if(opcode <= 186 && opcode >= 182)
											invokecount++;
									}
									// try {
									// 	trace.trace = tracepool.get(trace.traceindex);
									// 	writer.write(String.format(trace.toString()));
									// 	writer.flush();
									// } catch (Exception e) {
									// 	// TODO: handle exception
									// }

								}
								if(invokecount != retcount){
									canPress = false;
								}
								// try {
								// 	writer.write(String.format("\ninvokec = %d, retc = %d \n", invokecount, retcount));
								// 	writer.flush();
								// } catch (Exception e) {
								// 	// TODO: handle exception
								// }

							}
							if(canPress){
								for(int i = compressInfo.loopLast; i >= compressInfo.loopFirst; i--)
								{
									tracewriter.remove(i);
								}
								thistraceindex = tracewriter.size();
							}					
							else{
								compressInfo.compressFirst = thistraceindex;
								compressInfo.compressLast = -1;
							}
						}
					}
					compressInfo.loopFirst = thistraceindex;
				}
				loopID++;
			}
		}
		tracewriter.add(inst);
	}


	public static void retTrace(int poolindex) {
		logcount++;
		// if (logcount > loglimit) {
		// 	System.exit(0);
		// }
		if (tracewriter.size() > loglimit) {
			try {
				writer.write(String.format("Oversize Exit\n"));
				writer.flush();
			} catch (Exception e) {
				// TODO: handle exception
			}
			System.exit(0);
		}
		DynamicTrace inst;
		if(TraceTransformer.useIndexTrace)
		 	inst = new DynamicTrace(poolindex);
		else
			inst = new DynamicTrace(tracepool.get(poolindex));
		inst.setRetInfo();
		tracewriter.add(inst);

	}

	public static void flush() {
		try {
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
