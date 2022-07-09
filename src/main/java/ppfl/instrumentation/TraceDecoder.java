package ppfl.instrumentation;

import java.io.*
;
public class TraceDecoder {

    public static void main(String args[]) {
        Interpreter.init();
        String finename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/1/trace/logs/run/org.apache.commons.lang3.math.IEEE754rUtilsTest.testEnforceExceptions.log.ser";
        TraceSequence traceseq = null;
        try{
            FileInputStream fileIn = new FileInputStream(finename);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            traceseq = (TraceSequence) in.readObject();
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
        int size = traceseq.size();
        for(int i=0;i<size;i++){
            DynamicTrace trace = traceseq.get(i);
            System.out.print(trace.toString());
        }

	}

}
