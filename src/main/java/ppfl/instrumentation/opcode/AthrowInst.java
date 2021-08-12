package ppfl.instrumentation.opcode;

import java.util.ArrayList;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;

//191
//FIXME The frame should be poped when not being catched.
public class AthrowInst extends OpcodeInst {

	public AthrowInst(int _form) {
		super(_form, 1, 1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		// graph.throwuse = new ArrayList<>();
		// graph.throwuse.add(defnode);
		graph.unsolvedThrow = graph.parseinfo;
		graph.throwStmt = stmt;
		graph.throwuse = usenodes;
		graph.throwpred = prednodes;
	}
}
