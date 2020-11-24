package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;

//159-164
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
		ret.append(",default=" + this.gets32bitpara(ci, index));
		index += 4;
		int npairs = this.gets32bitpara(ci, index);
		ret.append(",switch=");
		for (int i = 0; i < npairs; i++) {
			index += 4;
			ret.append(this.gets32bitpara(ci, index));
			ret.append(",");
			index += 4;
			ret.append(this.gets32bitpara(ci, index));
			ret.append(";");
		}
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
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
			List<String> ops = new ArrayList<>();
			graph.buildFactor(defnode, prednodes, usenodes, ops, stmt);
		}
	}

}
