package ppfl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

public class StmtNode extends Node {
	private boolean isUnexe = false;
	private Map<Integer, StmtNode> unexeStmtMap = new HashMap<>();

	public void setUnexe() {
		this.isUnexe = true;
	}

	public boolean isUnexe() {
		return this.isUnexe;
	}

	public String getUnexeName(int id) {
		return String.format("%s#unexe#%d", this.name, id);
	}

	public StmtNode getUnexeStmtFromMap(int id) {
		return unexeStmtMap.get(id);
	}

	public void addUnexeStmt(int id, StmtNode stmt) {
		unexeStmtMap.put(id, stmt);
	}

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
		StringBuilder sb = new StringBuilder(lineinfos[0]);
		for (int i = 1; i < lineinfos.length - 2; i++) {
			sb.append("#");
			sb.append(lineinfos[i]);
		}
		// String classandmethod = lineinfos[0] + "#" + lineinfos[1].split("#")[0];

		return sb.toString();
	}

	public String getMethod() {
		String[] lineinfos = this.name.split(":");
		return lineinfos[1].split("#")[0];
	}

}
