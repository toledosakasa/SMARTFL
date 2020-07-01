package ppfl;

import java.util.List;
import java.util.ArrayList;

public class FactorNode {
	private List<Node> preds;
	private Node def;
	private Node stmt;
	private List<Node> uses;
	private List<String> ops;// TODO consider operators
	private static final String[] unkops = { "%", "<", "<=", ">", ">=", "==" };
	private double HIGH = 0.99;
	private double MEDIUM = 0.5;
	private double LOW = 0.01;
	private List<Double> tensor;
	private List<Node> allnodes;
	private List<Edge> alledges;
	private int nnodes;

	private Edge dedge;
	private Edge sedge;
	private List<Edge> pedges;
	private List<Edge> uedges;

	public FactorNode(Node def, Node stmt, List<Node> preds, List<Node> uses, List<String> ops, Edge dedge, Edge sedge,
			List<Edge> pedges, List<Edge> uedges) {
		this.preds = preds;
		this.stmt = stmt;
		this.def = def;
		this.uses = uses;
		this.ops = ops;
		this.dedge = dedge;
		this.sedge = sedge;
		this.pedges = pedges;
		this.uedges = uedges;
		this.tensor = new ArrayList<Double>();
		this.allnodes = new ArrayList<Node>();
		allnodes.add(stmt);
		allnodes.add(def);
		allnodes.addAll(preds);
		allnodes.addAll(uses);
		this.alledges = new ArrayList<Edge>();
		alledges.add(sedge);
		alledges.add(dedge);
		alledges.addAll(pedges);
		alledges.addAll(uedges);
		this.nnodes = allnodes.size();
		gettensor(allnodes, nnodes - 1);
	}

    public List<Node> getpunodes(){
        ArrayList<Node> ret = new ArrayList<Node>();
        ret.addAll(preds);
        ret.addAll(uses);
        return ret;
    }

	private void gettensor(List<Node> allnodes, int cur) {
		if (cur < 0) {
			tensor.add(getProb());
			return;
		}
		allnodes.get(cur).setTemp(false);
		gettensor(allnodes, cur - 1);
		allnodes.get(cur).setTemp(true);
		gettensor(allnodes, cur - 1);
	}

	public void send_message() {
		// used to save all the messages from the nodes
		List<Double> tmpvlist = new ArrayList<Double>();
		for (int i = 0; i < nnodes; i++) {
			tmpvlist.add(alledges.get(i).get_ntof());
		}

		for (int j = 0; j < nnodes; j++) {
			double v0 = 0;
			double v1 = 0;
			int step = (1 << j);
			int vnum = (1 << nnodes);
			// transform a tensor of nnodes-dimension into a one-dimension vector(two
			// values)
			for (int k = 0; k < vnum; k += 2 * step) {
				for (int o = 0; o < step; o++) {
					int index0 = k + o;
					double tmp0 = tensor.get(index0);

					int index1 = k + o + step;
					double tmp1 = tensor.get(index1);
					// get the bit and times the Corresponding message
					for (int mm = 0; mm < nnodes; mm++) {
						int bit0 = index0 % 2;
						index0 /= 2;
						int bit1 = index1 % 2;
						index1 /= 2;

						if (mm == j)
							continue;

						if (bit0 == 0)
							tmp0 *= (1 - tmpvlist.get(mm));
						else
							tmp0 *= tmpvlist.get(mm);

						if (bit1 == 0)
							tmp1 *= (1 - tmpvlist.get(mm));
						else
							tmp1 *= tmpvlist.get(mm);
					}

					v0 += tmp0;
					v1 += tmp1;
				}
			}
			alledges.get(j).set_fton(v1 / (v1 + v0));
		}
	}

	public double getProb() {
		boolean hasUNKoperator = false;
		if (ops != null)
			for (String op : ops) {
				for (String unk : unkops) {
					if (op.contentEquals(unk))
						hasUNKoperator = true;
				}
			}
		// if(hasUNKoperator)return MEDIUM;
		// hasUNKoperator = false;
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
		if (stmtv) {// if the statement is written correctly.
			if (defv && pu)
				return HIGH;
			if (!defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return HIGH;
			}
			if (!defv && pu)
				return LOW;
			if (defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return LOW;
			}
		} else {
			if (defv && pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return LOW;
			}
			if (!defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return HIGH;
			}
			if (!defv && pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return HIGH;
			}
			if (defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return LOW;
			}
//			if (!defv) {
//				if (hasUNKoperator)
//					return MEDIUM;
//				return HIGH;
//			}
//			// else: def = true
//			else if (pu) {
//				if (hasUNKoperator)
//					return MEDIUM;
//				return LOW;
//			}
//			// def = true stmt = false use = false
//			return LOW;// TODO should be medium when using certain ops.
		}
		return MEDIUM;
	}

	public void print() {
		System.out.print("Statement: ");
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
		if (ops != null) {
			System.out.print("ops:");
			System.out.println(ops);
		}
	}
}
