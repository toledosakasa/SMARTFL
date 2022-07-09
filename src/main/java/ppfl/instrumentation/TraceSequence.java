package ppfl.instrumentation;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class TraceSequence implements Serializable {
    private String name;
    private List<DynamicTrace> tracelist;

    TraceSequence(String name){
        this.name = name;
        tracelist = new ArrayList<>();
    }

    public void add(DynamicTrace trace){
        tracelist.add(trace);
    }

    public String getName(){
        return name;
    }

    public DynamicTrace get(int index){
        return tracelist.get(index);
    }

    public DynamicTrace top(){
        return tracelist.get(tracelist.size() - 1);
    }

    public int size(){
        return tracelist.size();
    }

}
