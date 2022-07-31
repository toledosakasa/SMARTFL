package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.instrumentation.CallBackIndex;

//50
public class XaloadInst extends OpcodeInst {

	public XaloadInst(int _form) {
		super(_form, 1, 2);
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
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// will be overrided.
	}

	@Override
	public void insertAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// will be overrided.
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		Node indexNode = graph.getRuntimeStack().pop();
		usenodes.add(indexNode);
		Node objectAddress = graph.getRuntimeStack().pop();
		usenodes.add(objectAddress);
		String index = "0";// TODO resolve from indexNode
		Node usenode = graph.getHeapNode(objectAddress, index);
		if (usenode != null)
			usenodes.add(usenode);
		defnode = graph.addNewStackNode(stmt);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

}
