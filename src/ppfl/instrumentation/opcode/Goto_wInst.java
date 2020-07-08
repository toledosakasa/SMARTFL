package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//200
public class Goto_wInst extends OpcodeInst {

	public Goto_wInst(int _form) {
		super(_form, 0, 0);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		// ret.append(",pushnum=" + this.pushnum);
		// ret.append(",popnum=" + this.popnum);
		return ret.toString();
	}

}
