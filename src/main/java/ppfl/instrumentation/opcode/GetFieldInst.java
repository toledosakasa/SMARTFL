package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.ProbGraph;
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
		String field = graph.dynamictrace.trace.getfield();
		Node usenode = graph.getHeapNode(objectAddress, field);
		if (usenode != null)
			usenodes.add(usenode);
		defnode = graph.addNewStackNode(stmt);
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);

	}

	@Override
	public void build(ProbGraph graph) {
		super.build(graph);
		Node objectAddress = graph.popStackNode();
		usenodes.add(objectAddress);
		String field = dtrace.trace.getfield();
		Node use = graph.getHeap(objectAddress, field);
		// in some pass tests, there can be uninstrumented methods and no first def of the use
		if(use != null)
			usenodes.add(use);
		else{
			Node obj = graph.getObj(objectAddress);
			if(obj != null)
				usenodes.add(obj);
		}
		int size = dtrace.trace.getfieldsize();
		defnode = graph.addStackNode(stmt, size);
		Integer addr = dtrace.getAddressFromStack();
		if(addr != null)
			defnode.setAddress(addr);
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

	@Override
	public void insertAfter(CodeIterator ci, int index,  ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		int fieldid = getu16bitpara(ci, index);
		String desc = constp.getFieldrefType(fieldid);
		int callback = dispatchByDesc(desc, cbi);
		// FIXME: now disable other type 
		if(callback == cbi.traceindex_object){
			int instpos = ci.insertExGap(4);// the gap must be long enough for the following instrumentation
			ci.writeByte(89, instpos);// dup
			ci.writeByte(184, instpos + 1);// invokestatic
			ci.write16bit(callback, instpos + 2);
		}
	}

}
