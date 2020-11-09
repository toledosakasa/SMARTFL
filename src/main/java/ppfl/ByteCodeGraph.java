package ppfl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ppfl.instrumentation.Interpreter;
import ppfl.instrumentation.RuntimeFrame;

import org.graphstream.graph.implementations.*;

public class ByteCodeGraph {

	private static Logger debugLogger = LoggerFactory.getLogger("Debugger");

	public List<FactorNode> factornodes;
	public List<Node> nodes;
	public List<StmtNode> stmts;
	public Map<String, Node> nodemap;
	public Map<String, Node> stmtmap;
	public Map<String, Integer> varcountmap;
	public Map<String, Integer> stmtcountmap;
	public org.graphstream.graph.Graph viewgraph;
	public Set<String> instset;
	public Map<String,List<String>> predataflowmap;
	public Map<String,List<String>> postdataflowmap;
	public Map<String,Set<String>> dataflowsets;
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
		this.stackframe.pop().runtimestack.clear();
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

	public ByteCodeGraph() {
		factornodes = new ArrayList<FactorNode>();
		nodes = new ArrayList<Node>();
		stmts = new ArrayList<StmtNode>();
		nodemap = new HashMap<String, Node>();
		stmtmap = new HashMap<String, Node>();
		varcountmap = new HashMap<String, Integer>();
		stmtcountmap = new HashMap<String, Integer>();
		instset = new HashSet<String>();
		predataflowmap = new HashMap<String, List<String>>();
		postdataflowmap = new HashMap<String, List<String>>();
		dataflowsets = new HashMap<String, Set<String>>();
		max_loop = -1;
		random = new Random();
		auto_oracle = true;
		stackframe = new ArrayDeque<>();
		viewgraph = new SingleGraph("Outgraph");
		viewgraph.setStrict(false);
		viewgraph.setAutoCreate(true);
		viewgraph.setAttribute("layout.quality", 4);
		viewgraph.setAttribute("layout.force");
		shouldview = true;
		String styleSheet = "node {" +
		// " text-background-mode: rounded-box;"+
				"\ttext-alignment: at-right;" + "\ttext-offset: 5px, 0px;" + "\ttext-style: italic;" + "\tsize: 15px, 15px;"
				+ "}" +
				// "node.thenode {" +
				// // " shape: box;"+
				// " size: 15px, 15px;"+
				// // " fill-color: green;" +
				// "}" +
				"node.factor {" + "\tshape: box;" + "\ttext-mode: hidden;" +
				// " size: 15px, 15px;"+
				"\tfill-color: red;" +
				// " stroke-mode: plain; /* Default is none. */"+
				// " stroke-color: blue; /* Default is black. */"+
				"}" + "node.stmt {" +
				// " shape: box;"+
				"\tsize: 10px, 10px;" + "\tfill-color: brown;" + "}" + "edge {" + "\tfill-color: red;" +
				// " layout.weight: 10;"+
				"}" + "edge.def {" + "\tfill-color: green;" + "}" + "edge.use {" + "\tfill-color: blue;" + "}" + "edge.pred {"
				+ "\tfill-color: yellow;" + "edge.stmt {" + "\tfill-color: black;" + "}";
		viewgraph.setAttribute("ui.stylesheet", styleSheet);
		viewgraph.setAttribute("ui.quality");
		viewgraph.setAttribute("ui.antialias");
		Interpreter.init();
	}

	public void addviewlabel() {
		for (Node n : nodes) {
			org.graphstream.graph.Node thenode = viewgraph.getNode(n.getPrintName());
			if (thenode != null)
				thenode.setAttribute("ui.label", n.getPrintName() + " prob_bp = " + n.bp_getprob());
		}
		for (StmtNode n : stmts) {
			org.graphstream.graph.Node thenode = viewgraph.getNode(n.getPrintName());
			if (thenode != null)
				thenode.setAttribute("ui.label", n.getPrintName() + " prob_bp = " + n.bp_getprob());
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
        Map<String,String> assistnamemap = new HashMap<String, String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(sourcefilename));
			String t;
			Set<String> nonextinsts = new HashSet<String>();
			nonextinsts.add("goto_w");
			nonextinsts.add("goto");
			nonextinsts.add("return");
			nonextinsts.add("areturn");
			nonextinsts.add("dreturn");
			nonextinsts.add("freturn");
			nonextinsts.add("ireturn");
			nonextinsts.add("lreturn");
			// TODO consider throw

			while ((t = reader.readLine()) != null) {
				if (t.isEmpty() || t.startsWith("###"))
					continue;
				ParseInfo info = new ParseInfo(t);
				String thisinst = info.getvalue("lineinfo");
				String classandmethod = info.traceclass +"#"+info.tracemethod;
				String assistkey = classandmethod + info.byteindex;
				assistnamemap.put(assistkey, thisinst);
				List<String> theedges = new ArrayList<String> ();
				if(!(nonextinsts.contains(info.opcode)))
				{
					String nextinst = classandmethod +info.getvalue("nextinst");
					theedges.add(nextinst);
				}
				Integer branchbyte = info.getintvalue("branchbyte");
				if(branchbyte != null)
				{
					String branchinst = classandmethod + (branchbyte.intValue() + info.byteindex);
					theedges.add(branchinst);
				}
				if(theedges.size()==0)
				{
					theedges.add("OUT_"+classandmethod);
					instset.add("OUT_"+classandmethod);
				}
				predataflowmap.put(thisinst,theedges);
				instset.add(thisinst);
			}
		} catch (IOException e) {
			e.printStackTrace();
        }
        //change the name (to include the line number)
        for(List<String> theedges : predataflowmap.values()){
            int valuelen = theedges.size();
            for(int i =0;i < valuelen;i++){
                if(assistnamemap.containsKey(theedges.get(i)))
                {
                    String newname = assistnamemap.get(theedges.get(i));
                    theedges.set(i, newname);
                }
            }
        }
        //init the postmap, keys including OUT_xx
        for(String instname : instset){
            List<String> theedges = new ArrayList<String> ();
            postdataflowmap.put(instname, theedges);
        }
        //get the postmap
        for(String instname : predataflowmap.keySet()){
            List<String> preedges = predataflowmap.get(instname);
            for(String prenode: preedges)
            {
                // System.out.println(preedges);
                // System.out.println(instname+"__"+prenode);
                List<String> postedges = postdataflowmap.get(prenode);
                postedges.add(instname);
            }
        }
        // init the dataflow set
        Set<String> changedset = new HashSet<String>();
        for (String instname : postdataflowmap.keySet()){
            if(instname.startsWith("OUT_"))
            {
                HashSet<String> emptyset = new HashSet<String>();
                emptyset.add(instname);
                dataflowsets.put(instname, emptyset);
            }
            else
            {
                HashSet<String> theallset = new HashSet<String>();
                theallset.addAll(instset);
                dataflowsets.put(instname, theallset);
                changedset.add(instname);
            }
        }

        while(!changedset.isEmpty()){	
            int changedsetsize = changedset.size();
            int item = new Random().nextInt(changedsetsize);
            int tmpindex = 0;
            String instname = "";
            for(String obj : changedset)
            {
                if (tmpindex == item)
                {
                    instname = obj;
                    break;
                }
                tmpindex++;
            }
            changedset.remove(instname);
            Set<String> oldset =  dataflowsets.get(instname);
            Set<String> newset = new HashSet<String>();
            newset.addAll(instset);
            List<String> succlist = predataflowmap.get(instname);
            for(String succinst : succlist){
                newset.retainAll (dataflowsets.get(succinst));
            }
            newset.add(instname);
            if(!((oldset.size()==newset.size())&&oldset.containsAll(newset)))
            {
                List<String> predlist = postdataflowmap.get(instname);
                for(String predinst : predlist){
                    changedset.add(predinst);
                }
            }
            dataflowsets.put(instname, newset);
        }
        boolean printdataflow = true;
        if(printdataflow){
            System.out.println("size =" + dataflowsets.size());
            for(String key : dataflowsets.keySet()){
                System.out.println("key_"+key);
                System.out.println(dataflowsets.get(key));
            }
        }
	}

	public void parsetrace(String tracefilename, String testname, boolean testpass) {
		this.testname = testname;
		this.initmaps();
		try (BufferedReader reader = new BufferedReader(new FileReader(tracefilename))) {
			String t;
			while ((t = reader.readLine()) != null) {
				if (t.isEmpty() || t.startsWith("###"))
					continue;
				this.parseinfo = new ParseInfo(t);
				// debugLogger.info(this.parseinfo.form);
				Interpreter.map[this.parseinfo.form].buildtrace(this);
			}
			// after all lines are parsed, auto-assign oracle for the last defined var
			// with test state(pass = true,fail = false)
			if (auto_oracle) {
				for (Node i : lastDefinedVar) {
					i.observe(testpass);
					debugLogger.info("Observe " + i.name + " as " + testpass);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public FactorNode buildFactor(Node defnode, List<Node> prednodes, List<Node> usenodes, List<String> ops,
			StmtNode stmt) {

		if (auto_oracle) {
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

	private String getVarName(String name, int count) {
		return name + "#" + count;
	}

	private String getVarName(String name, Map<String, Integer> map) {
		if (!map.containsKey(name))
			debugLogger.info(name);
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
			debugLogger.info("\nreduced Nodes: ");
			for (Node n : stmts) {
				if (n.getreduced())
					n.print();
			}
			for (Node n : nodes) {
				if (n.getreduced())
					n.print();
			}
			debugLogger.info("\n");
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
				debugLogger.info("\n\n" + i + "\n\n");
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
		debugLogger.info("\nNodes: ");
		for (Node n : stmts) {
			n.print();
		}
		for (Node n : nodes) {
			n.print();
		}
		debugLogger.info("Factors:");
		for (FactorNode n : factornodes) {
			n.print();
		}
	}

	public void printprobs() {
		debugLogger.info("\nProbabilities: ");
		debugLogger.info("Vars:" + nodes.size());
		for (Node n : nodes) {
			n.printprob();
		}
		debugLogger.info("Stmts:" + stmts.size());
		for (StmtNode n : stmts) {
			n.printprob();
		}
	}

	public void bp_printprobs() {
		debugLogger.info("\nProbabilities: ");
		debugLogger.info("Vars:" + nodes.size());
		for (Node n : nodes) {
			n.bpPrintProb();
		}
		debugLogger.info("Stmts:" + stmts.size());
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

		debugLogger.info("\nProbabilities: ");
		debugLogger.info("Vars:" + nodes.size());
		int cnt = 0;
		for (Node n : nodes) {
			if (!verbose) {
				cnt++;
				if (cnt > 10)
					break;
			}
			n.bpPrintProb();
		}
		debugLogger.info("Stmts:" + stmts.size());
		for (StmtNode n : stmts) {
			n.bpPrintProb();
		}
		debugLogger.info("Belief propagation time : " + bptime / 1000.0 + "s");
	}

	public double check_bp_with_bf(boolean verbose) {
		double maxdiff = 0;
		String diffname = null;
		double maxdiffstmt = 0;
		String diffnamestmt = null;
		long bftime = this.bf_inference();
		long bptime = this.bp_inference();
		if (verbose) {
			debugLogger.info("\nProbabilities: ");
			debugLogger.info("Vars:" + nodes.size());
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
			debugLogger.info("Stmts:" + stmts.size());
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
			debugLogger.info("Var max relative difference:" + maxdiff + " at " + diffname);
			debugLogger.info("Stmt max relative difference:" + maxdiffstmt + " at " + diffnamestmt);
			debugLogger.info("Brute force time : " + bftime / 1000.0 + "s");
			debugLogger.info("Belief propagation time : " + bptime / 1000.0 + "s");
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
				debugLogger.info("Node observed as " + v);
				n.observe(v);
			}
		}
		for (Node n : stmts) {
			if (n.getName().equals(s)) {
				valid = true;
				debugLogger.info("Stmt observed as " + v);
				n.observe(v);
			}
		}
		if (!valid) {
			debugLogger.info("Invalid Observe");
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

}
