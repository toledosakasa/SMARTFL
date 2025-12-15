package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.instrumentation.CallBackIndex;
import ppfl.ProbGraph;

//97,101,105,109,113,121,123,125
public class LarithInst extends OpcodeInst {

	boolean isLongInst = true;

	public LarithInst(int _form) {
		super(_form, 1, 2);
		if(this.form == 113)
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
		ci.write16bit(cbi.tsindex_long, instpos + 1);
	}

	@Override
	public void insertAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		// ci.writeByte(184, instpos);// invokestatic
		// ci.write16bit(cbi.traceindex_long, instpos + 1);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		defnode.setSize(2);
		if(this.form == 113){
			List<String> ops = new ArrayList<>();
			ops.add("%");
			graph.buildFactor(defnode, prednodes, usenodes, ops, stmt);
		}
	}

	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		defnode.setSize(2);
		if(this.form == 113){
			List<String> ops = new ArrayList<>();
			ops.add("%");
			graph.buildFactor(defnode, prednodes, usenodes, ops, stmt);
		}
	}
}