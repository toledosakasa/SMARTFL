package ppfl.instrumentation;

import java.io.Serializable;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.ArrayList;

public class TraceSequence implements Serializable {
    private String name;
    private List<DynamicTrace> tracelist;
    private TracePool tracepool;
    public boolean pass;

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

    public void setName(String name){
        this.name = name;
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
            if(ret.isStackTrace())
                return ret;
            ret.trace = tracepool.get(ret.traceindex);
        } catch (Exception e) {
            if(ret == null)
                System.out.println("null");
            System.out.println("name = "+ name+"\nindex = " + index);
            // throw(e);
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

    public void prune(Set<String> observeSet){
        ListIterator<DynamicTrace> iter = tracelist.listIterator(tracelist.size());
        while(iter.hasPrevious()){
            DynamicTrace dtrace = iter.previous();
            // if(observeSet.size()>1)
            //     System.out.println(dtrace.toString());
            if(dtrace == null || dtrace.isStackTrace()){
                iter.remove();
                continue;
            }
            Trace trace = tracepool.get(dtrace.traceindex);
            String location = trace.classname + ":" + trace.lineno;
            if(!observeSet.contains(location))
                iter.remove();
            else
                break;
        }
    }
}
