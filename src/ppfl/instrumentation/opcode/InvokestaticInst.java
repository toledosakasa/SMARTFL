package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.ParseInfo;
import ppfl.StmtNode;

//184
public class InvokestaticInst extends OpcodeInst {

	public InvokestaticInst(int _form) {
		super(_form, 0, 0);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		int callindex = getu16bitpara(ci, index);
		ret.append(getmethodinfo(ci, callindex, constp));
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		StmtNode stmt = buildstmt(graph);

		ParseInfo info = graph.parseinfo;
		// uses

		// defs
		int argcnt = OpcodeInst.getArgNumByDesc(info.getvalue("calltype"));
		for (int i = 0; i < argcnt; i++) {
			List<Node> prednodes = new ArrayList<Node>();
			List<Node> usenodes = new ArrayList<Node>();
			Node node = graph.runtimestack.pop();
			usenodes.add(node);
			// static arguments starts with 0
			String traceclass = info.getvalue("callclass");
			String tracemethod = info.getvalue("callname");
			graph.incVarIndex(i, traceclass, tracemethod);
			String nodename = graph.getFormalVarName(i, traceclass, tracemethod);
			Node defnode = new Node(nodename, graph.testname, stmt);
			graph.addNode(nodename, defnode);
			assert (defnode == graph.getNode(nodename));
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
//		if (info.getintvalue("pushnum") != null) {
//			int instpushnum = info.getintvalue("pushnum");
//			// push must not be more than 1
//			assert (instpushnum == 1);
//			graph.incStackIndex();
//			String nodename = graph.getFormalStackNameWithIndex();
//			Node node = new Node(nodename, graph.testname, stmt);
//			graph.addNode(nodename, node);
//			defnode = graph.getNode(nodename);
//			assert(node == defnode);
//			graph.runtimestack.add(defnode);
//		}
//		if (info.getintvalue("store") != null) {
//		int storevar = info.getintvalue("store");
//		graph.incVarIndex(storevar);
//		String nodename = graph.getFormalVarNameWithIndex(storevar);
//		Node node = new Node(nodename,graph.testname,stmt);
//		graph.addNode(nodename, node);
//		defnode = graph.getNode(nodename);
//		assert(node == defnode);
//		}

		// build factor.

	}
}
