package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ProbGraph;
import ppfl.Node;

//87
public class PopInst extends OpcodeInst {

	boolean ifPush = false;

	public PopInst(int _form) {
		super(_form, 0, 1);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void build(ProbGraph graph){
		Node pop1 = graph.popStackNode();
		assert(pop1.getSize() == 1);
	}

}
