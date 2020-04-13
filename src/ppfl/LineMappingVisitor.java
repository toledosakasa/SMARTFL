package ppfl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimplePropertyDescriptor;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

public class LineMappingVisitor extends ASTVisitor {
	private final LineInfo lineinfo;
	private Stack<Integer> predqueue;
	private List<String> methodargs;
	private Map<String, Map<Integer, Integer>> vtable;// varname, <domainstart,domainend>
	private Map<String, Map<Integer, Integer>> vmap;// varname, <usepos,defpos(domainstart)>
	private static String predvarname = "__PREDVAR__#";
	private static String retdefname = "__RETVALUE__#";
	private static String constantname = "__CONSTANT__#";

	public LineMappingVisitor(LineInfo l) {
		lineinfo = l;
		predqueue = new Stack<Integer>();
		vtable = new TreeMap<String, Map<Integer, Integer>>();
		vmap = new TreeMap<String, Map<Integer, Integer>>();
		// print(lineinfo.cu);
	}

	private String getNormVarname(String varname, int usepos) {
		return varname + "#" + String.valueOf(getVarDom(varname, usepos));
	}

	private int getVarDom(String varname, int usepos) {
		if (!vtable.containsKey(varname)) {// should not happen. temporal solution for x.f
			return usepos;
		}
		if (vmap.get(varname).containsKey(usepos)) {
			return vmap.get(varname).get(usepos);
		}

		int ret = -1;
		Map<Integer, Integer> m = vtable.get(varname);
		for (Integer k : m.keySet()) {
			// System.out.println(k+" "+usepos + " " +m.get(k));
			if (k <= usepos && usepos <= m.get(k)) {
				ret = k > ret ? k : ret;
			}
		}
		if (ret == -1)
			System.out.println(varname + usepos);
		assert (ret != -1);
		vmap.get(varname).put(usepos, ret);
		return ret;
	}

	private String getSimpleDef(ASTNode ls) {
		return ls.toString();// TODO deal with like x.f returning 'x' but not "x.f"
	}

	private void getUsesAndOps(ASTNode rs, Set<String> uses, List<String> ops) {
		int pos = lineinfo.getLineNumber(rs.getStartPosition());
		List properties = rs.structuralPropertiesForType();
		for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
			Object descriptor = iterator.next();

			if (descriptor instanceof SimplePropertyDescriptor) {
				SimplePropertyDescriptor simple = (SimplePropertyDescriptor) descriptor;
				Object value = rs.getStructuralProperty(simple);
				// System.out.println(simple.getId() + " (" + value.toString() + ")");
				if (simple.getId().equals("identifier")) {
					String s = getNormVarname(value.toString(), pos);
					uses.add(s);
				}
				if (simple.getId().equals("operator")) {
					ops.add(value.toString());
				}
				if (simple.getId().equals("token")) {
					uses.add(getConstName());
				}
			} else if (descriptor instanceof ChildPropertyDescriptor) {
				ChildPropertyDescriptor child = (ChildPropertyDescriptor) descriptor;
				ASTNode childNode = (ASTNode) rs.getStructuralProperty(child);
				if (childNode != null) {
					getUsesAndOps(childNode, uses, ops);
				}
			} else {
				ChildListPropertyDescriptor list = (ChildListPropertyDescriptor) descriptor;
				// System.out.println("List (" + list.getId() + "){");
				getUsesAndOps((List) rs.getStructuralProperty(list), uses, ops);
				// System.out.println("}");
			}
		}
	}

	private void getUsesAndOps(List nodes, Set<String> uses, List<String> ops) {
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
			ASTNode node = (ASTNode) iterator.next();
			getUsesAndOps(node, uses, ops);
		}
	}

	public ASTNode getparentstatement(ASTNode node) {
		while (!(node instanceof Statement)) {
			node = node.getParent();
		}
		return node;
	}

	public ASTNode getparentBlock(ASTNode node) {
		while (!(node instanceof Block || node instanceof BodyDeclaration || node instanceof ForStatement)) {
			node = node.getParent();
		}
		return node;
	}

	public static String getRetDefName(int pos) {
		return retdefname + String.valueOf(pos);
	}

	public static String getPredName(int pos) {
		return predvarname + String.valueOf(pos);
	}

	public static String getConstName() {
		return constantname;
	}

	public void varDeclare(VariableDeclaration node) {
		int pos = lineinfo.getLineNumber(node.getStartPosition());

		// System.out.print("At pos " + pos + " : ");
		Set<String> uses = new TreeSet<String>();
		List<String> ops = new ArrayList<String>();
		String defname = node.getName().getIdentifier();

		ASTNode parent = getparentBlock(node);
		int domstart = lineinfo.getLineNumber(parent.getStartPosition());
		int domend = lineinfo.getLineNumber(parent.getStartPosition() + parent.getLength());

		System.out.println("VDef:" + defname + "," + String.valueOf(domstart) + "-" + String.valueOf(domend));

		if (!vtable.containsKey(defname))
			vtable.put(defname, new TreeMap<Integer, Integer>());
		if (!vmap.containsKey(defname))
			vmap.put(defname, new TreeMap<Integer, Integer>());
		vtable.get(defname).put(domstart, domend);
		// System.out.println(vtable.get(defname));
		// print(node);
		// List properties = node.structuralPropertiesForType();

//		for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
//			Object descriptor = iterator.next();
//			if (descriptor instanceof ChildPropertyDescriptor) {
//				ChildPropertyDescriptor child = (ChildPropertyDescriptor) descriptor;
//				if (child.getId().equals("initializer")) {
//					ASTNode childNode = (ASTNode) node.getStructuralProperty(child);
//					getUsesAndOps(childNode, uses, ops);
//				}
//			}
//		}
		if (node.getInitializer() != null) {
			ASTNode childNode = node.getInitializer();
			getUsesAndOps(childNode, uses, ops);
			lineinfo.getLine(pos).setDef(getNormVarname(defname, pos));
			lineinfo.getLine(pos).addOps(ops);
			lineinfo.getLine(pos).addUses(uses);
			lineinfo.getLine(pos).addPreds_single(predqueue);
		}

	}

	public boolean visit(VariableDeclarationFragment node) {
		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		initLine(l);
		varDeclare(node);
		return true;
	}

	public boolean visit(SingleVariableDeclaration node) {

		varDeclare(node);
		return true;
	}

	public void initLine(Line l) {
		if (methodargs != null && !methodargs.isEmpty()) {
			l.initMethod(methodargs);
			methodargs = null;
		}
	}

	public boolean visit(Assignment node) {
		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		initLine(l);
		return true;
	}

	public void endVisit(Assignment node) {
		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		String defname = getSimpleDef(node.getLeftHandSide());

//		if (l.isMethodInvocation()) {
//			l.setRetDef(getNormVarname(defname, pos));
//			return;
//		}
		// System.out.println(defname+String.valueOf(pos)+getVarDom(defname, pos));
		l.setDef(getNormVarname(defname, pos));
		Set<String> uses = new TreeSet<String>();
		List<String> ops = new ArrayList<String>();
		getUsesAndOps(node.getRightHandSide(), uses, ops);
		l.addUses(uses);
		l.addOps(ops);
		l.addPreds_single(predqueue);
		// print(node.getRightHandSide());
	}

	public void visitBranch(Statement node) {
		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		initLine(l);
//		l.setPredDef(getPredName(pos));
//		Set<String> uses = new TreeSet<String>();
//		List<String> ops = new ArrayList<String>();
//		if (node instanceof IfStatement)
//			getUsesAndOps(((IfStatement) node).getExpression(), uses, ops);
//		if (node instanceof WhileStatement)
//			getUsesAndOps(((WhileStatement) node).getExpression(), uses, ops);
//		if (node instanceof DoStatement)
//			getUsesAndOps(((DoStatement) node).getExpression(), uses, ops);
//		if (node instanceof ForStatement)
//			getUsesAndOps(((ForStatement) node).getExpression(), uses, ops);
//
//		l.addPredUses(uses);
//		l.addPredOps(ops);
		predqueue.push(pos);
	}

	public void endVisitBranch(Statement node) {
		predqueue.pop();
		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		// initLine(l);
		l.setPredDef(getPredName(pos));
		Set<String> uses = new TreeSet<String>();
		List<String> ops = new ArrayList<String>();
		if (node instanceof IfStatement)
			getUsesAndOps(((IfStatement) node).getExpression(), uses, ops);
		if (node instanceof WhileStatement)
			getUsesAndOps(((WhileStatement) node).getExpression(), uses, ops);
		if (node instanceof DoStatement)
			getUsesAndOps(((DoStatement) node).getExpression(), uses, ops);
//		if (node instanceof ForStatement)
//			getUsesAndOps(((ForStatement) node).getExpression(), uses, ops);

		l.addPredUses(uses);
		l.addPredOps(ops);
		l.addPreds_single(predqueue);

	}

	public boolean visit(IfStatement node) {
		visitBranch(node);
		return true;// TODO merge else branch defs.
	}

	public void endVisit(IfStatement node) {
		endVisitBranch(node);
		return;
	}

	public boolean visit(WhileStatement node) {
		visitBranch(node);
		return true;// TODO merge else branch defs.
	}

	public void endVisit(WhileStatement node) {
		endVisitBranch(node);
		return;
	}

	public boolean visit(DoStatement node) {
		visitBranch(node);
		return true;// TODO merge else branch defs.
	}

	public void endVisit(DoStatement node) {
		endVisitBranch(node);
		return;
	}

	public boolean visit(ForStatement node) {
		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		initLine(l);
		predqueue.push(pos);
		return true;// TODO merge else branch defs.
	}

	public void endVisit(ForStatement node) {
		predqueue.pop();
		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		l.setPredDef(getPredName(pos));
		Set<String> uses = new TreeSet<String>();
		List<String> ops = new ArrayList<String>();
		getUsesAndOps(node.getExpression(), uses, ops);// TODO
		l.addPredUses(uses);
		l.addPredOps(ops);
		l.addPreds_single(predqueue);
		return;
	}
	// TODO EnhancedForStatement

	public boolean visit(MethodInvocation node) {
		// System.out.println(node.arguments());
		// System.out.println(node.getName());
		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		initLine(l);

		l.initMethodInvocation();
		l.setRetDef(getRetDefName(pos));
		List<Expression> args = node.arguments();
		for (Expression arg : args) {
			Set<String> uses = new TreeSet<String>();
			List<String> ops = new ArrayList<String>();
			getUsesAndOps(arg, uses, ops);
			l.arguses.add(uses);
			l.argops.add(ops);
		}
		return true;
	}

	public boolean visit(MethodDeclaration node) {

		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		initLine(l);

		List<SingleVariableDeclaration> pars = node.parameters();

		List<String> argdefs = new ArrayList<String>();
		for (SingleVariableDeclaration tmp : pars) {
			String s = tmp.getName().getIdentifier();
			argdefs.add(getNormVarname(s, pos));
		}
		if (!argdefs.isEmpty()) {
			methodargs = argdefs;
		}
		return true;
	}

	public boolean visit(ReturnStatement node) {
		int pos = lineinfo.getLineNumber(node.getStartPosition());
		Line l = lineinfo.getLine(pos);
		initLine(l);

		Set<String> uses = new TreeSet<String>();
		List<String> ops = new ArrayList<String>();
		getUsesAndOps(node.getExpression(), uses, ops);
		l.initRet(uses, ops);
		l.addPreds_single(predqueue);

		return true;
	}

	private void print(ASTNode node) {
		List properties = node.structuralPropertiesForType();

		for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
			Object descriptor = iterator.next();

			if (descriptor instanceof SimplePropertyDescriptor) {
				SimplePropertyDescriptor simple = (SimplePropertyDescriptor) descriptor;
				Object value = node.getStructuralProperty(simple);
				System.out.println(simple.getId() + " (" + value.toString() + ")");
			} else if (descriptor instanceof ChildPropertyDescriptor) {
				ChildPropertyDescriptor child = (ChildPropertyDescriptor) descriptor;
				ASTNode childNode = (ASTNode) node.getStructuralProperty(child);
				if (childNode != null) {
					System.out.println("Child (" + child.getId() + ") {");
					print(childNode);
					System.out.println("}");
				}
			} else {
				ChildListPropertyDescriptor list = (ChildListPropertyDescriptor) descriptor;
				System.out.println("List (" + list.getId() + "){");
				print((List) node.getStructuralProperty(list));
				System.out.println("}");
			}
		}
	}

	private void print(List nodes) {
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
			ASTNode node = (ASTNode) iterator.next();
			print(node);
		}
	}
}
