package ppfl.instrumentation;

import java.io.Serializable;

public class DynamicTrace implements Serializable {
    public Trace trace;
    public String stackType;
    public long stackValue;
    public double fstackValue;
    public boolean isret;

    public DynamicTrace(Trace trace){
        this.trace = trace;
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

    public String toString(){
        String ret = "\n";
        if(this.isret)
            ret += "###RET@";
        ret += trace.toString();
        if(stackType != null){
            ret +=  ",stack=" + stackType + ":" + String.valueOf(stackValue);
        }
        return ret;
    }

}
