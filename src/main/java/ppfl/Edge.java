package ppfl;

// import java.util.List;
// import org.apfloat.Apfloat;
// import org.apfloat.ApfloatMath;

public class Edge {
	public static MyWriter debugLogger;

	private Node node;
	private FactorNode factor;
	private double ntof = 0.5;
	private double fton = 0.5;
	// private Apfloat ap_ntof = new Apfloat("0.5", 100);
	// private Apfloat ap_fton = new Apfloat("0.5", 100);

	public Edge() {

	}

	public void setnode(Node n) {
		node = n;
	}

	public Node getnode() {
		return node;
	}

	public void setfactor(FactorNode f) {
		factor = f;
	}

	public FactorNode getfactor() {
		return factor;
	}

	public void set_ntof(double val) {
		ntof = val;
	}

	public double get_ntof() {
		return ntof;
	}

	public void set_fton(double val) {
		fton = val;
		//debugLogger.write("v = %.20f, from %s to %s\n", val, factor.id, node.getPrintName());
	}

	public double get_fton() {
		return fton;
	}

	// public void ap_set_ntof(Apfloat val) {
	// 	ap_ntof = val;
	// }

	// public Apfloat ap_get_ntof() {
	// 	return ap_ntof;
	// }

	// public void ap_set_fton(Apfloat val) {
	// 	ap_fton = val;
	// }

	// public Apfloat ap_get_fton() {
	// 	return ap_fton;
	// }
}
