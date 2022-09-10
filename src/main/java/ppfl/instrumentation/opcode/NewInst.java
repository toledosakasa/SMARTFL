package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.instrumentation.CallBackIndex;

//187
public class NewInst extends OpcodeInst {

	public NewInst(int _form) {
		super(_form, 1, 0);
		this.doBuild = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void insertReturnSite(CodeIterator ci, int previndex, ConstPool constp, String instinfo, CallBackIndex cbi)
			throws BadBytecode {
		int instpos = ci.insertExGap(8);
		String msg = "\n###RET@" + instinfo.trim();
		int instindex = constp.addStringInfo(msg);

		ci.writeByte(19, instpos);// ldc_w
		ci.write16bit(instindex, instpos + 1);

		ci.writeByte(184, instpos + 3);// invokestatic
		ci.write16bit(cbi.logstringindex, instpos + 4);
	}

	@Override
	public void insertReturn(CodeIterator ci, ConstPool constp, int poolindex, CallBackIndex cbi)
		throws BadBytecode {
		int instpos = ci.insertExGap(8);
		int instindex = constp.addIntegerInfo(poolindex);
		ci.writeByte(19, instpos);// ldc_w
		ci.write16bit(instindex, instpos + 1);
		ci.writeByte(184, instpos + 3);// invokestatic
		ci.write16bit(cbi.rettraceindex, instpos + 4);
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// FIXME this could be buggy, as sometimes new inst will be an invoke-like inst.
		int instpos = ci.insertExGap(3);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(cbi.tsindex_object, instpos + 1);
	}

	//FIXME:  now it seems that only insertReturn will be called
	@Override
	public void insertAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// FIXME this could be buggy, as sometimes new inst will be an invoke-like inst.
		int instpos = ci.insertExGap(4);// the gap must be long enough for the following instrumentation
		ci.writeByte(89, instpos);// dup
		ci.writeByte(184, instpos + 1);// invokestatic
		ci.write16bit(cbi.traceindex_object, instpos + 2);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		if (defnode != null) {
			Integer addr = graph.parseinfo.getAddressFromStack();
			if (addr != null)
				defnode.setAddress(addr);
		}
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

		graph.unsolvedStatic = graph.parseinfo;
		graph.staticStmt = stmt;
		graph.staticuse = usenodes;
		graph.staticpred = prednodes;
	}
}
