package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.opcode.OpcodeInst.paratype;

//178
public class GetstaticInst extends OpcodeInst {

	public GetstaticInst(int _form) {
		super(_form, 1, 0);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		ret.append(",pushnum=" + this.pushnum);
		//ret.append(",popnum=" + this.popnum);
		return ret.toString();
	}

    //TODO implement insertByteCodeAfter like ldc
    
	// @Override
	// public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
	// 		throws BadBytecode {
	// 	int instpos = ci.insertGap(4);// the gap must be long enough for the following instrumentation
	// 	ci.writeByte(184, instpos);// invokestatic
	// 	ci.write16bit(cbi.tsindex_object, instpos + 1);//may be wrong
	// }
}
