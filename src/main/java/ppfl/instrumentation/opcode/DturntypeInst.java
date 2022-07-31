package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;

//135,138,141
public class DturntypeInst extends OpcodeInst {

	boolean isDoubleInst = true;

	public DturntypeInst(int _form) {
		super(_form, 1, 1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_double, instpos + 1);
	}

	@Override
	public void insertAfter(CodeIterator ci, int index,  ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		// int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		// ci.writeByte(184, instpos);// invokestatic
		// ci.write16bit(cbi.traceindex_double, instpos + 1);
	}

}