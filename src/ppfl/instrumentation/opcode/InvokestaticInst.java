package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//184
public class InvokestaticInst extends OpcodeInst {

	public InvokestaticInst(int _form) {
		super(_form, 0, 0);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		int callindex = getu16bitpara(ci, index);
		ret.append(getmethodinfo(ci, callindex, constp));
		return ret.toString();
	}

}
