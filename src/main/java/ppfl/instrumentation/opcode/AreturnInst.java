package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.ProbGraph;
import ppfl.Node;
import ppfl.instrumentation.CallBackIndex;

//176
public class AreturnInst extends OpcodeInst {

	public AreturnInst(int form) {
		super(form, 0, 1);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	@Override
	public void insertByteCodeBefore(CodeIterator ci, int index, ConstPool constp, String inst, CallBackIndex cbi)
			throws BadBytecode {
		if (inst != null && !inst.equals("")) {
			// insertmap.get(ln).append(inst);
			int instpos = ci.insertGap(9);
			// inst = encode(inst);
			int instindex = constp.addStringInfo(inst);
			// System.out.println(constp.getStringInfo(instindex));

			ci.writeByte(19, instpos);// ldc_w
			ci.write16bit(instindex, instpos + 1);

			ci.writeByte(184, instpos + 3);// invokestatic
			ci.write16bit(cbi.logstringindex, instpos + 4);

			ci.writeByte(184, instpos + 6);// invokestatic
			ci.write16bit(cbi.tsindex_object, instpos + 7);
		}
	}

	@Override
	public void insertBefore(CodeIterator ci, ConstPool constp, int poolindex, CallBackIndex cbi, boolean isEx)
			throws BadBytecode {
		int instpos = ci.insertGap(10);
		int instindex = constp.addIntegerInfo(poolindex);
		ci.writeByte(19, instpos);// ldc_w
		ci.write16bit(instindex, instpos + 1);
		ci.writeByte(184, instpos + 3);// invokestatic
		ci.write16bit(cbi.logtraceindex, instpos + 4);
		ci.writeByte(89, instpos + 6);// dup
		ci.writeByte(184, instpos + 7);// invokestatic
		ci.write16bit(cbi.traceindex_object, instpos + 8);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		// uses
		usenodes.add(graph.getRuntimeStack().pop());
		// switch stack frame
		graph.popStackFrame();
		// def in caller frame
		Node defnode = graph.addNewStackNode(stmt);
		if (defnode != null) {
			Integer addr = graph.dynamictrace.getAddressFromStack();
			if (addr != null)
				defnode.setAddress(addr);
		}
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		graph.killPredStack("OUT_" + stmt.getClassMethod());
	}

	@Override
	public void build(ProbGraph graph){		
		super.build(graph);
		// uses
		usenodes.add(graph.popStackNode());
		// switch stack frame
		graph.popFrame();
		// may be some invalid frame from junit
		if(graph.topFrame() == null)
			return;
		// def in caller frame
		Node defnode = graph.addStackNode(stmt,1);
		Integer addr = dtrace.getAddressFromStack();
		assert(addr != null);
		defnode.setAddress(addr);
 
		graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
	}

}
