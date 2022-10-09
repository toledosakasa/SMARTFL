package ppfl.instrumentation;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

public class TraceDecoder {

    public static void main(String args[]) {
        String filename;
        String logname = "trace.log";
        String poollogname = "pool.log";
        String SAlogname = "SA.log";
        Interpreter.init();
        filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/53/trace/logs/run/org.apache.commons.lang.time.DateUtilsTest.testRoundLang346.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/91/trace/logs/run/org.apache.commons.math.fraction.FractionTest.testCompareTo.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/15/trace/logs/run/org.apache.commons.math3.util.FastMathTest.testMath904.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/25/trace/logs/run/org.apache.commons.lang3.text.translate.EntityArraysTest.testISO8859_1_ESCAPE.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/13/trace/logs/run/org.apache.commons.math3.optimization.fitting.PolynomialFitterTest.testLargeSample.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/43/trace/logs/run/org.apache.commons.lang.text.ExtendedMessageFormatTest.testEscapedQuote_LANG_477.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/1/trace/logs/run/org.apache.commons.lang3.ValidateTest.testNotBlankMsgNotBlankStringWithNewlinesShouldNotThrow.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/4/trace/logs/run/org.apache.commons.lang3.StringEscapeUtilsTest.testUnescapeHexCharsHtml.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/25/trace/logs/run/org.apache.commons.math3.optimization.fitting.HarmonicFitterTest.testMath844.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/31/trace/logs/run/org.apache.commons.math3.distribution.BinomialDistributionTest.testMath718.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/89/trace/logs/run/org.apache.commons.math.stat.FrequencyTest.testAddNonComparable.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Math/23/trace/logs/run/org.apache.commons.math3.optimization.univariate.BrentOptimizerTest.testKeepInitIfBest.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/9/trace/logs/run/org.apache.commons.lang3.time.FastDateFormatTest.testParseSync.log.ser";
        //String filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/8/trace/logs/run/org.apache.commons.lang3.time.FastDateFormatTest.testParseSync.log.ser";
        //filename = "/data/mhzeng/ppfl/tmp_checkout/SmartFL/Lang/4/trace/logs/run/org.apache.commons.lang3.reflect.TypeUtilsTest.testIsAssignable.log.ser";
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
                SAlogname = filename.substring(0, filename.lastIndexOf("/") + 1) + "SA.log";
            }
        }

        
        String poolname = filename.replaceAll("logs/run/(.*)", "logs/mytrace/TracePool.ser");
        TracePool tracepool = null;
        try{
            long startTime = System.currentTimeMillis();
            FileInputStream fileIn = new FileInputStream(poolname);
            BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
            ObjectInputStream in = new ObjectInputStream(bufferedIn);
            tracepool = (TracePool) in.readObject();
            in.close();
            fileIn.close();
            long endTime = System.currentTimeMillis();
            double time = (endTime - startTime) / 1000.0;
            System.out.println("[Agent] Pool read done");
            System.out.println("[Agent] Pool read time " + time);
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

        // String SAname = filename.replaceAll("logs/run/(.*)", "logs/mytrace/StaticAnalyzer.ser");
        // StaticAnalyzer staticAnalyzer = null;

        // try{
        //     long startTime = System.currentTimeMillis();
        //     FileInputStream fileIn = new FileInputStream(SAname);
        //     ObjectInputStream in = new ObjectInputStream(fileIn);
        //     staticAnalyzer = (StaticAnalyzer) in.readObject();
        //     in.close();
        //     fileIn.close();
        //     long endTime = System.currentTimeMillis();
        //     double time = (endTime - startTime) / 1000.0;
        //     System.out.println("[Agent] SA read done");
        //     System.out.println("[Agent] SA read time " + time);
        // }
        // catch(IOException i)
        // {
        //     i.printStackTrace();
        //     return;
        // }catch(ClassNotFoundException c)
        // {
        //     System.out.println("staticAnalyzer class not found");
        //     c.printStackTrace();
        //     return;
        // }
        // try{
        //     staticAnalyzer.setTracePool(tracepool);
        //     FileWriter SAwriter = new FileWriter(SAlogname);
        //     // SAwriter.write("\n###predataflowmap\n");
        //     // for (Map.Entry<Integer, List<Integer>> instname : staticAnalyzer.predataflowmap.entrySet()){
        //     //     Trace inst = tracepool.get(instname.getKey());
        //     //     SAwriter.write(inst.toString() + " :\n");
        //     //     SAwriter.write("    ");
        //     //     List<Integer> preedges = instname.getValue();
        //     //     for (Integer prenode : preedges) {
        //     //         Trace succ = tracepool.get(prenode);
        //     //         SAwriter.write(succ.toString() + "@ ");
        //     //     }
        //     //     SAwriter.write("\n");
        //     // }
        //     // SAwriter.write("\n###postdataflowmap\n");
        //     // for (Map.Entry<Integer, List<Integer>> instname : staticAnalyzer.postdataflowmap.entrySet()){
        //     //     Trace inst = tracepool.get(instname.getKey());
        //     //     SAwriter.write(inst.toString() + " :\n");
        //     //     SAwriter.write("    ");
        //     //     List<Integer> preedges = instname.getValue();
        //     //     for (Integer prenode : preedges) {
        //     //         Trace succ = tracepool.get(prenode);
        //     //         SAwriter.write(succ.toString() + "! ");
        //     //     }
        //     //     SAwriter.write("\n");
        //     // }
        //     // SAwriter.write("\n###pre_idom\n");
        //     // for (Map.Entry<Integer, Integer> instname : staticAnalyzer.pre_idom.entrySet()){
        //     //     Trace inst = tracepool.get(instname.getKey());
        //     //     SAwriter.write(inst.toString() + " :\n");
        //     //     SAwriter.write("    ");
        //     //     Integer pre_idom = instname.getValue();
        //     //     Trace pred = tracepool.get(pre_idom);
        //     //     SAwriter.write(pred.toString());
        //     //     SAwriter.write("\n");
        //     // }
        //     // SAwriter.write("\n###post_idom\n");
        //     // for (Map.Entry<Integer, Integer> instname : staticAnalyzer.post_idom.entrySet()){
        //     //     Trace inst = tracepool.get(instname.getKey());
        //     //     SAwriter.write(inst.toString() + " :\n");
        //     //     SAwriter.write("    ");
        //     //     Integer post_idom = instname.getValue();
        //     //     Trace succ = tracepool.get(post_idom);
        //     //     SAwriter.write(succ.toString());
        //     //     SAwriter.write("\n");
        //     // }

        //     SAwriter.write("\n###loop\n");
        //     // for (StaticAnalyzer.BackEdge backedge : staticAnalyzer.loopset){
        //     //     Trace start = tracepool.get(backedge.start);
        //     //     Trace end = tracepool.get(backedge.end);
        //     //     SAwriter.write(start.toString() + " :\n");
        //     //     SAwriter.write("    ");
        //     //     SAwriter.write(end.toString());
        //     //     SAwriter.write("\n");
        //     // }

        //     SAwriter.close();
        // } catch(IOException i)
        // {
        //     i.printStackTrace();
        //     return;
        // }

        TraceSequence traceseq = null;
        try{
            long startTime = System.currentTimeMillis();
            FileInputStream fileIn = new FileInputStream(filename);
            BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
            ObjectInputStream in = new ObjectInputStream(bufferedIn);
            traceseq = (TraceSequence) in.readObject();
            if(TraceTransformer.useIndexTrace)
                traceseq.setTracePool(tracepool);
            in.close();
            fileIn.close();
            long endTime = System.currentTimeMillis();
            double time = (endTime - startTime) / 1000.0;
            System.out.println("[Agent] Trace read done");
            System.out.println("[Agent] Trace read time " + time);
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
                // tracewriter.write(String.format("%d\n", trace.traceindex));
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
