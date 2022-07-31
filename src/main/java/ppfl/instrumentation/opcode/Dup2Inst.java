package ppfl.instrumentation.opcode;

import java.util.ArrayList;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.instrumentation.CallBackIndex;

//24
public class Dup2Inst extends OpcodeInst {

	int loadindex;

	public Dup2Inst(int _form) {
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
		// ci.write16bit(cbi.tsindex_long, instpos + 1);
	}

	@Override
	public void insertAfter(CodeIterator ci, int index,ConstPool constp, CallBackIndex cbi) throws BadBytecode {

	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		super.buildtrace(graph);

		Node top = graph.getRuntimeStack().peek();
		if (top.getSize() == 2) {
			usenodes.add(top);
			defnode = graph.addNewStackNode(stmt);
			if (top.isHeapObject()) {
				defnode.setAddress(top.getAddress());
			}
			defnode.setSize(2);
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		} else if (top.getSize() == 1) {
			top = graph.getRuntimeStack().pop();
			Node nextTop = graph.getRuntimeStack().peek();
			assert (nextTop.getSize() == 1);
			graph.getRuntimeStack().push(top);

			usenodes.add(nextTop);
			defnode = graph.addNewStackNode(stmt);
			if (nextTop.isHeapObject()) {
				defnode.setAddress(nextTop.getAddress());
			}
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

			usenodes = new ArrayList<>();
			usenodes.add(top);
			defnode = graph.addNewStackNode(stmt);
			if (top.isHeapObject()) {
				defnode.setAddress(top.getAddress());
			}
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}

	}

}
