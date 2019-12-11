package ppfl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Line {
	String def;
	Set<String> uses;
	List<String> ops;
	Set<Integer> preds;

	public Line(String _defs, List<String> _uses, List<String> _ops, List<Integer> _preds) {
		def = _defs;
		uses = new TreeSet<String>(_uses);
		ops = new ArrayList<String>(_ops);
		preds = new TreeSet<Integer>(_preds);
	}

	public Line() {
		def = "";
		uses = new TreeSet<String>();
		ops = new ArrayList<String>();
		preds = new TreeSet<Integer>();
	}

	public void setDef(String s) {
		def = s;
	}

	public void addUse(String s) {
		uses.add(s);
	}

	public void addUses(Collection<String> s)
	{
		uses.addAll(s);
	}
	
	public void addOp(String s) {
		ops.add(s);
	}
	
	public void addOps(Collection<String> s) {
		ops.addAll(s);
	}

	public void addPred(Integer i) {
		preds.add(i);
	}

	public void addPreds(Collection<Integer> qs) {
			preds.addAll(qs);
		return;
	}
	public void print()
	{
		System.out.println("def:"+def);
		System.out.println("uses:"+uses);
		System.out.println("ops:"+ops);
		System.out.println("preds:"+preds);
	}
}
