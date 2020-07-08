package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.opcode.OpcodeInst.paratype;

//153-158
public class IfInst extends OpcodeInst {
	
	public IfInst(int _form) {
		super(_form,0,1);
	}
	
	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		//ret.append(",pushnum=" + this.pushnum);
		ret.append(",popnum=" + this.popnum);
		return ret.toString();
	}
	
	// @Override
	// public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
	// 		throws BadBytecode {
	// 	int instpos = ci.insertGap(4);// the gap must be long enough for the following instrumentation
	// 	ci.writeByte(184, instpos);// invokestatic
	// 	ci.write16bit(cbi.tsindex_int, instpos + 1);
	// }
	
}
