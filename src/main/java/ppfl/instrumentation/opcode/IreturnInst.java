package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.ParseInfo;
import ppfl.StmtNode;
import ppfl.instrumentation.RuntimeFrame;

//172
public class IreturnInst extends OpcodeInst {

	public IreturnInst(int _form) {
		super(_form, 0, -1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		StmtNode stmt = buildstmt(graph);

		ParseInfo info = graph.parseinfo;
		List<Node> prednodes = new ArrayList<Node>();
		List<Node> usenodes = new ArrayList<Node>();
		// uses
		usenodes.add(graph.getRuntimeStack().pop());
		// switch stack frame
		graph.popStackFrame();
		// def in caller frame
		Node defnode = graph.addNewStackNode(stmt);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

	}

}
