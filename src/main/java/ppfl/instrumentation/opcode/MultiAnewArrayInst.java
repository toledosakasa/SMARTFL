package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.instrumentation.CallBackIndex;

//197
public class MultiAnewArrayInst extends OpcodeInst {

	public int dimension = 0;

	public MultiAnewArrayInst(int _form) {
		super(_form, -1, 1);
		this.doBuild = false;
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_object, instpos + 1);
	}

	@Override
	public void insertAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		int instpos = ci.insertExGap(4);// the gap must be long enough for the following instrumentation
		ci.writeByte(89, instpos);// dup
		ci.writeByte(184, instpos + 1);// invokestatic
		ci.write16bit(cbi.traceindex_object, instpos + 2);
	}
	
	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder("\n");
		ret.append("opcode=" + this.form + "(" + this.opcode + ")\t");
		ret.append(",popnum=" + ci.byteAt(index + 3));
		ret.append(",pushnum=1");
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		if (defnode != null) {
			Integer addr = graph.parseinfo.getAddressFromStack();
			if (addr != null)
				defnode.setAddress(addr);
		}
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

}
