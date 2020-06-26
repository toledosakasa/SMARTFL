package ppfl.instrumentation;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;

public class CallBackIndex {

	int logstringindex;
	int tsindex_int;
	int tsindex_short;
	int tsindex_byte;
	int tsindex_char;
	int tsindex_boolean;
	int tsindex_long;
	int tsindex_float;
	int tsindex_double;
	int tsindex_ldc;//use constp.getLdcValue to get type.
	int tsindex_string;
	int tsindex_object;

	public CallBackIndex(ConstPool constp) throws NotFoundException {
		ClassPool cp = ClassPool.getDefault();
		CtClass THISCLASS = cp.get("ppfl.instrumentation.TraceTransformer");
		int classindex = constp.addClassInfo(THISCLASS);
		logstringindex = constp.addMethodrefInfo(classindex, "logString", "(Ljava/lang/String;)V");
		tsindex_int = constp.addMethodrefInfo(classindex, "printTopStack1", "(I)I");
	}
}
