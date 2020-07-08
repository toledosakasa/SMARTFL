package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//79-86
public class XastoreInst extends OpcodeInst {

	public XastoreInst(int _form) {
		super(_form, 0, 3);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		ret.append(",popnum=" + this.popnum);
		return ret.toString();
	}

}
