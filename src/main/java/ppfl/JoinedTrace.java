package ppfl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ppfl.instrumentation.TraceDomain;

public class JoinedTrace {

  private Set<String> d4jMethodNames = new HashSet<>();
  private Set<String> d4jTriggerTestNames = new HashSet<>();
  private Set<TraceDomain> tracedDomain = new HashSet<>();
  private static final double MAX_FILE_LIMIT = 3e7;

  public List<TraceChunk> traceList = new ArrayList<>();

  private List<String> setUpTraces = null;

  private void addTraceChunk(String fullname) {
    TraceChunk toadd = new TraceChunk(fullname);
    toadd.testpass = getD4jTestState(fullname);
    traceList.add(toadd);
    if (setUpTraces != null) {
      toadd.addSetUp(setUpTraces);
      setUpTraces = null;
    }
  }

  private void addSingleTrace(String trace) {
    if (setUpTraces != null) {
      setUpTraces.add(trace);
      return;
    }
    if (!traceList.isEmpty())
      traceList.get(traceList.size() - 1).add(trace);
  }

  private boolean isD4jTestMethod(String longname) {
    // String longname = className + "::" + methodName;
    // System.out.println(longname);
    return d4jMethodNames.contains(longname);
  }

  private boolean isSetUp(String longname) {
    return longname.endsWith("::setUp");
  }

  private void addSetUp(String t) {
    this.setUpTraces = new ArrayList<>();
    // this.setUpTraces.add(t);
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

  public void parseFolder(String path) {
    File folder = new File(path);
    File[] fs = folder.listFiles();
    for (File f : fs) {
      if (f.getName().equals("all.log"))
        continue;
      if (f.length() < MAX_FILE_LIMIT) {
        parseSepFile(f, f.getName());
      }
    }
    parseToInfo();
  }

  private void parseSepFile(File f, String name) {
    String suffix = ".log";
    if (name.endsWith(suffix))
      name = name.substring(0, name.length() - suffix.length());
    int index = name.lastIndexOf('.');
    String fullname = name.substring(0, index) + "::" + name.substring(index + 1);
    this.addTraceChunk(fullname);
    try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
      String delimiterPrefix = "###";
      String t = null;
      while ((t = reader.readLine()) != null) {
        if (t.isEmpty()) {
          continue;
        }
        if (t.startsWith(delimiterPrefix)) {
          // System.out.println(t);
          t = t.substring(delimiterPrefix.length());
          // if (isSetUp(t)) {
          // this.addSetUp(t);
          // }
          if (isD4jTestMethod(t)) {
            // this.addTraceChunk(t);
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
          if (isSetUp(t)) {
            this.addSetUp(t);
          }
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
    parseToInfo();
  }

  private void parseToInfo() {
    // prune
    Iterator<TraceChunk> it = this.traceList.iterator();
    while (it.hasNext()) {
      TraceChunk chunk = it.next();
      try {
        chunk.prune(this.tracedDomain);
      } catch (Exception e) {
        System.err.println("prune at " + chunk.fullname + " failed");
        it.remove();
        // this.traceList.remove(chunk);
      }
    }
  }
}
