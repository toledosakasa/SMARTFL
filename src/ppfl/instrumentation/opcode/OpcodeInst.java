package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Mnemonic;
import ppfl.instrumentation.CallBackIndex;

public class OpcodeInst {
	private boolean isinvoke;
	int form;
	String opcode;

	public enum paratype {
		VAR, CONST, PARAVAR, PARACONST, STATIC, FIELD, NONE, POOL;
	}

	public enum datatype {
		BYTE, SHORT, CHAR, BOOLEAN, INT, LONG, FLOAT, DOUBLE, LDC, STRING, OBJECT, NONE
	}

	// pushs
	int pushnum;
	paratype pushtype = paratype.NONE;
	datatype pushdatatype = datatype.NONE;
	String pushvalue = null;
	// pops
	int popnum;
	// store(var)
	paratype storetype = paratype.NONE;
	String storevalue = null;
	// for weird instructions(e.g. iinc)
	paratype para[] = new paratype[2];

	public OpcodeInst(int _form, int _pushnum, int _popnum) {
		form = _form;
		opcode = Mnemonic.OPCODE[_form];
		pushnum = _pushnum;
		popnum = _popnum;
		this.isinvoke = false;
	}

	public void setStore(paratype t, String _storevalue) {
		this.storetype = t;
		this.storevalue = _storevalue;
	}

	public void setPush(paratype t, String _pushvalue) {
		this.pushtype = t;
		this.pushvalue = _pushvalue;
	}

	public void setPushDataType(datatype t) {
		this.pushdatatype = t;
	}

	public void setPara(int id, paratype t) {
		para[id] = t;
	}

	String getpara(CodeIterator ci, int cindex, int paraindex) {
		if (ci == null)
			return null;
		return String.valueOf(ci.byteAt(cindex + paraindex));
	}

	String getpool(CodeIterator ci, int cindex, int paraindex, ConstPool constp) {
		if (ci == null)
			return null;
		return constp.getLdcValue(ci.byteAt(cindex + paraindex)).toString();
	}

	int get2para(CodeIterator ci, int index) {
		if (ci == null)
			return 0;
		return ci.byteAt(index + 1) << (8) | ci.byteAt(index + 2);
	}

	String getparas(CodeIterator ci, int index) {
		if (ci == null)
			return null;
		String ret = "";
		if (this.para[0] != null)
			ret = ret + getpara(ci, index, 1);
		if (this.para[1] != null)
			ret = ret + getpara(ci, index, 2);
		return ret;
	}

	String getmethodinfo(CodeIterator ci, int callindex, ConstPool constp) {
		if (ci == null)
			return null;
		return ",calltype=" + constp.getMethodrefType(callindex) + ",callclass="
				+ constp.getMethodrefClassName(callindex) + ",callname=" + constp.getMethodrefName(callindex);
	}

	// temporary.
	// extended class should override this method.
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		if (this.isinvoke) {
			int callindex = get2para(ci, index);
			ret.append(getmethodinfo(ci, callindex, constp));
			return ret.toString();
		}

		if (this.popnum != 0)
			ret.append(",popnum=" + this.popnum);
		if (this.storetype != paratype.NONE) {
			ret.append(",storetype=" + this.storetype + ",store=");
			if (this.storetype == paratype.VAR) {
				ret.append(storevalue);
			} else {
				ret.append(getparas(ci, index));
			}
		}
		if (this.pushnum != 0)
			ret.append(",pushnum=" + this.pushnum);
		if (this.pushnum == 0 && this.popnum == 0) {// iinc
			if (this.para[0] != null && this.para[0] != paratype.NONE) {
				ret.append("," + this.para[0] + "=" + getpara(ci, index, 1));
			}
			if (this.para[1] != null && this.para[1] != paratype.NONE) {
				ret.append("," + this.para[1] + "=" + getpara(ci, index, 1));
			}
		}

		return ret.toString();
	}

	// there's no need to overload this.
	public void insertByteCodeBefore(CodeIterator ci, int index, ConstPool constp, String linenumberinfo, CallBackIndex cbi)
			throws BadBytecode {

		String inst = getinst(ci, index, constp);
		inst = linenumberinfo + inst;
		if (inst != null) {
			// insertmap.get(ln).append(inst);
			int instpos = ci.insertGap(8);
			int instindex = constp.addStringInfo(inst);
			System.out.println(constp.getStringInfo(instindex));
			ci.writeByte(19, instpos);// ldc_w
			ci.write16bit(instindex, instpos + 1);

			ci.writeByte(184, instpos + 3);// invokestatic
			ci.write16bit(cbi.logstringindex, instpos + 4);
		}
	}

	public void setinvoke() {
		this.isinvoke = true;
	}

	// temporary solution for integer insts.
	// extended class should override this method.
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		// print stack value pushed by this instruction.
		// this should be inserted after the instruction is executed
		// (after ci.next() is called)

		if (this.pushnum == 1 && (this.opcode.startsWith("i"))) {
			int instpos = ci.insertGap(8);
			// ci.writeByte(93, instpos + 1);// buggy dup. can't explain(?)
			// call (I)I callback instead of (I)V callback.
			ci.writeByte(184, instpos + 2);// invokestatic
			ci.write16bit(cbi.tsindex_int, instpos + 3);
		}

	}
}