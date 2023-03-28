package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;
import ppfl.ProbGraph;

//1
public class AconstInst extends OpcodeInst {

	public AconstInst(int _form) {
		super(_form, 1, 0);
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
		ci.write16bit(cbi.tsindex_object, instpos + 1);
	}

	@Override
	public void insertAfter(CodeIterator ci, int index,  ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
			int instpos = ci.insertExGap(4);// the gap must be long enough for the following instrumentation
			ci.writeByte(89, instpos);// dup
			ci.writeByte(184, instpos + 1);// invokestatic
			ci.write16bit(cbi.traceindex_object, instpos + 2);
	}

	@Override
	public void build(ProbGraph graph){
		super.build(graph);
		Integer addr = dtrace.getAddressFromStack();
		assert(addr != null);
		defnode.setAddress(addr);
	}

}
