package ppfl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node {

	private static Logger debugLogger = LoggerFactory.getLogger("Debugger");

	protected boolean obs;// obs = 1 means observed as a given value, which excludes this node from
	// inference procedure.
	protected boolean obsvalue;
	private boolean tempvalue;// for inference(sampling)
	protected boolean isStmt;
	private double p;// inferred chance to be correct
	protected String name;
	private double impT;
	private double impF;// importance for True and False in importance sampling.
	private String testname;
	private List<Edge> edges;
	private double epsilon = 1e-8;
	private Edge degde;
	protected boolean reduced;// should be reduced in the slice if val is true

	private int stacksize; // for double:2. Other type:1.
	StmtNode stmt;

	public Node(String name) {
		this.obs = false;
		this.p = 0.5;
		this.name = name;
		isStmt = false;
		tempvalue = true;// TODO init by statistics
		edges = new ArrayList<>();
		stmt = null;
		reduced = true;
		stacksize = 1;
	}

	public Node(String name, String testname, StmtNode stmt) {
		this.testname = testname;
		this.obs = false;
		this.p = 0.5;
		this.name = name;
		isStmt = false;
		tempvalue = true;// TODO init by statistics
		edges = new ArrayList<>();
		this.stmt = stmt;
		reduced = true;
		stacksize = 1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Node other = (Node) o;
		return this.name.equals(other.name);
	}

	@Override
	public int hashCode(){
		return this.name.hashCode();
	}

	public int getSize() {
		return this.stacksize;
	}

	public void setSize(int s) {
		if (s < 1) {
			throw new IllegalArgumentException("Invalid stack size: " + s);
		}
		this.stacksize = s;
	}

	public String getName() {
		return this.testname + "#" + this.name;
	}

	//not override ok?
	public String getStmtName(){
		String[] lineinfos = this.stmt.getName().split(":");
		String stmtname = lineinfos[0] + "#" + lineinfos[1];
		return stmtname;
	}

	public String getPrintName() {
		return this.testname + "#" + this.name + "@" + this.stmt.getName();
	}

	public void observe(boolean obsvalue) {
		obs = true;
		this.obsvalue = obsvalue;
	}

	public boolean getobs() {
		return obs;
	}

	public void setdedge(Edge e) {
		degde = e;
	}

	public Edge getdedge() {
		return degde;
	}

	// reduced should be false when the node is in the front slice of a obs node
	public void setreduced() {
		reduced = false;
	}

	public boolean getreduced() {
		return reduced;
	}

	public void setTemp(boolean t) {
		tempvalue = obs ? obsvalue : t;
	}

	public boolean getCurrentValue() {
		return obs ? obsvalue : tempvalue;
	}

	public void init() {
		if (!obs) {
			impT = 0;
			impF = 0;
		}
	}

	public void addimp(double imp) {
		if (!obs) {
			if (tempvalue)
				impT += imp;
			else
				impF += imp;
		}
	}

	public double getprob() {
		return impT / (impT + impF);
	}

	public boolean isStmt() {
		return this.isStmt;
	}

	public void add_edge(Edge edge) {
		edges.add(edge);
	}

	public boolean send_message() {
		if (obs) {
			double val = obsvalue ? 1.0 : 0.0;
			for (Edge n : edges) {
				n.set_ntof(val);
			}
			double delta = val - this.p;
			if (delta < 0)
				delta = -delta;
			this.p = val;
			return delta > epsilon;
		}

		double ratio = 1;
		for (Edge n : edges) {
			ratio = ratio * n.get_fton() / (1 - n.get_fton());
		}
		double result = ratio / (ratio + 1);
		double delta = result - this.p;
		if (delta < 0)
			delta = -delta;
		this.p = result;

		for (Edge n : edges) {
			double b = (1 - n.get_fton());
			double a = n.get_fton();
			// double tv1 = b/(b+a/result);
			double tv1 = b / (b + a / ratio);
			n.set_ntof(tv1);
		}
		return delta > epsilon;
	}

	public double bp_getprob() {
		return p;
	}

	public void print(String prefix){
		if (this.obs) {
			debugLogger.info(prefix + this.getPrintName() + " observed = " + this.obsvalue);
		} else {
			debugLogger.info(prefix + this.getPrintName());
		}
	}

	public void print() {
		if (this.obs) {
			debugLogger.info(this.getPrintName() + " observed = " + this.obsvalue);
		} else {
			debugLogger.info(this.getPrintName());
		}
	}

	public void printprob() {
		if (this.obs) {
			debugLogger
					.info(this.getPrintName() + "obs prob = " + (this.obsvalue ? String.valueOf(1.0) : String.valueOf(0.0)));
		} else
			debugLogger.info(this.getPrintName() + " prob = " + getprob());
	}

	public void bpPrintProb() {
		debugLogger.info(this.getPrintName() + " prob_bp = " + bp_getprob());
	}

}
