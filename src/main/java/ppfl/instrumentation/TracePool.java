package ppfl.instrumentation;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;;


public class TracePool implements Serializable{
    private List<Trace> pool;
    private int index;
    private boolean init;
    TracePool(){
        pool = new ArrayList<>();
        index = 0;
        init = false;
    }

    public void add(Trace trace){
        pool.add(trace);
        index += 1;
    }

    public Trace get(int index){
        return pool.get(index);
    }

    
    public int indexAt(){
        return index;
    }


    public boolean hsinit(){
        return init;
    }

    public void init(){
        init = true;
    }

    public int size(){
        return pool.size();
    }

    // private static Map<String> classPool = new HashMap<>();
    // private static Map<String> methodPool = new HashMap<>();
    // private static Map<String> sigPool = new HashMap<>();


    // public static String findClassPool(String classname){
    //     classPool.
    // }

}
