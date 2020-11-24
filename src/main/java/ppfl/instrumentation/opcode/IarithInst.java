package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.instrumentation.CallBackIndex;

//96,100,104,108,112,120,122,124
public class IarithInst extends OpcodeInst {

	boolean isIntInst = true;

	public IarithInst(int _form) {
		super(_form, 1, 2);
		if(this.form == 112)
			this.doBuild = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_int, instpos + 1);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		if(this.form == 112){
			List<String> ops = new ArrayList<>();
			ops.add("%");
			graph.buildFactor(defnode, prednodes, usenodes, ops, stmt);
		}
	}


}