package ppfl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ByteCodeGraph {

	private List<FactorNode> factornodes;
	private List<Node> nodes;
	private List<StmtNode> stmts;
	private Map<String, Node> nodemap;
	private Map<String, Node> stmtmap;
	private Map<String, Integer> varcountmap;
	private Map<String, Integer> stmtcountmap;
	// if max_loop is set to negative, then no limit is set(unlimited loop
	// unfolding)
	int max_loop;

	private String testname;

	private int nsamples = 1 << 20;
	private int bp_times = 100;
	Random random;

	Stack<Node> runtimestack;
	
	// auto-oracle: when set to TRUE, parsetrace() will auto-assign prob for:
	// input of test function as 1.0(always true)
	// output (return value) of the function being tested as 0.0/1.0 depends on
	// parameter testpass(true = 1.0,false = 0.0)
	private boolean auto_oracle;


	public ByteCodeGraph() {
		factornodes = new ArrayList<FactorNode>();
		nodes = new ArrayList<Node>();
		stmts = new ArrayList<StmtNode>();
		nodemap = new HashMap<String, Node>();
		stmtmap = new HashMap<String, Node>();
		varcountmap = new HashMap<String, Integer>();
		stmtcountmap = new HashMap<String, Integer>();
		max_loop = -1;
		random = new Random();
		auto_oracle = true;
		runtimestack = new Stack<Node>();
	}


	public void setMaxLoop(int i) {
		this.max_loop = i;
	}

	public void setAutoOracle(boolean b) {
		this.auto_oracle = b;
	}

	public void parsetrace(String tracefilename, String testname, boolean testpass) {
		this.testname = testname;
		varcountmap = new HashMap<String, Integer>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(tracefilename));
			String t;
			String last_defined_var = null;
			StmtNode last_defined_stmt = null;
			while ((t = reader.readLine()) != null) {
				if (t.isEmpty() || t.startsWith("###"))
					continue;
				String[] split = t.split(":");
				assert (split.length >= 5);
				String head = split[0];
				assert(head == "INFO");
				
				String domain = split[1];
				int splitp = domain.lastIndexOf('$');
				String traceclass = domain.substring(0, splitp);
				String trace_method = domain.substring(splitp + 1);

				int line = Integer.parseInt(split[2]);
				int byteindex = Integer.parseInt(split[3]);
				StmtNode stmt = null;
				String stmtname = domain + "#" + String.valueOf(line);
				// System.out.println("At line " + stmtname);
				if (!hasNode(stmtname)) {
					stmt = new StmtNode(stmtname);
					addNode(stmtname, stmt);
				} else {
					stmt = (StmtNode) getNode(stmtname);
					assert (stmt.isStmt());
				}

				// count how many times this statment has been executed
				if (stmtcountmap.containsKey(stmtname)) {
					stmtcountmap.put(stmtname, stmtcountmap.get(stmtname) + 1);
				} else {
					stmtcountmap.put(stmtname, 1);
				}
				
				if (max_loop > 0 && stmtcountmap.get(stmtname) > max_loop) {
					continue;
				}

				// auto-assigned observation: test function always true
				if (auto_oracle) {
					if (trace_method.contentEquals(testname)) {
						stmt.observe(true);
						System.out.println("Observe " + stmt.getName() + " as true");
					}
				}

				String instinfos[] = split[4].split(",");
				int pushnum;
				int popnum;
				for(String instinfo:instinfos) {
					String[] splitinstinfo = instinfo.split("=");
					String desc = splitinstinfo[0];
					if(desc == "opcode")
				}

			}
			// after all lines are parsed, auto-assign oracle for the last defined var
			// with test state(pass = true,fail = false)
			if (auto_oracle) {
				String observename = getVarName(last_defined_var, varcountmap);
				this.observe(observename, testpass);
				System.out.println("Observe " + observename + " as " + testpass);
			}

			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private FactorNode buildFactorWithArgUses(String def, Set<Integer> preds, List<Set<String>> uses, List<String> ops,
			StmtNode stmt) {
		HashSet<String> t = new HashSet<String>();
		for (Set<String> use : uses) {
			t.addAll(use);
		}
		return buildFactor(def, preds, t, ops, stmt);
	}

	private FactorNode buildFactor(String def, Set<Integer> preds, Set<String> uses, List<String> ops, StmtNode stmt) {

		// deal with Declaration and use/pred in the same line
		// (e.g. for(int i = 0;i < n;i++))
		// In this case, some use/pred are not in varcountmap.
		Node defnode = null;
		boolean initDef = false;
		if (preds != null)
			for (Integer i : preds) {
				String s = LineMappingVisitor.getPredName(i);
				if (!varcountmap.containsKey(s))
					initDef = true;
			}
		if (uses != null)
			for (String s : uses) {
				if (!s.equals(LineMappingVisitor.getConstName())) {// TODO deal with constants.
					if (!varcountmap.containsKey(s))
						initDef = true;
				}
			}
		// Add def earlier. for(int i = 1;i < n;i++) def:i#1 use:i#1
		if (initDef) {
			System.out.println("initdef " + def);
			if (!varcountmap.containsKey(def)) {
				varcountmap.put(def, 1);
			} else {
				varcountmap.put(def, varcountmap.get(def) + 1);
			}
			String defname = getVarName(def, varcountmap);
			defnode = new Node(defname, testname, stmt);
			// System.out.println("Adding def: " + defname);
			addNode(defname, defnode);
		}

		List<Node> prednodes = new ArrayList<Node>();
		if (preds != null)
			for (Integer i : preds) {
				String s = LineMappingVisitor.getPredName(i);
				if (!varcountmap.containsKey(s)) {
					// assert(i == )
					continue;
				}
				String predname = getVarName(s, varcountmap);
				prednodes.add(getNode(predname));
			}
		List<Node> usenodes = new ArrayList<Node>();
		if (uses != null)
			for (String s : uses) {
				if (!s.equals(LineMappingVisitor.getConstName())) {// TODO deal with constants.
					// System.out.print(s + " ");
					if (!varcountmap.containsKey(s)) {
						System.out.println("Undefined use:" + s);
						// assert(false);
						continue;
					}
					assert (varcountmap.containsKey(s));
					String usename = getVarName(s, varcountmap);
					// System.out.println("Setting uses: " + usename);
					usenodes.add(getNode(usename));
				}
			}

		// deal with def here.
		// when a = a + 1; occurs, use should be a#1, def should be a#2
		if (!initDef) {
			if (!varcountmap.containsKey(def)) {
				varcountmap.put(def, 1);
			} else {
				varcountmap.put(def, varcountmap.get(def) + 1);
			}
			String defname = getVarName(def, varcountmap);
			defnode = new Node(defname, testname, stmt);
			// System.out.println("Adding def: " + defname);
			addNode(defname, defnode);
		}

        Edge dedge = new Edge();
        dedge.setnode(defnode);
        defnode.add_edge(dedge);
        defnode.setdedge(dedge);
        Edge sedge = new Edge();
        sedge.setnode(stmt);
        stmt.add_edge(sedge);
        
		List<Edge> pedges = new ArrayList<Edge>();
		for (Node n : prednodes) {
            Edge nedge = new Edge();
            nedge.setnode(n);
			pedges.add(nedge);
			n.add_edge(nedge);
		}
		List<Edge> uedges = new ArrayList<Edge>();
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
        for(Edge n: pedges)
            n.setfactor(ret);
        for(Edge n: uedges)
            n.setfactor(ret);
		return ret;
	}

	private String getVarName(String name, Map<String, Integer> map) {
		int count = map.get(name);
		return name + "#" + String.valueOf(count);
	}

	private String getNodeName(String name) {
		return this.testname + "#" + name;
	}

	private boolean hasNode(String name) {
		return nodemap.containsKey(getNodeName(name)) || stmtmap.containsKey(name);
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

	private Node getNode(String name) {
		if (nodemap.containsKey(getNodeName(name)))
			return nodemap.get(getNodeName(name));
		else if (stmtmap.containsKey(name))
			return stmtmap.get(name);
		else
			return null;
	}

	private void solve(List<Node> allnodes, int cur, int tot) {
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

		List<Node> allnodes = new ArrayList<Node>();
		allnodes.addAll(nodes);
		allnodes.addAll(stmts);
		int nnodes = allnodes.size();
		solve(allnodes, 0, nnodes);

		nodes.sort(new Comparator<Node>() {
			@Override
			public int compare(Node arg0, Node arg1) {
				return Double.compare(arg0.getprob(), arg1.getprob());
			}
		});
		stmts.sort(new Comparator<Node>() {
			@Override
			public int compare(Node arg0, Node arg1) {
				return Double.compare(arg0.getprob(), arg1.getprob());
			}
		});
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

		nodes.sort(new Comparator<Node>() {
			@Override
			public int compare(Node arg0, Node arg1) {
				return Double.compare(arg0.getprob(), arg1.getprob());
			}
		});
		stmts.sort(new Comparator<Node>() {
			@Override
			public int compare(Node arg0, Node arg1) {
				return Double.compare(arg0.getprob(), arg1.getprob());
			}
		});
		return;
	}

    private void mark_reduce(Node node){
        node.setreduced();
        if(node.isStmt)
            return;
        FactorNode deffactor = (node.getdedge()).getfactor();
        List<Node> pulist = deffactor.getpunodes();
        for(Node n : pulist){
            mark_reduce(n);
        }
        deffactor.getstmt().setreduced();
    }

    private void path_reduce(){
        for (Node n : nodes) {
            if (n.getobs()){
                mark_reduce(n);
            }
        }
        //maybe won't be used?
        for(Node n: stmts){
            if(n.getobs()){
                mark_reduce(n);
            }
        }
    }

    //TODO: how to slice in the graph to optimize the procedure, though the stmt result can be cut as the mark
    private void node_reduce(){

    }

	public long bp_inference() {
        long startTime = System.currentTimeMillis();
        path_reduce();

        boolean outreduced = true;
        if(outreduced)
        {
            System.out.println("\nreduced Nodes: ");
            for (Node n : stmts) {
                if(n.getreduced())
                    n.print();
            }
            for (Node n : nodes) {
                if(n.getreduced())
                n.print();
            }
            System.out.println("\n");
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
				System.out.println("\n\n" + i + "\n\n");
				System.out.println("\n\n" + i + "\n\n");
				break;
			}
		}
		nodes.sort(new Comparator<Node>() {
			@Override
			public int compare(Node arg0, Node arg1) {
				return Double.compare(arg0.bp_getprob(), arg1.bp_getprob());
			}
		});
		stmts.sort(new Comparator<Node>() {
			@Override
			public int compare(Node arg0, Node arg1) {
				return Double.compare(arg0.bp_getprob(), arg1.bp_getprob());
			}
		});
		long endTime = System.currentTimeMillis();

		return (endTime - startTime);

	}

	public StmtNode getTopStmt() {// should be called after inference()
		return stmts.get(0);
	}

	public List<StmtNode> getTopkStmt(int k) {
		return stmts.subList(0, k);
	}

	public void merge(ByteCodeGraph oth) {
		return;// TODO
	}

	public void printgraph() {
		System.out.println("\nNodes: ");
		for (Node n : stmts) {
			n.print();
		}
		for (Node n : nodes) {
			n.print();
		}
		System.out.println("Factors:");
		for (FactorNode n : factornodes) {
			n.print();
		}
	}

	public void printprobs() {
		System.out.println("\nProbabilities: ");
		System.out.println("Vars:" + nodes.size());
		for (Node n : nodes) {
			n.printprob();
		}
		System.out.println("Stmts:" + stmts.size());
		for (StmtNode n : stmts) {
			n.printprob();
		}
	}

	public void bp_printprobs() {
		System.out.println("\nProbabilities: ");
		System.out.println("Vars:" + nodes.size());
		for (Node n : nodes) {
			n.bp_printprob();
		}
		System.out.println("Stmts:" + stmts.size());
		for (StmtNode n : stmts) {
			n.bp_printprob();
		}
	}

	private double getdiff(double a, double b) {
		double max = Math.max(Math.abs(a), Math.abs(b));
		return max == 0.0 ? 0 : Math.abs(a - b) / max;
	}

	public void check_bp(boolean verbose) {
		long bptime = this.bp_inference();

		System.out.println("\nProbabilities: ");
		System.out.println("Vars:" + nodes.size());
		int cnt = 0;
		for (Node n : nodes) {
			if (!verbose) {
				cnt++;
				if (cnt > 10)
					break;
			}
			n.bp_printprob();
		}
		System.out.println("Stmts:" + stmts.size());
		for (StmtNode n : stmts) {
			n.bp_printprob();
		}
		System.out.println("Belief propagation time : " + bptime / 1000.0 + "s");
	}

	public double check_bp_with_bf(boolean verbose) {
		double maxdiff = 0;
		String diffname = null;
		double maxdiffstmt = 0;
		String diffnamestmt = null;
		long bftime = this.bf_inference();
		long bptime = this.bp_inference();
		if (verbose) {
			System.out.println("\nProbabilities: ");
			System.out.println("Vars:" + nodes.size());
		}
		for (Node n : nodes) {
			if (verbose) {
				n.printprob();
				n.bp_printprob();
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
			System.out.println("Stmts:" + stmts.size());
		for (StmtNode n : stmts) {
			if (verbose) {
				n.printprob();
				n.bp_printprob();
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
			System.out.println("Var max relative difference:" + maxdiff + " at " + diffname);
			System.out.println("Stmt max relative difference:" + maxdiffstmt + " at " + diffnamestmt);
			System.out.println("Brute force time : " + bftime / 1000.0 + "s");
			System.out.println("Belief propagation time : " + bptime / 1000.0 + "s");
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
				System.out.println("Node observed as " + v);
				n.observe(v);
			}
		}
		for (Node n : stmts) {
			if (n.getName().equals(s)) {
				valid = true;
				System.out.println("Stmt observed as " + v);
				n.observe(v);
			}
		}
		if (!valid) {
			System.out.println("Invalid Observe");
		}
	}

	private static String readFileToString(String filePath) {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[10];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileData.toString();
	}

}
