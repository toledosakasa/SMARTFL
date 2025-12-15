package ppfl.instrumentation;

import java.io.Serializable;

public class Trace implements Serializable {
    public int opcode, lineno, index, nextinst;
    public Integer load, store, popnum, pushnum;
    public String classname, methodname, signature;
    public boolean isStatic;

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

    public Trace(String classname, String methodname, String signature){
        this.classname = classname;
        this.methodname = methodname;
        this.signature = signature;
        this.isStatic = false;
    }

    public void setTypeMethodLog (){
        this.type = LogType.MethodLog;
    }

    public void setTypeOutPoint (){
        this.type = LogType.OutPoint;
    }

    public void setStatic (){
        this.isStatic = true;
    }

    public boolean isStatic(){
        return this.isStatic;
    }



    public boolean isInst(){
        return this.type == LogType.Inst;
    }

    public boolean isMethodLog(){
        return this.type == LogType.MethodLog;
    }

    public Trace(String []split) {
        this.type = LogType.Inst;
        for (String instinfo : split) {
            String[] splitinstinfo = instinfo.split("=");
            String infotype = splitinstinfo[0];
            String infovalue = splitinstinfo[1];
            if (infotype.equals("load")) {
                this.load = Integer.valueOf(infovalue);
            }
            if (infotype.equals("store")) {
                this.store = Integer.valueOf(infovalue);
            }
            if (infotype.equals("popnum")) {
                this.popnum = Integer.valueOf(infovalue);
            }
            if (infotype.equals("pushnum")) {
                this.pushnum = Integer.valueOf(infovalue);
            }
            if (infotype.equals("lineinfo")) {
                String[] lineinfos = infovalue.split("#");
                if (lineinfos.length < 3) {
                    //System.out.println(str_trace);
                }
                this.classname = lineinfos[0];
                this.methodname = lineinfos[1];
                this.signature = lineinfos[2];
                this.lineno = Integer.valueOf(lineinfos[3]);
                this.index = Integer.valueOf(lineinfos[4]);
            }
            if (infotype.equals("opcode")) {
                String[] opcodeinfos = infovalue.split("\\(|\\)");
                this.opcode = Integer.valueOf(opcodeinfos[0]);
            }
            if (infotype.equals("nextinst")) {
                this.nextinst = Integer.valueOf(infovalue);
            }
        }

    }

    public String toString(){
        if(type == LogType.MethodLog){
            String ret = "###" + classname + "::" + methodname;
            if(isStatic)
                ret += ", static";
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
        int size = this.getfieldsize();
        if(field != null){
            ret += ",field=" + field + ",size=" + size;
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

    public String getlineinfo(){
        return classname + ":" + methodname + ":" + signature + "#" + lineno + "#" + index;
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

    public int getfieldsize(){
        return 0;
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
