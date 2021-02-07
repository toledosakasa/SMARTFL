package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.instrumentation.CallBackIndex;

//95
public class SwapInst extends OpcodeInst {

	int loadindex;

	public SwapInst(int _form) {
		super(_form, 1, 0);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",load=" + getpara(ci, index, 1));
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// int instpos = ci.insertExGap(3);// the gap must be long enough for the
		// following instrumentation
		// ci.writeByte(184, instpos);// invokestatic
		// ci.write16bit(cbi.tsindex_int, instpos + 1);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		super.buildtrace(graph);

		Node top = graph.getRuntimeStack().pop();
		Node NextTop = graph.getRuntimeStack().pop();
		assert (NextTop.getSize() == 1);
		graph.getRuntimeStack().push(top);
		graph.getRuntimeStack().push(NextTop);
	}

}
