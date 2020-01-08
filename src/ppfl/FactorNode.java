package ppfl;

import java.util.List;

public class FactorNode {
	private List<Node> preds;
	private Node def;
	private Node stmt;
	private List<Node> uses;
	// TODO consider operators
	private double HIGH = 0.99;
	private double MEDIUM = 0.5;
	private double LOW = 0.01;

	public FactorNode(Node def, Node stmt, List<Node> preds, List<Node> uses) {
		this.preds = preds;
		this.stmt = stmt;
		this.def = def;
		this.uses = uses;
	}

	public double getProb() {
		boolean defv = def.getCurrentValue();
		boolean predv = true;
		boolean usev = true;
		boolean stmtv = stmt.getCurrentValue();
		if (preds != null)
			for (Node p : preds) {
				if (!p.getCurrentValue()) {
					predv = false;
					break;
				}
			}
		if (uses != null)
			for (Node u : uses) {
				if (!u.getCurrentValue()) {
					usev = false;
					break;
				}
			}
		boolean pu = predv && usev;
		if (stmtv) {
			if (defv && pu)
				return HIGH;
			if (!defv && !pu)
				return HIGH;
			if (!defv && pu)
				return LOW;
			if (defv && !pu)
				return LOW;// TODO should be medium when using certain ops.
		} else {
			if (!defv)
				return HIGH;
			// else: def = true
			if (pu)
				return LOW;
			// def = true stmt = false use = false
			return LOW;// TODO should be medium when using certain ops.
		}
		return MEDIUM;
	}

	public void print() {
		stmt.print();
		System.out.print("def:");
		def.print();

		if (uses != null) {
			System.out.println("uses:");
			for (Node n : uses) {
				n.print();
			}
		}
		if (preds != null) {
			System.out.println("preds:");
			for (Node n : preds) {
				n.print();
			}
		}
	}
}
