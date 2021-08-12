package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.instrumentation.CallBackIndex;

//180
public class GetFieldInst extends OpcodeInst {

	public GetFieldInst(int _form) {
		super(_form, 1, 1);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(getFieldInfo(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		Node objectAddress = graph.getRuntimeStack().pop();
		usenodes.add(objectAddress);
		String field = graph.parseinfo.getvalue("field");
		Node usenode = graph.getHeapNode(objectAddress, field);
		if (usenode != null)
			usenodes.add(usenode);
		defnode = graph.addNewStackNode(stmt);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

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
