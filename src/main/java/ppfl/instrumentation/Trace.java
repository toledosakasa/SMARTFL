package ppfl.instrumentation;

import java.io.Serializable;

public class Trace implements Serializable {
    public int opcode, lineno, index, nextinst;
    public Integer load, store, popnum, pushnum;
    public String classname, methodname, signature;

    public enum LogType{
        Inst, MethodLog, OutPoint
    }

    public LogType type;

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
        this.type = LogType.Inst;
    }

    public Trace(String classname, String methodname){
        this.classname = classname;
        this.methodname = methodname;
        this.type = LogType.MethodLog;
    }

    public Trace(String classname, String methodname, String signature){
        this.classname = classname;
        this.methodname = methodname;
        this.signature = signature;
        this.type = LogType.OutPoint;
    }


    public String toString(){
        if(type == LogType.MethodLog){
            String ret = "###" + classname + "::" + methodname;
            return ret;
        }

        if(type == LogType.OutPoint){
            String ret = "OUT_" + getdomain();
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

        String calltype = this.getcalltype();
        String callclass = this.getcallclass();
        String callname = this.getcallname();
        if(calltype != null){
            ret += ",calltype=" + calltype;
            ret += ",callclass=" + callclass;
            ret += ",callname=" + callname;
        }

        String field = this.getfield();
        if(field != null){
            ret += ",field=" + field;
        }

        Integer branchbyte = this.getbranchbyte();
        if(branchbyte != null){
            ret += ",branchbyte=" + branchbyte.toString();
        }

        Integer _default = this.getdefault();
        String _switch = this.getswitch();
        if(_default != null){
            ret += ",default=" + _default.toString();
            ret += ",switch=" + _switch;
        }
        
        ret += ",lineinfo=" + classname + "#" + methodname + "#" + signature + "#" + lineno + "#" + index + ",nextinst=" + nextinst;
        return ret;
    }

    public String getdomain(){
        return this.classname + ":" + this.methodname + ":" + this.signature;
    }

    public String getcalltype(){
        return null;
    }

    public String getcallclass(){
        return null;
    }

    public String getcallname(){
        return null;
    }

    public String getfield(){
        return null;
    }

    public Integer getdefault(){
        return null;
    }

    public String getswitch(){
        return null;
    }

    public Integer getbranchbyte(){
        return null;
    }

}
