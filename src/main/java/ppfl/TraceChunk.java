package ppfl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;

import ppfl.instrumentation.TraceDomain;
import ppfl.instrumentation.Trace;
import ppfl.instrumentation.InvokeTrace;
import ppfl.instrumentation.FieldTrace;
import ppfl.instrumentation.DynamicTrace;
import java.io.FileWriter;
public class TraceChunk {

  private class MatchedPoint {
    public int id;
    // 0=normal return 1=catched 2=no match
    public int type;

    public MatchedPoint(int id, int type) {
      this.id = id;
      this.type = type;
    }
  }

  public List<String> traces;
  public boolean testpass;
  String fullname;
  boolean pruned = false;
  public List<ParseInfo> parsedTraces = new ArrayList<>();
  public List<DynamicTrace> parsedDTraces = new ArrayList<>();

  public TraceChunk(String fullname) {
    traces = new ArrayList<>();
    this.fullname = fullname;
  }

  public String getTestName() {
    return fullname.split("::")[1];
  }

  public void add(String s) {
    this.traces.add(s);
  }

  public void addSetUp(List<String> s) {
    this.traces.addAll(s);
  }

  public void pruneInit(Set<TraceDomain> TracedDomain) {
    int trace_size=traces.size();
    for (int i = 0; i < trace_size; i++) {
      Trace trace=null;
      DynamicTrace Dtrace=null;
      try {
        String str_trace = traces.get(i);
        String[] split = str_trace.split(",");
        trace = new Trace(split);
        String _calltype = null, _callclass = null, _callname = null;
        String _field = null;
        for (String instinfo : split) {
          String[] splitinstinfo = instinfo.split("=");
          String infotype = splitinstinfo[0];
          String infovalue = splitinstinfo[1];
          if (infotype.equals("calltype")) {
            _calltype = infovalue;
          }
          if (infotype.equals("callclass")) {
            _callclass = infovalue;
          }
          if (infotype.equals("callname")) {
            _callname = infovalue;
          }
          if (infotype.equals("field")) {
            _field = infovalue;
          }
        }
        if (_calltype != null) {
          trace = new InvokeTrace(trace, _calltype, _callclass, _callname);
        }
        if (_field != null) {
          trace = new FieldTrace(trace, _field);
        }
        Dtrace = new DynamicTrace(trace);
      } catch (Exception e) {
        System.out.println(this.fullname + " " + i);
        System.out.println(traces.get(i));
        throw (e);
      }
      if (trace.opcode == 179)// putstatic
        parsedDTraces.add(Dtrace);
      assert(Dtrace.toString().contentEquals("\n"+traces.get(i)));
    }
    this.traces.clear();
  }

  public void prune(Set<TraceDomain> TracedDomain) {
    // List<Integer> toadd = new ArrayList<>();
    for (int i = 0; i < traces.size(); i++) {
      ParseInfo parsed = null;
      try {
        parsed = new ParseInfo(traces.get(i));
      } catch (Exception e) {
        System.out.println(this.fullname + " " + i);
        System.out.println(traces.get(i));
        throw (e);
      }
      parsedTraces.add(parsed);
      // // remove normal return msg.
      // if (parsed.isReturnMsg) {
      // continue;
      // }
      // parsedTraces.add(parsed);
      // // toadd.add(i);
      // if (parsed.isUntracedInvoke(tracedClass)) {
      // MatchedPoint mPoint = matchLastReturnOrCatch(i, parsed, tracedClass);
      // if (mPoint.type == 0) {
      // i = mPoint.id;
      // } else if (mPoint.type == 1) {
      // ParseInfo matchedCatch = new ParseInfo(traces.get(mPoint.id));
      // parsedTraces.add(matchedCatch);
      // parsed.setException();

      // System.out.println("Catch-clause Matched, " + i + " " + mPoint.id);
      // matchedCatch.debugprint();
      // i = mPoint.id;
      // } else if (mPoint.type == 2) {
      // parsed.setNoMatch();
      // System.out.println("No Match, " + i);
      // parsed.debugprint();
      // }
      // }
    }
    this.traces.clear();
  }

  public void parseOneTrace(DynamicTrace dtrace) {
    parsedDTraces.add(dtrace);
  }

  private MatchedPoint matchLastReturnOrCatch(int pos, ParseInfo toMatch, Set<String> tracedClass) {
    int onlyret = matchLastReturnPoint(pos, toMatch, tracedClass, false);
    if (onlyret != -1) {
      return new MatchedPoint(onlyret, 0);
    }
    int catched = matchLastReturnPoint(pos, toMatch, tracedClass, true);
    if (catched != -1) {
      toMatch.setException();
      return new MatchedPoint(catched, 1);
    } else {
      toMatch.setNoMatch();
      return new MatchedPoint(-1, 2);
    }

  }

  private int matchLastReturnPoint(int pos, ParseInfo toMatch, Set<String> tracedClass, boolean matchCatch) {
    for (int i = pos + 1; i < traces.size(); i++) {
      ParseInfo cur = new ParseInfo(traces.get(i));
      if (toMatch.matchReturn(cur, matchCatch)) {
        // System.out.println(pos + " " + i);
        return i;
      }
      if (cur.isUntracedInvoke(tracedClass)) {
        int j = matchLastReturnPoint(i, cur, tracedClass, matchCatch);
        if (j == -1) {
          // System.out.println(traces.get(i));
          return -1;
        }
        i = j;
      }
    }
    return -1;
  }

  boolean parseInfoEqual(ParseInfo a, ParseInfo b) {
    return a.linenumber == b.linenumber && a.byteindex == b.byteindex
        && a.domain.equals(b.domain) && a.isReturnMsg == b.isReturnMsg;
  }

  public void loop_compress(Set<LoopEdge> loopset, MyWriter debuglogger) {
    int totalcompress = 0;
    for (LoopEdge theloop : loopset) {
      String[] lineinfos = theloop.start.split("#");
      ParseInfo startLine = new ParseInfo();
      startLine.isReturnMsg = false;
      startLine.domain = new TraceDomain(lineinfos[0], lineinfos[1], lineinfos[2]);
      startLine.linenumber = Integer.valueOf(lineinfos[3]);
      startLine.byteindex = Integer.valueOf(lineinfos[4]);
      lineinfos = theloop.end.split("#");
      ParseInfo endLine = new ParseInfo();
      endLine.isReturnMsg = false;
      endLine.domain = new TraceDomain(lineinfos[0], lineinfos[1], lineinfos[2]);
      endLine.linenumber = Integer.valueOf(lineinfos[3]);
      endLine.byteindex = Integer.valueOf(lineinfos[4]);
      List<Integer> compressList = new ArrayList<>(); // three int to identify a compressed loop
      int compressFirst = -1;
      int compressLast = -1;
      int compressCount = -1;
      int loopFirst = -1;
      int loopLast = -1;
      int length = parsedTraces.size();
      for (int i = 0; i < length - 1; i++) {
        ParseInfo thisLine = parsedTraces.get(i);
        if (thisLine.isReturnMsg)
          continue;
        if (parseInfoEqual(startLine, thisLine)) {
          ParseInfo nextLine = parsedTraces.get(i + 1); // i+1 should <length
          if (parseInfoEqual(endLine, nextLine)) {
            if (compressFirst == -1) // now start the compress
            {
              compressFirst = i + 1;
            } else {
              if (compressLast == -1) // the first compress ends
              {
                compressLast = i;
                compressCount = 0;
              } else {
                loopLast = i; // determine the end of this loop (the start is determined in last round)
                boolean canPress = true;
                if (loopLast - loopFirst != compressLast - compressFirst) {
                  canPress = false;
                } else {
                  for (int j = 0; j <= compressLast - compressFirst; j++) {
                    if (!parseInfoEqual(parsedTraces.get(j + compressFirst), parsedTraces.get(j + loopFirst))) {
                      canPress = false;
                      break;
                    }
                  }
                }
                if (canPress) {
                  compressCount++;
                } else {
                  if (compressCount > 0) {
                    compressList.add(compressFirst);
                    compressList.add(compressLast);
                    compressList.add(compressCount);
                  }
                  compressFirst = i + 1;
                  compressLast = -1;
                  compressCount = -1;
                }
              }
            }
            loopFirst = i + 1;
          }
        }
      }
      if (compressCount > 0) {
        compressList.add(compressFirst);
        compressList.add(compressLast);
        compressList.add(compressCount);
      }
      if (debuglogger != null) {
        int size = compressList.size() / 3;
        for (int k = 0; k < size; k++) {
          int start = compressList.get(3 * k);
          int end = compressList.get(3 * k + 1);
          int count = compressList.get(3 * k + 2);
          int looplen = end - start + 1;
          totalcompress += looplen * count;
          debuglogger.writeln("start = " + start + ", end = " + end + ", count = " + count +
              ", length = " + looplen + ", total = " + looplen * count + "\n");
          debuglogger.writeln("start = " + parsedTraces.get(start).getvalue("lineinfo")
              + ", end = " + parsedTraces.get(end).getvalue("lineinfo") + "\n");
        }
      }
      int size = compressList.size() / 3;
      for (int k = size - 1; k >= 0; k--) {
        int start = compressList.get(3 * k);
        int end = compressList.get(3 * k + 1);
        int count = compressList.get(3 * k + 2);
        int looplen = end - start + 1;
        if (debuglogger != null)
          debuglogger.writeln("remove from " + (end + 1) + " to " + (looplen * count + end) + "\n");
        for (int index = looplen * count + end; index > end; index--)
          parsedTraces.remove(index);
        // ArrayList<ParseInfo> tmp = parsedTraces;
        // List<ParseInfo> newlist = parsedTraces.subList(0,end+1);
        // newlist.addAll(parsedTraces.subList(looplen*count + end +1,
        // parsedTraces.size()));
        // parsedTraces = newlist;
      }

    }
    if (debuglogger != null)
      debuglogger.writeln("total compress = " + totalcompress + "\n");
  }

}
