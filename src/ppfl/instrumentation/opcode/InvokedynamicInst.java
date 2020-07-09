package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;

//186
public class InvokedynamicInst extends OpcodeInst {

	public InvokedynamicInst(int _form) {
		super(_form, 0, 0);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		// ret.append(",pushnum=" + this.pushnum);
		// ret.append(",popnum=" + this.popnum);
		int callindex = getu16bitpara(ci, index);
		ret.append(getmethodinfo(ci, callindex, constp));
		return ret.toString();
	}
	
	@Override
	public void parsetrace(ByteCodeGraph graph, String trace) {
		//TODO
	}

}
