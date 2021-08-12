package ppfl.instrumentation.opcode;

import ppfl.ByteCodeGraph;

//184
public class InvokestaticInst extends InvokeInst {

	public InvokestaticInst(int _form) {
		super(_form, 0);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		// String traceclass = info.getvalue("callclass");
		// String tracemethod = info.getvalue("callname");
		// // defs
		// String desc = info.getvalue("calltype");
		// int argcnt = OpcodeInst.getArgNumByDesc(desc);
		// // collect arguments
		// if (graph.getRuntimeStack().size() < argcnt) {
		// System.out.println(String.format("%d stacks is not enough for %d args",
		// graph.getRuntimeStack().size(), argcnt));
		// }
		// for (int i = 0; i < argcnt; i++) {
		// Node node = graph.getRuntimeStack().pop();
		// usenodes.add(node);
		// }

		// // if not traced
		// if (!graph.isTraced(traceclass)) {
		// System.out.println("Not traced");
		// if (!OpcodeInst.isVoidMethodByDesc(desc)) {
		// defnode = graph.addNewStackNode(stmt);
		// graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		// }
		// return;
		// }

		// // switch stack frame
		// graph.pushStackFrame(traceclass, tracemethod);

		// // static arguments starts with 0
		// int paravarindex = 0;
		// // non-static
		// // paravarindex = 1;
		// for (int i = 0; i < argcnt; i++) {

		// List<Node> adduse = new ArrayList<>();
		// Node curArgument = usenodes.get(argcnt - i - 1);
		// adduse.add(curArgument);

		// Node defnode = graph.addNewVarNode(paravarindex, stmt, traceclass,
		// tracemethod);
		// graph.buildFactor(defnode, prednodes, adduse, null, stmt);
		// paravarindex += curArgument.getSize();
		// }
	}
}
