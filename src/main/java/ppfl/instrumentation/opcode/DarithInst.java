package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.instrumentation.CallBackIndex;

//99,103,107,111,115
public class DarithInst extends OpcodeInst {

	boolean isDoubleInst = true;

	public DarithInst(int form) {
		super(form, 1, 2);
		if(this.form == 115)
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
		ci.write16bit(cbi.tsindex_double, instpos + 1);
	}

	@Override
	public void insertAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		// ci.writeByte(184, instpos);// invokestatic
		// ci.write16bit(cbi.traceindex_double, instpos + 1);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		defnode.setSize(2);
		if(this.form == 115){
			List<String> ops = new ArrayList<>();
			ops.add("%");
			graph.buildFactor(defnode, prednodes, usenodes, ops, stmt);
		}
	}
}