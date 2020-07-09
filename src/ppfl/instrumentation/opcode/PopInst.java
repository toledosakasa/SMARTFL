package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//87
public class PopInst extends OpcodeInst {

	boolean ifPush = false;

	public PopInst(int _form) {
		super(_form, 0, 1);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

}
