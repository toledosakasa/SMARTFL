package ppfl.instrumentation;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;;

// TODO: change the static in TracePool, for implementating Serializable
// TODO: Now we have multiple Transformer so we need to use static now
public class TracePool{
    static private List<Trace> pool = new ArrayList<>();
    static private int index = 0;
    static private boolean init = false;

    static public void add(Trace trace){
        pool.add(trace);
        index += 1;
    }

    static public Trace get(int index){
        return pool.get(index);
    }

    
    static public int indexAt(){
        return index;
    }

    static public List<Trace> getpool(){
        return pool;
    }

    static public void setpool(List<Trace> pool){
        TracePool.pool = pool;
        TracePool.index = pool.size();
    }

    static public boolean hsinit(){
        return init;
    }

    static public void init(){
        init = true;
    }

    // private static Map<String> classPool = new HashMap<>();
    // private static Map<String> methodPool = new HashMap<>();
    // private static Map<String> sigPool = new HashMap<>();


    // public static String findClassPool(String classname){
    //     classPool.
    // }

}
