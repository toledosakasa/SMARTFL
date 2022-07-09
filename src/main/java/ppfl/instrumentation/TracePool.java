package ppfl.instrumentation;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;;

public class TracePool{
    static private List<Trace> pool = new ArrayList<>();
    static private int index = 0;

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

    // private static Map<String> classPool = new HashMap<>();
    // private static Map<String> methodPool = new HashMap<>();
    // private static Map<String> sigPool = new HashMap<>();


    // public static String findClassPool(String classname){
    //     classPool.
    // }

}
