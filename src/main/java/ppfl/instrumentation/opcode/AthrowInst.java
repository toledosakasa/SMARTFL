package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//191
//FIXME The frame should be poped when not being catched.
public class AthrowInst extends OpcodeInst {

	public AthrowInst(int _form) {
		super(_form, 1, 1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}
}
