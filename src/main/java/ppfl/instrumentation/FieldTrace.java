package ppfl.instrumentation;

public class FieldTrace extends Trace{
    public String FieldrefClassName, FieldrefName;

    public FieldTrace(Trace trace, String field) {
        super(trace.opcode, trace.lineno, trace.index, trace.nextinst, trace.load, trace.store, trace.popnum,
                trace.pushnum, trace.classname, trace.methodname, trace.signature);
        if(field.contains("#")){
            this.FieldrefClassName = field.split("#")[0];
            this.FieldrefName = field.split("#")[1];
        }
        else{
            this.FieldrefClassName =null;
            this.FieldrefName = field;
        }
    }

    // @Override
    // public String toString(){
    //     String ret = "opcode=" + opcode + "(" + Interpreter.map[opcode].opcode + ")\t";
    //     if(popnum != null)
    //         ret += ",popnum=" + popnum;
    //     if(pushnum != null)
    //         ret += ",pushnum=" + pushnum;
    //     String field = FieldrefName;
    //     if(FieldrefClassName != null)
    //         field = FieldrefClassName + "#" + field;
    //     ret += ",field=" + field;
    //     ret += ",lineinfo=" + classname + "#" + methodname + "#" + signature + "#" + lineno + "#" + index + ",nextinst=" + nextinst;
    //     return ret;
    // }

    @Override
    public String getfield(){
        String field = FieldrefName;
        if(FieldrefClassName != null)
            field = FieldrefClassName + "#" + field;
        return field;
    }
}
