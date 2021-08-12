package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;

//167
public class GotoInst extends OpcodeInst {

	public GotoInst(int _form) {
		super(_form, 0, 0);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",branchbyte=" + String.valueOf(this.gets16bitpara(ci, index)));
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		if(!graph.gotoNoBranch){
			defnode = graph.addNewPredNode(stmt);
			graph.pushPredStack(defnode);
			// build factor.
			if (defnode != null) {
				List<String> ops = new ArrayList<>();
				// ops.add("==");
				graph.buildFactor(defnode, prednodes, usenodes, ops, stmt);
			}
		}
	}

}
