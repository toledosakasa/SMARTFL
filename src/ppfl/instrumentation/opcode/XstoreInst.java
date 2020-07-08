package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.opcode.OpcodeInst.paratype;

//54-78
public class XstoreInst extends OpcodeInst {

	public XstoreInst(int _form) {
		super(_form, 0, 1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		ret.append(",popnum=" + this.popnum);
		ret.append(",store=" + getpara(ci, index, 1));
		return ret.toString();
	}

}
