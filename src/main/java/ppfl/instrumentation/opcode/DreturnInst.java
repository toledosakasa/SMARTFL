package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.StmtNode;

//175
public class DreturnInst extends OpcodeInst {

	public DreturnInst(int form) {
		super(form, 0, -1);
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

		//ParseInfo info = graph.parseinfo;
		List<Node> prednodes = new ArrayList<>();
		List<Node> usenodes = new ArrayList<>();
		// uses
		usenodes.add(graph.getRuntimeStack().pop());
		// switch stack frame
		graph.popStackFrame();
		// def in caller frame
		Node defnode = graph.addNewStackNode(stmt);
		defnode.setSize(2);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

	}

}
