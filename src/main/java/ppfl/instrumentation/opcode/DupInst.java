package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.instrumentation.CallBackIndex;

//24
public class DupInst extends OpcodeInst {

	int loadindex;

	public DupInst(int _form) {
		super(_form, 1, 0);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",load=" + getpara(ci, index, 1));
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_double, instpos + 1);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		super.buildtrace(graph);

		usenodes.add(graph.getRuntimeStack().peek());

		// defs
		// stack
		if (info.getintvalue("pushnum") != null) {
			int instpushnum = info.getintvalue("pushnum");
			// push must not be more than 1
			assert (instpushnum == 1);
			defnode = graph.addNewStackNode(stmt);
			defnode.setSize(usenodes.get(0).getSize());
		}
		// build factor.
		if (defnode != null) {
			// TODO should consider ops.
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
	}

}
