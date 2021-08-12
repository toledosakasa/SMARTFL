package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;

//79-86
public class XastoreInst extends OpcodeInst {

	public XastoreInst(int _form) {
		super(_form, 0, 3);
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
		super.buildtrace(graph);
		Node objectAddress = null;
		for (int i = 0; i < 3; i++) {
			objectAddress = graph.getRuntimeStack().pop();
			usenodes.add(objectAddress);
		}
		String field = "0";
		defnode = graph.addNewHeapNode(objectAddress, field, stmt);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

}
