package ppfl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Line {
	String def;
	Set<Integer> preds;
	Set<String> uses;
	List<String> ops;
	
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

	public Line() {
		this.ismethod = false;
		this.ismethodinvocation = false;
		this.isret = false;
	}

	public void setDef(String s) {
		def = s;
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

	public void addPred(Integer i) {
		if (preds == null) {
			preds = new TreeSet<Integer>();
		}
		preds.add(i);
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
		System.out.println("def:" + def);
		System.out.println("uses:" + uses);
		System.out.println("ops:" + ops);
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
