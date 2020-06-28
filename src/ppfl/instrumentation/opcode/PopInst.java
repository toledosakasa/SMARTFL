package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.opcode.OpcodeInst.paratype;

//87
public class PopInst extends OpcodeInst {

	boolean ifPush = false;
	
	public PopInst(int _form) {
		super(_form, 0, 1);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		ret.append(",popnum=" + this.popnum);
		return ret.toString();
	}
	
}
