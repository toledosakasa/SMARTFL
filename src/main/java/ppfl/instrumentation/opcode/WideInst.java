package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.Interpreter;

//196
public class WideInst extends OpcodeInst {

	public WideInst(int _form) {
		super(_form, 0, 0);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		int realform = Integer.parseInt(getpara(ci, index, 1));
		return Interpreter.map[realform].getinst_wide(ci, index + 1, constp);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// should never be called
		assert (false);
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		int realform = Integer.parseInt(getpara(ci, index, 1));
		Interpreter.map[realform].insertByteCodeAfter(ci, index, constp, cbi);
	}

}
