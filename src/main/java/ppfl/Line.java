package ppfl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

public class Line {
	int linenumber;
	String def;
	Set<String> uses;
	List<String> ops;

	String preddef;
	Set<String> preduses;
	List<String> predops;

	Set<Integer> preds;

	boolean ismethodinvocation;
	String retdef;
	List<Set<String>> arguses;
	List<List<String>> argops;

	boolean isret;
	Set<String> retuses;
	List<String> retops;

	boolean ismethod;
	List<String> argdefs;

	public Line(String _defs, List<String> _uses, List<String> _ops, List<Integer> _preds) {
		def = _defs;
		uses = new TreeSet<String>(_uses);
		ops = new ArrayList<String>(_ops);
		preds = new TreeSet<Integer>(_preds);
	}

	public Line(int ln) {
		this.ismethod = false;
		this.ismethodinvocation = false;
		this.isret = false;
		this.linenumber = ln;
	}

	public void setDef(String s) {
		def = s;
	}

	public void setPredDef(String s) {
		preddef = s;
	}

	public void setRetDef(String s) {
		retdef = s;
	}

	public void addUse(String s) {
		if (uses == null) {
			uses = new TreeSet<String>();
		}
		uses.add(s);
	}

	public void addUses(Collection<String> s) {
		if (uses == null) {
			uses = new TreeSet<String>();
		}
		uses.addAll(s);
	}

	public void addPredUse(String s) {
		if (preduses == null) {
			preduses = new TreeSet<String>();
		}
		preduses.add(s);
	}

	public void addPredUses(Collection<String> s) {
		if (preduses == null) {
			preduses = new TreeSet<String>();
		}
		preduses.addAll(s);
	}

	public void addOp(String s) {
		if (ops == null) {
			ops = new ArrayList<String>();
		}
		ops.add(s);
	}

	public void addOps(Collection<String> s) {
		if (ops == null) {
			ops = new ArrayList<String>();
		}
		ops.addAll(s);
	}

	public void addPredOp(String s) {
		if (predops == null) {
			predops = new ArrayList<String>();
		}
		predops.add(s);
	}

	public void addPredOps(Collection<String> s) {
		if (predops == null) {
			predops = new ArrayList<String>();
		}
		predops.addAll(s);
	}

	public void addPred(Integer i) {
		if (preds == null) {
			preds = new TreeSet<Integer>();
		}
		preds.add(i);
	}

	public void addPreds_single(Stack<Integer> qs) {
		if (preds == null) {
			preds = new TreeSet<Integer>();
		}
		if (!qs.isEmpty()) {
			int t = qs.peek();
			if (t != this.linenumber)
				preds.add(t);
		}
	}

	public void addPreds(Collection<Integer> qs) {
		if (preds == null) {
			preds = new TreeSet<Integer>();
		}
		preds.addAll(qs);
		return;
	}

	public void initMethodInvocation() {
		this.ismethodinvocation = true;
		arguses = new ArrayList<Set<String>>();
		argops = new ArrayList<List<String>>();
	}

	public boolean isMethodInvocation() {
		return this.ismethodinvocation;
	}

	public void initRet() {
		this.isret = true;
		retuses = new TreeSet<String>();
		retops = new ArrayList<String>();
	}

	public void initRet(Set<String> retuses, List<String> retops) {
		this.isret = true;
		this.retuses = retuses;
		this.retops = retops;
	}

	public boolean isRet() {
		return this.isret;
	}

	public void initMethod(List<String> methodargs) {
		this.ismethod = true;
		this.argdefs = methodargs;
	}

	public boolean isMethod() {
		return this.ismethod;
	}

	public void print() {
		if (def != null)
			System.out.println("def:" + def);
		if (uses != null)
			System.out.println("uses:" + uses);
		if (ops != null)
			System.out.println("ops:" + ops);
		if (preddef != null)
			System.out.println("preddef:" + preddef);
		if (preduses != null)
			System.out.println("preduses:" + preduses);
		if (predops != null)
			System.out.println("predops:" + predops);

		if (preds != null)
			System.out.println("preds:" + preds);
		if (this.ismethodinvocation) {
			System.out.println("retdef:" + retdef);
			System.out.println("arguses:" + arguses);
			System.out.println("argops:" + argops);
		}
		if (this.isret) {
			System.out.println("retuses:" + retuses);
			System.out.println("retops:" + retops);
		}
		if (this.ismethod) {
			System.out.println("argdefs:" + argdefs);
		}

	}
}
