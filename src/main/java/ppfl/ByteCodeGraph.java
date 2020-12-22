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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.graphstream.graph.implementations.SingleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ppfl.instrumentation.Interpreter;
import ppfl.instrumentation.RuntimeFrame;

public class ByteCodeGraph {

	private static Logger graphLogger = LoggerFactory.getLogger("Debugger");
	private static Logger resultLogger = LoggerFactory.getLogger("Debugger");

	public static void setGraphLogger(String graphfile) {
		MDC.put("graphfile", graphfile);
		graphLogger = LoggerFactory.getLogger("GraphLogger");
	}

	public static void setResultLogger(String resultfile) {
		MDC.put("resultfile", resultfile);
		resultLogger = LoggerFactory.getLogger("ResultLogger");
	}

	private static Set<String> tracedClass = new HashSet<>();
	private boolean traceAllClasses = true;

	public void setTraceAllClassed(boolean value) {
		this.traceAllClasses = value;
	}

	public void addTracedClass(String className) {
		tracedClass.add(className);
	}

	public void addTracedClass(Collection<String> className) {
		tracedClass.addAll(className);
	}

	public boolean isTraced(String className) {
		if (this.traceAllClasses)
			return true;
		return tracedClass.contains(className);

	}

	public List<FactorNode> factornodes;
	public List<Node> nodes;
	public List<StmtNode> stmts;
	public Map<String, Node> nodemap;
	public Map<String, Node> stmtmap;
	public Map<String, Integer> varcountmap;
	public Map<String, Integer> stmtcountmap;
	public org.graphstream.graph.Graph viewgraph;
	public Set<String> instset;
	public Set<String> outset;
	public Map<String, List<String>> predataflowmap;
	public Map<String, List<String>> postdataflowmap;
	public Map<String, Set<String>> dataflowsets;
	public Map<String, String> post_idom;
	public Map<String, Integer> store_num;
	public Map<String, Set<Integer>> branch_stores;
	private boolean shouldview;

	public void stopview() {
		shouldview = false;
	}

	// if max_loop is set to negative, then no limit is set(unlimited loop
	// unfolding)
	public int max_loop;

	public String testname;

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
	public void pushStackFrame(String tclass, String tmethod) {
		RuntimeFrame topush = RuntimeFrame.getFrame(tclass, tmethod);
		topush.entercnt++;
		this.stackframe.push(topush);
	}

	public RuntimeFrame getFrame() {
		if (stackframe.isEmpty()) {
			stackframe.push(RuntimeFrame.getFrame(parseinfo.traceclass, parseinfo.tracemethod));
		}
		return stackframe.peek();
	}

	public Deque<Node> getRuntimeStack() {
		return getFrame().runtimestack;
	}

	// local vars used by parsing
	public ParseInfo parseinfo;

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

	//the stack for stores in branchs
	private Deque<Set<Integer>> store_stack;

	public void killPredStack(String thisinst) {
		boolean willcontinue = true;
		while (willcontinue) {
			willcontinue = false;
			if (this.predstack.peek() != null) {
				String stmtName = this.predstack.peek().getStmtName();
				// System.out.println("in kill "+stmtName);
				if (post_idom.get(stmtName).equals(thisinst)) {
					this.predstack.pop();
					Set<Integer> stores;
					if(!this.store_stack.isEmpty())
						stores = this.store_stack.pop();
					// System.out.println("kill "+stores);
					//TODO make unexecuted complement
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
		instset = new HashSet<>();
		outset = new HashSet<>();
		predataflowmap = new HashMap<>();
		postdataflowmap = new HashMap<>();
		dataflowsets = new TreeMap<>();
		post_idom = new TreeMap<>();
		branch_stores = new TreeMap<>();
		store_num = new HashMap<>();
		max_loop = -1;
		random = new Random();
		auto_oracle = true;
		stackframe = new ArrayDeque<>();
		predstack = new ArrayDeque<>();
		store_stack = new ArrayDeque<>();
		viewgraph = new SingleGraph("Outgraph");
		viewgraph.setStrict(false);
		viewgraph.setAutoCreate(true);
		viewgraph.setAttribute("layout.quality", 4);
		viewgraph.setAttribute("layout.force");
		shouldview = true;
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
		viewgraph.setAttribute("ui.stylesheet", styleSheet);
		viewgraph.setAttribute("ui.quality");
		viewgraph.setAttribute("ui.antialias");
		Interpreter.init();
	}

	public void addviewlabel() {
		for (Node n : nodes) {
			org.graphstream.graph.Node thenode = viewgraph.getNode(n.getPrintName());
			if (thenode != null)
				thenode.setAttribute("ui.label", " prob_bp = " + (double) Math.round(n.bp_getprob() * 1000) / 1000);
			// thenode.setAttribute("ui.label", n.getPrintName() + " prob_bp = " +
			// n.bp_getprob());
		}
		for (StmtNode n : stmts) {
			org.graphstream.graph.Node thenode = viewgraph.getNode(n.getPrintName());
			if (thenode != null)
				thenode.setAttribute("ui.label", " prob_bp = " + (double) Math.round(n.bp_getprob() * 1000) / 1000);
			// thenode.setAttribute("ui.label", n.getPrintName() + " prob_bp = " +
			// n.bp_getprob());
		}
	}

	public void setMaxLoop(int i) {
		this.max_loop = i;
	}

	public void setAutoOracle(boolean b) {
		this.auto_oracle = b;
	}

	public void initmaps() {
		// TODO this could be incomplete.
		this.varcountmap = new HashMap<>();
		this.stackframe = new ArrayDeque<>();
		this.predicates.clear();
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
			// TODO consider throw
			Set<String> switchinsts = new HashSet<>();
			switchinsts.add("tableswitch");
			switchinsts.add("lookupswitch");
			while ((t = reader.readLine()) != null) {
				if (t.isEmpty() || t.startsWith("###"))
					continue;
				ParseInfo info = new ParseInfo(t);
				String thisinst = info.getvalue("lineinfo");
				Integer storen = info.getintvalue("store");
				if(storen != null)
					store_num.put(thisinst,storen);
				String classandmethod = info.traceclass + "#" + info.tracemethod;
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
		visited.add(inst);
		List<String> thesuccs = postdataflowmap.get(inst);
		for (String succ : thesuccs) {
			if (!visited.contains(succ)) {
				dfssearch(succ);
			}
		}
		reverse_postorder.addFirst(inst);
		postorder.put(inst, new Integer(cnt));
		cnt++;
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
				if(predataflowmap.get(instname).size()>1){
					// System.out.println("add set" + branch_stores.get(instname));
					Set<Integer> stores = branch_stores.get(instname);
					if(stores != null)
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
					graphLogger.info("Observe {} as {}", i.name, testpass);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	private Set<String> visited_for_stores = new HashSet<>();
	String theidom_for_stores;
	Set<Integer> thestores;
	private void dfs_for_stores(String inst){
		if(inst.equals(theidom_for_stores))
			return;
		visited_for_stores.add(inst);
		Integer storen = store_num.get(inst);
		if(storen != null)
			thestores.add(storen);
		List<String> thenexts = predataflowmap.get(inst);
		for(String next : thenexts){
			if(!visited_for_stores.contains(next))
				dfs_for_stores(next);
		}
	}

	public void get_stores(){
		// System.out.println(store_num);
		for(String inst: instset){
			List<String> nextlist = predataflowmap.get(inst);
			//nextinst might be OUT_xx and there is no term in predataflowmap
			if(nextlist!=null && nextlist.size()>1){
				theidom_for_stores = post_idom.get(inst);
				thestores = new HashSet<>();
				visited_for_stores.clear();
				dfs_for_stores(inst);
				branch_stores.put(inst, thestores);
			}
		}
		// System.out.println(branch_stores);
	}

	public FactorNode buildStmtFactor(StmtNode stmt, double value){
		Edge sedge = new Edge();
		sedge.setnode(stmt);
		stmt.add_edge(sedge);
		FactorNode ret = new FactorNode(stmt, sedge, value);
		factornodes.add(ret);
		sedge.setfactor(ret);
		if (!shouldview)
			return ret;
		String factorname = "Factor" + factornodes.size();
		viewgraph.addEdge(factorname + stmt.getPrintName(), factorname, stmt.getPrintName());
		org.graphstream.graph.Node outfactor = viewgraph.getNode(factorname);
		outfactor.setAttribute("ui.class", "factor");
		org.graphstream.graph.Node outstmt = viewgraph.getNode(stmt.getPrintName());
		outstmt.setAttribute("ui.class", "stmt");
		org.graphstream.graph.Edge outedge = viewgraph.getEdge(factorname + stmt.getPrintName());
		outedge.setAttribute("ui.class", "stmt");
		outedge.setAttribute("layout.weight", 3);
		return ret;
	}

	public FactorNode buildFactor(Node defnode, List<Node> prednodes, List<Node> usenodes, List<String> ops,
			StmtNode stmt) {

		if (auto_oracle && !stmt.getMethod().contentEquals(this.testname)) {
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
		viewgraph.addEdge(factorname + stmt.getPrintName(), factorname, stmt.getPrintName());
		org.graphstream.graph.Node outfactor = viewgraph.getNode(factorname);
		outfactor.setAttribute("ui.class", "factor");
		org.graphstream.graph.Node outstmt = viewgraph.getNode(stmt.getPrintName());
		outstmt.setAttribute("ui.class", "stmt");
		org.graphstream.graph.Edge outedge = viewgraph.getEdge(factorname + stmt.getPrintName());
		outedge.setAttribute("ui.class", "stmt");
		outedge.setAttribute("layout.weight", 3);

		viewgraph.addEdge(factorname + defnode.getPrintName(), factorname, defnode.getPrintName());
		org.graphstream.graph.Node outdef = viewgraph.getNode(defnode.getPrintName());

		// debugLogger.info("hhhhhhhhhhhui"+outdef.getId());
		outdef.setAttribute("ui.class", "thenode");
		outedge = viewgraph.getEdge(factorname + defnode.getPrintName());
		outedge.setAttribute("ui.class", "def");
		outedge.setAttribute("layout.weight", 2);
		for (Node node : prednodes) {
			viewgraph.addEdge(factorname + node.getPrintName(), factorname, node.getPrintName());
			org.graphstream.graph.Node outpred = viewgraph.getNode(node.getPrintName());
			outpred.setAttribute("ui.class", "thenode");
			outedge = viewgraph.getEdge(factorname + node.getPrintName());
			outedge.setAttribute("ui.class", "pred");
			outedge.setAttribute("layout.weight", 3);
		}
		for (Node node : usenodes) {
			viewgraph.addEdge(factorname + node.getPrintName(), factorname, node.getPrintName());
			org.graphstream.graph.Node outuse = viewgraph.getNode(node.getPrintName());
			outuse.setAttribute("ui.class", "thenode");
			outedge = viewgraph.getEdge(factorname + node.getPrintName());
			outedge.setAttribute("ui.class", "use");
			outedge.setAttribute("layout.weight", 3);
		}
		return ret;
    }
    
    public NWrongFactorNode buildNWrongFactor(){
        List<Edge> stmtedges = new ArrayList<>();
        for (Node n : stmts) {
			Edge theedge = new Edge();
			theedge.setnode(n);
			stmtedges.add(theedge);
			n.add_edge(theedge);
		}
        NWrongFactorNode ret = new NWrongFactorNode(stmtedges,1);
        factornodes.add(ret);
        for (Edge e : stmtedges)
            e.setfactor(ret);
            
        return ret;
    } 

	private void incVarIndex(int varindex, String traceclass, String tracemethod) {
		assert (traceclass.equals(this.getFrame().traceclass));
		assert (tracemethod.equals(this.getFrame().tracemethod));
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

	private String getFormalVarName(int varindex, String traceclass, String tracemethod) {
		assert (traceclass.equals(this.getFrame().traceclass));
		assert (tracemethod.equals(this.getFrame().tracemethod));
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

	private String getFormalVarNameWithIndex(int varindex, String traceclass, String tracemethod) {
		return getVarName(getFormalVarName(varindex, traceclass, tracemethod), this.varcountmap);
	}

	private String getFormalVarNameWithIndex(int varindex) {
		return getVarName(getFormalVarName(varindex), this.varcountmap);
	}

	private String getVarName(String name, Integer count) {
		if (count == null) {
			count = 0;
		}
		return name + "#" + count;
	}

	private String getVarName(String name, Map<String, Integer> map) {
		if (!map.containsKey(name)) {
			graphLogger.info("varmap does not contains {}", name);
			graphLogger.info("map entries are {}", map);
		}
		return getVarName(name, map.get(name));
	}

	private String getNodeName(String name) {
		return this.testname + "#" + name;
	}

	private boolean hasNode(String name) {
		return nodemap.containsKey(getNodeName(name)) || stmtmap.containsKey(name);
	}

	public Node addNewStackNode(StmtNode stmt) {
		// TODO stack node's name may get confused(same name, different node).
		// TODO currently works fine, but may bring difficulty to debugging.
		String nodename = this.getFormalStackNameWithIndex();
		Node node = new Node(nodename, this.testname, stmt);
		this.addNode(nodename, node);
		this.getRuntimeStack().add(node);
		return node;
	}

	public Node addNewVarNode(int varindex, StmtNode stmt) {
		this.incVarIndex(varindex);
		String nodename = this.getFormalVarNameWithIndex(varindex);
		Node defnode = new Node(nodename, this.testname, stmt);
		this.addNode(nodename, defnode);
		return defnode;
	}

	public Node addNewVarNode(int varindex, StmtNode stmt, String traceclass, String tracemethod) {
		this.incVarIndex(varindex, traceclass, tracemethod);
		String nodename = this.getFormalVarNameWithIndex(varindex, traceclass, tracemethod);
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

	public StmtNode getStmt(String stmtname) {
		StmtNode stmt;
		if (!this.hasNode(stmtname)) {
			stmt = this.addNewStmt(stmtname);
		} else {
			stmt = (StmtNode) this.getNode(stmtname);
			assert (stmt != null && stmt.isStmt());
		}
		return stmt;
	}

	private StmtNode addNewStmt(String name) {
		StmtNode stmt = new StmtNode(name);
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

	public Node getLoadNodeAsUse(int loadvar) {
		return this.getNode(this.getFormalVarNameWithIndex(loadvar));
	}

	private Node getNode(String name) {
		if (nodemap.containsKey(getNodeName(name)))
			return nodemap.get(getNodeName(name));
		else if (stmtmap.containsKey(name))
			return stmtmap.get(name);
		else
			return null;
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
		if(node.getreduced() == false)
			return;
		node.setreduced();
		if (node.isStmt)
			return;
		FactorNode deffactor = (node.getdedge()).getfactor();
		List<Node> pulist = deffactor.getpunodes();
		for (Node n : pulist) {
			mark_reduce(n);
		}
		deffactor.getstmt().setreduced();
	}

	public void path_reduce() {
		for (Node n : nodes) {
			if (n.getobs()) {
				mark_reduce(n);
			}
		}
		// maybe won't be used?
		for (Node n : stmts) {
			if (n.getobs()) {
				mark_reduce(n);
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
		if (outreduced) {
			graphLogger.info("\nReduced Nodes: ");
			for (Node n : stmts) {
				if (n.getreduced())
					n.print(graphLogger);
			}
			for (Node n : nodes) {
				if (n.getreduced())
					n.print(graphLogger);
			}
			graphLogger.info("\n");
		}

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
				graphLogger.info("\n\n{}\n\n", i);
				break;
			}
		}
		Comparator<Node> comp = (arg0, arg1) -> Double.compare(arg0.bp_getprob(), arg1.bp_getprob());
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
		graphLogger.info("\nNodes: stmt={},node={}", stmts.size(), nodes.size());
		for (Node n : stmts) {
			n.print(graphLogger);
		}
		for (Node n : nodes) {
			n.print(graphLogger);
		}
		graphLogger.info("Factors: {}", factornodes.size());
		for (FactorNode n : factornodes) {
			n.print(graphLogger);
		}
	}

	public void printprobs() {
		graphLogger.info("\nProbabilities: ");
		graphLogger.info("Vars:{}", nodes.size());
		for (Node n : nodes) {
			n.printprob();
		}
		graphLogger.info("Stmts:{}", stmts.size());
		for (StmtNode n : stmts) {
			n.printprob();
		}
	}

	public void bp_printprobs() {
		graphLogger.info("\nProbabilities: ");
		graphLogger.info("Vars:{}", nodes.size());
		for (Node n : nodes) {
			n.bpPrintProb();
		}
		graphLogger.info("Stmts:{}", stmts.size());
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
		resultLogger.info("\nProbabilities: ");
		resultLogger.info("Vars:{}", nodes.size());
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
		resultLogger.info("Stmts:{}", stmts.size());
		for (StmtNode n : stmts) {
			n.bpPrintProb(resultLogger);
		}
		resultLogger.info("Belief propagation time : {}s", bptime / 1000.0);
	}

	public double check_bp_with_bf(boolean verbose) {
		double maxdiff = 0;
		String diffname = null;
		double maxdiffstmt = 0;
		String diffnamestmt = null;
		long bftime = this.bf_inference();
		long bptime = this.bp_inference();
		if (verbose) {
			graphLogger.info("\nProbabilities: ");
			graphLogger.info("Vars:{}", nodes.size());
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
			graphLogger.info("Stmts:{}", stmts.size());
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
			graphLogger.info("Var max relative difference:{} at {}", maxdiff, diffname);
			graphLogger.info("Stmt max relative difference:{} at {}", maxdiffstmt, diffnamestmt);
			graphLogger.info("Brute force time : {}s", bftime / 1000.0);
			graphLogger.info("Belief propagation time : {}s", bptime / 1000.0);
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
				graphLogger.info("Node observed as {}", v);
				n.observe(v);
			}
		}
		for (Node n : stmts) {
			if (n.getName().equals(s)) {
				valid = true;
				graphLogger.info("Stmt observed as {}", v);
				n.observe(v);
			}
		}
		if (!valid) {
			graphLogger.info("Invalid Observe");
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
                if(!s.isEmpty())
				this.addTracedClass(s);
			}
		}
		if (sourcepath != null) {
			for (String s : sourcepath.split(";")) {
                if(!s.isEmpty())
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
                if(s.isEmpty())
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
