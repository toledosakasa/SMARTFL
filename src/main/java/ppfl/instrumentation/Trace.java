package ppfl.instrumentation;

import java.io.Serializable;

public class Trace implements Serializable {
    public int opcode, lineno, index, nextinst;
    public Integer load, store, popnum, pushnum;
    public String classname, methodname, signature;
    public boolean ismethodlog;

    public Trace(int opcode, int lineno, int index, int nextinst, Integer load, Integer store,
            Integer popnum, Integer pushnum, String classname, String methodname, String signature) {
        this.opcode = opcode;
        this.lineno = lineno;
        this.index = index;
        this.nextinst = nextinst;
        this.load = load;
        this.store = store;
        this.popnum = popnum;
        this.pushnum = pushnum;
        this.classname = classname;
        this.methodname = methodname;
        this.signature = signature;
        this.ismethodlog = false;
    }

    public Trace(String classname, String methodname){
        this.ismethodlog = true;
        this.classname = classname;
        this.methodname = methodname;
    }

    public String toString(){
        if(ismethodlog){
            String ret = "###" + classname + "::" + methodname;
            return ret;
        }
        String ret = "opcode=" + opcode + "(" + Interpreter.map[opcode].opcode + ")\t";
        if(popnum != null)
            ret += ",popnum=" + popnum;
        if(pushnum != null)
            ret += ",pushnum=" + pushnum;
        if(load != null)
            ret += ",load=" + load;
        if(store != null)
            ret += ",store=" + store;
        ret += ",lineinfo=" + classname + "#" + methodname + "#" + signature + "#" + lineno + "#" + index + ",nextinst=" + nextinst;
        return ret;
    }

}
