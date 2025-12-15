package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.ProbGraph;
import ppfl.instrumentation.CallBackIndex;

//38-41
public class Dload_NInst extends OpcodeInst {

	int loadindex;

	public Dload_NInst(int _form, int _loadindex) {
		super(_form, 1, 0);
		loadindex = _loadindex;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",load=" + loadindex);
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_double, instpos + 1);
	}

	@Override
	public void insertAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		// ci.writeByte(184, instpos);// invokestatic
		// ci.write16bit(cbi.traceindex_double, instpos + 1);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		defnode.setSize(2);
	}

	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		defnode.setSize(2);
	}
}
