package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;

//132
public class IincInst extends OpcodeInst {

	boolean isIntInst = true;
	int loadedconst;

	public IincInst(int _form) {
		super(_form, 0, 0);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		super.buildtrace(graph);

		int varindex = info.getintvalue("store");
		// int incconst = info.getintvalue("CONST");
		// if (varindex == 31) {
		// System.out.println("iinc:" + graph.getDomain());
		// }
		Node changed = graph.getLoadNodeAsUse(varindex);
		if (changed != null) // evaluation switch
			usenodes.add(changed);
		defnode = graph.addNewVarNode(varindex, stmt);

		// build factor.
		if (defnode != null) {
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		String incVar = getpara(ci, index, 1);
		ret.append(",store=" + incVar);
		ret.append(",CONST=" + getpara(ci, index, 2));
		return ret.toString();
	}

	@Override
	public String getinst_wide(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		int incVar = getu16bitpara(ci, index);
		ret.append(",store=" + incVar);
		ret.append(",CONST=" + gets16bitpara(ci, index + 2));
		return ret.toString();
	}

}