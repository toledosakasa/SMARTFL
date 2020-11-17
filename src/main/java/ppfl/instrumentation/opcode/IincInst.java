package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.ParseInfo;
import ppfl.StmtNode;

//132
public class IincInst extends OpcodeInst {

	boolean isIntInst = true;
	int loadedconst;

	public IincInst(int _form) {
		super(_form, 0, 0);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		buildstmt(graph);

		ParseInfo info = graph.parseinfo;
		// info.print();
		// uses
		List<Node> prednodes = new ArrayList<Node>();
		//prednodes.addAll(graph.predicates);
		List<Node> usenodes = new ArrayList<Node>();
		Node defnode = null;
		
		int varindex = info.getintvalue("VAR");
		//int incconst = info.getintvalue("CONST");
		
		//use
		usenodes.add(graph.getLoadNodeAsUse(varindex));
//		String nodename = graph.getFormalVarNameWithIndex(varindex);
//		usenodes.add(graph.getNode(nodename));
		//def
		defnode = graph.addNewVarNode(varindex, stmt);
//		graph.incVarIndex(varindex);
//		nodename = graph.getFormalVarNameWithIndex(varindex);
//		defnode = new Node(nodename, graph.testname, stmt);
//		graph.addNode(nodename, defnode);

		// build factor.
		if (defnode != null) {
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
//			if (graph.auto_oracle) {
//				graph.last_defined_var = defnode;
//			}
		}
	}
	
	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",VAR=" + getpara(ci, index, 1));
		ret.append(",CONST=" + getpara(ci, index, 2));
		return ret.toString();
	}

	// @Override
	// public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp,
	// CallBackIndex cbi)
	// throws BadBytecode {
	// int instpos = ci.insertGap(4);// the gap must be long enough for the
	// following instrumentation
	// ci.writeByte(184, instpos);// invokestatic
	// ci.write16bit(cbi.tsindex_int, instpos + 1);
	// }

}