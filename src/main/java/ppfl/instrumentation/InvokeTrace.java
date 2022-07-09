package ppfl.instrumentation;

public class InvokeTrace extends Trace {
    public String calltype, callclass, callname;

    public InvokeTrace(Trace trace, String calltype, String callclass, String callname) {
        super(trace.opcode, trace.lineno, trace.index, trace.nextinst, trace.load, trace.store, trace.popnum,
                trace.pushnum, trace.classname, trace.methodname, trace.signature);
        this.calltype = calltype;
        this.callclass = callclass;
        this.callname = callname;
    }

    @Override
    public String toString(){
        String ret = "opcode=" + opcode + "(" + Interpreter.map[opcode].opcode + ")\t";
        ret += ",calltype=" + calltype;
        ret += ",callclass=" + callclass;
        ret += ",callname=" + callname;
        ret += ",lineinfo=" + classname + "#" + methodname + "#" + signature + "#" + lineno + "#" + index + ",nextinst=" + nextinst;
        return ret;
    }

}
