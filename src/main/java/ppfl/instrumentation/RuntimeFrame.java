package ppfl.instrumentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import ppfl.Node;

public class RuntimeFrame {
	public int entercnt;
	public Stack<Node> runtimestack;
	public String traceclass;
	public String tracemethod;

	public RuntimeFrame() {
		entercnt = 0;
		runtimestack = new Stack<Node>();
	}

	private RuntimeFrame(String tclass, String tmethod) {
		entercnt = 0;
		runtimestack = new Stack<Node>();
		traceclass = tclass;
		tracemethod = tmethod;
	}

	public String getDomain() {
		return this.traceclass + ":" + this.tracemethod + "#" + String.valueOf(entercnt) + ":";
	}

	public static RuntimeFrame getFrame(String tclass, String tmethod) {
		String id = tclass + ":" + tmethod;
		if (!framemap.containsKey(id)) {
			framemap.put(id, new RuntimeFrame(tclass, tmethod));
		}
		return framemap.get(id);
	}

	private static Map<String, RuntimeFrame> framemap = new HashMap<String, RuntimeFrame>();
}
