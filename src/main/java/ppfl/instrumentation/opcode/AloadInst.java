package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.instrumentation.CallBackIndex;

//25
public class AloadInst extends OpcodeInst {

	int loadindex;

	public AloadInst(int _form) {
		super(_form, 1, 0);
		this.doBuild = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",load=" + getpara(ci, index, 1));
		return ret.toString();
	}

	@Override
	public String getinst_wide(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",load=" + getu16bitpara(ci, index));
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_object, instpos + 1);
	}

	@Override
	public void insertAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.traceindex_object, instpos + 1);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		if (defnode != null) {
			Integer addr = graph.parseinfo.getAddressFromStack();
			if(addr != null)
				defnode.setAddress(addr);
		}
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

}
