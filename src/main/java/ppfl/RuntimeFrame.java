package ppfl;

import java.util.HashMap;
import java.util.Map;
import java.util.Deque;
import java.util.Set;
import java.util.ArrayDeque;

import ppfl.instrumentation.TraceDomain;

public class RuntimeFrame {
	// public int entercnt;
	private Deque<Node> runtimeStack;
	private int stackCount;
	private Map<Integer, Node> varMap;
	private Deque<Node> predStack;
	private int predCount;
	private Deque<Set<Integer>> storeStack;
	private TraceDomain domain;
	private InvokeItem invoke;
	private boolean valid;

	public RuntimeFrame(TraceDomain domain, boolean valid) {
		runtimeStack = new ArrayDeque<>();
		stackCount = 0;
		varMap = new HashMap<>();
		predStack = new ArrayDeque<>();
		predCount = 0;
		storeStack = new ArrayDeque<>();
		this.domain = domain;
		this.valid = valid;
		invoke = null;
	}

	public boolean isValid(){
		return valid;
	}

	public void pushStack(Node stacknode){
		runtimeStack.push(stacknode);
	}

	public Node pushStack(String testname, StmtNode stmt){
		String nodename = this.getName() + "#" + "stack-" + stackCount;
		stackCount += 1;
		Node stacknode = new Node(nodename, testname, stmt);
		runtimeStack.push(stacknode);
		return stacknode;
	}

	public Node popStack(){
		Node ret = runtimeStack.peek();
		if(ret != null)
		runtimeStack.pop();
		return ret;
	}

	public Node peekStack(){
		return runtimeStack.peek();
	}

	public void printStack(MyWriter debugLogger){
		debugLogger.write("\n	stack node = ");
		for(Node node: runtimeStack)
			debugLogger.write(node.getNodeName()+ ",");
	}

	public void clearStack(){
		runtimeStack.clear();
		predStack.clear();
		storeStack.clear();
		// invoke may lead to NullPointerException or for some untraced invoke, not return
		cleanInvoke();
	}

	public Node putMap(Integer index, String testname, StmtNode stmt){
        String nodename = this.getName() + "#" + "var-" + index;
		int defcnt = 0;
		if(varMap.get(index) != null)
			defcnt = varMap.get(index).defcnt;
		Node varnode = new Node(nodename, testname, stmt, defcnt);
		varMap.put(index, varnode);
		return varnode;
	}

	public Node getMap(Integer id){
		return varMap.get(id);
	}

	public void printMap(MyWriter debugLogger){
		debugLogger.write("\n	map node = ");
		for(Integer nodename: varMap.keySet())
            debugLogger.write(nodename+ ",");
	}


	public Node popPred() {
		return this.predStack.pop();
	}

	public Node pushPred(String testname, StmtNode stmt) {

		String nodename = this.getName() + "#" + "pred-" + stmt.name + "-" + predCount;
		predCount += 1;
		Node prednode = new Node(nodename, testname, stmt);
		this.predStack.push(prednode);
		return prednode;
	}

	public Node getPred() {
		return this.predStack.peek();
	}

	public void pushStore(Set<Integer> stores){
		storeStack.push(stores);
	}

	public Set<Integer> popStore(){
		return storeStack.pop();
	}

	public void setInvoke(InvokeItem invoke){
        this.invoke = invoke;
    }

	public InvokeItem getInvoke(){
		return this.invoke;
	}

    public void cleanInvoke(){
        this.invoke = null;
    }

	public String getName(){
		return this.domain.toString();
	}

	public TraceDomain getDomain(){
		return this.domain;
	}

	// private RuntimeFrame(TraceDomain domain) {
	// 	runtimestack = new ArrayDeque<>();
	// 	this.domain = domain;
	// }

	// public String getDomain() {
	// 	return domain.toString() + "#" + entercnt + ":";
	// }

	// public static RuntimeFrame getFrame(TraceDomain domain) {
	// 	String id = domain.toString();
	// 	if (!framemap.containsKey(id)) {
	// 		framemap.put(id, new RuntimeFrame(domain));
	// 	}
	// 	return framemap.get(id);
	// }

	// private static Map<String, RuntimeFrame> framemap = new HashMap<>();
}
