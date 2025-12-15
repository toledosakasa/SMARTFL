package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.ProbGraph;

//171
public class LookupSwitchInst extends OpcodeInst {

	public LookupSwitchInst(int _form) {
		super(_form, 0, 1);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		index += (3 - (index % 4));
		ret.append(",default=" + this.gets32bitpara(ci, index));
		index += 4;
		int npairs = this.gets32bitpara(ci, index);
		if (npairs > 0) {
			ret.append(",switch=");
			for (int i = 0; i < npairs; i++) {
				index += 4;
				ret.append(this.gets32bitpara(ci, index));
				ret.append(":");
				index += 4;
				ret.append(this.gets32bitpara(ci, index));
				ret.append(";");
			}
		}
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		if (dtrace.trace.popnum != null) {
			int instpopnum = dtrace.trace.popnum;
			for (int i = 0; i < instpopnum; i++) {
				usenodes.add(graph.getRuntimeStack().pop());
			}
		}
		defnode = graph.addNewPredNode(stmt);
		graph.pushPredStack(defnode);
		// build factor.
		if (defnode != null) {
			List<String> ops = new ArrayList<>();
			graph.buildFactor(defnode, prednodes, usenodes, ops, stmt);
		}
	}

	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		int instpopnum = dtrace.trace.popnum;
		for (int i = 0; i < instpopnum; i++) {
			usenodes.add(graph.popStackNode());
		}
		defnode = graph.pushPred(stmt);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

}
