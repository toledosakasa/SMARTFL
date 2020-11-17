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

//9-10
public class LconstInst extends OpcodeInst {

	long loadedconst;

	public LconstInst(int _form, long _loadedconst) {
		super(_form, 1, 0);
		this.loadedconst = _loadedconst;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_long, instpos + 1);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		StmtNode stmt = buildstmt(graph);
		ParseInfo info = graph.parseinfo;
		List<Node> prednodes = new ArrayList<Node>();
		List<Node> usenodes = new ArrayList<Node>();
		Node defnode = null;
		if (info.getintvalue("pushnum") != null) {
			int instpushnum = info.getintvalue("pushnum");
			// push must not be more than 1
			assert (instpushnum == 1);
			defnode = graph.addNewStackNode(stmt);
			defnode.setSize(2);
		}
		// build factor.
		if (defnode != null) {
			// TODO should consider ops.
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
	}

}
