package ppfl;

import java.util.ArrayList;
import java.util.List;

// import org.apfloat.Apfloat;
// import org.apfloat.ApfloatMath;

public class Node {

	// private static Logger debugLogger = LoggerFactory.getLogger("Debugger");
	// protected static Logger printLogger = debugLogger;
	public static MyWriter debugLogger;
	protected static MyWriter printLogger = debugLogger;

	protected boolean obs;// obs = 1 means observed as a given value, which excludes this node from
	// inference procedure.
	protected boolean obsvalue;
	private boolean tempvalue;// for inference(sampling)
	protected boolean isStmt;
	private double p;// inferred chance to be correct
	private boolean use_ap = false;
	// private Apfloat ap_p;
	protected String name;
	protected int defcnt; 
	private double impT;
	private double impF;// importance for True and False in importance sampling.
	private String testname;
	private List<Edge> edges;
	private double epsilon = 1e-8;
	// private Apfloat ap_epsilon = new Apfloat(String.valueOf(1e-8), 100);
	private Edge degde;
	protected boolean reduced;// should be reduced in the slice if val is true

	private int stacksize; // for double:2. Other type:1.
	StmtNode stmt;

	// Heap
	private boolean isHeapObject = false;
	private Integer address = 0;

	// Stack Value
	String[] stackValue;

	public Node(String name) {
		this.obs = false;
		this.p = 0.5;
		// this.ap_p = new Apfloat("0.5", 100);
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
		// this.ap_p = new Apfloat("0.5", 100);
		this.name = name;
		isStmt = false;
		tempvalue = true;// TODO init by statistics
		edges = new ArrayList<>();
		this.stmt = stmt;
		reduced = true;
		stacksize = 1;
		this.defcnt = 0;
	}

	public Node(String name, String testname, StmtNode stmt, int defcnt){
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
		this.defcnt = defcnt;
	}

	public String getNodeName(){
		return this.testname + "#" + this.name + ":" + this.defcnt + "@" + this.stmt.getName();
	}

	public Integer getStmtIndex(){
		return this.stmt.getIndex();
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
	public int hashCode() {
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

	public boolean isHeapObject() {
		return this.isHeapObject;
	}

	public void setHeapObject() {
		this.isHeapObject = true;
	}

	public void setAddress(Integer add) {
		this.setHeapObject();
		this.address = add;
	}

	public void setStackValue(String[] v) {
		this.stackValue = v;
	}

	public String[] getStackValue() {
		return this.stackValue;
	}

	public int getAddress() {
		assert(isHeapObject);
		// if (!isHeapObject)
		// 	return 0;
		// if (address == null) {
		// 	return 0;
		// }
		return address;
	}

	public String getName() {
		return this.testname + "#" + this.name;
	}

	// not override ok?
	public String getStmtName() {
		// System.out.println(this.stmt.getName());
		String[] lineinfos = this.stmt.getName().split(":");
		StringBuilder sb = new StringBuilder(lineinfos[0]);
		for (int i = 1; i < lineinfos.length; i++) {
			sb.append("#");
			sb.append(lineinfos[i]);
		}
		// String stmtname = "#".join(lineinfos);
		// String stmtname = lineinfos[0] + "#" + lineinfos[1];
		return sb.toString();
	}

	public String getPrintName() {
		return getNodeName();
		// return this.testname + "#" + this.name + "@" + this.stmt.getName();
	}

	public void observe(boolean obsvalue) {
		obs = true;
		this.obsvalue = obsvalue;
	}

	private boolean onlyfalse = true;

	public boolean getobs() {
		if (onlyfalse)
			return obs && (!obsvalue);
		else
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
		//tempvalue = t;
		tempvalue = obs ? obsvalue : t;
	}

	public boolean getCurrentValue() {
		//return tempvalue;
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
		// if (!use_ap) {
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
		int countnan = 0;
		for (Edge n : edges) {
			double tmpratio = n.get_fton() / (1 - n.get_fton());
			if (Double.isInfinite(tmpratio))
				countnan++;
			else if (tmpratio == 0.0)
				countnan--;
			ratio = ratio * n.get_fton() / (1 - n.get_fton());
		}
		double result = ratio / (ratio + 1);
		if (Double.isNaN(result)) {
			if (countnan > 0)
				result = 1;
			else if (countnan < 0)
				result = 0;
			else {
				// System.out.println("0.5 error here");
				result = 0.5;
			}
		}
		double delta = result - this.p;
		if (delta < 0)
			delta = -delta;
		this.p = result;
		// if(isStmt)
		// 	debugLogger.write("%s, prob = %.20f\n", getPrintName(), result);

		for (Edge n : edges) {
			double b = (1 - n.get_fton());
			double a = n.get_fton();
			// double tv1 = b/(b+a/result);
			double tv1;
			if (Double.isNaN(ratio) || Double.isInfinite(ratio)) {
				if (countnan > 0)
					tv1 = 1;
				else if (countnan < 0)
					tv1 = 0;
				else {
					tv1 = 1 - b;
				}
			} else if (Double.isNaN(a / ratio) || Double.isInfinite(a / ratio))
				tv1 = 0;
			else
				tv1 = b / (b + a / ratio);
			// if(Double.isNaN(tv1))
			// System.out.println("here is bug nan + a/ratio = "+ a/ratio+ ", b = "+ b+ ",
			// ratio = " + ratio);
			n.set_ntof(tv1);
		}
		return delta > epsilon;
		// } else {
		// if (obs) {
		// Apfloat val = obsvalue ? new Apfloat("1.0", 100) : new Apfloat("0.0", 100);
		// for (Edge n : edges) {
		// n.ap_set_ntof(val);
		// }
		// Apfloat delta = val.subtract(this.ap_p);
		// if (delta.compareTo(new Apfloat("0.0", 100)) == -1)
		// delta = delta.negate();
		// this.ap_p = val;
		// return (delta.compareTo(ap_epsilon) == 1);
		// }

		// Apfloat ratio = new Apfloat("1.0", 100);
		// int countnan = 0;
		// for (Edge n : edges) {
		// Apfloat divisor = new Apfloat("1.0", 100).subtract(n.ap_get_fton());
		// if (n.ap_get_fton().equals(new Apfloat("0.0", 100)))
		// countnan--;
		// try {
		// Apfloat tmpratio = n.ap_get_fton().divide(divisor);
		// ratio = ratio.multiply(tmpratio);
		// } catch (Exception ApfloatRuntimeException) {
		// countnan++;
		// }
		// }
		// Apfloat result = ratio.divide(ratio.add(new Apfloat("1.0", 100)));
		// if (countnan != 0) {
		// if (countnan > 0)
		// result = new Apfloat("1.0", 100);
		// else if (countnan < 0)
		// result = new Apfloat("0.0", 100);
		// }
		// Apfloat delta = result.subtract(this.ap_p);
		// if (delta.compareTo(new Apfloat("0.0", 100)) == -1)
		// delta = delta.negate();
		// this.ap_p = result;

		// for (Edge n : edges) {
		// Apfloat b = new Apfloat("1.0", 100).subtract(n.ap_get_fton());
		// Apfloat a = n.ap_get_fton();
		// // double tv1 = b/(b+a/result);
		// Apfloat tv1;
		// if (countnan != 0) {
		// if (countnan > 0)
		// tv1 = new Apfloat("1.0", 100);
		// else
		// tv1 = new Apfloat("0.0", 100);
		// // else{
		// // tv1 = 1-b;
		// // }
		// }
		// // else if (Double.isNaN(a / ratio)||Double.isInfinite(a/ratio))
		// // tv1 = 0;
		// else {
		// try {
		// tv1 = b.divide(b.add(a.divide(ratio)));
		// } catch (Exception ApfloatRuntimeException) {
		// tv1 = new Apfloat("0.0", 100);
		// }
		// }
		// // if(Double.isNaN(tv1))
		// // System.out.println("here is bug nan + a/ratio = "+ a/ratio+ ", b = "+ b+
		// ",
		// // ratio = " + ratio);
		// n.ap_set_ntof(tv1);
		// }
		// return (delta.compareTo(ap_epsilon) == 1);
		// }
	}

	public double bp_getprob() {
		return p;
	}

	// public Apfloat ap_bp_getprob() {
	// return ap_p;
	// }

	public static void setLogger(MyWriter lgr) {
		// printLogger = lgr;
	}

	public void print(MyWriter lgr, String prefix) {
		if (this.obs) {
			lgr.writeln("%s%s observed = %b", prefix, this.getPrintName(), this.obsvalue);
		} else {
			lgr.writeln("%s%s", prefix, this.getPrintName());
		}
	}

	public void print(String prefix) {
		if (this.obs) {
			printLogger.writeln("%s%s observed = %b", prefix, this.getPrintName(), this.obsvalue);
		} else {
			printLogger.writeln("%s%s", prefix, this.getPrintName());
		}
	}

	public void print(MyWriter lgr) {
		if (this.obs) {
			lgr.writeln("%s observed = %b", this.getPrintName(), this.obsvalue);
			// lgr.info("{} observed = {}", this.getPrintName(), this.obsvalue);
		} else {
			lgr.writeln(this.getPrintName());
		}
	}

	public void print() {
		if (this.obs) {
			printLogger.writeln("%s observed = %b", this.getPrintName(), this.obsvalue);
		} else {
			printLogger.writeln(this.getPrintName());
		}
	}

	public void printprob() {
		if (this.obs) {
			printLogger.writeln("%sobs prob = %s", this.getPrintName(), (this.obsvalue ? 1.0 : 0.0));
		} else
			printLogger.writeln("%s prob = %f", this.getPrintName(), getprob());
	}

	public void bpPrintProb() {
		// if (!use_ap)
		printLogger.writeln("%s prob_bp = %f", this.getPrintName(), bp_getprob());
		// else
		// printLogger.writeln("%s prob_bp = %s", this.getPrintName(), ap_bp_getprob());
	}

	public void bpPrintProb(MyWriter lgr) {
		// if (!use_ap)
		lgr.writeln("%s prob_bp = %.20f", this.getPrintName(), bp_getprob());
		// else
		// lgr.writeln("%s prob_bp = %.20s", this.getPrintName(), ap_bp_getprob());
	}

}
