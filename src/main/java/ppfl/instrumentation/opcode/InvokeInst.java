package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;

//Utils for all invoke insts.
public class InvokeInst extends OpcodeInst {
	public int argadd;

	public InvokeInst(int _form, int argadd) {
		super(_form, 0, 0);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
		this.argadd = argadd;
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
		super.buildtrace(graph);
		String traceclass = info.getvalue("callclass");
		String tracemethod = info.getvalue("callname");
		// defs
		String desc = info.getvalue("calltype");
		int argcnt = OpcodeInst.getArgNumByDesc(desc);

		// add An extra argument: caller:object->callee:this
		// for non-static invokes.
		argcnt += this.argadd;

		// collect arguments
		if (graph.getRuntimeStack().size() < argcnt) {
			System.out.println(String.format("%d stacks is not enough for %d args", graph.getRuntimeStack().size(), argcnt));
		}
		for (int i = 0; i < argcnt; i++) {
			Node node = graph.getRuntimeStack().pop();
			usenodes.add(node);
		}

		// if not traced
		if (!graph.isTraced(traceclass)) {
			System.out.println("Not traced");
			if (!OpcodeInst.isVoidMethodByDesc(desc)) {
				defnode = graph.addNewStackNode(stmt);
				graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
			}
			return;
		}

		// switch stack frame
		graph.pushStackFrame(traceclass, tracemethod);

		// static arguments starts with 0
		int paravarindex = 0;
		// non-static
		// paravarindex = 1;
		for (int i = 0; i < argcnt; i++) {

			List<Node> adduse = new ArrayList<>();
			Node curArgument = usenodes.get(argcnt - i - 1);
			adduse.add(curArgument);

			Node defnode = graph.addNewVarNode(paravarindex, stmt, traceclass, tracemethod);
			graph.buildFactor(defnode, prednodes, adduse, null, stmt);
			paravarindex += curArgument.getSize();
		}
	}
}
