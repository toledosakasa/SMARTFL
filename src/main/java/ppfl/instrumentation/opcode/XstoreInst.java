package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ProbGraph;

//54-78
public class XstoreInst extends OpcodeInst {

	public XstoreInst(int _form) {
		super(_form, 0, 1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",store=" + getpara(ci, index, 1));
		return ret.toString();
	}

	@Override
	public String getinst_wide(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",store=" + getu16bitpara(ci, index));
		return ret.toString();
	}

	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		if(this.form == 55 || this.form == 57)
			defnode.setSize(2);
	}

}
