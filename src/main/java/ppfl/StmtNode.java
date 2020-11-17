package ppfl;

public class StmtNode extends Node {

	public StmtNode(String sname) {
		super(sname);
		this.isStmt = true;
	}

	@Override
	public void print(String prefix) {
		if (this.obs) {
			printLogger.info("{}{}(Statement) observed = {}", prefix, this.name, this.obsvalue);
		} else {
			printLogger.info("{}{}(Statement)", prefix, this.name);
		}
	}

	@Override
	public void print() {
		if (this.obs) {
			printLogger.info("{}(Statement) observed = {}", this.name, this.obsvalue);
		} else {
			printLogger.info("{}(Statement)", this.name);
		}
	}

	public int getLineNumber() {
		String[] sp = this.name.split("#");
		return Integer.parseInt(sp[1]);
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getPrintName() {
		return this.name;
	}

}
