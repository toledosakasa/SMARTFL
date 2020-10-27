package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.ParseInfo;
import ppfl.StmtNode;
import ppfl.instrumentation.CallBackIndex;

//176
public class AreturnInst extends OpcodeInst {

	public AreturnInst(int _form) {
		super(_form, 0, -1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		int instpos = ci.insertGap(4);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_object, instpos + 1);
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
