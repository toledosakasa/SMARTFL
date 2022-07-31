package ppfl.instrumentation;

public class SwitchTrace extends Trace{
    public Integer _default;
    public String _switch;
    
    public SwitchTrace(Trace trace, Integer _default, String _switch) {
        super(trace.opcode, trace.lineno, trace.index, trace.nextinst, trace.load, trace.store, trace.popnum,
                trace.pushnum, trace.classname, trace.methodname, trace.signature);
        this._default = _default;
        this._switch = _switch;
    }

    // @Override
    // public String toString(){
    //     String ret = "opcode=" + opcode + "(" + Interpreter.map[opcode].opcode + ")\t";
    //     if(popnum != null)
    //         ret += ",popnum=" + popnum;
    //     if(pushnum != null)
    //         ret += ",pushnum=" + pushnum;
    //     ret += ",default=" + _default;
    //     ret += ",switch=" + _switch;
    //     ret += ",lineinfo=" + classname + "#" + methodname + "#" + signature + "#" + lineno + "#" + index + ",nextinst=" + nextinst;
    //     return ret;
    // }

    @Override
    public String getdefault(){
        return _default.toString();
    }

    @Override
    public String getswitch(){
        return _switch;
    }


}
