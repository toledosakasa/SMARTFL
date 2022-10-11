package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;

//153-158
public class IfInst extends OpcodeInst {

	public IfInst(int _form) {
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
		ret.append(",branchbyte=" + this.gets16bitpara(ci, index));
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
			if(this.form == 153)
				ops.add("==");
			else if (this.form == 154)
				ops.add("!=");
			else if (this.form == 155)
				ops.add("<");
			else if (this.form == 156)
				ops.add(">=");
			else if (this.form == 157)
				ops.add(">");
			else
				ops.add("<=");
			graph.buildFactor(defnode, prednodes, usenodes, ops, stmt);
		}
	}

}
