package ppfl.instrumentation;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.FileWriter;

public class TraceDecoder {

    public static void main(String args[]) {
        Interpreter.init();
        String finename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/3/trace/logs/run/org.apache.commons.math3.util.MathArraysTest.testLinearCombinationInfinite.log.ser";
        //String finename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/6/trace/logs/run/org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizerTest.testAckley.log.ser";
        //String finename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/43/trace/logs/run/org.apache.commons.lang.text.ExtendedMessageFormatTest.testEscapedQuote_LANG_477.log.ser";
        //String finename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/4/trace/logs/run/org.apache.commons.lang3.StringEscapeUtilsTest.testEscapeJava.log.ser";
        //String finename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/4/trace/logs/run/org.apache.commons.lang3.StringEscapeUtilsTest.testEscapeXmlSupplementaryCharacters.log.ser";
        //String finename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/1/trace/logs/run/org.apache.commons.lang3.math.NumberUtilsTest.TestLang747.log.ser";
        String poolname = finename.replaceAll("logs/run/(.*)", "classcache/TracePool.ser");
        TracePool tracepool = null;
        if(TraceTransformer.useIndexTrace){
            try{
                FileInputStream fileIn = new FileInputStream(poolname);
                ObjectInputStream in = new ObjectInputStream(fileIn);
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
        }
        TraceSequence traceseq = null;
        try{
            FileInputStream fileIn = new FileInputStream(finename);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            traceseq = (TraceSequence) in.readObject();
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
        try{
            FileWriter writer = new FileWriter("trace.log");
            int size = traceseq.size();
            for(int i=0;i<size;i++){
                DynamicTrace trace = traceseq.get(i);
                writer.write(trace.toString());
                // System.out.print(trace.toString());
            }
            writer.close();
        }
        catch(IOException i)
        {
            i.printStackTrace();
            return;
        }
	}

}
