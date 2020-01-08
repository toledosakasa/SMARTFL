package ppfl;

public class Node {
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

	public Node(String name) {
		this.obs = false;
		this.p = 0.5;
		this.name = name;
		isStmt = false;
		tempvalue = true;// TODO init by statistics
	}

	public Node(String name, String testname) {
		this.testname = testname;
		this.obs = false;
		this.p = 0.5;
		this.name = name;
		isStmt = false;
		tempvalue = true;// TODO init by statistics
	}

	public String getName() {
		return this.testname + "#" + this.name;
	}

	public void observe(boolean obsvalue) {
		obs = true;
		this.obsvalue = obsvalue;
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

	public void print() {
		System.out.print(this.getName());
		System.out.print("(Statement)");
		if (this.obs) {
			System.out.print(" observed = " + this.obsvalue);
		}
		System.out.println("");
	}

	public void printprob() {
		System.out.println(this.getName() + " prob = " + String.valueOf(getprob()));
	}
}
