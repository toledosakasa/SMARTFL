package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.ProbGraph;
import ppfl.Node;

//174
public class FreturnInst extends OpcodeInst {

	public FreturnInst(int form) {
		super(form, 0, -1);
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
		// uses
		usenodes.add(graph.getRuntimeStack().pop());
		// switch stack frame
		graph.popStackFrame();
		// def in caller frame
		Node defnode = graph.addNewStackNode(stmt);
		if (isLongReturnMethodByDesc(getCurrDomainType(graph))) {
			defnode.setSize(2);
		}
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		graph.killPredStack("OUT_" + stmt.getClassMethod());
	}

	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		// uses
		usenodes.add(graph.popStackNode());
		// switch stack frame
		graph.popFrame();
		// may be some invalid frame from junit
		if(graph.topFrame() == null)
			return;
		// def in caller frame
		Node defnode = graph.addStackNode(stmt,1);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

}
