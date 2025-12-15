package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ProbGraph;

//54-78
public class Xstore_NInst extends OpcodeInst {

	int storeindex;

	public Xstore_NInst(int _form, int _storeindex) {
		super(_form, 0, 1);
		this.storeindex = _storeindex;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",store=" + storeindex);
		return ret.toString();
	}
	
	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		if((form >= 63 && form <= 66) || (form >= 71 && form <= 74))
			defnode.setSize(2);
	}
}
