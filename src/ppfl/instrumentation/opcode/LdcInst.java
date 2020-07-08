package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.opcode.OpcodeInst.paratype;

//18,19,20
public class LdcInst extends OpcodeInst {

	boolean isIntInst = true;
	int loadedconst;

	public LdcInst(int _form) {
		super(_form, 1, 0);
	}

	public LdcInst(int _form, int _loadedconst) {
		super(_form, 1, 0);
		this.loadedconst = _loadedconst;
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		ret.append(",pushnum=" + this.pushnum);
		return ret.toString();
	}

	@Override
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		int callbackindex = -1;
		int instpara = -1;
		// ldc
		if (this.form == 18) {
			instpara = this.get1para(ci, index);
		} else {
			instpara = this.get2para(ci, index);
		}
		Object v = constp.getLdcValue(instpara);
		callbackindex = cbi.getLdcCallBack(v);

		int instpos = ci.insertGap(4);// the gap must be long enough for the following instrumentation
		ci.writeByte(184, instpos);// invokestatic
		ci.write16bit(callbackindex, instpos + 1);
	}

}
