package ppfl.instrumentation;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.Mnemonic;

public class OpcodeInst {
	int form;
	String opcode;

	public enum paratype {
		VAR, CONST, PARAVAR, PARACONST, NONE;
	}

	// pushs
	int pushnum;
	paratype pushtype = paratype.NONE;
	String pushvalue =  null;
	// pops
	int popnum;
	// store(var)
	paratype storetype = paratype.NONE;
	String storevalue = null;
	// for weird instructions(e.g. iinc)
	paratype para[] = new paratype[2];

	OpcodeInst(int _form, int _pushnum, int _popnum) {
		form = _form;
		opcode = Mnemonic.OPCODE[_form];
		pushnum = _pushnum;
		popnum = _popnum;
	}

	public void setStore(paratype t, String _storevalue) {
		this.storetype = t;
		this.storevalue = _storevalue;
	}

	public void setPush(paratype t, String _pushvalue) {
		this.pushtype = t;
		this.pushvalue = _pushvalue;
	}

	public void setPara(int id, paratype t) {
		para[id] = t;
	}

	String getinst(CodeIterator ci, int index) {
		StringBuilder ret = new StringBuilder();
		ret.append("opcode=" + this.opcode);
		if (this.popnum != 0)
			ret.append(",popnum=" + this.popnum);
		if (this.storetype != paratype.NONE) {
			ret.append(",store=");
			if (this.storetype == paratype.VAR) {
				ret.append(storevalue);
			} else {
				ret.append(ci.byteAt(index + 1));
			}
		}
		if (this.pushnum != 0)
			ret.append(",pushnum=" + this.pushnum);
		if (this.pushtype != paratype.NONE) {
			ret.append(",pushtype=" + this.pushtype + ",pushvalue=");
			if (this.pushtype == paratype.CONST || this.pushtype == paratype.VAR) {
				ret.append(pushvalue);
			} else {
				ret.append(ci.byteAt(index + 1));
			}
		}
		if (this.pushnum == 0 && this.popnum == 0) {// iinc
			if (this.para[0] != null && this.para[0] != paratype.NONE) {
				ret.append("," + this.para[0] + "=" + ci.byteAt(index + 1));
			}
			if (this.para[1] != null && this.para[1] != paratype.NONE) {
				ret.append("," + this.para[1] + "=" + ci.byteAt(index + 2));
			}
		}

		return ret.toString();
	}
}
