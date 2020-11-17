package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.ParseInfo;
import ppfl.StmtNode;

//153-158
public class IfInst extends OpcodeInst {

	public IfInst(int _form) {
		super(_form, 0, 1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",branchbyte="+this.gets16bitpara(ci, index));
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		buildstmt(graph);
		ParseInfo info = graph.parseinfo;
		List<Node> prednodes = new ArrayList<>();
		List<Node> usenodes = new ArrayList<>();
		Node defnode = null;
		if (info.getintvalue("popnum") != null) {
			int instpopnum = info.getintvalue("popnum");
			for (int i = 0; i < instpopnum; i++) {
				usenodes.add(graph.getRuntimeStack().pop());
			}
		}
		defnode = graph.addNewPredNode(stmt);
		graph.pushPredStack(defnode);
		// build factor.
		if (defnode != null) {
			// TODO should consider ops.
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
	}

}
