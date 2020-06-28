package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.opcode.OpcodeInst.paratype;

//201
public class Jsr_wInst extends OpcodeInst {
	
	public Jsr_wInst(int _form) {
		super(_form,1,0);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		ret.append(",pushnum=" + this.pushnum);
		//ret.append(",popnum=" + this.popnum);
		return ret.toString();
	}
	
}
