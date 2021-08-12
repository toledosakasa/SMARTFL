package ppfl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

// import org.graphstream.graph.implementations.SingleGraph;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.slf4j.MDC;

import ppfl.defects4j.GraphBuilder;
import ppfl.instrumentation.Interpreter;
import ppfl.instrumentation.RuntimeFrame;
import ppfl.instrumentation.TraceDomain;
import ppfl.instrumentation.opcode.OpcodeInst;

public class ByteCodeGraph {

	private MyWriter graphLogger = WriterUtils.getWriter("Debugger");
	private MyWriter resultLogger = WriterUtils.getWriter("Debugger");
	private MyWriter reduceLogger = WriterUtils.getWriter("Debugger");

	private boolean debug_logger_switch = false;

	public void setGraphLogger(String graphfile) {
		// MDC.put("graphfile", graphfile);
		WriterUtils.setPath("trace/logs/mytrace/");
		graphLogger = WriterUtils.getWriter(graphfile);
	}

	public void setResultLogger(String resultfile) {
		// MDC.put("resultfile", resultfile);
		WriterUtils.setPath("trace/logs/mytrace/");
		resultLogger = WriterUtils.getWriter(resultfile);
	}

	public void setReduceLogger(String resultfile) {
		// MDC.put("resultfile", resultfile);
		WriterUtils.setPath("trace/logs/mytrace/");
		reduceLogger = WriterUtils.getWriter(resultfile);
	}

	private static Set<TraceDomain> tracedDomain = new HashSet<>();
	private static Map<String, String> superClassMap = new HashMap<>();
	private boolean traceAllClasses = true;
	String currentTestMethod = null;

	public void setTraceAllClassed(boolean value) {
		this.traceAllClasses = value;
	}

	public void addTracedDomain(TraceDomain domain) {
		tracedDomain.add(domain);
	}

	public void addTracedDomain(Collection<TraceDomain> domain) {
		tracedDomain.addAll(domain);
	}

	public boolean isUntracedInvoke(ParseInfo p) {
		return p.isInvoke() && (!isTraced(p.getCallDomain()));
	}

	public TraceDomain resolveMethod(TraceDomain domain) {
		if (domain == null)
			return null;
		String traceclass = domain.traceclass;
		String tracemethod = domain.tracemethod;
		String signature = domain.signature;
		while (!isTraced(domain)) {
			traceclass = superClassMap.get(traceclass);
			if (traceclass == null)
				return null;
			domain = new TraceDomain(traceclass, tracemethod, signature);
		}
		return domain;
	}

	public boolean isTraced(TraceDomain domain) {
		// FIXME: lookup class inheritance
		if (this.traceAllClasses)
			return true;
		boolean ret = tracedDomain.contains(domain);
		// if (!ret) {
		// System.out.println("untraced:" + domain.toString());
		// }
		return ret;

	}

	public boolean useD4jTest = false;
	public Set<String> d4jMethodNames = new HashSet<>();
	public Set<String> d4jTestClasses = new HashSet<>();
	public Set<String> d4jTriggerTestNames = new HashSet<>();

	private boolean isD4jTestMethod(String className, String methodName) {
		if (!useD4jTest) {
			return false;
		}
		String longname = className + "::" + methodName;
		return d4jMethodNames.contains(longname);
	}

	private boolean getD4jTestState(String fullname) {
		assert (d4jMethodNames.contains(fullname));
		return !d4jTriggerTestNames.contains(fullname);
	}

	public List<FactorNode> factornodes;
	public List<Node> nodes;
	public List<StmtNode> stmts;
	public Map<String, Node> nodemap;
	public Map<String, Node> stmtmap;
	public Map<String, Integer> varcountmap;
	public Map<String, Integer> stmtcountmap;
	public Map<String, Integer> heapcountmap;
	public Map<String, Integer> objectcountmap;
	public Map<String, Integer> staticheapcountmap;
	public Map<Integer, Set<String>> objectFieldMap;
	// public org.graphstream.graph.Graph viewgraph;
	public Set<String> instset;
	public Set<String> outset;
	public Set<String> inset;
	public Map<String, List<String>> predataflowmap;
	public Map<String, List<String>> postdataflowmap;
	public Map<String, Set<String>> dataflowsets;
	public Map<String, String> pre_idom;
	public Map<String, String> post_idom;
	public Map<String, Integer> store_num;
	public Map<String, Set<Integer>> branch_stores;
	public Set<LoopEdge> loopset;
	private boolean shouldview;

	private boolean resultFilter = true;

	// to prevent lazy-evaluation on static initializers.
	private boolean solveTracedInvoke = true;
	public StmtNode tracedStmt;
	public List<Node> traceduse;
	public List<Node> tracedpred;
	public ParseInfo tracedInvoke = null;
	public Node tracedObj = null;

	public StmtNode untracedStmt;
	public List<Node> untraceduse;
	public List<Node> untracedpred;
	public ParseInfo untracedInvoke = null;
	public Node untracedObj = null;

	public StmtNode throwStmt;
	public List<Node> throwuse;
	public List<Node> throwpred;
	public ParseInfo unsolvedThrow = null;

	private boolean solveStatic = true;
	public StmtNode staticStmt;
	public List<Node> staticuse;
	public List<Node> staticpred;
	public ParseInfo unsolvedStatic = null;

	boolean compareCallClass = false;

	// compromise on minor faults.
	boolean compromise = true;

	public void stopview() {
		shouldview = false;
	}

	// if max_loop is set to negative, then no limit is set(unlimited loop
	// unfolding)
	public int max_loop;

	public String testname = null;

	public int nsamples = 1 << 20;
	public int bp_times = 100;
	Random random;

	// stack tracing
	// private Stack<RuntimeFrame> stackframe;
	private Deque<RuntimeFrame> stackframe;

	// should be called while returning.
	// e.g. return ireturn
	public void popStackFrame() {
		this.stackframe.pop();
		// buggy.
		// .runtimestack.clear();
	}

	// should be called while invoking.
	// e.g. invokestatic
	public void pushStackFrame(TraceDomain domain) {
		RuntimeFrame topush = RuntimeFrame.getFrame(domain);
		topush.entercnt++;
		this.stackframe.push(topush);
	}

	public RuntimeFrame getFrame() {
		if (stackframe.isEmpty()) {
			stackframe.push(RuntimeFrame.getFrame(parseinfo.domain));
		}
		return stackframe.peek();
	}

	private void emptyFrame() {
		this.stackframe.clear();
	}

	public RuntimeFrame getPrevFrame() {
		if (stackframe.size() < 2)
			return null;
		RuntimeFrame top = stackframe.removeFirst();
		RuntimeFrame ret = stackframe.peekFirst();
		stackframe.addFirst(top);
		return ret;
	}

	public SafeRunTimeStack getRuntimeStack() {
		return getFrame().runtimestack;
	}

	public boolean isInCallStack(ParseInfo p) {
		TraceDomain tDomain = p.domain;
		for (RuntimeFrame rFrame : stackframe) {
			if (rFrame.domain.equals(tDomain)) {
				return true;
			}
		}
		return false;
	}

	// local vars used by parsing
	public ParseInfo parseinfo = new ParseInfo();

	// auto-oracle: when set to TRUE, parsetrace() will auto-assign prob for:
	// input of test function as 1.0(always true)
	// output (return value) of the function being tested as 0.0/1.0 depends on
	// parameter testpass(true = 1.0,false = 0.0)
	public boolean auto_oracle;

	public int lastDefinedLine = 0;
	public List<Node> lastDefinedVar = new ArrayList<>();
	public List<StmtNode> lastDefinedStmt = new ArrayList<>();

	//
	public List<Node> predicates = new ArrayList<>();

	// predicate stack for building factor
	private Deque<Node> predstack;

	public void popPredStack() {
		this.predstack.pop();
	}

	public void pushPredStack(Node newpred) {
		this.predstack.push(newpred);
	}

	public Node getPredStack() {
		return this.predstack.peek();
	}

	public List<Node> reversePredStack() {
		List<Node> newlist = new ArrayList<>();
		newlist.addAll(predstack);
		Collections.reverse(newlist);
		return newlist;
	}

	// the stack for stores in branchs
	private Deque<Set<Integer>> store_stack;

	public void killPredStack(String thisinst) {
		boolean willcontinue = true;
		while (willcontinue) {
			willcontinue = false;
			if (this.predstack.peek() != null) {
				String stmtName = this.predstack.peek().getStmtName();
				// System.out.println("in kill "+stmtName);
				if (post_idom.get(stmtName).equals(thisinst)) {
					Node curPred = this.predstack.pop();
					StmtNode curPredStmt = curPred.stmt;
					Set<Integer> stores = null;
					// if (!this.store_stack.isEmpty())
					stores = this.store_stack.pop();
					// System.out.println("kill "+stores);
					boolean unexecuted_complement = true;
					if (unexecuted_complement) {
						if (stores != null) {
							// StmtNode curStmt = curPred.stmt;
							for (Integer i : stores) {
								StmtNode curStmt = getUnexeStmt(curPredStmt, i);
								Node usenode = getLoadNodeAsUse(i);
								if (usenode == null) {
									// System.out.println("null use " + i);
									// TODO this can be unsound?
									continue;
								}
								Node defnode = addNewVarNode(i, curStmt);
								buildFactor(defnode, curPred, usenode, null, curStmt);
							}
						}
					}
					willcontinue = true;
				}
			}
		}
	}

	public ByteCodeGraph() {
		factornodes = new ArrayList<>();
		nodes = new ArrayList<>();
		stmts = new ArrayList<>();
		nodemap = new HashMap<>();
		stmtmap = new HashMap<>();
		varcountmap = new HashMap<>();
		stmtcountmap = new HashMap<>();
		heapcountmap = new HashMap<>();
		objectcountmap = new HashMap<>();
		staticheapcountmap = new HashMap<>();
		objectFieldMap = new HashMap<>();
		instset = new HashSet<>();
		outset = new HashSet<>();
		inset = new HashSet<>();
		predataflowmap = new HashMap<>();
		postdataflowmap = new HashMap<>();
		dataflowsets = new TreeMap<>();
		pre_idom = new TreeMap<>();
		post_idom = new TreeMap<>();
		branch_stores = new TreeMap<>();
		store_num = new HashMap<>();
		max_loop = -1;
		random = new Random();
		auto_oracle = true;
		stackframe = new ArrayDeque<>();
		predstack = new ArrayDeque<>();
		store_stack = new ArrayDeque<>();
		loopset = new TreeSet<>();
		// viewgraph = new SingleGraph("Outgraph");
		// viewgraph.setStrict(false);
		// viewgraph.setAutoCreate(true);
		// viewgraph.setAttribute("layout.quality", 4);
		// viewgraph.setAttribute("layout.force");
		shouldview = false;
		String styleSheet = "node {" +
		// " text-background-mode: rounded-box;"+
				"	text-alignment: at-right;" + "	text-offset: 5px, 0px;" + "	text-style: italic;" + "	size: 15px, 15px;" + "}"
				+
				// "node.thenode {" +
				// // " shape: box;"+
				// " size: 15px, 15px;"+
				// // " fill-color: green;" +
				// "}" +
				"node.factor {" + "	shape: box;" + "	text-mode: hidden;" +
				// " size: 15px, 15px;"+
				"	fill-color: red;" +
				// " stroke-mode: plain; /* Default is none. */"+
				// " stroke-color: blue; /* Default is black. */"+
				"}" + "node.stmt {" +
				// " shape: box;"+
				"	size: 10px, 10px;" + "	fill-color: brown;" + "}" + "edge {" + "	fill-color: red;" +
				// " layout.weight: 10;"+
				"}" + "edge.def {" + "	fill-color: green;" + "}" + "edge.use {" + "	fill-color: blue;" + "}" + "edge.pred {"
				+ "	fill-color: yellow;" + "}" + "edge.stmt {" + "	fill-color: black;" + "}";
		// viewgraph.setAttribute("ui.stylesheet", styleSheet);
		// viewgraph.setAttribute("ui.quality");
		// viewgraph.setAttribute("ui.antialias");
		Interpreter.init();
	}

	// public void addviewlabel() {
	// for (Node n : nodes) {
	// // org.graphstream.graph.Node thenode = viewgraph.getNode(n.getPrintName());
	// if (thenode != null)
	// thenode.setAttribute("ui.label", " prob_bp = " + (double)
	// Math.round(n.bp_getprob() * 1000) / 1000);
	// // thenode.setAttribute("ui.label", n.getPrintName() + " prob_bp = " +
	// // n.bp_getprob());
	// }
	// for (StmtNode n : stmts) {
	// // org.graphstream.graph.Node thenode = viewgraph.getNode(n.getPrintName());
	// if (thenode != null)
	// thenode.setAttribute("ui.label", " prob_bp = " + (double)
	// Math.round(n.bp_getprob() * 1000) / 1000);
	// // thenode.setAttribute("ui.label", n.getPrintName() + " prob_bp = " +
	// // n.bp_getprob());
	// }
	// }

	public void setMaxLoop(int i) {
		this.max_loop = i;
	}

	public void setAutoOracle(boolean b) {
		this.auto_oracle = b;
	}

	public void initmaps() {
		// TODO this could be incomplete...
		this.varcountmap = new HashMap<>();
		this.stackframe = new ArrayDeque<>();
		this.predicates.clear();
		stmtcountmap = new HashMap<>();
		heapcountmap = new HashMap<>();
		objectcountmap = new HashMap<>();
		staticheapcountmap = new HashMap<>();
		objectFieldMap = new HashMap<>();
	}

	public void parseWhatIsTracedLog(String logfilename) {
		try (BufferedReader reader = new BufferedReader(new FileReader(logfilename))) {
			String t;
			while ((t = reader.readLine()) != null) {
				if (t.isEmpty())
					continue;
				String[] splt = t.split("::");
				String traceclass = splt[0];
				if (splt.length < 2) {
					continue;
				}
				splt = splt[1].split(",");
				for (String methodAndDesc : splt) {
					splt = methodAndDesc.split("#");
					String tracemethod = splt[0];
					if (splt.length < 2) {
						System.out.println(t);
					}
					String signature = splt[1];
					if (signature.equals("SuperClass")) {
						superClassMap.put(traceclass, tracemethod);
					} else {
						TraceDomain tDomain = new TraceDomain(traceclass, tracemethod, signature);
						tracedDomain.add(tDomain);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void parseD4jSource(String project, int id, String classname) {
		String checkoutbase = GraphBuilder.getCheckoutBase();
		String fullname = String.format("%s/%s/%s/trace/logs/mytrace/%s.source.log", checkoutbase, project, id, classname);
		try {
			parsesource(fullname);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("parse source crash at " + fullname);
			throw (e);
		}
	}

	public void parsesource(String sourcefilename) {
		Map<String, String> assistnamemap = new HashMap<>();
		Map<String, List<String>> _predataflowmap = new HashMap<>();
		Map<String, List<String>> _postdataflowmap = new HashMap<>();
		Set<String> _instset = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(sourcefilename))) {
			String t;
			Set<String> nonextinsts = new HashSet<>();
			nonextinsts.add("goto_w");
			nonextinsts.add("goto");
			nonextinsts.add("return");
			nonextinsts.add("areturn");
			nonextinsts.add("dreturn");
			nonextinsts.add("freturn");
			nonextinsts.add("ireturn");
			nonextinsts.add("lreturn");
			nonextinsts.add("athrow");
			// TODO consider throw
			Set<String> switchinsts = new HashSet<>();
			switchinsts.add("tableswitch");
			switchinsts.add("lookupswitch");
			while ((t = reader.readLine()) != null) {
				if (t.isEmpty() || t.startsWith("###"))
					continue;
				ParseInfo info = new ParseInfo(t);
				String thisinst = info.getvalue("lineinfo");
				// the entry of a method
				if (info.byteindex == 0) {
					inset.add(thisinst);
				}
				Integer storen = info.getintvalue("store");
				if (storen != null)
					store_num.put(thisinst, storen);
				// String classandmethod = info.traceclass + "#" + info.tracemethod;
				String classandmethod = info.domain.toString();
				String assistkey = classandmethod + info.byteindex;
				assistnamemap.put(assistkey, thisinst);
				List<String> theedges = new ArrayList<>();
				// deal with switch
				if (switchinsts.contains(info.opcode)) {
					Integer defaultbyte = info.getintvalue("default");
					if (defaultbyte != null) {
						String defaultinst = classandmethod + (defaultbyte.intValue() + info.byteindex);
						theedges.add(defaultinst);
					}
					String switchlist = info.getvalue("switch");
					if (switchlist != null) {
						String[] switchterms = switchlist.split(";");
						for (String switchterm : switchterms) {
							String jumpinst = classandmethod
									+ (Integer.valueOf(switchterm.split(":")[1]).intValue() + info.byteindex);
							theedges.add(jumpinst);
						}
					}
					_predataflowmap.put(thisinst, theedges);
					_instset.add(thisinst);
					continue;
				}
				if (!(nonextinsts.contains(info.opcode))) {
					String nextinst = classandmethod + info.getvalue("nextinst");
					theedges.add(nextinst);
				}
				Integer branchbyte = info.getintvalue("branchbyte");
				if (branchbyte != null) {
					String branchinst = classandmethod + (branchbyte.intValue() + info.byteindex);
					theedges.add(branchinst);
				}
				if (theedges.isEmpty()) {
					String outname = "OUT_" + classandmethod;
					theedges.add(outname);
					_instset.add(outname);
					_predataflowmap.put(outname, new ArrayList<>());
					outset.add(outname);
				}
				_predataflowmap.put(thisinst, theedges);
				_instset.add(thisinst);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// change the name (to include the line number)
		for (List<String> theedges : _predataflowmap.values()) {
			int valuelen = theedges.size();
			for (int i = 0; i < valuelen; i++) {
				if (assistnamemap.containsKey(theedges.get(i))) {
					String newname = assistnamemap.get(theedges.get(i));
					theedges.set(i, newname);
				}
			}
		}
		// init the postmap, keys including OUT_xx
		for (String instname : _instset) {
			List<String> theedges = new ArrayList<>();
			_postdataflowmap.put(instname, theedges);
		}
		// get the postmap
		for (Map.Entry<String, List<String>> instname : _predataflowmap.entrySet()) {
			List<String> preedges = instname.getValue();
			for (String prenode : preedges) {
				// System.out.println(preedges);
				// System.out.println(instname+"__"+prenode);
				List<String> postedges = _postdataflowmap.get(prenode);
				postedges.add(instname.getKey());
			}
		}
		predataflowmap.putAll(_predataflowmap);
		postdataflowmap.putAll(_postdataflowmap);
		instset.addAll(_instset);
		// System.out.println("size =" + postdataflowmap.size());
		// for(String key : postdataflowmap.keySet()){
		// System.out.println("key_"+key);
		// System.out.println(predataflowmap.get(key));
		// }
	}

	private Deque<String> reverse_postorder = new ArrayDeque<>();
	private Set<String> visited = new HashSet<>();
	private Map<String, Integer> postorder = new HashMap<>();
	private int cnt;

	private void dfssearch(String inst) {
		// visited.add(inst);
		// List<String> thesuccs = postdataflowmap.get(inst);
		// for (String succ : thesuccs) {
		// if (!visited.contains(succ)) {
		// dfssearch(succ);
		// }
		// }
		// reverse_postorder.addFirst(inst);
		// postorder.put(inst, new Integer(cnt));
		// cnt++;

		Deque<String> searchstack = new ArrayDeque<>();
		searchstack.push(inst);
		visited.add(inst);
		while (!searchstack.isEmpty()) {
			String theinst = searchstack.peek();
			List<String> thesuccs = postdataflowmap.get(theinst);
			boolean isleaf = true;
			for (String succ : thesuccs) {
				if (!visited.contains(succ)) {
					visited.add(succ);
					searchstack.push(succ);
					isleaf = false;
				}
			}
			if (isleaf) {
				reverse_postorder.addFirst(theinst);
				postorder.put(theinst, cnt);
				cnt++;
				searchstack.pop();
			}
		}
	}

	private String intersect(String b1, String b2) {
		String finger1 = b1;
		String finger2 = b2;
		while (!finger1.equals(finger2)) {
			while (postorder.get(finger1).intValue() < postorder.get(finger2).intValue()) {
				finger1 = post_idom.get(finger1);
			}
			while (postorder.get(finger1).intValue() > postorder.get(finger2).intValue()) {
				finger2 = post_idom.get(finger2);
			}
		}
		return finger1;
	}

	// Keith D. Cooper algorithm
	public void get_idom() {
		cnt = 1;
		reverse_postorder = new ArrayDeque<>();
		visited = new HashSet<>();
		postorder = new HashMap<>();
		for (String outname : outset) {
			dfssearch(outname);
		}
		for (String inst : reverse_postorder) {
			post_idom.put(inst, "Undefined");
		}
		for (String outname : outset) {
			post_idom.put(outname, outname);
		}
		boolean changed = true;
		while (changed) {
			changed = false;
			for (String inst : reverse_postorder) {
				if (outset.contains(inst))
					continue;
				List<String> thepreds = predataflowmap.get(inst);
				int predsnum = thepreds.size();
				int tmpmax = -1;
				int tmpindex = 0;
				// seems should get the pred with the max postorder
				for (int i = 0; i < predsnum; i++) {
					// to deal with pre_idom, some nodes can not be visited in pre order in the
					// graph so it has no order after dfs
					if (postorder.get(thepreds.get(i)) == null) {
						continue;

						// resultLogger.writeln("null at inst %s, pred %s\n", inst, thepreds.get(i));
						// for(Map.Entry<String, Integer> entry : postorder.entrySet())
						// resultLogger.writeln("key = " + entry.getKey() + ", value = " +
						// entry.getValue());

						// for(String tmps: outset)
						// resultLogger.writeln("outset0 %s\n", tmps);
					}
					if (tmpmax < postorder.get(thepreds.get(i)).intValue()) {
						tmpmax = postorder.get(thepreds.get(i)).intValue();
						tmpindex = i;
					}
				}
				String new_idom = thepreds.get(tmpindex);
				for (int i = 0; i < predsnum; i++) {
					if (i == tmpindex)
						continue;
					String otherpred = thepreds.get(i);
					// to deal with pre_idom, some nodes can not be visited in pre order in the
					// graph so it has no order after dfs
					if (post_idom.get(otherpred) == null)
						continue;
					if (!post_idom.get(otherpred).equals("Undefined")) {
						new_idom = intersect(otherpred, new_idom);
					}
				}
				if (!post_idom.get(inst).equals(new_idom)) {
					post_idom.put(inst, new_idom);
					changed = true;
				}
			}
		}
		// System.out.println("size =" + post_idom.size());
		// for (String key : post_idom.keySet()) {
		// System.out.println("key_" + key);
		// System.out.println("post_idom = " + post_idom.get(key));
		// }
	}

	public void get_pre_idom() {
		// for (String tmp :
		// predataflowmap.get("org.apache.commons.lang3.ValidateTest#testNoNullElementsArray1#()V#558#60"))
		// resultLogger.writeln("the next"+tmp);

		// for(Map.Entry<String, List<String>> entry : postdataflowmap.entrySet())
		// if(!inset.contains(entry.getKey()) && entry.getValue().size() == 0)
		// resultLogger.writeln("key = " + entry.getKey() + ", value = " +
		// entry.getValue().size());

		Map<String, List<String>> tmp_map1 = this.predataflowmap;
		this.predataflowmap = this.postdataflowmap;
		this.postdataflowmap = tmp_map1;
		Set<String> tmp_set = this.outset;
		// for(String instname : this.inset)
		// resultLogger.writeln("inset %s\n", instname);
		// for(String instname : this.outset)
		// resultLogger.writeln("out %s\n", instname);
		this.outset = this.inset;
		this.inset = tmp_set;
		Map<String, String> tmp_map2 = this.post_idom;
		this.get_idom();
		this.pre_idom = this.post_idom;
		this.post_idom = tmp_map2;
		tmp_map1 = this.predataflowmap;
		this.predataflowmap = this.postdataflowmap;
		this.postdataflowmap = tmp_map1;
		tmp_set = this.outset;
		this.outset = this.inset;
		this.inset = tmp_set;
	}

	public void find_loop() {
		for (Map.Entry<String, List<String>> entry : predataflowmap.entrySet()) {
			String edge_start = entry.getKey();
			for (String edge_end : entry.getValue()) {
				String end_dom = pre_idom.get(edge_end);
				if (end_dom != null && end_dom.equals(edge_start))
					continue;
				String dominator = pre_idom.get(edge_start);
				boolean isloop = false;
				while (dominator != null && !inset.contains(dominator)) {
					if (dominator.equals(edge_end)) {
						isloop = true;
						break;
					}
					dominator = pre_idom.get(dominator);
				}
				if (isloop) {
					LoopEdge theloop = new LoopEdge(edge_start, edge_end);
					loopset.add(theloop);
					// reduceLogger.writeln("start "+ edge_start + ", end " + edge_end +
					// ", length = " + theloop.length + "\n");
				}
			}
		}
		// for(LoopEdge theloop : loopset)
		// reduceLogger.writeln("start "+ theloop.start + ", end " + theloop.end +
		// ", length = " + theloop.length + "\n");

	}

	public void dataflow() {
		// init the dataflow set
		Set<String> changedset = new HashSet<>();
		for (String instname : postdataflowmap.keySet()) {
			if (instname.startsWith("OUT_")) {
				HashSet<String> emptyset = new HashSet<>();
				emptyset.add(instname);
				dataflowsets.put(instname, emptyset);
			} else {
				HashSet<String> theallset = new HashSet<>();
				theallset.addAll(instset);
				dataflowsets.put(instname, theallset);
				changedset.add(instname);
			}
		}

		while (!changedset.isEmpty()) {
			int changedsetsize = changedset.size();
			int item = new Random().nextInt(changedsetsize);
			int tmpindex = 0;
			String instname = "";
			for (String obj : changedset) {
				if (tmpindex == item) {
					instname = obj;
					break;
				}
				tmpindex++;
			}
			changedset.remove(instname);
			Set<String> oldset = dataflowsets.get(instname);
			Set<String> newset = new HashSet<>();
			newset.addAll(instset);
			List<String> succlist = predataflowmap.get(instname);
			for (String succinst : succlist) {
				newset.retainAll(dataflowsets.get(succinst));
			}
			newset.add(instname);
			if (!((oldset.size() == newset.size()) && oldset.containsAll(newset))) {
				List<String> predlist = postdataflowmap.get(instname);
				for (String predinst : predlist) {
					changedset.add(predinst);
				}
			}
			dataflowsets.put(instname, newset);
		}
		// boolean printdataflow = true;
		// if (printdataflow) {
		// // System.out.println("size =" + dataflowsets.size());
		// // for(String key : dataflowsets.keySet()){
		// // System.out.println("key_"+key);
		// // System.out.println(dataflowsets.get(key));
		// // }
		// debugLogger.info("size ={}", dataflowsets.size());
		// for (Entry<String, Set<String>> entry : dataflowsets.entrySet()) {
		// debugLogger.info("key_{}", entry);
		// debugLogger.info("value_{}", entry.getValue());
		// }
		// }

		// get the post_idom
		List<String> allkeys = new ArrayList<>();
		allkeys.addAll(instset);
		Comparator<String> comp = (arg0, arg1) -> Integer.compare(dataflowsets.get(arg0).size(),
				dataflowsets.get(arg1).size());
		allkeys.sort(comp);
		Set<String> oldvset = new HashSet<>();
		Set<String> newvset = new HashSet<>();
		int nowsize = 2;
		for (String thekey : allkeys) {
			Set<String> thevalue = dataflowsets.get(thekey);
			int thesize = thevalue.size();
			if (thesize == 1) {
				oldvset.add(thekey); // OUT_xxx
				continue;
			}
			if (thesize > nowsize) {
				nowsize++;
				oldvset.clear();
				oldvset.addAll(newvset);
				newvset.clear();
			}
			newvset.add(thekey);
			for (String dominst : thevalue) {
				if (oldvset.contains(dominst)) {
					post_idom.put(thekey, dominst);
					break;
				}
			}
		}
		// if (printdataflow) {
		// System.out.println("size =" + post_idom.size());
		// for(String key : post_idom.keySet()){
		// System.out.println("key_"+key);
		// System.out.println("post_idom = " + post_idom.get(key));
		// }
		// }
	}

	private void debugStack(Deque<RuntimeFrame> s) {
		for (RuntimeFrame r : s) {
			System.out.print(r.runtimestack.size());
			System.out.print("-");
		}
		System.out.print("\n");
		for (RuntimeFrame r : s) {
			System.out.print(r.domain.traceclass + ":" + r.domain.tracemethod);
			System.out.print("-");
		}
		System.out.print("\n");
	}

	private void cleanTraced() {
		this.tracedInvoke = null;
		this.tracedStmt = null;
		this.traceduse = null;
		this.tracedpred = null;
		this.tracedObj = null;
	}

	private void cleanUntraced() {
		this.untracedInvoke = null;
		this.untracedStmt = null;
		this.untraceduse = null;
		this.untracedpred = null;
		this.untracedObj = null;
	}

	private void cleanThrow() {
		this.unsolvedThrow = null;
		this.throwStmt = null;
		this.throwuse = null;
		this.throwpred = null;
	}

	private void cleanStatic() {
		this.unsolvedStatic = null;
		this.staticStmt = null;
		this.staticuse = null;
		this.staticpred = null;
	}

	private void cleanupOnChunkSwitch() {
		this.predstack.clear();
		this.cleanUntraced();
		this.cleanThrow();
		this.cleanTraced();
		this.cleanStatic();
		this.initmaps();
	}

	public void parseFolder(String folder, String sourcefolder, boolean usesimple) {
		JoinedTrace jTrace = new JoinedTrace(d4jMethodNames, d4jTriggerTestNames, tracedDomain);
		try {
			jTrace.parseFolder(folder);
			jTrace.parseSourceFolder(sourcefolder);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("parse failed.");
		}
		boolean debug_compress = false;
		for (TraceChunk tChunk : jTrace.traceList) {
			if (debug_compress) {
				reduceLogger.writeln("\n" + "start " + tChunk.fullname + "\n");
				tChunk.loop_compress(loopset, reduceLogger);
			} else {
				tChunk.loop_compress(loopset, null);
			}
		}

		parseJoinedTracePruned(jTrace, usesimple);
	}

	public void pruneAndParse(String tracefilename) {
		JoinedTrace jTrace = new JoinedTrace(d4jMethodNames, d4jTriggerTestNames, tracedDomain);
		try {
			jTrace.parseFile(tracefilename);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("parse failed.");
		}
		parseJoinedTracePruned(jTrace, false);
	}

	private void parseSimpleTrace(ParseInfo pInfo, boolean debugswitch, int linec) {
		this.parseinfo = pInfo;
		String instname = this.parseinfo.getvalue("lineinfo");
		if (this.parseinfo.isReturnMsg) {
			return;
		}
		this.getFrame();
		Interpreter.map[255].buildtrace_simple(this);
	}

	private void parseInitTrace(ParseInfo pInfo, boolean debugswitch) {
		// FIXME
		this.parseinfo = pInfo;
		this.getFrame();
		Interpreter.map[this.parseinfo.form].buildinit(this);
		// Interpreter.map[this.parseinfo.form].buildtrace(this);
	}

	private boolean matchTracedInvoke(ParseInfo oth) {
		if (oth.isReturnMsg) {
			return this.tracedInvoke.matchDomain(oth) && this.tracedInvoke.linenumber == oth.linenumber
					&& this.tracedInvoke.byteindex == oth.byteindex;
		} else {
			TraceDomain td = this.tracedInvoke.getCallDomain();
			if (!compareCallClass) {
				return td.signature.equals(oth.domain.signature) && td.tracemethod.equals(oth.domain.tracemethod);
			}

			td = resolveMethod(td);
			if (td == null)
				return false;
			// FIXME consider polymorphism
			return td.signature.equals(oth.domain.signature) && td.tracemethod.equals(oth.domain.tracemethod);
		}
	}

	private void parseSingleTrace(ParseInfo pInfo, boolean debugswitch, int linec) {
		// if (debugswitch) {
		// try {
		// System.in.read();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		this.parseinfo = pInfo;
		String instname = this.parseinfo.getvalue("lineinfo");

		// solve traced invoke
		if (this.solveTracedInvoke && this.tracedInvoke != null) {
			if (matchTracedInvoke(pInfo)) {
				if (pInfo.isReturnMsg) {
					// actually untraced due to instrumentation bug.
					// pop stackframe that is pushed for nothing.
					this.popStackFrame();
					buildTracedInvoke();
					return;
				}
				this.cleanTraced();
			} else {
				// System.out.println("skipped" + linec);
				// System.out.println(this.tracedInvoke.getCallDomain());
				// skip
				return;
			}
		}

		//
		if (this.untracedInvoke != null) {
			if (!compareCallClass && !pInfo.isReturnMsg) {// compare only signature and method name
				TraceDomain td = this.untracedInvoke.getCallDomain();
				// if fit
				if (td.signature.equals(pInfo.domain.signature) && td.tracemethod.equals(pInfo.domain.tracemethod)) {
					this.pushStackFrame(pInfo.domain);
					this.cleanUntraced();
				}
			}
		}

		// solve current untraced invoke
		if (this.untracedInvoke != null) {
			if (untracedInvoke.matchReturn(pInfo, true)) {
				if (pInfo.isReturnMsg) {
					// normally returned
					if (debugswitch)
						System.out.println("normally returned");
					buildUntracedInvoke();
					return;
				} else {
					if (debugswitch)
						System.out.println("normally catched");
					// catched
					buildUntracedInvokeException();
				}
			} else {
				// check if the control flow had already been thrown upward
				if (pInfo.isCatch() && this.isInCallStack(pInfo)) {
					if (debugswitch) {
						System.out.println("thrown");
						pInfo.debugprint();
					}
					maintainStackframeUpward(pInfo);
					buildUntracedInvokeException();
				} else {
					if (debugswitch) {
						System.out.println("skipped:");
						pInfo.debugprint();
					}
					// is inside untraced call. should skip
					return;
				}
			}
		}

		// solve current throw.
		if (this.unsolvedThrow != null) {
			if (pInfo.isCatch() && this.isInCallStack(pInfo)) {
				maintainStackframeUpward(pInfo);
				buildThrowException();
			} else {
				// should not happen
				// maybe quit on crash.
				// System.out.println("Athrow is not catched!");
				// pInfo.debugprint();
			}
		}

		if (solveStatic && this.unsolvedStatic != null) {
			if (unsolvedStatic.matchStaticReturn(pInfo)) {
				this.cleanStatic();
				return;
			} else {
				// skip
				return;
			}
		}
		// untraced invoke has been resolved.
		// skip @ret message.
		if (this.parseinfo.isReturnMsg) {
			return;
		}

		// Begin with the initial testmethod.
		this.getFrame();
		// if (!this.getFrame().domain.equals(this.parseinfo.domain)) {
		// try {
		// System.out.println(linec);
		// System.in.read();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// pInfo.debugprint();
		// debugStack(this.stackframe);
		// }
		// Debug use
		// System.out.println(instname);

		killPredStack(instname);
		if (predataflowmap.get(instname).size() > 1) {
			// System.out.println("add set" + branch_stores.get(instname));
			Set<Integer> stores = branch_stores.get(instname);
			// if (stores != null)
			store_stack.push(stores);
		}
		Interpreter.map[this.parseinfo.form].buildtrace(this);

		// if (pInfo.linenumber == 94 && pInfo.byteindex == 0) {
		// debugswitch = true;
		// }
		// debug runtime stacks
		if (debugswitch) {
			pInfo.debugprint();
			debugStack(this.stackframe);
		}
	}

	private void parseSimpleChunk(TraceChunk tChunk) {
		this.cleanupOnChunkSwitch();
		int tracelength = tChunk.parsedTraces.size();
		System.out.println("parsing trace,length=" + tracelength + ":");
		System.out.println("\t" + tChunk.fullname);
		boolean testpass = tChunk.testpass;
		this.testname = tChunk.getTestName();
		boolean debugswitch = false;
		int linec = 0;
		for (ParseInfo pInfo : tChunk.parsedTraces) {
			try {
				linec++;
				parseSimpleTrace(pInfo, debugswitch, linec);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("parse trace crashed");
				System.out.println("Test name is: " + tChunk.fullname);
				pInfo.debugprint();
				// System.exit(0);
				throw (e);
			}
		}
		if (auto_oracle) {
			for (Node i : lastDefinedVar) {
				i.observe(testpass);
				if (debug_logger_switch)
					graphLogger.writeln("Observe %s as %b", i.name, testpass);
			}
		}
	}

	private void buildThrowException() {
		Node exceptDef = addNewExceptionNode(throwStmt);
		buildFactor(exceptDef, throwpred, throwuse, null, throwStmt);
		this.cleanThrow();
	}

	private void maintainStackframeUpward(ParseInfo pInfo) {
		TraceDomain curDomain = pInfo.domain;
		TraceDomain frameDomain = this.getFrame().domain;
		while (!curDomain.equals(frameDomain)) {
			this.stackframe.pop();
			frameDomain = this.getFrame().domain;
		}
	}

	private void buildUntracedInvokeException() {
		Node exceptDef = addNewExceptionNode(untracedStmt);
		buildFactor(exceptDef, untracedpred, untraceduse, null, untracedStmt);
		this.cleanUntraced();
	}

	private void buildUntracedInvoke() {
		String desc = this.untracedInvoke.getvalue("calltype");
		List<Node> toadd = new ArrayList<>();
		for (Node n : this.untraceduse) {
			if (n.isHeapObject()) {
				Node nd = getObjectNode(n);
				if (nd != null)
					toadd.add(nd);
			}
		}
		this.untraceduse.addAll(toadd);
		if (!OpcodeInst.isVoidMethodByDesc(desc)) {
			Node defnode = this.addNewStackNode(this.untracedStmt);
			if (OpcodeInst.isLongReturnMethodByDesc(desc)) {
				defnode.setSize(2);
			}
			buildFactor(defnode, this.untracedpred, this.untraceduse, null, this.untracedStmt);
		}
		for (Node n : this.untraceduse) {
			if (n.isHeapObject()) {
				buildFactorForAllField(n, this.untracedpred, this.untraceduse, null, this.untracedStmt);
			}
		}
		// build untraced object
		if (this.untracedObj != null && untracedObj.isHeapObject()) {
			Node obj = addNewObjectNode(this.untracedObj, this.untracedStmt);
			buildFactor(obj, this.untracedpred, this.untraceduse, null, this.untracedStmt);
		}
		this.cleanUntraced();
	}

	private void buildTracedInvoke() {
		String desc = this.tracedInvoke.getvalue("calltype");
		if (!OpcodeInst.isVoidMethodByDesc(desc)) {
			Node defnode = this.addNewStackNode(this.tracedStmt);
			buildFactor(defnode, this.tracedpred, this.traceduse, null, this.tracedStmt);
		} else {
			// if (this.tracedObj != null) {
			// Node defnode = this.getObjectNode(this.tracedObj);
			// buildFactor(defnode, this.tracedpred, this.traceduse, null, this.tracedStmt);
			// }
		}
		this.cleanTraced();
	}

	private void parseChunk(TraceChunk tChunk, TraceChunk inits) {
		this.cleanupOnChunkSwitch();
		int tracelength = tChunk.parsedTraces.size();
		// if (!tChunk.fullname.endsWith("testKeepInitIfBest")) {
		// return;
		// }
		System.out.println("parsing trace,length=" + tracelength + ":");
		System.out.println("\t" + tChunk.fullname);
		boolean limitSingleTrace = false;
		if (limitSingleTrace && tracelength > 100000 && tChunk.testpass) {
			System.out.println("Trace is too long, skipping");
			return;
		}
		boolean debugswitch = false;
		boolean testpass = tChunk.testpass;
		this.testname = tChunk.getTestName();

		boolean useStaticInit = true;
		// prepare static inits
		if (useStaticInit) {
			for (ParseInfo pInfo : inits.parsedTraces) {
				try {
					parseInitTrace(pInfo, debugswitch);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("parse trace crashed at init");
					pInfo.debugprint();
					throw (e);
				}
			}
		}

		this.emptyFrame();

		int linec = 0;
		for (ParseInfo pInfo : tChunk.parsedTraces) {
			try {
				// if (pInfo.linenumber == 94 && pInfo.byteindex == 0) {
				// debugswitch = true;
				// }
				linec++;
				parseSingleTrace(pInfo, debugswitch, linec);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("parse trace crashed at line " + linec);
				System.out.println("Test name is: " + tChunk.fullname);
				pInfo.debugprint();
				// System.out.println(this.getDomain());
				// System.exit(0);
				throw (e);
			}
		}
		// if crash occurs, lastDefinedVar should be modified with last call
		if (untracedInvoke != null) {
			System.out.println("end in untraced crash at" + tChunk.fullname);
			untracedInvoke.debugprint();
			Node exceptDef = addNewExceptionNode(untracedStmt);
			buildFactor(exceptDef, untracedpred, untraceduse, null, untracedStmt);
		}
		if (tracedInvoke != null) {
			System.out.println("end in traced crash at" + tChunk.fullname);
			tracedInvoke.debugprint();
			Node exceptDef = addNewExceptionNode(tracedStmt);
			buildFactor(exceptDef, tracedpred, traceduse, null, tracedStmt);
		}
		// after all lines are parsed, auto-assign oracle for the last defined var
		// with test state(pass = true,fail = false)
		if (auto_oracle) {
			for (Node i : lastDefinedVar) {
				i.observe(testpass);
				if (debug_logger_switch)
					graphLogger.writeln("Observe %s as %b", i.name, testpass);
			}
		}
	}

	public void parseJoinedTracePruned(JoinedTrace jTrace, boolean usesimple) {
		this.predstack.clear();
		this.initmaps();
		jTrace.sortChunk();
		int totalSize = 0;
		int thres = 1000000;// 1M lines
		for (TraceChunk tChunk : jTrace.traceList) {
			try {
				if (usesimple)
					parseSimpleChunk(tChunk);
				else {
					parseChunk(tChunk, jTrace.staticInits);
					totalSize += tChunk.parsedTraces.size();
				}
			} catch (Exception e) {
				System.err.println("parse " + tChunk.fullname + " failed");
				// e.printStackTrace();
				// throw (e);
			}
			if (totalSize > thres) {
				break;
			}
		}
	}

	public void parseJoinedTrace(String tracefilename) {
		this.predstack.clear();
		this.initmaps();
		try (BufferedReader reader = new BufferedReader(new FileReader(tracefilename))) {
			String t = null;
			boolean testpass = true;
			do {
				try {
					t = parseTraceFromReader(reader, t, testpass);
					System.out.println(t);
					testpass = getD4jTestState(t);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("parse joined trace crashed at " + t);
					throw (e);
				}
			} while (t != null);

			// while ((t = parseTraceFromReader(reader, t, testpass)) != null) {
			// // Debug use
			// System.out.println(t);
			// testpass = getD4jTestState(t);
			// }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String parseTraceFromReader(BufferedReader reader, String testname, boolean testpass) throws IOException {
		this.predstack.clear();
		if (testname != null)
			this.testname = testname.split("::")[1];
		this.initmaps();
		String t;
		String delimiterPrefix = "###";
		int linecounter = 0;
		while ((t = reader.readLine()) != null) {
			linecounter++;
			if (linecounter % 10000 == 0) {
				System.out.println(linecounter);
			}
			if (t.isEmpty()) {
				continue;
			}
			if (t.startsWith(delimiterPrefix)) {
				t = t.substring(delimiterPrefix.length());
				String[] splt = t.split("::");
				if (splt.length >= 2 && isD4jTestMethod(splt[0], splt[1])) {
					// after all lines are parsed, auto-assign oracle for the last defined var
					// with test state(pass = true,fail = false)
					if (auto_oracle) {
						for (Node i : lastDefinedVar) {
							i.observe(testpass);
							if (debug_logger_switch)
								graphLogger.writeln("Observe %s as %b", i.name, testpass);
						}
					}
					return t;
				}
				continue;
			}
			try {
				// Debug use
				// System.out.println(t);
				this.parseinfo = new ParseInfo(t);
				String instname = this.parseinfo.getvalue("lineinfo");
				killPredStack(instname);
				if (predataflowmap.get(instname).size() > 1) {
					// System.out.println("add set" + branch_stores.get(instname));
					Set<Integer> stores = branch_stores.get(instname);
					// if (stores != null)
					store_stack.push(stores);
				}
				Interpreter.map[this.parseinfo.form].buildtrace(this);
				// debug runtime stack
				// debugStack(this.stackframe);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("parse trace crashed at " + t);
				throw (e);
			}

		}
		// after all lines are parsed, auto-assign oracle for the last defined var
		// with test state(pass = true,fail = false)
		if (auto_oracle) {
			for (Node i : lastDefinedVar) {
				i.observe(testpass);
				graphLogger.writeln("Observe %s as %b", i.name, testpass);
			}
		}
		return null;
	}

	public void parsetrace(String tracefilename, String testname, boolean testpass) {
		this.predstack.clear();
		this.testname = testname;
		this.initmaps();
		try (BufferedReader reader = new BufferedReader(new FileReader(tracefilename))) {
			String t;
			while ((t = reader.readLine()) != null) {
				// System.err.println(t);
				if (t.isEmpty() || t.startsWith("###"))
					continue;
				this.parseinfo = new ParseInfo(t);
				// System.out.println(t);
				// System.out.println(this.parseinfo.getvalue("lineinfo"));
				String instname = this.parseinfo.getvalue("lineinfo");
				// System.out.println(instname);
				killPredStack(instname);
				if (predataflowmap.get(instname).size() > 1) {
					// System.out.println("add set" + branch_stores.get(instname));
					Set<Integer> stores = branch_stores.get(instname);
					// if (stores != null)
					store_stack.push(stores);
				}
				// debugLogger.info(this.parseinfo.form);
				Interpreter.map[this.parseinfo.form].buildtrace(this);
				// System.out.println("After "+this.parseinfo.getvalue("lineinfo")+" preds "+
				// predstack);
			}
			// after all lines are parsed, auto-assign oracle for the last defined var
			// with test state(pass = true,fail = false)
			if (auto_oracle) {
				for (Node i : lastDefinedVar) {
					i.observe(testpass);
					if (debug_logger_switch)
						graphLogger.writeln("Observe %s as %b", i.name, testpass);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Set<String> visited_for_stores = new HashSet<>();
	String theidom_for_stores;
	Set<Integer> thestores;

	private void dfs_for_stores(String inst) {
		if (inst.equals(theidom_for_stores))
			return;
		visited_for_stores.add(inst);
		Integer storen = store_num.get(inst);
		if (storen != null)
			thestores.add(storen);
		List<String> thenexts = predataflowmap.get(inst);
		for (String next : thenexts) {
			if (!visited_for_stores.contains(next))
				dfs_for_stores(next);
		}
	}

	public void get_stores() {
		// System.out.println(store_num);
		for (String inst : instset) {
			List<String> nextlist = predataflowmap.get(inst);
			// nextinst might be OUT_xx and there is no term in predataflowmap
			if (nextlist != null && nextlist.size() > 1) {
				theidom_for_stores = post_idom.get(inst);
				thestores = new HashSet<>();
				visited_for_stores.clear();
				dfs_for_stores(inst);
				branch_stores.put(inst, thestores);
			}
		}
		// System.out.println(branch_stores);
	}

	public FactorNode buildStmtFactor(StmtNode stmt, double value) {
		Edge sedge = new Edge();
		sedge.setnode(stmt);
		stmt.add_edge(sedge);
		FactorNode ret = new FactorNode(stmt, sedge, value);
		factornodes.add(ret);
		sedge.setfactor(ret);
		if (!shouldview)
			return ret;
		String factorname = "Factor" + factornodes.size();
		// viewgraph.addEdge(factorname + stmt.getPrintName(), factorname,
		// stmt.getPrintName());
		// org.graphstream.graph.Node outfactor = viewgraph.getNode(factorname);
		// outfactor.setAttribute("ui.class", "factor");
		// org.graphstream.graph.Node outstmt = viewgraph.getNode(stmt.getPrintName());
		// outstmt.setAttribute("ui.class", "stmt");
		// org.graphstream.graph.Edge outedge = viewgraph.getEdge(factorname +
		// stmt.getPrintName());
		// outedge.setAttribute("ui.class", "stmt");
		// outedge.setAttribute("layout.weight", 3);
		return ret;
	}

	public FactorNode buildFactor(Node defnode, Node prednode, Node usenode, List<String> ops, StmtNode stmt) {
		List<Node> prednodes = new ArrayList<>();
		prednodes.add(prednode);
		List<Node> usenodes = new ArrayList<>();
		usenodes.add(usenode);
		return buildFactor(defnode, prednodes, usenodes, ops, stmt);
	}

	public FactorNode buildFactor(Node defnode, List<Node> prednodes, List<Node> usenodes, List<String> ops,
			StmtNode stmt) {

		// substitution for heap object
		// for (int i = 0; i < usenodes.size(); i++) {
		// Node n = usenodes.get(i);
		// if (n.isHeapObject()) {
		// Node newNode = this.getObjectNode(n);
		// if (newNode == null) {
		// System.out.println("addr not found:" + n.getAddress());
		// }
		// usenodes.set(i, newNode);
		// }
		// }
		// if (defnode.isHeapObject()) {
		// defnode = this.addNewObjectNode(defnode, stmt);
		// }

		// FIXME this may conceal bugs in various insts.
		// temporary solution.
		if (compromise) {
			Iterator<Node> iter = usenodes.iterator();
			while (iter.hasNext()) {
				Node tmp = iter.next();
				if (tmp == null) {
					iter.remove();
				}
			}
		}

		if (auto_oracle && !stmt.getMethod().contentEquals(this.testname) && !stmt.isUnexe()) {
			int ln = stmt.getLineNumber();
			if (this.lastDefinedLine != ln) {
				this.lastDefinedLine = ln;
				this.lastDefinedVar.clear();
			}
			this.lastDefinedVar.add(defnode);
		}

		Edge dedge = new Edge();
		dedge.setnode(defnode);
		defnode.add_edge(dedge);
		defnode.setdedge(dedge);
		Edge sedge = new Edge();
		sedge.setnode(stmt);
		stmt.add_edge(sedge);

		List<Edge> pedges = new ArrayList<>();
		for (Node n : prednodes) {
			Edge nedge = new Edge();
			nedge.setnode(n);
			pedges.add(nedge);
			n.add_edge(nedge);
		}
		List<Edge> uedges = new ArrayList<>();
		for (Node n : usenodes) {
			Edge nedge = new Edge();
			nedge.setnode(n);
			uedges.add(nedge);
			n.add_edge(nedge);
		}
		FactorNode ret = new FactorNode(defnode, stmt, prednodes, usenodes, ops, dedge, sedge, pedges, uedges);
		factornodes.add(ret);

		dedge.setfactor(ret);
		sedge.setfactor(ret);
		for (Edge n : pedges)
			n.setfactor(ret);
		for (Edge n : uedges)
			n.setfactor(ret);
		if (!shouldview)
			return ret;
		String factorname = "Factor" + factornodes.size();
		// viewgraph.addEdge(factorname + stmt.getPrintName(), factorname,
		// stmt.getPrintName());
		// org.graphstream.graph.Node outfactor = viewgraph.getNode(factorname);
		// outfactor.setAttribute("ui.class", "factor");
		// org.graphstream.graph.Node outstmt = viewgraph.getNode(stmt.getPrintName());
		// outstmt.setAttribute("ui.class", "stmt");
		// org.graphstream.graph.Edge outedge = viewgraph.getEdge(factorname +
		// stmt.getPrintName());
		// outedge.setAttribute("ui.class", "stmt");
		// outedge.setAttribute("layout.weight", 3);

		// viewgraph.addEdge(factorname + defnode.getPrintName(), factorname,
		// defnode.getPrintName());
		// org.graphstream.graph.Node outdef =
		// viewgraph.getNode(defnode.getPrintName());

		// debugLogger.info("hhhhhhhhhhhui"+outdef.getId());
		// outdef.setAttribute("ui.class", "thenode");
		// outedge = viewgraph.getEdge(factorname + defnode.getPrintName());
		// outedge.setAttribute("ui.class", "def");
		// outedge.setAttribute("layout.weight", 2);
		// for (Node node : prednodes) {
		// viewgraph.addEdge(factorname + node.getPrintName(), factorname,
		// node.getPrintName());
		// org.graphstream.graph.Node outpred = viewgraph.getNode(node.getPrintName());
		// outpred.setAttribute("ui.class", "thenode");
		// outedge = viewgraph.getEdge(factorname + node.getPrintName());
		// outedge.setAttribute("ui.class", "pred");
		// outedge.setAttribute("layout.weight", 3);
		// }
		// for (Node node : usenodes) {
		// viewgraph.addEdge(factorname + node.getPrintName(), factorname,
		// node.getPrintName());
		// org.graphstream.graph.Node outuse = viewgraph.getNode(node.getPrintName());
		// outuse.setAttribute("ui.class", "thenode");
		// outedge = viewgraph.getEdge(factorname + node.getPrintName());
		// outedge.setAttribute("ui.class", "use");
		// outedge.setAttribute("layout.weight", 3);
		// }
		return ret;
	}

	public NWrongFactorNode buildNWrongFactor() {
		List<Edge> stmtedges = new ArrayList<>();
		for (Node n : stmts) {
			Edge theedge = new Edge();
			theedge.setnode(n);
			stmtedges.add(theedge);
			n.add_edge(theedge);
		}
		NWrongFactorNode ret = new NWrongFactorNode(stmtedges, 1);
		factornodes.add(ret);
		for (Edge e : stmtedges)
			e.setfactor(ret);

		return ret;
	}

	private void incStaticHeapIndex(String field) {
		String def = getFormalStaticHeapName(field);
		if (!staticheapcountmap.containsKey(def)) {
			staticheapcountmap.put(def, 1);
		} else {
			staticheapcountmap.put(def, staticheapcountmap.get(def) + 1);
		}
	}

	private void incHeapIndex(Node objectAddress, String field) {
		String def = getFormalHeapName(objectAddress, field);
		if (!heapcountmap.containsKey(def)) {
			heapcountmap.put(def, 1);
		} else {
			heapcountmap.put(def, heapcountmap.get(def) + 1);
		}
	}

	private void incObjectIndex(Node objectAddress) {
		String def = getFormalObjectName(objectAddress);
		if (!objectcountmap.containsKey(def)) {
			objectcountmap.put(def, 1);
		} else {
			objectcountmap.put(def, objectcountmap.get(def) + 1);
		}
	}

	private void incVarIndex(int varindex, TraceDomain domain) {
		assert (this.getFrame().domain.equals(domain));
		// assert (traceclass.equals(this.getFrame().traceclass));
		// assert (tracemethod.equals(this.getFrame().tracemethod));
		incVarIndex(varindex);
		// String def = this.getFormalVarName(varindex, traceclass, tracemethod);
		// if (!varcountmap.containsKey(def)) {
		// varcountmap.put(def, 1);
		// } else {
		// varcountmap.put(def, varcountmap.get(def) + 1);
		// }
	}

	private void incVarIndex(int varindex) {
		String def = this.getFormalVarName(varindex);
		if (!varcountmap.containsKey(def)) {
			varcountmap.put(def, 1);
		} else {
			varcountmap.put(def, varcountmap.get(def) + 1);
		}
	}

	private void incPredIndex(StmtNode stmt) {
		String def = this.getFormalPredName(stmt);
		if (!varcountmap.containsKey(def)) {
			varcountmap.put(def, 1);
		} else {
			varcountmap.put(def, varcountmap.get(def) + 1);
		}
	}

	private String getDomain() {
		return getFrame().getDomain();
	}

	private String getFormalStackName() {
		String domain = this.getDomain();
		return domain + "#Stack";
	}

	private String getFormalStackNameWithIndex() {
		// FIXME
		return getVarName(this.getFormalStackName(), this.getRuntimeStack().size());
	}

	private String getFormalVarName(int varindex, TraceDomain domain) {
		assert (this.getFrame().domain.equals(domain));
		// assert (traceclass.equals(this.getFrame().traceclass));
		// assert (tracemethod.equals(this.getFrame().tracemethod));
		return getFormalVarName(varindex);
		// String name = String.valueOf(varindex);
		// return this.getDomain() + name;
	}

	private String getFormalPredName(StmtNode stmt) {
		String name = stmt.getName();
		return "Pred#" + name;
	}

	private String getFormalPredNameWithIndex(StmtNode stmt) {
		return getVarName(getFormalPredName(stmt), this.varcountmap);
	}

	private String getFormalVarName(int varindex) {
		String name = String.valueOf(varindex);
		return this.getDomain() + name;
	}

	private String getFormalStaticHeapName(String field) {
		return field;
	}

	private String getFormalObjectName(Node objectAddress) {
		return String.format("%x", objectAddress.getAddress());
	}

	private void buildFactorForAllField(Node objectNode, List<Node> prednodes, List<Node> usenodes, List<String> ops,
			StmtNode stmt) {
		int addr = objectNode.getAddress();
		// System.out.println("building factor for addr:" + addr);
		if (!objectFieldMap.containsKey(addr)) {
			return;
		}
		Set<String> allfields = objectFieldMap.get(addr);
		for (String field : allfields) {
			Node defnode = addNewHeapNode(objectNode, field, stmt);
			buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
	}

	private void addField(int addr, String field) {
		if (objectFieldMap.containsKey(addr)) {
			objectFieldMap.get(addr).add(field);
		} else {
			Set<String> sset = new HashSet<>();
			sset.add(field);
			objectFieldMap.put(addr, sset);
		}
	}

	private String getFormalHeapName(Node objectAddress, String field) {
		addField(objectAddress.getAddress(), field);
		return String.format("%x.%s", objectAddress.getAddress(), field);
	}

	private String getFormalVarNameWithIndex(int varindex, TraceDomain domain) {
		return getVarName(getFormalVarName(varindex, domain), this.varcountmap);
	}

	private String getFormalVarNameWithIndex(int varindex) {
		return getVarName(getFormalVarName(varindex), this.varcountmap);
	}

	private String getFormalStaticHeapNameWithIndex(String field) {
		return getVarName(getFormalStaticHeapName(field), this.heapcountmap);
	}

	private String getFormalHeapNameWithIndex(Node objectAddress, String field) {
		return getVarName(getFormalHeapName(objectAddress, field), this.heapcountmap);
	}

	private String getFormalObjectNameWithIndex(Node objectAddress) {
		return getVarName(getFormalObjectName(objectAddress), this.objectcountmap);
	}

	private String getVarName(String name, Integer count) {
		if (count == null) {
			count = 0;
		}
		return name + "#" + count;
	}

	private String getVarName(String name, Map<String, Integer> map) {
		if (!map.containsKey(name) && debug_logger_switch) {
			graphLogger.writeln("varmap does not contains %s", name);
			graphLogger.writeln("map entries are %s", map);
		}
		return getVarName(name, map.get(name));
	}

	private String getNodeName(String name) {
		return this.testname + "#" + name;
	}

	private boolean hasNode(String name) {
		return nodemap.containsKey(getNodeName(name)) || stmtmap.containsKey(name);
	}

	public Node addNewExceptionNode(StmtNode stmt) {
		Node ret = this.addNewStackNode(stmt);
		return ret;
	}

	public Node addNewExceptionNode() {
		Node ret = this.addNewStackNode(this.untracedStmt);
		return ret;
	}

	public Node addNewStaticHeapNode(String field, StmtNode stmt) {
		this.incStaticHeapIndex(field);
		String nodename = this.getFormalStaticHeapNameWithIndex(field);
		Node node = new Node(nodename, this.testname, stmt);
		this.addNode(nodename, node);
		return node;
	}

	public Node getStaticHeapNode(String field) {
		return this.getNode(this.getFormalStaticHeapNameWithIndex(field));
	}

	public Node addNewHeapNode(Node objectAddress, String field, StmtNode stmt) {
		assert (objectAddress.isHeapObject());
		this.incHeapIndex(objectAddress, field);
		String nodename = this.getFormalHeapNameWithIndex(objectAddress, field);
		Node node = new Node(nodename, this.testname, stmt);
		this.addNode(nodename, node);
		return node;
	}

	public Node addNewObjectNode(Node objectAddress, StmtNode stmt) {
		assert (objectAddress.isHeapObject());
		this.incObjectIndex(objectAddress);
		String nodename = this.getFormalObjectNameWithIndex(objectAddress);
		Node node = new Node(nodename, this.testname, stmt);
		node.setAddress(objectAddress.getAddress());
		this.addNode(nodename, node);
		return node;
	}

	public Node addNewStackNode(StmtNode stmt) {
		// TODO stack node's name may get confused(same name, different node).
		// TODO currently works fine, but may bring difficulty to debugging.
		String nodename = this.getFormalStackNameWithIndex();
		Node node = new Node(nodename, this.testname, stmt);
		this.addNode(nodename, node);
		this.getRuntimeStack().push(node);
		return node;
	}

	public Node addNewVarNode(int varindex, StmtNode stmt) {
		this.incVarIndex(varindex);
		String nodename = this.getFormalVarNameWithIndex(varindex);
		Node defnode = new Node(nodename, this.testname, stmt);
		this.addNode(nodename, defnode);
		return defnode;
	}

	public Node addNewVarNode(int varindex, StmtNode stmt, TraceDomain domain) {
		this.incVarIndex(varindex, domain);
		String nodename = this.getFormalVarNameWithIndex(varindex, domain);
		Node defnode = new Node(nodename, this.testname, stmt);
		this.addNode(nodename, defnode);
		return defnode;
	}

	public Node addNewPredNode(StmtNode stmt) {
		this.incPredIndex(stmt);
		String nodename = this.getFormalPredNameWithIndex(stmt);
		Node defnode = new Node(nodename, this.testname, stmt);
		this.addNode(nodename, defnode);
		// TODO use interface to manipulate vector(predicates).
		this.predicates.add(defnode);
		return defnode;
	}

	public StmtNode getStmt(String stmtname, int form) {
		StmtNode stmt;
		if (!this.hasNode(stmtname)) {
			stmt = this.addNewStmt(stmtname, form);
		} else {
			stmt = (StmtNode) this.getNode(stmtname);
			assert (stmt != null && stmt.isStmt());
		}
		return stmt;
	}

	private StmtNode getUnexeStmt(StmtNode predstmt, int storeid) {
		StmtNode ret = predstmt.getUnexeStmtFromMap(storeid);
		if (ret == null) {
			ret = this.addNewStmt(predstmt.getUnexeName(storeid), -1);
			ret.setUnexe();
			predstmt.addUnexeStmt(storeid, ret);
		}
		return ret;
	}

	private StmtNode addNewStmt(String name, int form) {
		StmtNode stmt = new StmtNode(name, form);
		this.addNode(name, stmt);
		return stmt;
	}

	private void addNode(String name, Node node) {
		if (node instanceof StmtNode) {
			stmtmap.put(name, node);
			stmts.add((StmtNode) node);
		} else {
			nodemap.put(getNodeName(name), node);
			nodes.add(node);
		}
	}

	public Node getHeapNode(Node objectAddress, String field) {
		return this.getNode(this.getFormalHeapNameWithIndex(objectAddress, field));
	}

	public Node getObjectNode(Node objectAddress) {
		return this.getNode(this.getFormalObjectNameWithIndex(objectAddress));
	}

	public Node getLoadNodeAsUse(int loadvar) {
		return this.getNode(this.getFormalVarNameWithIndex(loadvar));
	}

	private Node getNode(String name) {
		if (nodemap.containsKey(getNodeName(name)))
			return nodemap.get(getNodeName(name));
		else if (stmtmap.containsKey(name))
			return stmtmap.get(name);
		else {
			// System.out.println("getnode return null:" + name);
			return null;
		}

	}

	public void solve(List<Node> allnodes, int cur, int tot) {
		if (cur == tot) {
			double product = 1.0;
			for (FactorNode n : factornodes) {
				product = product * n.getProb();// TODO consider log-add
			}
			for (Node n : nodes) {
				n.addimp(product);
			}
			for (Node n : stmts) {
				n.addimp(product);
			}
			return;
		}
		allnodes.get(cur).setTemp(true);
		solve(allnodes, cur + 1, tot);
		allnodes.get(cur).setTemp(false);
		solve(allnodes, cur + 1, tot);
	}

	public long bf_inference() {
		long startTime = System.currentTimeMillis();
		for (Node n : nodes) {
			n.init();
		}
		for (Node n : stmts) {
			n.init();
		}

		List<Node> allnodes = new ArrayList<>();
		allnodes.addAll(nodes);
		allnodes.addAll(stmts);
		int nnodes = allnodes.size();
		solve(allnodes, 0, nnodes);

		Comparator<Node> comp = (arg0, arg1) -> Double.compare(arg0.getprob(), arg1.getprob());
		nodes.sort(comp);
		stmts.sort(comp);

		long endTime = System.currentTimeMillis();
		return (endTime - startTime);
	}

	public void inference() {
		for (Node n : nodes) {
			n.init();
		}
		for (Node n : stmts) {
			n.init();
		}
		for (int i = 0; i < nsamples; i++) {
			for (Node n : nodes) {
				n.setTemp(random.nextBoolean());
			}
			for (Node n : stmts) {
				n.setTemp(random.nextBoolean());
			}
			double product = 1.0;
			for (FactorNode n : factornodes) {
				product = product * n.getProb();// TODO consider log-add
			}
			for (Node n : nodes) {
				n.addimp(product);
			}
			for (Node n : stmts) {
				n.addimp(product);
			}
		}

		Comparator<Node> comp = (arg0, arg1) -> Double.compare(arg0.getprob(), arg1.getprob());
		nodes.sort(comp);
		stmts.sort(comp);

	}

	public void mark_reduce(Node node) {
		Deque<Node> reducestack = new ArrayDeque<>();
		reducestack.push(node);
		node.setreduced();
		// resultLogger.writeln("node %s\n", node.getPrintName());
		while (!reducestack.isEmpty()) {
			Node reducNode = reducestack.pop();
			// resultLogger.writeln("pop %s\n", reducNode.getPrintName());
			FactorNode deffactor = (reducNode.getdedge()).getfactor();
			deffactor.getstmt().setreduced();
			// resultLogger.writeln("stmt %s\n", deffactor.getstmt().getPrintName());
			List<Node> pulist = deffactor.getpunodes();
			for (Node n : pulist) {
				if (n.getreduced() == true && !n.isStmt) {
					n.setreduced();
					// resultLogger.writeln("node %s\n", n.getPrintName());
					reducestack.push(n);
				}
			}
		}
		// if (node.getreduced() == false)
		// return;
		// node.setreduced();
		// if (node.isStmt)
		// return;
		// FactorNode deffactor = (node.getdedge()).getfactor();
		// List<Node> pulist = deffactor.getpunodes();
		// for (Node n : pulist) {
		// mark_reduce(n);
		// }
		// deffactor.getstmt().setreduced();
	}

	public void path_reduce() {
		for (Node n : nodes) {
			if (n.getobs()) {
				if (n.getreduced())
					mark_reduce(n);
			}
		}
		// maybe won't be used?
		for (Node n : stmts) {
			if (n.getobs()) {
				n.setreduced();
			}
		}
	}

	// TODO: how to slice in the graph to optimize the procedure, though the stmt
	// result can be cut as the mark
	public void node_reduce() {

	}

	public long bp_inference() {
		long startTime = System.currentTimeMillis();
		path_reduce();
		boolean outreduced = true;
		if (outreduced && debug_logger_switch) {
			graphLogger.writeln("\nReduced Nodes: ");
			for (Node n : stmts) {
				if (n.getreduced())
					n.print(graphLogger);
			}
			for (Node n : nodes) {
				if (n.getreduced())
					n.print(graphLogger);
			}
			graphLogger.writeln("\n");
		}

		if (outreduced) {
			reduceLogger.writeln("\nReduced Nodes: ");
			for (Node n : stmts) {
				if (n.getreduced())
					n.print(reduceLogger);
			}
			// for (Node n : nodes) {
			// if (n.getreduced())
			// n.print(reduceLogger);
			// }
			reduceLogger.writeln("\n");
		}

		boolean loopend = false;
		for (int i = 0; i < bp_times; i++) {
			boolean isend = true;
			for (FactorNode n : factornodes) {
				n.send_message();
			}
			for (Node n : nodes) {
				if (n.send_message())
					isend = false;
			}
			for (Node n : stmts) {
				if (n.send_message())
					isend = false;
			}
			if (isend) {
				if (debug_logger_switch)
					graphLogger.writeln("\n\n%d\n\n", i);
				resultLogger.writeln("Belief propagation time: %d\n", i);
				loopend = true;
				break;
			}
		}
		if (!loopend)
			resultLogger.writeln("Belief propagation time: %d\n", bp_times);
		boolean use_ap = false;
		Comparator<Node> comp;
		// if (!use_ap)
		comp = (arg0, arg1) -> Double.compare(arg0.bp_getprob(), arg1.bp_getprob());
		// else
		// comp = (arg0, arg1) ->
		// (arg0.ap_bp_getprob().compareTo(arg1.ap_bp_getprob()));
		nodes.sort(comp);
		stmts.sort(comp);

		long endTime = System.currentTimeMillis();
		return (endTime - startTime);
	}

	public StmtNode getTopStmt() {// should be called after inference()
		return stmts.get(0);
	}

	public List<StmtNode> getTopkStmt(int k) {
		return stmts.subList(0, k);
	}

	public void printgraph() {
		graphLogger.writeln("\nNodes: stmt=%d,node=%d", stmts.size(), nodes.size());
		for (Node n : stmts) {
			n.print(graphLogger);
		}
		for (Node n : nodes) {
			n.print(graphLogger);
		}
		graphLogger.writeln("Factors: %d", factornodes.size());
		for (FactorNode n : factornodes) {
			n.print(graphLogger);
		}
	}

	public void printprobs() {
		graphLogger.writeln("\nProbabilities: ");
		graphLogger.writeln("Vars:%d", nodes.size());
		for (Node n : nodes) {
			n.printprob();
		}
		graphLogger.writeln("Stmts:%d", stmts.size());
		for (StmtNode n : stmts) {
			n.printprob();
		}
	}

	public void bp_printprobs() {
		graphLogger.writeln("\nProbabilities: ");
		graphLogger.writeln("Vars:%d", nodes.size());
		for (Node n : nodes) {
			n.bpPrintProb();
		}
		graphLogger.writeln("Stmts:%d", stmts.size());
		for (StmtNode n : stmts) {
			n.bpPrintProb();
		}
	}

	public double getdiff(double a, double b) {
		double max = Math.max(Math.abs(a), Math.abs(b));
		return max == 0.0 ? 0 : Math.abs(a - b) / max;
	}

	public void check_bp(boolean verbose) {
		long bptime = this.bp_inference();
		resultLogger.writeln("\nProbabilities: ");
		resultLogger.writeln("Vars:%d", nodes.size());
		// Node.setLogger(resultLogger);

		int cnt = 0;
		for (Node n : nodes) {
			if (!verbose) {
				cnt++;
				if (cnt > 10)
					break;
			}
			n.bpPrintProb();
		}
		resultLogger.writeln("Stmts:%d", stmts.size());
		for (StmtNode n : stmts) {
			if (!this.resultFilter || !n.getreduced())
				n.bpPrintProb(resultLogger);
		}
		resultLogger.writeln("Belief propagation time : %fs", bptime / 1000.0);
		// resultLogger.flush();
	}

	public double check_bp_with_bf(boolean verbose) {
		double maxdiff = 0;
		String diffname = null;
		double maxdiffstmt = 0;
		String diffnamestmt = null;
		long bftime = this.bf_inference();
		long bptime = this.bp_inference();
		if (verbose) {
			graphLogger.writeln("\nProbabilities: ");
			graphLogger.writeln("Vars:%d", nodes.size());
		}
		for (Node n : nodes) {
			if (verbose) {
				n.printprob();
				n.bpPrintProb();
			}
			if (!n.obs) {
				double diff = getdiff(n.bp_getprob(), n.getprob());
				if (diff > maxdiff) {
					maxdiff = diff;
					diffname = n.getName();
				}

			}
		}
		if (verbose)
			graphLogger.writeln("Stmts:{}", stmts.size());
		for (StmtNode n : stmts) {
			if (verbose) {
				n.printprob();
				n.bpPrintProb();
			}
			if (!n.obs) {
				double diff = getdiff(n.bp_getprob(), n.getprob());
				if (diff > maxdiffstmt) {
					maxdiffstmt = diff;
					diffnamestmt = n.getName();
				}
			}
		}
		if (verbose) {
			graphLogger.writeln("Var max relative difference:%f at %s", maxdiff, diffname);
			graphLogger.writeln("Stmt max relative difference:%f at %s", maxdiffstmt, diffnamestmt);
			graphLogger.writeln("Brute force time : %fs", bftime / 1000.0);
			graphLogger.writeln("Belief propagation time : %fs", bptime / 1000.0);
		}

		return maxdiff;

	}

	// deprecated.
	public void observe(String s, boolean v) {
		String name = getNodeName(s);
		boolean valid = false;
		for (Node n : nodes) {
			if (n.getName().equals(name)) {
				valid = true;
				graphLogger.writeln("Node observed as %b", v);
				n.observe(v);
			}
		}
		for (Node n : stmts) {
			if (n.getName().equals(s)) {
				valid = true;
				graphLogger.writeln("Stmt observed as %b", v);
				n.observe(v);
			}
		}
		if (!valid) {
			graphLogger.writeln("Invalid Observe");
		}
	}

	public static String readFileToString(String filePath) {
		StringBuilder fileData = new StringBuilder(1000);
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			char[] buf = new char[10];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileData.toString();
	}

	public void initD4jProject(String project, int id) {
		this.useD4jTest = true;
		String triggerTests = null;
		String relevantClasses = null;
		String allTestMethods = null;
		String allTestClasses = null;
		String configpath = String.format("d4j_resources/metadata_cached/%s%d.log", project, id);
		try (BufferedReader reader = new BufferedReader(new FileReader(configpath));) {
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

		if (relevantClasses != null) {
			for (String s : relevantClasses.split(";")) {
				if (!s.isEmpty()) {
					// this.addTracedDomain(s);
					this.parseD4jSource(project, id, s);
				}
			}
		}
		if (allTestClasses != null) {
			for (String s : allTestClasses.split(";")) {
				if (!s.isEmpty()) {
					// this.addTracedDomain(s);
					this.parseD4jSource(project, id, s);
				}
			}
		}
		if (triggerTests != null) {
			for (String s : triggerTests.split(";")) {
				if (!s.isEmpty())
					this.d4jTriggerTestNames.add(s);
			}
		}
		if (allTestMethods != null) {
			for (String s : allTestMethods.split(";")) {
				if (!s.isEmpty())
					this.d4jMethodNames.add(s);
			}
		}
		// long startTime = System.currentTimeMillis();
		this.get_idom();
		this.get_stores();
		// long endTime = System.currentTimeMillis();
		// long thetime = endTime-startTime;
		// System.out.println("idom time is "+ thetime);
		String checkoutbase = GraphBuilder.getCheckoutBase();
		String tracefilename = String.format("%s/%s/%s/trace/logs/mytrace/all.log", checkoutbase, project, id);
		this.parseJoinedTrace(tracefilename);
		// this.parseD4jTrace(tracefilename);
	}

	public void initFromConfigFile(String baseDir, String configpath) {
		String sourcepath = null;
		String tracepath = null;
		String traceclass = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(configpath));) {
			String tmp;
			while ((tmp = reader.readLine()) != null) {
				String[] splt = tmp.split("=");
				if (splt[0].equals("tracepath")) {
					tracepath = splt[1];
				}
				if (splt[0].equals("sourcepath")) {
					sourcepath = splt[1];
				}
				if (splt[0].equals("tracedclass")) {
					traceclass = splt[1];
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (traceclass != null) {
			for (String s : traceclass.split(";")) {
				// if (!s.isEmpty())
				// this.addTracedDomain(s);
			}
		}
		if (sourcepath != null) {
			for (String s : sourcepath.split(";")) {
				if (!s.isEmpty())
					this.parsesource(baseDir + s);
			}
			// long startTime = System.currentTimeMillis();
			this.get_idom();
			this.get_stores();
			// long endTime = System.currentTimeMillis();
			// long thetime = endTime-startTime;
			// System.out.println("idom time is "+ thetime);

		}
		if (tracepath != null)
			for (String s : tracepath.split(";")) {
				if (s.isEmpty())
					continue;
				String[] tmp = s.split(":");
				String testpath = tmp[0];
				String testClassAndMethod = testpath.substring(0, testpath.lastIndexOf('.'));
				String testMethod = testClassAndMethod.substring(testClassAndMethod.lastIndexOf('.') + 1,
						testClassAndMethod.length());
				boolean testpass = tmp[1].equals("1");
				this.parsetrace(baseDir + tmp[0], testMethod, testpass);
			}
	}

}
