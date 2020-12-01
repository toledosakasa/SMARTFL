package ppfl;

import org.slf4j.Logger;

public class StmtNode extends Node {

	public StmtNode(String sname) {
		super(sname);
		this.isStmt = true;
	}

	@Override
	public void print(Logger lgr, String prefix) {
		if (this.obs) {
			lgr.info("{}{}(Statement) observed = {}", prefix, this.name, this.obsvalue);
		} else {
			lgr.info("{}{}(Statement)", prefix, this.name);
		}
	}

	@Override
	public void print(Logger lgr) {
		if (this.obs) {
			lgr.info("{}(Statement) observed = {}", this.name, this.obsvalue);
		} else {
			lgr.info("{}(Statement)", this.name);
		}
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

	public String getClassMethod() {
		String[] lineinfos = this.name.split(":");
		String classandmethod = lineinfos[0] + "#" + lineinfos[1].split("#")[0];
		return classandmethod;
	}

}
