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

    private Edge dedge;
    private Edge sedge;
    private List<Edge> puedges;

    public FactorNode(Node def, Node stmt, List<Node> preds, List<Node> uses, 
                        Edge dedge, Edge sedge, List<Edge> puedges) {
        this.preds = preds;
        this.stmt = stmt;
        this.def = def;
        this.uses = uses;
        this.dedge = dedge;
        this.sedge = sedge;
        this.puedges = puedges;
    }

    // double mysum(double lst[], int n) {
    //     int times = (1 << n);
    //     double res = 0;
    //     for (int i = 0; i < times; i++) {
            
    //     }
    //     return 0;
    // }

    public void send_message() {
        // int num = preds.size() + uses.size();
        // double puv[] = new double[num];
        // int i = 0;
        double put = 1;
        for (Edge n : puedges) {
            put = put * n.get_ntof();
        }
        double dv = dedge.get_ntof();
        double sv = sedge.get_ntof();

        double sv1 = HIGH * (dv * put + (1-dv) * (1-put)) + LOW *((1-dv) * put + dv * (1-put));
        double sv0 = HIGH * (1 - dv) + LOW * dv;
        sedge.set_fton(sv1 / (sv1 + sv0));

        double dv1 = HIGH * sv * put + LOW * (sv * (1-put) + (1-sv));
        double dv0 = HIGH * ((1-sv) +sv * (1-put)) + LOW * sv * put;
        dedge.set_fton(dv1 / (dv1 + dv0));

        for (Edge n : puedges) {
            double except_put = put/n.get_ntof();
            double nv1 = HIGH * (sv * dv * except_put + sv * (1-dv) * (1-except_put)) + (1-sv) * (1-dv)
                        + LOW * (sv * dv * (1-except_put) + sv * (1-dv)*except_put) + (1-sv) * dv;
            double nv0 = HIGH * (sv * (1-dv) + (1-sv) * (1-dv)) + LOW * (sv * dv + (1-sv) * dv);
            n.set_fton(nv1 / (nv1 + nv0));
        }

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
