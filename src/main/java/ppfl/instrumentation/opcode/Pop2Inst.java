package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;

//88
public class Pop2Inst extends OpcodeInst {

	public Pop2Inst(int _form) {
		super(_form, 0, 2);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		Node poped1 = graph.getRuntimeStack().pop();
		if (poped1.getSize() == 1)
			graph.getRuntimeStack().pop();
	}
}