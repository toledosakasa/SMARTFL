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

    public FactorNode(Node def, Node stmt, List<Node> preds, List<Node> uses, List<String> ops, 
        Edge dedge, Edge sedge,List<Edge> pedges, List<Edge> uedges) {
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
        gettensor(allnodes, nnodes-1);
	}

	// double mysum(double lst[], int n) {
	// int times = (1 << n);
	// double res = 0;
	// for (int i = 0; i < times; i++) {

	// }
	// return 0;
	// }

    private void gettensor(List<Node> allnodes, int cur)
    {
        if (cur < 0) 
        {
        	tensor.add(getProb());
        	return;
        }
        allnodes.get(cur).setTemp(false);
		gettensor(allnodes, cur - 1);
		allnodes.get(cur).setTemp(true);
		gettensor(allnodes, cur - 1);
    }

	public void send_message() {
		// int num = preds.size() + uses.size();
		// double puv[] = new double[num];
		// int i = 0;
		// double put = 1;
		// for (Edge n : puedges) {
		// 	put = put * n.get_ntof();
		// }
		// double dv = dedge.get_ntof();
        // double sv = sedge.get_ntof();
        
        List<Double> tmpvlist = new ArrayList<Double>();


        for (int i = 0; i < nnodes; i++)
        {
            tmpvlist.add(alledges.get(i).get_ntof());
        }

        for (int j = 0; j < nnodes; j++)
        {
            
            double v0 = 0;
            double v1 = 0;
            int step = (1 << j);
            int vnum = (1<<nnodes);
            for (int k = 0; k < vnum; k += 2 * step)
            {
                for (int o = 0; o < step; o++)
                {
                    int index0 = k + o;
                    double tmp0 = tensor.get(index0);

                    int index1 = k + o + step;
                    double tmp1 = tensor.get(index1);

                    for (int mm = 0; mm < nnodes; mm++)
                    {
                        int bit0 = index0 % 2;
                        index0 /= 2;
                        int bit1 = index1 % 2;
                        index1 /= 2;

                        if (mm == j)
                            continue;

                        if (bit0 == 0)
                            tmp0 *= (1-tmpvlist.get(mm));
                        else
                            tmp0 *= tmpvlist.get(mm);

                        if (bit1 == 0)
                            tmp1 *= (1-tmpvlist.get(mm));
                        else
                            tmp1 *= tmpvlist.get(mm);
                    }

                    v0 += tmp0;
                    v1 += tmp1;
                }
            }
            alledges.get(j).set_fton(v1/(v1+v0));
        }

		// double sv1 = HIGH * (dv * put + (1 - dv) * (1 - put)) + LOW * ((1 - dv) * put + dv * (1 - put));
		// double sv0 = HIGH * (1 - dv) + LOW * dv;
		// sedge.set_fton(sv1 / (sv1 + sv0));

		// double dv1 = HIGH * sv * put + LOW * (sv * (1 - put) + (1 - sv));
		// double dv0 = HIGH * ((1 - sv) + sv * (1 - put)) + LOW * sv * put;
		// dedge.set_fton(dv1 / (dv1 + dv0));

		// for (Edge n : puedges) {
		// 	double except_put = put / n.get_ntof();
		// 	double nv1 = HIGH * (sv * dv * except_put + sv * (1 - dv) * (1 - except_put) + (1 - sv) * (1 - dv))
		// 			+ LOW * (sv * dv * (1 - except_put) + sv * (1 - dv) * except_put + (1 - sv) * dv);
		// 	double nv0 = HIGH * (sv * (1 - dv) + (1 - sv) * (1 - dv)) + LOW * (sv * dv + (1 - sv) * dv);
		// 	n.set_fton(nv1 / (nv1 + nv0));
		// }

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
