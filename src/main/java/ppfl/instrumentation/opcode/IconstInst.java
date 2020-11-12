package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;

//2-8
public class IconstInst extends OpcodeInst {

	boolean isIntInst = true;
	int loadedconst;

	public IconstInst(int _form, int _loadedconst) {
		super(_form, 1, 0);
		this.loadedconst = _loadedconst;
		// TODO Auto-generated constructor stub
	}

//	@Override
//	public String getinst(CodeIterator ci, int index, ConstPool constp) {
//		StringBuilder ret = new StringBuilder();
//		ret.append("opcode=" + this.opcode);
//		ret.append(",pushnum=" + this.pushnum);
//		return ret.toString();
//	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		int instpos = ci.insertExGap(4);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_int, instpos + 1);
	}

}
