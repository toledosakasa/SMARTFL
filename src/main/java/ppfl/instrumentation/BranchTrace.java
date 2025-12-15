package ppfl.instrumentation;

public class BranchTrace extends Trace {
    public Integer branchbyte;

    public BranchTrace(Trace trace, Integer branchbyte) {
        super(trace.opcode, trace.lineno, trace.index, trace.nextinst, trace.load, trace.store, trace.popnum,
                trace.pushnum, trace.classname, trace.methodname, trace.signature);
        this.branchbyte = branchbyte;
    }

    @Override
    public Integer getbranchbyte() {
        return branchbyte;
    }

}
