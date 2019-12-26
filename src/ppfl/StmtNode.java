package ppfl;

public class StmtNode extends Node {

	public StmtNode(String sname) {
		super(sname);
		this.isStmt = true;
		// TODO Auto-generated constructor stub
	}

	public boolean equals(StmtNode oth) {
		return this.getName().equals(oth.getName());
	}

}
