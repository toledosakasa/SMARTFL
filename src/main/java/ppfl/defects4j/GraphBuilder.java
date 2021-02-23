package ppfl.defects4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import ppfl.ByteCodeGraph;

public class GraphBuilder {

  private static void setupD4jProject(ByteCodeGraph pgraph, String project, int id) {
    pgraph.useD4jTest = true;
    String triggerTests = null;
    String relevantClasses = null;
    String allTestMethods = null;
    String configpath = String.format("d4j_resources/metadata_cached/%s/%d.log", project, id);
    try (BufferedReader reader = new BufferedReader(new FileReader(configpath))) {
      String tmp;
      while ((tmp = reader.readLine()) != null) {
        String[] splt = tmp.split("=");
        // if (splt[0].equals("classes.relevant")) {
        // relevantClasses = splt[1];
        // }
        // if (splt[0].equals("tests.all")) {
        // allTestClasses = splt[1];
        // }
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

    if (relevantClasses != null) {
      for (String s : relevantClasses.split(";")) {
        if (!s.isEmpty()) {
          pgraph.addTracedClass(s);
          pgraph.parseD4jSource(project, id, s);
        }
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
    // long startTime = System.currentTimeMillis();
    pgraph.get_idom();
    pgraph.get_stores();
    // long endTime = System.currentTimeMillis();
    // long thetime = endTime-startTime;
    // System.out.println("idom time is "+ thetime);
    String tracefilename = String.format("tmp_checkout/%s/%s/trace/logs/mytrace/all.log", project, id);
    pgraph.parseJoinedTrace(tracefilename);
    // this.parseD4jTrace(tracefilename);
  }

  public static void main(String args[]) {
    ByteCodeGraph pgraph = new ByteCodeGraph();

    String resultfile = "InfResult";
    ByteCodeGraph.setResultLogger(resultfile);
    String graphfile = "ProbGraph";
    ByteCodeGraph.setGraphLogger(graphfile);

    pgraph.setAutoOracle(true);
    pgraph.setTraceAllClassed(false);
    if (args.length >= 2) {
      setupD4jProject(pgraph, args[0], Integer.parseInt(args[1]));
    } else {
      setupD4jProject(pgraph, "Lang", 3);
    }
    // pgraph.initD4jProject();
    pgraph.printgraph();
    pgraph.check_bp(true);
  }
}
