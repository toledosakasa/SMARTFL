package ppfl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ppfl.instrumentation.TraceDomain;

public class JoinedTrace {

  private Set<String> d4jMethodNames = new HashSet<>();
  private Set<String> d4jTriggerTestNames = new HashSet<>();
  private Set<TraceDomain> tracedDomain = new HashSet<>();

  public List<TraceChunk> traceList;

  private void addTraceChunk(String fullname) {
    TraceChunk toadd = new TraceChunk(fullname);
    toadd.testpass = getD4jTestState(fullname);
    traceList.add(toadd);
  }

  private void addSingleTrace(String trace) {
    traceList.get(traceList.size() - 1).add(trace);
  }

  private boolean isD4jTestMethod(String longname) {
    // String longname = className + "::" + methodName;
    // System.out.println(longname);
    return d4jMethodNames.contains(longname);
  }

  private boolean getD4jTestState(String fullname) {
    assert (d4jMethodNames.contains(fullname));
    return !d4jTriggerTestNames.contains(fullname);
  }

  public JoinedTrace(Set<String> d4jMethodNames, Set<String> d4jTriggerTestNames, Set<TraceDomain> tracedDomain) {
    this.d4jMethodNames = d4jMethodNames;
    this.d4jTriggerTestNames = d4jTriggerTestNames;
    this.tracedDomain = tracedDomain;
  }

  // this could be memory-unfriendly.
  // some pruning will be done here.
  public void parseFile(String tracefilename) {
    this.traceList = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(tracefilename))) {
      String t = null;
      String delimiterPrefix = "###";
      int i = 0;
      while ((t = reader.readLine()) != null) {
        // System.out.println(t.length());
        ++i;
        // if (t.endsWith("AggregateTrans")) {
        // System.out.println(i);
        // System.out.println(t);
        // }
        if (t.isEmpty()) {
          continue;
        }
        if (t.startsWith(delimiterPrefix)) {
          // System.out.println(t);
          t = t.substring(delimiterPrefix.length());
          // String[] splt = t.split("::");
          if (isD4jTestMethod(t)) {
            this.addTraceChunk(t);
          }
          if (t.startsWith("RET@")) {
            this.addSingleTrace(t);
          }
        } else {
          this.addSingleTrace(t);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    // prune
    for (TraceChunk chunk : this.traceList) {
      // System.out.println("Pruning " + chunk.fullname);
      try {
        chunk.prune(this.tracedDomain);
      } catch (Exception e) {
        System.err.println("prune at " + chunk.fullname + " failed");
        this.traceList.remove(chunk);
      }
    }

  }
}
