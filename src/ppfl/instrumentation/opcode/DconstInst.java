package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;

//14-15
public class DconstInst extends OpcodeInst {

	double loadedconst;

	public DconstInst(int _form, double _loadedconst) {
		super(_form, 1, 0);
		this.loadedconst = _loadedconst;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		ret.append(",pushnum=" + this.pushnum);
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		int instpos = ci.insertGap(4);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_double, instpos + 1);
	}

}
