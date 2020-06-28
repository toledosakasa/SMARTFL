package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//186
public class InvokedynamicInst extends OpcodeInst {
	
	public InvokedynamicInst(int _form) {
		super(_form, 0, 0);
	}
	
	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		//ret.append(",pushnum=" + this.pushnum);
		//ret.append(",popnum=" + this.popnum);
		int callindex = get2para(ci, index);
		ret.append(getmethodinfo(ci, callindex, constp));
		return ret.toString();
	}
	
}
