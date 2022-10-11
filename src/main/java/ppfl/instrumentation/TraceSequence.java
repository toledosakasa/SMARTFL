package ppfl.instrumentation;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class TraceSequence implements Serializable {
    private String name;
    private List<DynamicTrace> tracelist;
    private TracePool tracepool;

    TraceSequence(String name){
        this.name = name;
        tracelist = new ArrayList<>();
        this.tracepool = null;
    }

    public void add(DynamicTrace trace){
        tracelist.add(trace);
    }

    public String getName(){
        return name;
    }

    public void setTracePool(TracePool tracepool){
        this.tracepool = tracepool;
    }

    public DynamicTrace getRaw(int index){
        DynamicTrace ret = tracelist.get(index);
        return ret;
    }

    public DynamicTrace get(int index){
        DynamicTrace ret = tracelist.get(index);
        if(!TraceTransformer.useIndexTrace)
            return ret;
        try {
            ret.trace = tracepool.get(ret.traceindex);
        } catch (Exception e) {
            if(ret == null)
                System.out.println("null");
            System.out.println("index = " + index);
        }
        return ret;
    }

    public DynamicTrace top(){
        return tracelist.get(tracelist.size() - 1);
    }

    public int size(){
        return tracelist.size();
    }

    public void remove(int index){
        tracelist.remove(index);
    }

}
