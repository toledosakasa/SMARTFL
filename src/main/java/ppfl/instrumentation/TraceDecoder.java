package ppfl.instrumentation;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.FileWriter;

public class TraceDecoder {

    public static void main(String args[]) {
        String filename;
        String logname = "trace.log";
        String poollogname = "pool.log";
        Interpreter.init();
        filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/31/trace/logs/run/org.apache.commons.math3.distribution.BinomialDistributionTest.testMath718.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/89/trace/logs/run/org.apache.commons.math.stat.FrequencyTest.testAddNonComparable.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/23/trace/logs/run/org.apache.commons.math3.optimization.univariate.BrentOptimizerTest.testKeepInitIfBest.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/9/trace/logs/run/org.apache.commons.lang3.time.FastDateFormatTest.testParseSync.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/8/trace/logs/run/org.apache.commons.lang3.time.FastDateFormatTest.testParseSync.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/4/trace/logs/run/org.apache.commons.lang3.reflect.TypeUtilsTest.testIsAssignable.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/3/trace/logs/run/org.apache.commons.math3.util.MathArraysTest.testLinearCombinationInfinite.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/6/trace/logs/run/org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizerTest.testAckley.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/43/trace/logs/run/org.apache.commons.lang.text.ExtendedMessageFormatTest.testEscapedQuote_LANG_477.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/4/trace/logs/run/org.apache.commons.lang3.StringEscapeUtilsTest.testEscapeJava.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/4/trace/logs/run/org.apache.commons.lang3.StringEscapeUtilsTest.testEscapeXmlSupplementaryCharacters.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/9/trace/logs/run/org.apache.commons.lang3.time.DateFormatUtilsTest.testDateISO.log.ser";

        if (args.length >= 1)
            filename = args[0];
        if (args.length == 2){
            if (args[1].equals("incheckout")){
                logname = filename.substring(0, filename.length() - ".ser".length());
                poollogname = filename.substring(0, filename.lastIndexOf("/") + 1) + "pool.log";
            }
        }

        
        String poolname = filename.replaceAll("logs/run/(.*)", "classcache/TracePool.ser");
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
            try{
                FileWriter poolwriter = new FileWriter(poollogname);
                int poolsize=tracepool.size();
                for(int i=0;i<poolsize;i++){
                    poolwriter.write(tracepool.get(i).toString()+"\n");
                }
                poolwriter.close();
            } catch(IOException i)
            {
                i.printStackTrace();
                return;
            }
        }

        TraceSequence traceseq = null;
        try{
            FileInputStream fileIn = new FileInputStream(filename);
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
            FileWriter tracewriter = new FileWriter(logname);
            int size = traceseq.size();
            for(int i=0;i<size;i++){
                DynamicTrace trace = traceseq.get(i);
                tracewriter.write(trace.toString());
                // System.out.print(trace.toString());
            }
            tracewriter.close();
        }
        catch(IOException i)
        {
            i.printStackTrace();
            return;
        }
	}

}
