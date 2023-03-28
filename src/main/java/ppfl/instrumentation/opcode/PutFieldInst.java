package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.ProbGraph;
import ppfl.instrumentation.CallBackIndex;

import java.util.ArrayList;
import java.util.List;

//181
public class PutFieldInst extends OpcodeInst {

	public PutFieldInst(int _form) {
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
		ret.append(getFieldInfo(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		usenodes.add(graph.getRuntimeStack().pop());
		Node objectAddress = graph.getRuntimeStack().pop();
		usenodes.add(objectAddress);
		String field = graph.dynamictrace.trace.getfield();
		defnode = graph.addNewHeapNode(objectAddress, field, stmt);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		usenodes.add(graph.popStackNode());
		Node objectAddress = graph.popStackNode();
		usenodes.add(objectAddress);

		Node self = graph.getObj(objectAddress);
		List<Node> usenodes2 = new ArrayList<>();
		usenodes2.addAll(usenodes);
		if(self != null)
			usenodes2.add(self);
		defnode = graph.addObj(objectAddress, stmt);
		graph.buildFactor(defnode, prednodes, usenodes2, null, stmt);

		String field = dtrace.trace.getfield();
		int size = dtrace.trace.getfieldsize();
		defnode = graph.addHeap(objectAddress, field, stmt, size);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// int instpos = ci.insertExGap(3);// the gap must be long enough for the
		// following instrumentation
		// ci.writeByte(184, instpos);// invokestatic
		// ci.write16bit(cbi.tsindex_object, instpos + 1);
	}

}
