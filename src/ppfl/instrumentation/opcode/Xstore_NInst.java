package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.opcode.OpcodeInst.paratype;

//54-78
public class Xstore_NInst extends OpcodeInst {

	int storeindex;
	
    public Xstore_NInst(int _form,int _storeindex) {
        super(_form, 0, 1);
        this.storeindex = _storeindex;
    }

    @Override
    public String getinst(CodeIterator ci, int index, ConstPool constp) {
        StringBuilder ret = new StringBuilder();
        ret.append("opcode=" + this.opcode);
        ret.append(",popnum=" + this.popnum);
        ret.append(",store=" + storeindex);
        return ret.toString();
    }

}
