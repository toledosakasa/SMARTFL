package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.ProbGraph;

//177, type void?
public class ReturnInst extends OpcodeInst {

	public ReturnInst(int _form) {
		super(_form, 0, -1);
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
		// switch stack frame
		graph.popStackFrame();
		graph.killPredStack("OUT_" + stmt.getClassMethod());
	}

	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		graph.popFrame();
	}

}
