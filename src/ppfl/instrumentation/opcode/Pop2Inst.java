package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//88
public class Pop2Inst extends OpcodeInst {

	boolean ifPush = false;

	public Pop2Inst(int _form) {
		super(_form, 0, 2);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		ret.append(",popnum=" + this.popnum);
		return ret.toString();
	}

}