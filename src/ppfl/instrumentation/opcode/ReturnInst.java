package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//177, type void?
public class ReturnInst extends OpcodeInst {

	public ReturnInst(int _form) {
		super(_form, 0, -1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	/*
	 * @Override public void insertByteCodeAfter(CodeIterator ci, int index,
	 * ConstPool constp, CallBackIndex cbi) throws BadBytecode { int instpos =
	 * ci.insertGap(4);// the gap must be long enough for the following
	 * instrumentation ci.writeByte(184, instpos);// invokestatic
	 * ci.write16bit(cbi.tsindex_int, instpos + 1); }
	 */
}
