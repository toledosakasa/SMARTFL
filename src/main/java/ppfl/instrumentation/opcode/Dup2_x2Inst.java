package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.instrumentation.CallBackIndex;

//94
public class Dup2_x2Inst extends OpcodeInst {

	int loadindex;

	public Dup2_x2Inst(int _form) {
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
		// FIXME sure this is buggy, I'll fix this later.
		// build the stmtnode(common)
		super.buildtrace(graph);

		Node top = graph.getRuntimeStack().peek();
		if (top.getSize() == 4) {
			top = graph.getRuntimeStack().pop();
			Node nextTop = graph.getRuntimeStack().pop();
			// assert (nextTop.getSize() == 1);
			usenodes.add(top);
			defnode = graph.addNewStackNode(stmt);
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
			graph.getRuntimeStack().push(nextTop);
			graph.getRuntimeStack().push(top);
		} else if (top.getSize() == 3) {
			top = graph.getRuntimeStack().pop();
			Node nextTop = graph.getRuntimeStack().pop();
			Node ThirdTop = graph.getRuntimeStack().pop();
			// assert (ThirdTop.getSize() == 1);
			usenodes.add(nextTop);
			defnode = graph.addNewStackNode(stmt);
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

			usenodes.clear();
			usenodes.add(top);
			defnode = graph.addNewStackNode(stmt);
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

			graph.getRuntimeStack().push(ThirdTop);
			graph.getRuntimeStack().push(nextTop);
			graph.getRuntimeStack().push(top);
		} else if (top.getSize() == 2) {
			top = graph.getRuntimeStack().pop();
			Node nextTop = graph.getRuntimeStack().pop();
			Node ThirdTop = graph.getRuntimeStack().pop();
			// assert (ThirdTop.getSize() == 1);
			usenodes.add(top);
			defnode = graph.addNewStackNode(stmt);
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
			graph.getRuntimeStack().push(ThirdTop);
			graph.getRuntimeStack().push(nextTop);
			graph.getRuntimeStack().push(top);
		} else if (top.getSize() == 1) {
			top = graph.getRuntimeStack().pop();
			Node nextTop = graph.getRuntimeStack().pop();
			Node ThirdTop = graph.getRuntimeStack().pop();
			Node FourthTop = graph.getRuntimeStack().pop();
			// assert (ThirdTop.getSize() == 1);
			usenodes.add(nextTop);
			defnode = graph.addNewStackNode(stmt);
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

			usenodes.clear();
			usenodes.add(top);
			defnode = graph.addNewStackNode(stmt);
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

			graph.getRuntimeStack().push(FourthTop);
			graph.getRuntimeStack().push(ThirdTop);
			graph.getRuntimeStack().push(nextTop);
			graph.getRuntimeStack().push(top);
		}

	}

}
