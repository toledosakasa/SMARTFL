package ppfl;

public class StmtNode extends Node {

	public StmtNode(String sname) {
		super(sname);
		this.isStmt = true;
		// TODO Auto-generated constructor stub
	}

	public boolean equals(StmtNode oth) {
		return this.name.equals(oth.name);
	}

	public void print() {
		System.out.print(this.name);
		System.out.print("(Statement)");
		if (this.obs) {
			System.out.print(" observed = " + this.obsvalue);
		}
		System.out.println("");
	}

	public int getLineNumber() {
		String[] sp = this.name.split("#");
		return Integer.parseInt(sp[1]);
	}
	
	public String getName() {
		return this.name;
	}

	public String getPrintName() {
		return this.name;
	}

}
