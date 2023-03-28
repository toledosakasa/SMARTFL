package ppfl.defects4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import ppfl.ByteCodeGraph;
import ppfl.ProbGraph;
import ppfl.WriterUtils;

public class GraphBuilder {

  public static String getCheckoutBase() {
    String checkoutbase = null;
    try (BufferedReader reader = new BufferedReader(new FileReader("checkout.config"))) {
      checkoutbase = reader.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return checkoutbase.trim();
  }

  private static void setupD4jProject(ByteCodeGraph pgraph, String project, int id, boolean usesimple) {
    String resultfile = String.format("InfResult-%s%d", project, id);
    pgraph.setResultLogger(resultfile);
    String graphfile = String.format("ProbGraph-%s%d", project, id);
    pgraph.setGraphLogger(graphfile);
    String reducefile = String.format("ReduceStmt-%s%d", project, id);
    pgraph.setReduceLogger(reducefile);

    pgraph.useD4jTest = true;
    String triggerTests = null;
    String relevantClasses = null;
    String allTestMethods = null;
    String allTestClasses = null;
    String configpath = String.format("d4j_resources/metadata_cached/%s/%d.log", project, id);
    try (BufferedReader reader = new BufferedReader(new FileReader(configpath))) {
      String tmp;
      while ((tmp = reader.readLine()) != null) {
        String[] splt = tmp.split("=");
        // if (splt[0].equals("classes.relevant")) {
        // relevantClasses = splt[1];
        // }
        if (splt[0].equals("tests.all")) {
          allTestClasses = splt[1];
        }
        if (splt[0].equals("tests.trigger")) {
          triggerTests = splt[1];
        }
        if (splt[0].equals("methods.test.all")) {
          allTestMethods = splt[1];
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    String instpath = String.format("d4j_resources/metadata_cached/%s/%d.inst.log", project, id);
    try (BufferedReader reader = new BufferedReader(new FileReader(instpath))) {
      relevantClasses = reader.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }

    String checkoutbase = getCheckoutBase();
    String whatIsTracedLog = String.format("%s/%s/%s/trace/logs/mytrace/traced.source.log", checkoutbase, project, id);
    pgraph.parseWhatIsTracedLog(whatIsTracedLog);

    if (allTestClasses != null) {
      for (String s : allTestClasses.split(";")) {
        if (!s.isEmpty())
          pgraph.d4jTestClasses.add(s);
      }
    }

    if (triggerTests != null) {
      for (String s : triggerTests.split(";")) {
        if (!s.isEmpty())
          pgraph.d4jTriggerTestNames.add(s);
      }
    }
    if (allTestMethods != null) {
      for (String s : allTestMethods.split(";")) {
        if (!s.isEmpty()) {
          String[] splt = s.split("::");
          if (splt.length < 2)
            continue;
          String[] methodsname = splt[1].split(",");
          for (String methodname : methodsname) {
            pgraph.d4jMethodNames.add(splt[0] + "::" + methodname);
          }
        }
      }
    }

    // if (relevantClasses != null) {
    // for (String s : relevantClasses.split(";")) {
    // if (!s.isEmpty()) {
    // // pgraph.addTracedDomain(s);
    // pgraph.parseD4jSource(project, id, s);
    // }
    // }
    // }

    String poolpath = String.format("%s/%s/%s/trace/logs/mytrace/TracePool.ser", checkoutbase, project, id);
    pgraph.setPool(poolpath);

    String sourcebase = String.format("%s/%s/%s/trace/logs/mytrace/", checkoutbase, project, id);
    File tracefolder = new File(sourcebase);
    File[] fs = tracefolder.listFiles();
    for (File f : fs) {
      if (!f.getName().endsWith(".source.log") || f.getName().endsWith("traced.source.log"))
        continue;
      pgraph.parsesource(f.getAbsolutePath());
    }

    System.out.println("Parse source complete");
    // long startTime = System.currentTimeMillis();
    pgraph.get_pre_idom();
    pgraph.find_loop();
    pgraph.get_idom();
    pgraph.get_stores();
    System.out.println("Static analyze complete");
    // long endTime = System.currentTimeMillis();
    // long thetime = endTime-startTime;
    // System.out.println("idom time is "+ thetime);
    String folder = String.format("%s/%s/%s/trace/logs/run/", checkoutbase, project, id);
    String sourcefolder = String.format("%s/%s/%s/trace/logs/mytrace/", checkoutbase, project, id);
    pgraph.parseFolder(folder, sourcefolder, usesimple);
    System.out.println("Parse complete");
    // this.parseD4jTrace(tracefilename);
  }

  private static void buildD4jProject(ProbGraph pgraph, String project, int id){
    pgraph.setResultLogger(project, id);
    pgraph.setGraphLogger(project, id);
    pgraph.setDebugLogger(project, id);

    // pgraph.useD4jTest = true;
    String triggerTests = null;
    String relevantClasses = null;
    String allTestMethods = null;
    String allTestClasses = null;

    String configpath = String.format("d4j_resources/metadata_cached/%s/%d.log", project, id);
    try (BufferedReader reader = new BufferedReader(new FileReader(configpath))) {
      String tmp;
      while ((tmp = reader.readLine()) != null) {
        String[] splt = tmp.split("=");
        if (splt[0].equals("classes.relevant")) {
        relevantClasses = splt[1];
        }
        if (splt[0].equals("tests.all")) {
          allTestClasses = splt[1];
        }
        if (splt[0].equals("tests.trigger")) {
          triggerTests = splt[1];
        }
        if (splt[0].equals("methods.test.all")) {
          allTestMethods = splt[1];
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (allTestClasses != null) {
      for (String s : allTestClasses.split(";")) {
        if (!s.isEmpty())
          pgraph.d4jTestClasses.add(s);
      }
    }

    if (triggerTests != null) {
      for (String s : triggerTests.split(";")) {
        if (!s.isEmpty())
          pgraph.d4jTriggerTestNames.add(s);
      }
    }
    if (allTestMethods != null) {
      for (String s : allTestMethods.split(";")) {
        if (!s.isEmpty()) {
          String[] splt = s.split("::");
          if (splt.length < 2)
            continue;
          String[] methodsname = splt[1].split(",");
          for (String methodname : methodsname) {
            pgraph.d4jMethodNames.add(splt[0] + "::" + methodname);
          }
        }
      }
    }

    String checkoutbase = getCheckoutBase();
    String whatIsTracedLog = String.format("%s/%s/%s/trace/logs/mytrace/traced.source.log", checkoutbase, project, id);
    pgraph.parseWhatIsTracedLog(whatIsTracedLog);

    String poolpath = String.format("%s/%s/%s/trace/logs/mytrace/TracePool.ser", checkoutbase, project, id);
    pgraph.setPool(poolpath);
    String post_Idom_Path = String.format("%s/%s/%s/trace/logs/mytrace/post_idom.log", checkoutbase, project, id);
    pgraph.set_Post_Idom(post_Idom_Path);
    String branch_Stores_Path = String.format("%s/%s/%s/trace/logs/mytrace/branch_stores.log", checkoutbase, project, id);
    pgraph.set_Branch_Stores(branch_Stores_Path);
    String exception_Obs_Path = String.format("./observe/%s/Exception-%d.log",project,id);
    pgraph.set_Exception_Observe(exception_Obs_Path);
    String assert_Obs_Path = String.format("./observe/%s/Assert-%d.log",project,id);
    pgraph.set_Assert_Observe(assert_Obs_Path);

    System.out.println("Read Static Info Complete");


    String traceFolder = String.format("%s/%s/%s/trace/logs/run/", checkoutbase, project, id);
    // String sourcefolder = String.format("%s/%s/%s/trace/logs/mytrace/", checkoutbase, project, id);
    pgraph.parseTrace(traceFolder);
    System.out.println("Parse complete");

  }

  public static void main(String args[]) {

    // Runtime.getRuntime().addShutdownHook(WriterUtils.cleanup());
    long startTime = System.currentTimeMillis();
    boolean useNewParse = true;
    if(useNewParse){
      ProbGraph pgraph = new ProbGraph();
      if (args.length == 2) {
        System.out.println("Start parsing for " + args[0] + args[1]);
        buildD4jProject(pgraph, args[0], Integer.parseInt(args[1]));
      } else {
        System.out.println("Error, need parameters, parsed4j {proj} {id} ");
      }
      pgraph.check_bp(true);
      if(pgraph.debug_logger_switch){
        // pgraph.printreduced();
        pgraph.printgraph();
      }
    }
    else{
      ByteCodeGraph pgraph = new ByteCodeGraph();

      pgraph.setAutoOracle(true);
      pgraph.setTraceAllClassed(false);
      boolean usesimple = false;
      if (args.length >= 2) {
        System.out.println("Start parsing for " + args[0] + args[1]);
        setupD4jProject(pgraph, args[0], Integer.parseInt(args[1]), usesimple);
      } else {
        setupD4jProject(pgraph, "Lang", 7, false);
      }
      // pgraph.initD4jProject();
      //
      pgraph.printgraph();
      pgraph.check_bp(true);
    }
    System.out.println("BP finished for " + args[0] + args[1]);

    // shutdownhook not working when using exec:java.
    WriterUtils.cleanup().run();
    boolean logparsetime = true;
    if(logparsetime){
      long endTime = System.currentTimeMillis();
      long thetime = endTime - startTime;
      File f = new File(String.format("./tracetime/parsetime_%s.log", args[0]));
      try (BufferedWriter bWriter = new BufferedWriter(new FileWriter(f, true))) {
        bWriter.write(args[1] + ":" + thetime / 1000.0 + "\n");
        bWriter.flush();
      } catch (IOException e) {
  
      }
    }

  }
}
