package ppfl.instrumentation;

import java.io.Serializable;

public class DynamicTrace implements Serializable {
    public int traceindex;
    public Trace trace;
    public String stackType;
    public long stackValue;
    public double fstackValue;
    public boolean isret;
    private TraceDomain domain = null;

    public DynamicTrace(Trace trace){
        this.trace = trace;
        this.stackType = null;
        this.isret = false;
    }

    public DynamicTrace(int traceindex){
        this.traceindex = traceindex;
        this.trace = null;
        this.stackType = null;
        this.isret = false;
    }

    public void setRetInfo(){
        this.isret = true;
    }

    public void addDynamicInfo(String stackType, long stackValue) {
        this.stackType = stackType;
        this.stackValue = stackValue;
    }

    public void addDynamicInfo(String stackType, double stackValue) {
        this.stackType = stackType;
        this.fstackValue = stackValue;
    }

    public boolean isCatch() {
        return trace.opcode == 58 || (trace.opcode >= 75 && trace.opcode <= 78);
    }

    // TODO: use index, not string as id
    public String getLineinfo() {
        return trace.classname + "#" + trace.methodname + "#" + trace.signature + "#"
                + trace.lineno + "#" + trace.index;
    }

    public int getLinenumber() {
        return trace.lineno;
    }

    public int getByteindex() {
        return trace.index;
    }

    public TraceDomain getDomain() {
        if (domain == null)
            domain = new TraceDomain(trace.classname, trace.methodname, trace.signature);
        return domain;
    }

    public TraceDomain getCallDomain() {
        String signature = trace.getcalltype();
        String callclass = trace.getcallclass();
        String callmethod = trace.getcallname();
        return new TraceDomain(callclass, callmethod, signature);
    }

    public boolean matchDomain(DynamicTrace oth) {
        return oth != null && oth.trace.classname.equals(this.trace.classname)
                && oth.trace.methodname.equals(this.trace.methodname)
                && oth.trace.signature.equals(this.trace.signature);
    }

    public boolean matchReturn(DynamicTrace returnMsg, boolean matchCatch) {
        if (!this.matchDomain(returnMsg) || (!returnMsg.isret && !matchCatch)) {
            return false;
        }
        String calltype = this.trace.getcalltype();
        String callclass = this.trace.getcallclass();
        String callname = this.trace.getcallname();

        if (callclass != null && calltype != null && callname != null) {
            if (matchCatch && returnMsg.isCatch()) {
                return true;
            }
            String msgcalltype = returnMsg.trace.getcalltype();
            String msgcallclass = returnMsg.trace.getcallclass();
            String msgcallname = returnMsg.trace.getcallname();

            return (calltype.equals(msgcalltype) && callclass.equals(msgcallclass) && callname.equals(msgcallname));
        }
        return false;
    }

    public boolean matchStaticReturn(DynamicTrace returnMsg) {
        if (returnMsg.isret && (this.trace.opcode == 178 || this.trace.opcode == 187)) {
            return this.matchDomain(returnMsg) && this.trace.lineno == returnMsg.getLinenumber()
                    && this.trace.index == returnMsg.getByteindex();
        }
        return false;
    }

    public Integer getAddressFromStack() {
        if (stackType != null) {
            if (stackType.contentEquals("Obj") || stackType.equals("Str")) {
                int objValue = (int) stackValue;
                return new Integer(objValue);
            }
        }
        return null;
    }

    public String[] getStackValue() {
        if (stackType != null) {
            String stackVal = stackType + ":";
            if (stackType.equals("D") || stackType.equals("F"))
                stackVal += String.valueOf(fstackValue);
            else
                stackVal += String.valueOf(stackValue);

            String[] stackValue = stackVal.split(":");
            return stackValue;
        }
        return null;
    }

    public String toString(){
        String ret = "\n";
        if(this.isret)
            ret += "###RET@";
        ret += trace.toString();
        if(stackType != null){
            ret +=  ",stack=" + stackType + ":";
            if(stackType.equals("D") || stackType.equals("F"))
                ret += String.valueOf(fstackValue);
            else
                ret += String.valueOf(stackValue);
        }
        return ret;
    }

    public void debugprint() {
        if (this.isret)
            System.err.println("@return:");
        System.err.println("\t" + this.toString());
    }

}
