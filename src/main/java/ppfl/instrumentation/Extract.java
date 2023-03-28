package ppfl.instrumentation;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.Comparator;

public class Extract {

    private static final int foldSize = CallBackIndex.loglimit*9/10; // should fold 0.9 *1200000 now

    public static void main(String args[]) {
        String filename;
        Interpreter.init();
        if (args.length < 1){
            System.out.println("Needs parameter");
            System.exit(-1);
        }
        filename = args[0];        
        String poolname = filename.replaceAll("logs/run/(.*)", "logs/mytrace/TracePool.ser");
        
        TracePool tracepool = null;
        try{
            FileInputStream fileIn = new FileInputStream(poolname);
            BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
            ObjectInputStream in = new ObjectInputStream(bufferedIn);
            tracepool = (TracePool) in.readObject();
            in.close();
            fileIn.close();
        }
        catch(IOException i)
        {
            i.printStackTrace();
            return;
        }catch(ClassNotFoundException c)
        {
            System.out.println("TracePool class not found");
            c.printStackTrace();
            return;
        }

        TraceSequence traceseq = null;
        try{
            FileInputStream fileIn = new FileInputStream(filename);
            BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
            ObjectInputStream in = new ObjectInputStream(bufferedIn);
            traceseq = (TraceSequence) in.readObject();
            if(TraceTransformer.useIndexTrace)
                traceseq.setTracePool(tracepool);
            in.close();
            fileIn.close();
        }
        catch(IOException i)
        {
            i.printStackTrace();
            return;
        }catch(ClassNotFoundException c)
        {
            System.out.println("TraceSequence class not found");
            c.printStackTrace();
            return;
        }

        Map<TraceDomain,Integer> mymap = new HashMap<>();
        int size = traceseq.size();
        int count = 0;
        for(int i=0;i<size;i++){
            DynamicTrace trace = traceseq.get(i);
            if(trace.isStackTrace())
                continue;
            // if(trace.isret || trace.trace.type != Trace.LogType.Inst)
            //     continue;
            TraceDomain domain = trace.getDomain();
            count++;
            if(mymap.containsKey(domain))
                mymap.put(domain, mymap.get(domain)+1);
            else
                mymap.put(domain, 1);
        }
        Stream<Map.Entry<TraceDomain,Integer>> sorted = mymap.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        List<Map.Entry<TraceDomain,Integer>> mylist = sorted.collect(Collectors.toList());

        int sumlen = 0;
        try{
            String logname = filename.replaceAll("logs/run/(.*)", "logs/mytrace/fold.log");
            FileWriter logwriter = new FileWriter(logname, true);
            for(Map.Entry<TraceDomain,Integer> item: mylist){
                sumlen += item.getValue();
                logwriter.write( item.getKey().toString() + "\n");
                if(sumlen > foldSize)
                    break;
            }
            logwriter.write("[count] " + "count = " + count  + ", sum = " + sumlen + "\n");
            for(Map.Entry<TraceDomain,Integer> item: mylist){
                logwriter.write("[count] " + item.getKey().toString() + "    " + item.getValue() + "\n");
            }
            logwriter.close();
        } catch(IOException i)
        {
            i.printStackTrace();
            return;
        }

        // try{
        //     String logname = filename.replaceAll("\\.ser", "");
        //     FileWriter logwriter = new FileWriter(logname, true);
        //     for(Map.Entry<TraceDomain,Integer> item: mylist){
        //         logwriter.write(item.getKey().toString() + "    " + item.getValue() + "\n");
        //     }
        //     logwriter.close();
        // } catch(IOException i)
        // {
        //     i.printStackTrace();
        //     return;
        // }
	}
}