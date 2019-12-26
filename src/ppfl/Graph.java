package ppfl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Graph {

	private List<FactorNode> factornodes;
	private List<Node> nodes;
	private List<StmtNode> stmts;
	private Map<String, Node> nodemap;
	private Map<String, Integer> varcountmap;

	private StmtNode callernode;
	private Line caller;
	private List<Set<String>> passedargs;
	private String returnDef;

	private int nsamples = 10000;
	Random random;

	public Graph() {

	}

	public Graph(LineInfo lineinfo, String tracefilename) {
		factornodes = new ArrayList<FactorNode>();
		nodes = new ArrayList<Node>();
		stmts = new ArrayList<StmtNode>();
		nodemap = new TreeMap<String, Node>();
		varcountmap = new TreeMap<String, Integer>();
		random = new Random();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(tracefilename));
			String t;
			while ((t = reader.readLine()) != null) {
				if (t.isEmpty() || t.startsWith("###"))
					continue;
				String[] split = t.split(":");
				assert (split.length >= 2);
				String domain = split[0];// TODO utilize this to search lineinfo in different files.
				int line = Integer.parseInt(split[1]);
				Line curline = lineinfo.getLine(line);
				StmtNode stmt = null;
				String stmtname = domain + "#" + String.valueOf(line);
				// System.out.println("At line " + stmtname);
				if (!nodemap.containsKey(stmtname)) {
					stmt = new StmtNode(stmtname);
					nodemap.put(stmtname, stmt);
					stmts.add(stmt);
					nodes.add(stmt);
				} else {
					stmt = (StmtNode) nodemap.get(stmtname);
					assert (stmt.isStmt());
				}

				// deal with inter-procedure call

				if (curline.ismethod) {
					// this is the very first statement inside a method.

					if (passedargs != null && !passedargs.isEmpty()) {
						// if caller exist. This should apply in most cases(except program main entry)
						assert (curline.argdefs.size() == passedargs.size());
						for (int i = 0; i < passedargs.size(); i++) {
							String d = curline.argdefs.get(i);
							Set<String> arguses = passedargs.get(i);
							FactorNode factor = buildFactor(d, curline.preds, arguses, callernode);
						}
					}
				}
				if (curline.isret) {
					// if caller exist.
					if (returnDef != null) {
						FactorNode factor = buildFactor(returnDef, curline.preds, curline.uses, callernode);
					}
				}

				if (curline.ismethodinvocation) {
					callernode = stmt;
					caller = curline;
					returnDef = curline.retdef;
					passedargs = curline.arguses;
				}

				if (curline.def != null) {
					//curline.print();
					FactorNode factor = buildFactor(curline.def, curline.preds, curline.uses, stmt);
				}

			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private FactorNode buildFactor(String def, Set<Integer> preds, Set<String> uses, StmtNode stmt) {
		

		List<Node> prednodes = new ArrayList<Node>();
		if (preds != null)
			for (Integer i : preds) {
				String s = LineMappingVisitor.getPredName(i);
				assert (nodemap.containsKey(s));
				String predname = getVarName(s, varcountmap);
				prednodes.add(nodemap.get(predname));
			}
		List<Node> usenodes = new ArrayList<Node>();
		if (uses != null)
			for (String s : uses) {
				if (!s.equals(LineMappingVisitor.getConstName())) {//TODO deal with constants.
					// System.out.print(s + " ");
					assert (varcountmap.containsKey(s));
					String usename = getVarName(s, varcountmap);
					// System.out.println("Setting uses: " + usename);
					usenodes.add(nodemap.get(usename));
				}
			}
		if (!varcountmap.containsKey(def)) {
			varcountmap.put(def, 1);
		} else {
			varcountmap.put(def, varcountmap.get(def) + 1);
		}
		String defname = getVarName(def, varcountmap);
		Node defnode = new Node(defname);
		// System.out.println("Adding def: " + defname);
		nodemap.put(defname, defnode);
		nodes.add(defnode);
		
		FactorNode ret = new FactorNode(defnode, stmt, prednodes, usenodes);
		factornodes.add(ret);
		return ret;
	}

	private String getVarName(String name, Map<String, Integer> map) {
		int count = map.get(name);
		return name + "#" + String.valueOf(count);
	}

	public void inference() {
		for (Node n : nodes) {
			n.init();
		}
		for (int i = 0; i < nsamples; i++) {
			for (Node n : nodes) {
				n.setTemp(random.nextBoolean());
			}
			double product = 1.0;
			for (FactorNode n : factornodes) {
				product = product * n.getProb();// TODO consider log-add
			}
			for (Node n : nodes) {
				n.addimp(product);
			}
		}

		stmts.sort(new Comparator<Node>() {
			@Override
			public int compare(Node arg0, Node arg1) {
				return (arg0.getprob() - arg1.getprob()) > 0 ? 1 : -1;
			}
		});
		return;
	}

	public StmtNode getTopStmt() {// should be called after inference()
		return stmts.get(0);
	}

	public List<StmtNode> getTopkStmt(int k) {
		return stmts.subList(0, k);
	}

	public void merge(Graph oth) {
		return;// TODO
	}

	public void printgraph() {
		System.out.println("\nNodes: ");
		for (Node n : nodes) {
			if (n.isStmt)
				n.print();
		}
		for (Node n : nodes) {
			if (!n.isStmt)
				n.print();
		}
		System.out.println("Factors:");
		for (FactorNode n : factornodes) {
			n.print();
		}
	}

	public void printprobs() {
		System.out.println("\nProbabilities: ");
		System.out.println("Vars:");
		for (Node n : nodes) {
			if (!n.isStmt)
				n.printprob();
		}
		System.out.println("Stmts:");
		for (StmtNode n : stmts) {
			n.printprob();
		}
	}

	public void observe(String s, boolean v) {
		for (Node n : nodes) {
			if (n.getName().equals(s)) {
				n.observe(v);
			}
		}
	}

}
