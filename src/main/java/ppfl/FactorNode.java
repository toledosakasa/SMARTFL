package ppfl;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

// import org.apfloat.Apfloat;
// import org.apfloat.ApfloatMath;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class FactorNode {

	//protected static MyWriter debugLogger = WriterUtils.getWriter("Debugger");
	protected static MyWriter printLogger = WriterUtils.getWriter("GraphLogger");

	public static MyWriter debugLogger;

	public static boolean nontrivial = false;
	
	public static int count = 0;
	public int id;
	
	private List<Node> preds;
	private Node def;
	private Node stmt;
	private List<Node> uses;
	private List<String> ops;// TODO consider operators
	private static final String[] unkops = { "%", "<", "<=", ">", ">=", "==", "!=" };
	private double HIGH = 1.0; // can change to 1-1e-10
	private double VHIGH = 0.99999;
	private double MEDIUM_HIGH = 0.5;
	private double MEDIUM = 0.5;
	private double MEDIUM_LOW=0.5;
	private double LOW = 0.0; // can change to 1e-10
	private List<Double> tensor;
	// private Apfloat ap_HIGH = new Apfloat("1.0", 100);
	// private Apfloat ap_MEDIUM = new Apfloat("0.5", 100);
	// private Apfloat ap_LOW = new Apfloat("0.0", 100);
	// private List<Apfloat> ap_tensor;
	private boolean use_ap = false;
	private List<Node> allnodes;
	private List<Edge> alledges;
	private int nnodes;
	private double stmtvalue;
	boolean hasUNKoperator = true;

	private Edge dedge;
	private Edge sedge;
	private List<Edge> pedges;
	private List<Edge> uedges;
	//private static List<Double> numbersArray2 = {0,2.3283064365e-10,5.4210108624e-20,0.00390625,1.5258789063e-5,0.3333333333,0.5};
	// private static List<Double> numbersArray2;
	private static List<Double> numbersArray;
	static {
		// numbersArray2 = new ArrayList<>();
		// numbersArray2.add(0.0);
		// numbersArray2.add(2.3283064365e-10);
		// numbersArray2.add(5.4210108624e-20);
		// numbersArray2.add(0.00390625);
		// numbersArray2.add(1.5258789063e-5);
		// numbersArray2.add(0.3333333333);
		// numbersArray2.add(0.5);

		try {
			BufferedReader readTxt=new BufferedReader(new FileReader("./infer.txt"));
			String str = readTxt.readLine();
			String[] numbersArray_s=str.split(",");
			numbersArray = new ArrayList<>();
			for(String tmp : numbersArray_s)
				numbersArray.add(Double.parseDouble(tmp));

		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	private static final int[][] spopcodes = {{108,109,110,111},{112,113,114,115},{126,127,128,129,130,131},{136,139,140,142,143,144,147},{148,149,150,151,152},{153,154,155,156,157,158},{159,160,161,162,163,164},{165,166},{198,199}}; 
	//private static final int[][] spopcodes ={{96,98,100,102,104,106,108,110,112,114,116,118,120,122,124,126,128,130,132,134,136,137,139,142,144},{97,99,101,103,105,107,109,111,113,115,117,119,121,123,125,127,129,131,135,138,140,141,143},{145},{146,147},{148,149,150,151,152},{153,154,155,156,157,158,159,160,161,162,163,164,165,166,198,199}};

	public FactorNode() {
		this.stmt = null;
		this.def = null;
		this.dedge = null;
	}

	// factor with only a stmt node
	public FactorNode(Node stmt, Edge sedge, double value) {
		this.stmt = stmt;
		this.def = null;
		this.dedge = null;
		this.sedge = sedge;
		this.tensor = new ArrayList<>();
		// this.ap_tensor = new ArrayList<>();
		this.nnodes = 1;
		this.stmtvalue = value;
		this.alledges = new ArrayList<>();
		alledges.add(sedge);
		if(use_ap){
			// ap_tensor.add(new Apfloat("1.0", 100).subtract(new Apfloat(String.valueOf(value),100)));
			// ap_tensor.add(new Apfloat(String.valueOf(value),100));
		}
		else{
			tensor.add(1 - value);
			tensor.add(value);
		}
	}

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
		this.tensor = new ArrayList<>();
		// this.ap_tensor = new ArrayList<>();
		this.allnodes = new ArrayList<>();
		allnodes.add(stmt);
		allnodes.add(def);
		allnodes.addAll(preds);
		allnodes.addAll(uses);
		this.alledges = new ArrayList<>();
		alledges.add(sedge);
		alledges.add(dedge);
		alledges.addAll(pedges);
		alledges.addAll(uedges);
		this.nnodes = allnodes.size();
		if (this.ops != null)
			for (String op : this.ops) {
				for (String unk : unkops) {
					if (op.contentEquals(unk))
					this.hasUNKoperator = true;
				}
			}
		change_parameters();
		
		// if (use_ap)
		// 	ap_gettensor(allnodes, nnodes - 1);
		// else
		count += 1;
		this.id = count;

		if(nontrivial){
			gettensor(allnodes, nnodes - 1);
			// if(id == 201){
			// 	debugLogger.write("id = %d, nnodes = %d\n", id, nnodes);
			// 	for(Double d  : tensor)
			// 		debugLogger.write("%f, ", d);
			// 	debugLogger.write("\n");
			// }
		}
	}

	public List<Node> getpunodes() {
		ArrayList<Node> ret = new ArrayList<>();
		ret.addAll(preds);
		ret.addAll(uses);
		return ret;
	}

	public Node getstmt() {
		return this.stmt;
	}

	private void gettensor(List<Node> allnodes, int cur) {
		if (cur < 0) {
			double prob = getProb();
			// if(id == 201)
			// 	debugLogger.write("getprob = %f\n", prob);
			tensor.add(prob);
			return;
		}
		allnodes.get(cur).setTemp(false);
		// if(id == 201 && cur == 5)
		// 	debugLogger.write("set u3 = %b\n", allnodes.get(cur).getCurrentValue());
		// if(id == 201 && cur == 6)
		// 	debugLogger.write("set u4 = %b\n", allnodes.get(cur).getCurrentValue());
		gettensor(allnodes, cur - 1);
		allnodes.get(cur).setTemp(true);
		// if(id == 201 && cur == 5)
		// 	debugLogger.write("set u3 = %b\n", allnodes.get(cur).getCurrentValue());
		// if(id == 201 && cur == 6)
		// 	debugLogger.write("set u4 = %b\n", allnodes.get(cur).getCurrentValue());
		gettensor(allnodes, cur - 1);
	}

	// private void ap_gettensor(List<Node> allnodes, int cur) {
	// 	if (cur < 0) {
	// 		ap_tensor.add(ap_getProb());
	// 		return;
	// 	}
	// 	allnodes.get(cur).setTemp(false);
	// 	ap_gettensor(allnodes, cur - 1);
	// 	allnodes.get(cur).setTemp(true);
	// 	ap_gettensor(allnodes, cur - 1);
	// }

	public void sendMessage(){
		List<Edge> puedges = new ArrayList<>();
		puedges.addAll(pedges);
		puedges.addAll(uedges);

		double put = 1;
		for (Edge n : puedges) 
			put = put * n.get_ntof();

		double dv = dedge.get_ntof();
		double sv = sedge.get_ntof();

		// if(id == 16){
		// 	double u1 = uedges.get(0).get_ntof();
		// 	double p1 = pedges.get(0).get_ntof();
		// 	debugLogger.write("sv = %.20f, dv = %.20f, u1 = %.20f, p1 = %.20f\n", dv, sv, u1, p1);
		// }

		double sv1 = HIGH * dv * put + MEDIUM_HIGH * (1 - dv) * (1 - put) + LOW * (1 - dv) * put + MEDIUM_LOW * dv * (1 - put);
		//double sv1 = 1.98 * dv * put + 0.99 - 0.98 *dv - 0.99 * put;
		double sv0 = MEDIUM_HIGH * (1 - dv) + MEDIUM_LOW * dv;
		sedge.set_fton(sv1 / (sv1 + sv0));

		// if(id == 16){
		// 	debugLogger.write("sv1 = %.20f, sv0 = %.20f,\n", sv1, sv0);
		// }

		double dv1 = HIGH * sv * put + MEDIUM_LOW * (1 - sv * put);
		double dv0 = MEDIUM_HIGH * (1 - sv * put) + LOW * sv * put;
		// if(id == 201){
		// 	debugLogger.write("dv1 = %.10f, dv0 = %.10f,\n", dv1, dv0);
		// }
		dedge.set_fton(dv1 / (dv1 + dv0));

		for (Edge n : puedges) {
			double spu = sv * put / n.get_ntof();
			if(Double.isNaN(spu)){
				spu = sv;
				for (Edge e : puedges)
					if(e != n) 
						spu = spu * e.get_ntof();
			}
			double nv1 = HIGH * spu * dv + MEDIUM_LOW * (1 - spu) * dv + LOW * spu * (1-dv) + MEDIUM_HIGH * (1- spu) * (1-dv); 
			double nv0 = MEDIUM_HIGH * (1 - dv) + MEDIUM_LOW * dv;
			//debugLogger.write("put = %f, n = %f,  nv1 = %f, nv0 = %f\n",put, n.get_ntof(), nv1, nv0);
			n.set_fton(nv1 / (nv1 + nv0));
		}

	}

	public void send_message() {
		if (!use_ap) {
			// used to save all the messages from the nodes
			List<Double> tmpvlist = new ArrayList<>();
			for (int i = 0; i < nnodes; i++) {
				tmpvlist.add(alledges.get(i).get_ntof());
			}
			// if(id == 201){
			// 	for(Double prob :tmpvlist)
			// 		debugLogger.write(prob + " ,");
			// 	debugLogger.write("\n");
			// }
			// System.out.println("tmplist = "+tmpvlist);
			for (int j = 0; j < nnodes; j++) {
				double v0 = 0;
				double v1 = 0;
				int step = (1 << j);
				int vnum = (1 << nnodes);
				// transform a tensor of nnodes-dimension into a one-dimension vector(two
				// values)
				// if(id == 201)
				// 	debugLogger.write("start\n");
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

							if (bit0 == 0) {
								// double tmp00 = tmp0* (1 - tmpvlist.get(mm));
								// if(Double.isNaN(tmp00))
								// System.out.println("in 0 , tmp0 = "+tmp0+", val = "+(1 - tmpvlist.get(mm)));
								tmp0 *= (1 - tmpvlist.get(mm));

							} else {
								// double tmp01 = tmp0* tmpvlist.get(mm);
								// if(Double.isNaN(tmp01))
								// System.out.println("in 1 , tmp0 = "+tmp0+", val = "+tmpvlist.get(mm));
								tmp0 *= tmpvlist.get(mm);
							}

							if (bit1 == 0) {
								// double tmp10 = tmp1* (1 - tmpvlist.get(mm));
								// if(Double.isNaN(tmp10))
								// System.out.println("in 0 , tmp1 = "+tmp1+", val = "+(1 - tmpvlist.get(mm)));
								tmp1 *= (1 - tmpvlist.get(mm));

							} else {
								// double tmp11 = tmp1*tmpvlist.get(mm);
								// if(Double.isNaN(tmp11))
								// System.out.println("in 0 , tmp1 = "+tmp1+", val = "+tmpvlist.get(mm));
								tmp1 *= tmpvlist.get(mm);
							}
						}

						v0 += tmp0;
						v1 += tmp1;
						// if(id == 201)
						// 	debugLogger.write("tmp0 = %.10f, tmp1 = %.10f, v0 = %.10f, v1 = %.10f  \n", tmp0, tmp1, v0, v1);
					}
				}
				// if(v1 + v0 == 0.0){
				// System.out.println("one 0 detected");
				// alledges.get(j).set_fton(0.0);
				// }
				// else
				// if(Double.isNaN(v1 / (v1 + v0))){
				// System.out.println("find nan , v1 = "+v1+", v0 = "+v0);
				// }
				alledges.get(j).set_fton(v1 / (v1 + v0));
			}
		}
		else{
			// // used to save all the messages from the nodes
			// List<Apfloat> tmpvlist = new ArrayList<>();
			// for (int i = 0; i < nnodes; i++) {
			// 	tmpvlist.add(alledges.get(i).ap_get_ntof());
			// }
			// // System.out.println("tmplist = "+tmpvlist);
			// for (int j = 0; j < nnodes; j++) {
			// 	Apfloat v0 = new Apfloat("0.0", 100);
			// 	Apfloat v1 = new Apfloat("0.0", 100);
			// 	int step = (1 << j);
			// 	int vnum = (1 << nnodes);
			// 	// transform a tensor of nnodes-dimension into a one-dimension vector(two
			// 	// values)
			// 	for (int k = 0; k < vnum; k += 2 * step) {
			// 		for (int o = 0; o < step; o++) {
			// 			int index0 = k + o;
			// 			Apfloat tmp0 = ap_tensor.get(index0);

			// 			int index1 = k + o + step;
			// 			Apfloat tmp1 = ap_tensor.get(index1);
			// 			// get the bit and times the Corresponding message
			// 			for (int mm = 0; mm < nnodes; mm++) {
			// 				int bit0 = index0 % 2;
			// 				index0 /= 2;
			// 				int bit1 = index1 % 2;
			// 				index1 /= 2;

			// 				if (mm == j)
			// 					continue;

			// 				if (bit0 == 0) {
			// 					// double tmp00 = tmp0* (1 - tmpvlist.get(mm));
			// 					// if(Double.isNaN(tmp00))
			// 					// System.out.println("in 0 , tmp0 = "+tmp0+", val = "+(1 - tmpvlist.get(mm)));
			// 					// tmp0 *= (1 - tmpvlist.get(mm));
			// 					tmp0 = tmp0.multiply(new Apfloat("1.0", 100).subtract(tmpvlist.get(mm)));

			// 				} else {
			// 					// double tmp01 = tmp0* tmpvlist.get(mm);
			// 					// if(Double.isNaN(tmp01))
			// 					// System.out.println("in 1 , tmp0 = "+tmp0+", val = "+tmpvlist.get(mm));
			// 					// tmp0 *= tmpvlist.get(mm);
			// 					tmp0 = tmp0.multiply(tmpvlist.get(mm));
			// 				}

			// 				if (bit1 == 0) {
			// 					// double tmp10 = tmp1* (1 - tmpvlist.get(mm));
			// 					// if(Double.isNaN(tmp10))
			// 					// System.out.println("in 0 , tmp1 = "+tmp1+", val = "+(1 - tmpvlist.get(mm)));
			// 					// tmp1 *= (1 - tmpvlist.get(mm));
			// 					tmp1 = tmp1.multiply(new Apfloat("1.0", 100).subtract(tmpvlist.get(mm)));

			// 				} else {
			// 					// double tmp11 = tmp1*tmpvlist.get(mm);
			// 					// if(Double.isNaN(tmp11))
			// 					// System.out.println("in 0 , tmp1 = "+tmp1+", val = "+tmpvlist.get(mm));
			// 					// tmp1 *= tmpvlist.get(mm);
			// 					tmp1 = tmp1.multiply(tmpvlist.get(mm));
			// 				}
			// 			}

			// 			v0 = v0.add(tmp0);
			// 			v1 = v1.add(tmp1);

			// 			// v0 += tmp0;
			// 			// v1 += tmp1;
			// 		}
			// 	}
			// 	// if(v1 + v0 == 0.0){
			// 	// System.out.println("one 0 detected");
			// 	// alledges.get(j).set_fton(0.0);
			// 	// }
			// 	// else
			// 	// if(Double.isNaN(v1 / (v1 + v0))){
			// 	// System.out.println("find nan , v1 = "+v1+", v0 = "+v0);
			// 	// }
			// 	// alledges.get(j).set_fton(v1 / (v1 + v0));
			// 	alledges.get(j).ap_set_fton(v1.divide(v1.add(v0)));
			// }
		}
	}

	private void change_parameters(){
		StmtNode tstmt = (StmtNode)stmt;
		int tform = tstmt.form;
		//boolean special = false;
		//for (int i : spopcodes){
		//	if (tform == i)
		//		special = true;
		//}
		int special = 0;
		for (int group=0; group < spopcodes.length; group++){
			for (int i: spopcodes[group]){
				if (tform == i)
					special = group + 1; //0 for determined instruction
			}
			if(special != 0)
				break;
		}
		MEDIUM_LOW = numbersArray.get(special);
		//MEDIUM_LOW = numbersArray2.get(special);
		MEDIUM_HIGH = 1-MEDIUM_LOW;	
	}

	public double getProb() {
		// boolean hasUNKoperator = false;
		// if (ops != null)
		// 	for (String op : ops) {
		// 		for (String unk : unkops) {
		// 			if (op.contentEquals(unk))
		// 				hasUNKoperator = true;
		// 		}
		// 	}
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

		// if(id == 201){
		// 	boolean u1 = uses.get(0).getCurrentValue();
		// 	boolean u2 = uses.get(1).getCurrentValue();
		// 	boolean u3 = uses.get(2).getCurrentValue();
		// 	boolean u4 = uses.get(3).getCurrentValue();
		// 	boolean p1 = preds.get(0).getCurrentValue();
		// 	debugLogger.write("%b, %b, %b, %b, %b, %b, %b\n", stmtv, defv, p1, u1, u2, u3, u4);
		// }

		if (stmtv) {// if the statement is written correctly.
			if (defv && pu)
				return HIGH;
			if (!defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM_HIGH;
				return HIGH;
			}
			if (!defv && pu)
				return LOW;
			if (defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM_LOW;
				return LOW;
			}
		} else {
			if (defv && pu) {
				if (hasUNKoperator)
					return MEDIUM_LOW;
				return LOW;
			}
			if (!defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM_HIGH;
				return HIGH;
			}
			if (!defv && pu) {
				if (hasUNKoperator)
					return MEDIUM_HIGH;
				return HIGH;
			}
			if (defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM_LOW;
				return LOW;
			}
			// if (!defv) {
			// if (hasUNKoperator)
			// return MEDIUM;
			// return HIGH;
			// }
			// // else: def = true
			// else if (pu) {
			// if (hasUNKoperator)
			// return MEDIUM;
			// return LOW;
			// }
			// // def = true stmt = false use = false
			// return LOW;// TODO should be medium when using certain ops.
		}
		return MEDIUM;
	}

	// public Apfloat ap_getProb() {
	// 	boolean hasUNKoperator = false;
	// 	if (ops != null)
	// 		for (String op : ops) {
	// 			for (String unk : unkops) {
	// 				if (op.contentEquals(unk))
	// 					hasUNKoperator = true;
	// 			}
	// 		}
	// 	// if(hasUNKoperator)return MEDIUM;
	// 	// hasUNKoperator = false;
	// 	boolean defv = def.getCurrentValue();
	// 	boolean predv = true;
	// 	boolean usev = true;
	// 	boolean stmtv = stmt.getCurrentValue();
	// 	if (preds != null)
	// 		for (Node p : preds) {
	// 			if (!p.getCurrentValue()) {
	// 				predv = false;
	// 				break;
	// 			}
	// 		}
	// 	if (uses != null)
	// 		for (Node u : uses) {
	// 			if (!u.getCurrentValue()) {
	// 				usev = false;
	// 				break;
	// 			}
	// 		}
	// 	boolean pu = predv && usev;
	// 	if (stmtv) {// if the statement is written correctly.
	// 		if (defv && pu)
	// 			return ap_HIGH;
	// 		if (!defv && !pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_HIGH;
	// 		}
	// 		if (!defv && pu)
	// 			return ap_LOW;
	// 		if (defv && !pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_LOW;
	// 		}
	// 	} else {
	// 		if (defv && pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_LOW;
	// 		}
	// 		if (!defv && !pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_HIGH;
	// 		}
	// 		if (!defv && pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_HIGH;
	// 		}
	// 		if (defv && !pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_LOW;
	// 		}
	// 		// if (!defv) {
	// 		// if (hasUNKoperator)
	// 		// return MEDIUM;
	// 		// return HIGH;
	// 		// }
	// 		// // else: def = true
	// 		// else if (pu) {
	// 		// if (hasUNKoperator)
	// 		// return MEDIUM;
	// 		// return LOW;
	// 		// }
	// 		// // def = true stmt = false use = false
	// 		// return LOW;// TODO should be medium when using certain ops.
	// 	}
	// 	return ap_MEDIUM;
	// }

	public void print(MyWriter lgr) {

		stmt.print(lgr, "Statement: ");
		if (def != null) {
			lgr.writeln("\tdef:");
			def.print(lgr, "\t\t");
		} else {
			lgr.writeln("\tstmtvalue = " + this.stmtvalue);
		}

		if (uses != null) {
			lgr.writeln("\tuses:");
			for (Node n : uses) {
				n.print(lgr, "\t\t");
			}
		}
		if (preds != null) {
			lgr.writeln("\tpreds:");
			for (Node n : preds) {
				n.print(lgr, "\t\t");
			}
		}
		if (ops != null) {
			lgr.writeln("\tops:");
			for (String eachop : ops) {
				lgr.writeln("\t\t{}", eachop);
			}
		}
	}

	public void print() {
		stmt.print("Statement: ");
		if (def != null) {
			debugLogger.writeln("\tdef:");
			def.print("\t\t");
		} else {
			debugLogger.writeln("\tstmtvalue = " + this.stmtvalue);
		}

		if (uses != null) {
			debugLogger.writeln("\tuses:");
			for (Node n : uses) {
				n.print("\t\t");
			}
		}
		if (preds != null) {
			debugLogger.writeln("\tpreds:");
			for (Node n : preds) {
				n.print("\t\t");
			}
		}
		if (ops != null) {
			debugLogger.writeln("\tops:");
			for (String eachop : ops) {
				debugLogger.writeln("\t\t{}", eachop);
			}
		}
	}
}
