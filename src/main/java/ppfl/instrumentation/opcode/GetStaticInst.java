package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.instrumentation.CallBackIndex;

//178
public class GetStaticInst extends OpcodeInst {

	public GetStaticInst(int _form) {
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
		ret.append(getStaticFieldInfo(ci, index, constp));
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
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		String field = graph.parseinfo.getvalue("field");
		Node usenode = graph.getStaticHeapNode(field);
		if (usenode != null)
			usenodes.add(usenode);
		defnode = graph.addNewStackNode(stmt);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

		graph.unsolvedStatic = graph.parseinfo;
		graph.staticStmt = stmt;
		graph.staticuse = usenodes;
		graph.staticpred = prednodes;
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		int fieldid = getu16bitpara(ci, index);
		String desc = constp.getFieldrefType(fieldid);

		int callback = dispatchByDesc(desc, cbi);
		int instpos = ci.insertExGap(3);
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(callback, instpos + 1);
	}
}
