package ppfl;

// import java.util.List;

public class Edge {
	// private Node node;
	// private FactorNode factor;
	private double ntof = 0.5;
	private double fton = 0.5;

	public Edge() {

	}

	public void set_ntof(double val) {
		ntof = val;
	}

	public double get_ntof() {
		return ntof;
	}

	public void set_fton(double val) {
		fton = val;
	}

	public double get_fton() {
		return fton;
	}
}
