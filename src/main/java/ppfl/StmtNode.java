package ppfl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StmtNode extends Node {
	private static Logger debugLogger = LoggerFactory.getLogger("Debugger");

	public StmtNode(String sname) {
		super(sname);
		this.isStmt = true;
	}

	@Override
	public void print() {
		if (this.obs) {
			debugLogger.info(this.name + "(Statement) observed = " + this.obsvalue);
		} else {
			debugLogger.info(this.name + "(Statement)");
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
