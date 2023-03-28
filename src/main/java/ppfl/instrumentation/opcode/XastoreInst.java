package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.ProbGraph;
import ppfl.Node;

import java.util.ArrayList;
import java.util.List;

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

	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		usenodes.add(graph.popStackNode());
		Node indexNode = graph.popStackNode();
		usenodes.add(indexNode);
		Node objectAddress = graph.popStackNode();
		usenodes.add(objectAddress);

		Node self = graph.getObj(objectAddress);
		List<Node> usenodes2 = new ArrayList<>();
		usenodes2.addAll(usenodes);
		if(self != null)
			usenodes2.add(self);

		defnode = graph.addObj(objectAddress, stmt);
		graph.buildFactor(defnode, prednodes, usenodes2, null, stmt);

		String field = "0";
		int size = 1;
		if(this.form == 80 || this.form == 82)
			size = 2;
		defnode = graph.addHeap(objectAddress, field, stmt, size);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

}
